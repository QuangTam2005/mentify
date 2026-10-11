import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient } from '@tanstack/react-query';
import { clearPrivateUserCache, clearPrivateUserCacheSync } from './auth-cache';
import { authKeys } from './query-keys';
import { userKeys } from '@/features/user/query-keys';
import { mentorKeys } from '@/features/mentor/query-keys';
import { masterDataKeys } from '@/features/master-data/query-keys';
import { AUTH_EXPIRED_EVENT } from './auth-events';
import { setAccessToken, clearAccessToken } from './store/access-token';
import { useAuthStore } from './store/auth-store';
import { UserRole, UserType, AccountStatus, MentorStatus } from '@/shared/types/domain-enums';
import type { AuthMeResponse } from './types/auth.types';

const userA: AuthMeResponse = {
  id: 'usr-A',
  email: 'userA@mentify.vn',
  userType: UserType.SCHOOL_STUDENT,
  role: UserRole.USER,
  accountStatus: AccountStatus.ACTIVE,
  mentorStatus: MentorStatus.NOT_APPLIED,
  acceptingBookings: false,
  fullName: 'User A',
  avatarUrl: null,
  token: null,
};

const userB: AuthMeResponse = {
  id: 'usr-B',
  email: 'userB@mentify.vn',
  userType: UserType.UNIVERSITY_STUDENT,
  role: UserRole.USER,
  accountStatus: AccountStatus.ACTIVE,
  mentorStatus: MentorStatus.NOT_APPLIED,
  acceptingBookings: false,
  fullName: 'User B',
  avatarUrl: null,
  token: null,
};

describe('Cross-Session Cache Isolation & Security Suite', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    clearAccessToken();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isInitialized: false,
    });
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Infinity },
      },
    });
  });

  it('Scenario 13 & 14: Logout cleanup removes private auth, user, and mentor queries while preserving public master-data', async () => {
    // 1. Seed private session data
    queryClient.setQueryData(authKeys.me(), userA);
    queryClient.setQueryData(userKeys.profile(), { bio: 'Private bio of user A' });
    queryClient.setQueryData(mentorKeys.myApplication(), { mentorApplicationId: 'app-A' });

    // 2. Seed public master-data
    const publicSchools = [{ id: 'sch-1', name: 'Đại học Quốc Gia' }];
    queryClient.setQueryData(masterDataKeys.schools(), publicSchools);

    // 3. Perform private cache cleanup (as happens on logout)
    await clearPrivateUserCache(queryClient);

    // Private cache must be removed
    expect(queryClient.getQueryData(authKeys.me())).toBeUndefined();
    expect(queryClient.getQueryData(userKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(mentorKeys.myApplication())).toBeUndefined();

    // Public master-data cache must remain untouched
    expect(queryClient.getQueryData(masterDataKeys.schools())).toEqual(publicSchools);
  });

  it('Scenario 15: HTTP 401 expiry removes private session data and prevents stale reuse', async () => {
    queryClient.setQueryData(authKeys.me(), userA);
    queryClient.setQueryData(userKeys.profile(), { bio: 'Secret user notes' });
    queryClient.setQueryData(mentorKeys.myApplication(), { mentorApplicationId: 'app-secret' });
    queryClient.setQueryData(masterDataKeys.majorGroups(), [{ id: 'mg-1', name: 'CNTT' }]);

    // Execute synchronous or asynchronous cleanup triggered by auth-expiry
    clearPrivateUserCacheSync(queryClient);

    expect(queryClient.getQueryData(authKeys.me())).toBeUndefined();
    expect(queryClient.getQueryData(userKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(mentorKeys.myApplication())).toBeUndefined();
    // Master data retained
    expect(queryClient.getQueryData(masterDataKeys.majorGroups())).toBeDefined();
  });

  it('Scenario 16: Switching from account A to account B does not expose cached data from A', async () => {
    // Session A active
    setAccessToken('token-user-A');
    useAuthStore.getState().setUser(userA);
    queryClient.setQueryData(authKeys.me(), userA);
    queryClient.setQueryData(userKeys.profile(), { fullName: 'User A', email: 'userA@mentify.vn' });
    queryClient.setQueryData(mentorKeys.myApplication(), { mentorApplicationId: 'app-user-A' });

    // Switch to Session B: clear private cache first
    await clearPrivateUserCache(queryClient);
    setAccessToken('token-user-B');
    useAuthStore.getState().setUser(userB);
    queryClient.setQueryData(authKeys.me(), userB);

    // Verify User B cannot observe User A's cached profile, me, or mentor application data
    expect(queryClient.getQueryData(userKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(mentorKeys.myApplication())).toBeUndefined();
    expect(queryClient.getQueryData(authKeys.me())).toEqual(userB);
    expect(queryClient.getQueryData(authKeys.me())).not.toEqual(userA);
  });

  it('Scenario 17: A late in-flight response from session A cannot restore As private data into session B', async () => {
    let resolveUserAQuery!: (val: unknown) => void;
    const pendingQueryPromise = new Promise((resolve) => {
      resolveUserAQuery = resolve;
    });

    // Start in-flight query for Session A
    const queryPromise = queryClient.fetchQuery({
      queryKey: authKeys.me(),
      queryFn: () => pendingQueryPromise,
    });

    // Before query resolves, Session A ends and cache is cleaned up
    await clearPrivateUserCache(queryClient);

    // Session B sets its authenticated data
    queryClient.setQueryData(authKeys.me(), userB);

    // Now Session A's late promise resolves
    resolveUserAQuery(userA);
    try {
      await queryPromise;
    } catch {
      // Cancellation might reject or settle
    }

    // Session B's data must NEVER be overwritten by late User A response
    const currentData = queryClient.getQueryData(authKeys.me());
    expect(currentData).toEqual(userB);
    expect(currentData).not.toEqual(userA);
  });

  it('Scenario 18: Cache cleanup does not dispatch duplicate auth-expiry events', async () => {
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

    await clearPrivateUserCache(queryClient);
    clearPrivateUserCacheSync(queryClient);

    const expiredEventCalls = dispatchSpy.mock.calls.filter(
      ([event]) => event.type === AUTH_EXPIRED_EVENT
    );
    expect(expiredEventCalls).toHaveLength(0);
  });

  it('Scenario 19: Repeated cleanup is safe and idempotent', async () => {
    queryClient.setQueryData(authKeys.me(), userA);
    queryClient.setQueryData(mentorKeys.myApplication(), { mentorApplicationId: 'app-A' });

    await clearPrivateUserCache(queryClient);
    await clearPrivateUserCache(queryClient);
    clearPrivateUserCacheSync(queryClient);

    expect(queryClient.getQueryData(authKeys.me())).toBeUndefined();
    expect(queryClient.getQueryData(userKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(mentorKeys.myApplication())).toBeUndefined();
  });
});
