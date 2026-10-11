'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mentorApi } from '../api/mentor-api';
import type { SubmitMentorApplicationRequest } from '../types/mentor.types';
import { mentorKeys } from '../query-keys';
import { authKeys } from '@/features/auth/query-keys';
import { userKeys } from '@/features/user/query-keys';

import { ApiError } from '@/core/errors/api-error';

export function useSubmitMentorApplication() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: SubmitMentorApplicationRequest) => {
      const res = await mentorApi.submitApplication(payload);
      return res.data;
    },
    retry: false, // POST submissions must never be automatically retried
    onSuccess: () => {
      // Invalidate auth, profile, and mentor application queries
      void queryClient.invalidateQueries({ queryKey: authKeys.me() });
      void queryClient.invalidateQueries({ queryKey: userKeys.profile() });
      void queryClient.invalidateQueries({ queryKey: mentorKeys.myApplication() });
    },
  });
}

export function useResubmitMentorApplication() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: SubmitMentorApplicationRequest) => {
      const res = await mentorApi.resubmitApplication(payload);
      return res.data;
    },
    retry: false, // Non-idempotent POST submissions must never be automatically retried
    onSuccess: () => {
      // Invalidate auth, profile, and mentor application queries
      void queryClient.invalidateQueries({ queryKey: authKeys.me() });
      void queryClient.invalidateQueries({ queryKey: userKeys.profile() });
      void queryClient.invalidateQueries({ queryKey: mentorKeys.myApplication() });
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError && err.status === 409) {
        // Refresh stale application state on 409 conflict
        void queryClient.invalidateQueries({ queryKey: mentorKeys.myApplication() });
      }
    },
  });
}
