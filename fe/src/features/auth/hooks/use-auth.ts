'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { authApi } from '../api/auth-api';
import { useAuthStore } from '../store/auth-store';
import { clearAccessToken, setAccessToken } from '../store/access-token';
import type { DevLoginRequest } from '../types/auth.types';

export const AUTH_QUERY_KEYS = {
  me: ['auth', 'me'] as const,
};

export function useAuth() {
  const queryClient = useQueryClient();
  const { user, isAuthenticated, isInitialized, setUser, clearUser } = useAuthStore();

  const meQuery = useQuery({
    queryKey: AUTH_QUERY_KEYS.me,
    queryFn: async () => {
      try {
        const res = await authApi.getMe();
        return res.data;
      } catch (err: unknown) {
        // Nếu 401 Unauthenticated, JWT rỗng hoặc hết hạn
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
  });

  // Đồng bộ session từ query vào Zustand store
  useEffect(() => {
    if (meQuery.isSuccess) {
      setUser(meQuery.data);
    } else if (meQuery.isError) {
      clearUser();
    }
  }, [meQuery.isSuccess, meQuery.isError, meQuery.data, setUser, clearUser]);

  const devLoginMutation = useMutation({
    mutationFn: async (payload: DevLoginRequest) => {
      const res = await authApi.devLogin(payload);
      if (!res.data.token) {
        throw new Error('BE không trả về access token');
      }
      setAccessToken(res.data.token.value);
      return res.data;
    },
    onSuccess: (userData) => {
      setUser(userData);
      queryClient.setQueryData(AUTH_QUERY_KEYS.me, userData);
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
  });

  const logoutMutation = useMutation({
    mutationFn: async () => {
      await authApi.logout();
    },
    onSettled: () => {
      clearAccessToken();
      clearUser();
      queryClient.clear();
    },
  });

  return {
    user: user || meQuery.data,
    isAuthenticated: isAuthenticated || !!meQuery.data,
    isLoading: meQuery.isLoading && !isInitialized,
    isPendingLogin: devLoginMutation.isPending,
    isPendingLogout: logoutMutation.isPending,
    devLogin: devLoginMutation.mutateAsync,
    logout: logoutMutation.mutateAsync,
    refetchMe: meQuery.refetch,
  };
}
