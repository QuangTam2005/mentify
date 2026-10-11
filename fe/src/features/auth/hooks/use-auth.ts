'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { authApi } from '../api/auth-api';
import { useAuthStore } from '../store/auth-store';
import { clearAccessToken, getAccessToken, setAccessToken } from '../store/access-token';
import { ApiError } from '@/core/errors/api-error';
import type { DevLoginRequest } from '../types/auth.types';

export const AUTH_QUERY_KEYS = {
  me: ['auth', 'me'] as const,
};

export function useAuth() {
  const queryClient = useQueryClient();
  const { user, isAuthenticated, isInitialized, setUser, clearUser, setInitialized } = useAuthStore();
  const hasToken = !!getAccessToken();

  const meQuery = useQuery({
    queryKey: AUTH_QUERY_KEYS.me,
    queryFn: async () => {
      try {
        const res = await authApi.getMe();
        return res.data;
      } catch (err) {
        if (err instanceof ApiError && err.isUnauthenticated()) {
          return null;
        }
        throw err;
      }
    },
    enabled: hasToken,
    staleTime: 5 * 60 * 1000,
  });

  // Mark initialized if no token exists without waiting for query
  useEffect(() => {
    if (!hasToken && !isInitialized) {
      setInitialized(true);
    }
  }, [hasToken, isInitialized, setInitialized]);

  // Đồng bộ thông tin người dùng từ query vào Zustand store
  useEffect(() => {
    if (meQuery.isSuccess) {
      setUser(meQuery.data);
    } else if (meQuery.isError) {
      if (meQuery.error instanceof ApiError && meQuery.error.isUnauthenticated()) {
        clearUser();
      }
    }
  }, [meQuery.isSuccess, meQuery.isError, meQuery.error, meQuery.data, setUser, clearUser]);

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
    user: user || meQuery.data || null,
    isAuthenticated: isAuthenticated || !!meQuery.data,
    isLoading: (meQuery.isLoading && !isInitialized) || (!hasToken && !isInitialized),
    isError: meQuery.isError,
    error: meQuery.error,
    isPendingLogin: devLoginMutation.isPending,
    isPendingLogout: logoutMutation.isPending,
    devLogin: devLoginMutation.mutateAsync,
    logout: logoutMutation.mutateAsync,
    refetchMe: meQuery.refetch,
  };
}
