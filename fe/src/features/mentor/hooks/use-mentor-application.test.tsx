import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useSubmitMentorApplication, useResubmitMentorApplication } from './use-mentor-application';
import { mentorApi } from '../api/mentor-api';
import { mentorKeys } from '../query-keys';
import { authKeys } from '@/features/auth/query-keys';
import { userKeys } from '@/features/user/query-keys';
import type {
  SubmitMentorApplicationRequest,
  MentorApplicationSubmissionResponse,
} from '../types/mentor.types';
import { MentorStatus } from '@/shared/types/domain-enums';
import { ApiError } from '@/core/errors/api-error';

vi.mock('../api/mentor-api', () => ({
  mentorApi: {
    submitApplication: vi.fn(),
    resubmitApplication: vi.fn(),
  },
}));

const mockSubmissionResponse: MentorApplicationSubmissionResponse = {
  mentorProfileId: 'prof-uuid-1',
  mentorApplicationId: 'app-uuid-1',
  userId: 'usr-uuid-1',
  mentorStatus: MentorStatus.PENDING,
  applicationStatus: 'PENDING',
  attemptNumber: 1,
  appliedUserType: 'UNIVERSITY_STUDENT',
  acceptingBookings: false,
  submittedAt: '2026-10-11T14:30:00Z',
};

describe('useSubmitMentorApplication Hook', () => {
  let queryClient: QueryClient;

  function createWrapper() {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
  }

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits application payload to mentorApi and returns data on success', async () => {
    vi.mocked(mentorApi.submitApplication).mockResolvedValueOnce({
      success: true,
      data: mockSubmissionResponse,
      meta: null,
      timestamp: '2026-10-11T14:30:00Z',
      requestId: 'req-test-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useSubmitMentorApplication(), { wrapper });

    const payload: SubmitMentorApplicationRequest = {
      bio: 'Tiểu sử ứng viên mentor',
      expertiseAreas: 'Java, Spring Boot',
      customMajorName: 'Kỹ thuật phần mềm',
    };

    let responseData;
    await act(async () => {
      responseData = await result.current.mutateAsync(payload);
    });

    expect(mentorApi.submitApplication).toHaveBeenCalledTimes(1);
    expect(mentorApi.submitApplication).toHaveBeenCalledWith(payload);
    expect(responseData).toEqual(mockSubmissionResponse);
  });

  it('invalidates authKeys.me(), userKeys.profile(), and mentorKeys.myApplication() on successful submission', async () => {
    vi.mocked(mentorApi.submitApplication).mockResolvedValueOnce({
      success: true,
      data: mockSubmissionResponse,
      meta: null,
      timestamp: '2026-10-11T14:30:00Z',
      requestId: 'req-test-2',
    });

    const wrapper = createWrapper();
    // Pre-populate queries so we can inspect invalidation state
    queryClient.setQueryData(authKeys.me(), { id: 'usr-1', email: 'test@mentify.vn' });
    queryClient.setQueryData(userKeys.profile(), { id: 'usr-1', fullName: 'Tester' });
    queryClient.setQueryData(mentorKeys.myApplication(), null);

    const { result } = renderHook(() => useSubmitMentorApplication(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({
        bio: 'Tiểu sử',
        expertiseAreas: 'Kỹ năng',
      });
    });

    const authQueryState = queryClient.getQueryState(authKeys.me());
    const profileQueryState = queryClient.getQueryState(userKeys.profile());
    const mentorQueryState = queryClient.getQueryState(mentorKeys.myApplication());

    expect(authQueryState?.isInvalidated).toBe(true);
    expect(profileQueryState?.isInvalidated).toBe(true);
    expect(mentorQueryState?.isInvalidated).toBe(true);
  });

  it('does NOT automatically retry failed POST requests (retry: false)', async () => {
    vi.mocked(mentorApi.submitApplication).mockRejectedValue(
      new ApiError(500, {
        error: { code: 'INTERNAL_ERROR', message: 'Lỗi máy chủ nội bộ' },
      })
    );

    const wrapper = createWrapper();
    const { result } = renderHook(() => useSubmitMentorApplication(), { wrapper });

    await act(async () => {
      try {
        await result.current.mutateAsync({
          bio: 'Tiểu sử',
          expertiseAreas: 'Kỹ năng',
        });
      } catch {
        // Expected mutation failure
      }
    });

    // POST must only be invoked exactly once — no automatic retries
    expect(mentorApi.submitApplication).toHaveBeenCalledTimes(1);
  });
});

describe('useResubmitMentorApplication Hook', () => {
  let queryClient: QueryClient;

  function createWrapper() {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
  }

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits resubmission payload to mentorApi.resubmitApplication and returns data', async () => {
    const mockResubmitResponse: MentorApplicationSubmissionResponse = {
      ...mockSubmissionResponse,
      attemptNumber: 2,
    };

    vi.mocked(mentorApi.resubmitApplication).mockResolvedValueOnce({
      success: true,
      data: mockResubmitResponse,
      meta: null,
      timestamp: '2026-10-11T16:00:00Z',
      requestId: 'req-resubmit-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useResubmitMentorApplication(), { wrapper });

    const payload: SubmitMentorApplicationRequest = {
      bio: 'Tiểu sử cập nhật',
      expertiseAreas: 'TypeScript, React',
      schoolId: 'sch-uuid-1',
    };

    let responseData;
    await act(async () => {
      responseData = await result.current.mutateAsync(payload);
    });

    expect(mentorApi.resubmitApplication).toHaveBeenCalledTimes(1);
    expect(mentorApi.resubmitApplication).toHaveBeenCalledWith(payload);
    expect(responseData).toEqual(mockResubmitResponse);
  });

  it('invalidates auth, profile, and myApplication queries on successful resubmission', async () => {
    vi.mocked(mentorApi.resubmitApplication).mockResolvedValueOnce({
      success: true,
      data: { ...mockSubmissionResponse, attemptNumber: 2 },
      meta: null,
      timestamp: '2026-10-11T16:00:00Z',
      requestId: 'req-resubmit-2',
    });

    const wrapper = createWrapper();
    queryClient.setQueryData(authKeys.me(), { id: 'usr-1' });
    queryClient.setQueryData(userKeys.profile(), { id: 'usr-1' });
    queryClient.setQueryData(mentorKeys.myApplication(), { applicationStatus: 'REJECTED' });

    const { result } = renderHook(() => useResubmitMentorApplication(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ bio: 'Tiểu sử mới' });
    });

    expect(queryClient.getQueryState(authKeys.me())?.isInvalidated).toBe(true);
    expect(queryClient.getQueryState(userKeys.profile())?.isInvalidated).toBe(true);
    expect(queryClient.getQueryState(mentorKeys.myApplication())?.isInvalidated).toBe(true);
  });

  it('enforces retry: false policy and does not retry failed POST resubmissions', async () => {
    vi.mocked(mentorApi.resubmitApplication).mockRejectedValue(
      new ApiError(500, {
        error: { code: 'INTERNAL_ERROR', message: 'Internal server error' },
      })
    );

    const wrapper = createWrapper();
    const { result } = renderHook(() => useResubmitMentorApplication(), { wrapper });

    await act(async () => {
      try {
        await result.current.mutateAsync({ bio: 'Tiểu sử' });
      } catch {
        // Expected
      }
    });

    expect(mentorApi.resubmitApplication).toHaveBeenCalledTimes(1);
  });

  it('invalidates mentorKeys.myApplication() on HTTP 409 Conflict to refresh stale status', async () => {
    vi.mocked(mentorApi.resubmitApplication).mockRejectedValueOnce(
      new ApiError(409, {
        error: { code: 'CONFLICT', message: 'Đơn mentor đã được nộp lại bởi một yêu cầu khác' },
      })
    );

    const wrapper = createWrapper();
    queryClient.setQueryData(mentorKeys.myApplication(), { applicationStatus: 'REJECTED' });

    const { result } = renderHook(() => useResubmitMentorApplication(), { wrapper });

    await act(async () => {
      try {
        await result.current.mutateAsync({ bio: 'Tiểu sử' });
      } catch {
        // Expected
      }
    });

    expect(queryClient.getQueryState(mentorKeys.myApplication())?.isInvalidated).toBe(true);
  });
});
