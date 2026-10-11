import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from './use-auth';
import { authApi } from '../api/auth-api';
import { useAuthStore } from '../store/auth-store';
import { getAccessToken, setAccessToken, clearAccessToken } from '../store/access-token';
import { ApiError } from '@/core/errors/api-error';
import type { AuthMeResponse } from '../types/auth.types';
import { UserRole, UserType, AccountStatus, MentorStatus } from '@/shared/types/domain-enums';

vi.mock('../api/auth-api', () => ({
  authApi: {
    getMe: vi.fn(),
    devLogin: vi.fn(),
    logout: vi.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

const mockUser: AuthMeResponse = {
  id: 'user-123',
  email: 'test@mentify.vn',
  userType: UserType.UNIVERSITY_STUDENT,
  role: UserRole.USER,
  accountStatus: AccountStatus.ACTIVE,
  mentorStatus: MentorStatus.NOT_APPLIED,
  acceptingBookings: false,
  fullName: 'Test User',
  avatarUrl: null,
  token: null,
};

describe('useAuth Hook - Error Handling & Authentication Classification', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearAccessToken();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isInitialized: false,
    });
  });

  it('Scenario 1: No token - does not call /auth/me and preserves unauthenticated state', async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(authApi.getMe).not.toHaveBeenCalled();
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.isError).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('Scenario 2: Successful authentication - returns authenticated user', async () => {
    setAccessToken('valid-jwt-token');
    vi.mocked(authApi.getMe).mockResolvedValueOnce({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
    });

    expect(authApi.getMe).toHaveBeenCalledTimes(1);
    expect(result.current.user).toEqual(mockUser);
    expect(result.current.isError).toBe(false);
    expect(useAuthStore.getState().user).toEqual(mockUser);
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
  });

  it('Scenario 3: HTTP 401 - handles authentication failure without crashing or retrying', async () => {
    setAccessToken('expired-jwt-token');
    const error401 = new ApiError(401, {
      error: { code: 'UNAUTHENTICATED', message: 'Token hết hạn' },
    });
    vi.mocked(authApi.getMe).mockRejectedValueOnce(error401);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(authApi.getMe).toHaveBeenCalledTimes(1);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.isError).toBe(false); // 401 converted to unauthenticated result
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
  });

  it('Scenario 4: HTTP 403 - does not clear token, does not log out, propagates forbidden error', async () => {
    setAccessToken('valid-jwt-token');
    const error403 = new ApiError(403, {
      error: { code: 'FORBIDDEN', message: 'Không có quyền truy cập' },
    });
    vi.mocked(authApi.getMe).mockRejectedValueOnce(error403);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    // Token must be preserved
    expect(getAccessToken()).toBe('valid-jwt-token');
    // Error must be preserved and visible
    expect(result.current.error).toBe(error403);
    expect(result.current.error).toBeInstanceOf(ApiError);
    expect((result.current.error as ApiError).isForbidden()).toBe(true);
  });

  it('Scenario 5: Network failure - preserves token and exposes network failure', async () => {
    setAccessToken('valid-jwt-token');
    const networkError = new TypeError('Failed to fetch');
    vi.mocked(authApi.getMe).mockRejectedValueOnce(networkError);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    // Token must be preserved
    expect(getAccessToken()).toBe('valid-jwt-token');
    // Error propagated to TanStack Query consumer
    expect(result.current.error).toBe(networkError);
  });

  it('Scenario 6: HTTP 500 - does not log out and preserves server error', async () => {
    setAccessToken('valid-jwt-token');
    const error500 = new ApiError(500, {
      error: { code: 'INTERNAL_ERROR', message: 'Lỗi máy chủ' },
    });
    vi.mocked(authApi.getMe).mockRejectedValueOnce(error500);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(getAccessToken()).toBe('valid-jwt-token');
    expect(result.current.error).toBe(error500);
    expect((result.current.error as ApiError).status).toBe(500);
  });

  it('Scenario 7: Retry policy - disables retry on 401 but allows retry config for transient failures', async () => {
    setAccessToken('expired-token');
    const error401 = new ApiError(401, {
      error: { code: 'UNAUTHENTICATED', message: 'Unauthorized' },
    });
    vi.mocked(authApi.getMe).mockRejectedValue(error401);

    // Create wrapper with retry enabled to verify useAuth's own retry function disables 401 retries
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: true,
        },
      },
    });

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // Despite global queries.retry: true, 401 should NEVER be retried
    expect(authApi.getMe).toHaveBeenCalledTimes(1);
  });

  it('Scenario 8: Cached user data retention - does not deliberately clear user on transient server error', async () => {
    setAccessToken('valid-jwt-token');
    // Seed initial user into store
    useAuthStore.setState({
      user: mockUser,
      isAuthenticated: true,
      isInitialized: true,
    });

    const error500 = new ApiError(500, {
      error: { code: 'INTERNAL_ERROR', message: 'Lỗi tạm thời' },
    });
    vi.mocked(authApi.getMe).mockRejectedValueOnce(error500);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    // Cached user in state remains intact despite transient 500
    expect(result.current.user).toEqual(mockUser);
    expect(result.current.isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user).toEqual(mockUser);
  });
});
