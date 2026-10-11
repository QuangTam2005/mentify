'use client';

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { mentorApi } from '../api/mentor-api';
import { mentorKeys } from '../query-keys';
import { getAccessToken } from '@/features/auth/store/access-token';
import { ApiError } from '@/core/errors/api-error';

export interface UseCurrentMentorApplicationOptions {
  retry?: boolean | ((failureCount: number, error: unknown) => boolean);
}

export function useCurrentMentorApplication(options?: UseCurrentMentorApplicationOptions) {
  const queryClient = useQueryClient();
  const hasToken = typeof window !== 'undefined' ? !!getAccessToken() : true;
  const defaultRetry = queryClient.getDefaultOptions().queries?.retry;

  return useQuery({
    queryKey: mentorKeys.myApplication(),
    queryFn: async () => {
      const res = await mentorApi.getCurrentApplication();
      return res.data;
    },
    enabled: hasToken,
    staleTime: 5 * 60 * 1000,
    retry:
      options?.retry !== undefined
        ? options.retry
        : defaultRetry === false
        ? false
        : (failureCount, error) => {
            // Do not automatically retry 401, 403, or 404
            if (error instanceof ApiError) {
              if (error.status === 401 || error.status === 403 || error.status === 404) {
                return false;
              }
            }
            return failureCount < 2;
          },
  });
}
