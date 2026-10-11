import type { QueryClient } from '@tanstack/react-query';
import { authKeys } from './query-keys';
import { userKeys } from '@/features/user/query-keys';
import { mentorKeys } from '@/features/mentor/query-keys';

/**
 * Cancels and removes all private user/auth/mentor queries from TanStack Query cache.
 * Preserves public master-data and reference caches.
 * Idempotent and safe to invoke at any authentication boundary.
 */
export async function clearPrivateUserCache(queryClient: QueryClient): Promise<void> {
  const cancelPromise = Promise.all([
    queryClient.cancelQueries({ queryKey: authKeys.all }),
    queryClient.cancelQueries({ queryKey: userKeys.all }),
    queryClient.cancelQueries({ queryKey: mentorKeys.all }),
  ]);

  queryClient.removeQueries({ queryKey: authKeys.all });
  queryClient.removeQueries({ queryKey: userKeys.all });
  queryClient.removeQueries({ queryKey: mentorKeys.all });

  await cancelPromise;
}

export function clearPrivateUserCacheSync(queryClient: QueryClient): void {
  void queryClient.cancelQueries({ queryKey: authKeys.all });
  void queryClient.cancelQueries({ queryKey: userKeys.all });
  void queryClient.cancelQueries({ queryKey: mentorKeys.all });
  queryClient.removeQueries({ queryKey: authKeys.all });
  queryClient.removeQueries({ queryKey: userKeys.all });
  queryClient.removeQueries({ queryKey: mentorKeys.all });
}
