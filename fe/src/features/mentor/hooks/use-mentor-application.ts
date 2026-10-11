'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mentorApi } from '../api/mentor-api';
import type { SubmitMentorApplicationRequest } from '../types/mentor.types';
import { authKeys } from '@/features/auth/query-keys';
import { userKeys } from '@/features/user/query-keys';

export function useSubmitMentorApplication() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: SubmitMentorApplicationRequest) => {
      const res = await mentorApi.submitApplication(payload);
      return res.data;
    },
    retry: false, // POST submissions must never be automatically retried
    onSuccess: () => {
      // Invalidate auth and profile queries so mentorStatus reflects the new PENDING state
      void queryClient.invalidateQueries({ queryKey: authKeys.me() });
      void queryClient.invalidateQueries({ queryKey: userKeys.profile() });
    },
  });
}
