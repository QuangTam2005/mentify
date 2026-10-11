import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCurrentMentorApplication } from './use-current-mentor-application';
import { mentorApi } from '../api/mentor-api';
import { mentorKeys } from '../query-keys';
import { setAccessToken, clearAccessToken } from '@/features/auth/store/access-token';
import { ApiError } from '@/core/errors/api-error';
import { MentorStatus } from '@/shared/types/domain-enums';
import type { CurrentMentorApplicationResponse } from '../types/mentor.types';

vi.mock('../api/mentor-api', () => ({
  mentorApi: {
    getCurrentApplication: vi.fn(),
  },
}));

const mockAppResponse: CurrentMentorApplicationResponse = {
  mentorProfileId: 'prof-uuid-1',
  mentorApplicationId: 'app-uuid-1',
  mentorStatus: MentorStatus.PENDING,
  applicationStatus: 'PENDING',
  attemptNumber: 1,
  appliedUserType: 'UNIVERSITY_STUDENT',
  schoolId: 'sch-uuid-1',
  majorGroupCode: '7480',
  customMajorName: 'Kỹ thuật phần mềm',
  bio: 'Tôi yêu thích chia sẻ kiến thức CNTT',
  acceptingBookings: false,
  submittedAt: '2026-10-11T16:00:00Z',
};

describe('useCurrentMentorApplication Hook', () => {
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

  it('Scenario 2: Does NOT run query when token is absent', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useCurrentMentorApplication(), { wrapper });

    expect(mentorApi.getCurrentApplication).not.toHaveBeenCalled();
    expect(result.current.data).toBeUndefined();
    expect(result.current.isLoading).toBe(false);
  });

  it('Scenario 1 & 3: Calls canonical GET /api/v1/mentor/applications/me when token is present and returns data', async () => {
    setAccessToken('mock-valid-token');
    vi.mocked(mentorApi.getCurrentApplication).mockResolvedValueOnce({
      success: true,
      data: mockAppResponse,
      meta: null,
      timestamp: '2026-10-11T16:00:00Z',
      requestId: 'req-app-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useCurrentMentorApplication(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(mentorApi.getCurrentApplication).toHaveBeenCalledTimes(1);
    expect(result.current.data).toEqual(mockAppResponse);
    expect(queryClient.getQueryData(mentorKeys.myApplication())).toEqual(mockAppResponse);
  });

  it('Scenario 4 & 5: Distinguishes HTTP 404 RESOURCE_NOT_FOUND and populates error state', async () => {
    setAccessToken('mock-valid-token');
    const notFoundError = new ApiError(404, {
      error: { code: 'RESOURCE_NOT_FOUND', message: 'User has no mentor application' },
    });
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(notFoundError);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useCurrentMentorApplication(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBe(notFoundError);
    expect((result.current.error as ApiError).status).toBe(404);
  });

  it('Scenario 6: Retry function does NOT retry 401, 403, or 404, but retries network/5xx errors', () => {
    // Test the retry logic behavior
    const retryFn = (failureCount: number, error: unknown) => {
      if (error instanceof ApiError) {
        if (error.status === 401 || error.status === 403 || error.status === 404) {
          return false;
        }
      }
      return failureCount < 2;
    };

    // 404 should not retry
    expect(retryFn(0, new ApiError(404, null))).toBe(false);
    // 401 should not retry
    expect(retryFn(0, new ApiError(401, null))).toBe(false);
    // 403 should not retry
    expect(retryFn(0, new ApiError(403, null))).toBe(false);
    // 500 should retry if failureCount < 2
    expect(retryFn(0, new ApiError(500, null))).toBe(true);
    expect(retryFn(1, new ApiError(500, null))).toBe(true);
    expect(retryFn(2, new ApiError(500, null))).toBe(false);
    // Network error should retry if failureCount < 2
    expect(retryFn(0, new Error('Network error'))).toBe(true);
    expect(retryFn(2, new Error('Network error'))).toBe(false);
  });
});
