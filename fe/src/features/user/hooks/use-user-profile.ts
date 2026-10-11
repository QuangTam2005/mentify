'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/user-api';
import type { RecordConsentRequest, UpdateProfileRequest } from '../types/user.types';
import { AUTH_QUERY_KEYS } from '@/features/auth/hooks/use-auth';

export const USER_QUERY_KEYS = {
  profile: ['user', 'profile'] as const,
};

export function useUserProfile() {
  const queryClient = useQueryClient();

  const profileQuery = useQuery({
    queryKey: USER_QUERY_KEYS.profile,
    queryFn: async () => {
      const res = await userApi.getProfile();
      return res.data;
    },
    staleTime: 5 * 60 * 1000,
  });

  const updateProfileMutation = useMutation({
    mutationFn: async (payload: UpdateProfileRequest) => {
      const res = await userApi.updateProfile(payload);
      return res.data;
    },
    onSuccess: (updatedProfile) => {
      // Invalidate và cập nhật cache cả user profile lẫn auth/me
      queryClient.setQueryData(USER_QUERY_KEYS.profile, updatedProfile);
      queryClient.invalidateQueries({ queryKey: AUTH_QUERY_KEYS.me });
    },
  });

  const consentMutation = useMutation({
    mutationFn: async (payload: RecordConsentRequest) => {
      const res = await userApi.recordConsent(payload);
      return res.data;
    },
  });

  return {
    profile: profileQuery.data,
    isLoading: profileQuery.isLoading,
    isUpdating: updateProfileMutation.isPending,
    isSubmittingConsent: consentMutation.isPending,
    updateProfile: updateProfileMutation.mutateAsync,
    recordConsent: consentMutation.mutateAsync,
    refetchProfile: profileQuery.refetch,
  };
}
