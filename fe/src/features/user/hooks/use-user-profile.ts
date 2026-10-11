'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../api/user-api';
import type { RecordConsentRequest, UpdateProfileRequest } from '../types/user.types';
import { userKeys } from '../query-keys';
import { authKeys } from '@/features/auth/query-keys';
import { getAccessToken } from '@/features/auth/store/access-token';

export const USER_QUERY_KEYS = userKeys;

export function useUserProfile() {
  const queryClient = useQueryClient();
  const hasToken = typeof window !== 'undefined' ? !!getAccessToken() : true;

  const profileQuery = useQuery({
    queryKey: userKeys.profile(),
    queryFn: async () => {
      const res = await userApi.getProfile();
      return res.data;
    },
    enabled: hasToken,
    staleTime: 5 * 60 * 1000,
  });

  const updateProfileMutation = useMutation({
    mutationFn: async (payload: UpdateProfileRequest) => {
      const res = await userApi.updateProfile(payload);
      return res.data;
    },
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(userKeys.profile(), updatedProfile);
      queryClient.invalidateQueries({ queryKey: authKeys.me() });
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
