import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUserProfile } from './use-user-profile';
import { userApi } from '../api/user-api';
import { userKeys } from '../query-keys';
import { authKeys } from '@/features/auth/query-keys';
import { masterDataKeys } from '@/features/master-data/query-keys';
import { setAccessToken, clearAccessToken } from '@/features/auth/store/access-token';
import { UserRole, UserType, AccountStatus } from '@/shared/types/domain-enums';
import type { UserProfileResponse } from '../types/user.types';

vi.mock('../api/user-api', () => ({
  userApi: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    recordConsent: vi.fn(),
  },
}));

const mockProfile: UserProfileResponse = {
  id: 'usr-1',
  email: 'student@mentify.vn',
  userType: UserType.SCHOOL_STUDENT,
  role: UserRole.USER,
  accountStatus: AccountStatus.ACTIVE,
  fullName: 'Nguyễn Văn A',
  avatarUrl: null,
  bio: 'Học sinh lớp 12',
  school: null,
  majorGroup: null,
  customMajorName: null,
};

describe('useUserProfile Hook - Query Keys & Mutation Conventions', () => {
  let queryClient: QueryClient;

  function createWrapper() {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Infinity },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
  }

  beforeEach(() => {
    vi.clearAllMocks();
    clearAccessToken();
  });

  it('Scenario 9: Does not run profile query without an in-memory token', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useUserProfile(), { wrapper });

    expect(userApi.getProfile).not.toHaveBeenCalled();
    expect(result.current.profile).toBeUndefined();
  });

  it('Scenario 7: Uses userKeys.profile() when token is present', async () => {
    setAccessToken('mock-valid-token');
    vi.mocked(userApi.getProfile).mockResolvedValueOnce({
      success: true,
      data: mockProfile,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-prof-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useUserProfile(), { wrapper });

    await waitFor(() => {
      expect(result.current.profile).toEqual(mockProfile);
    });

    expect(userApi.getProfile).toHaveBeenCalledTimes(1);
    expect(queryClient.getQueryData(userKeys.profile())).toEqual(mockProfile);
  });

  it('Scenario 11 & 12: Profile mutation updates profile cache, invalidates authKeys.me(), and leaves master-data intact', async () => {
    setAccessToken('mock-valid-token');

    const wrapper = createWrapper();

    // Seed master-data and auth cache
    const mockSchoolData = [{ id: 'sch-1', name: 'ĐHQG Hà Nội' }];
    queryClient.setQueryData(masterDataKeys.schools(), mockSchoolData);

    const mockAuthMeData = { id: 'usr-1', email: 'student@mentify.vn', fullName: 'Nguyễn Văn A' };
    queryClient.setQueryData(authKeys.me(), mockAuthMeData);

    const updatedProfile: UserProfileResponse = {
      ...mockProfile,
      fullName: 'Nguyễn Văn An (Updated)',
    };
    vi.mocked(userApi.updateProfile).mockResolvedValueOnce({
      success: true,
      data: updatedProfile,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-prof-update',
    });

    const { result } = renderHook(() => useUserProfile(), { wrapper });

    await act(async () => {
      await result.current.updateProfile({ fullName: 'Nguyễn Văn An (Updated)' });
    });

    // Profile query data immediately reflects updated profile
    expect(queryClient.getQueryData(userKeys.profile())).toEqual(updatedProfile);

    // authKeys.me query was invalidated
    const authQueryState = queryClient.getQueryState(authKeys.me());
    expect(authQueryState?.isInvalidated).toBe(true);

    // Scenario 12: Master data cache was NOT invalidated and remains completely intact
    const masterDataState = queryClient.getQueryState(masterDataKeys.schools());
    expect(masterDataState?.isInvalidated).toBe(false);
    expect(queryClient.getQueryData(masterDataKeys.schools())).toEqual(mockSchoolData);
  });
});
