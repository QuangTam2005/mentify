import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React, { StrictMode } from 'react';
import AuthCallbackPage from './page';
import { authApi } from '@/features/auth/api/auth-api';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { getAccessToken, clearAccessToken } from '@/features/auth/store/access-token';
import {
  rememberReturnUrl,
  clearReturnUrl,
  isSafeReturnUrl,
  consumeReturnUrl,
} from '@/features/auth/auth-navigation';
import { resetExchangeCoordinator } from '@/features/auth/oauth-callback';
import { ApiError } from '@/core/errors/api-error';
import type { AuthMeResponse } from '@/features/auth/types/auth.types';
import { UserRole, UserType, AccountStatus, MentorStatus } from '@/shared/types/domain-enums';

const mockReplace = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
    push: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
}));

vi.mock('@/features/auth/api/auth-api', () => ({
  authApi: {
    exchangeOAuth2Code: vi.fn(),
    getMe: vi.fn(),
    devLogin: vi.fn(),
    logout: vi.fn(),
  },
}));

const mockUser: AuthMeResponse = {
  id: 'user-google-1',
  email: 'google-user@mentify.vn',
  userType: UserType.UNIVERSITY_STUDENT,
  role: UserRole.USER,
  accountStatus: AccountStatus.ACTIVE,
  mentorStatus: MentorStatus.NOT_APPLIED,
  acceptingBookings: false,
  fullName: 'Google User',
  avatarUrl: 'https://example.com/avatar.png',
  token: {
    value: 'jwt-token-google-123',
    tokenType: 'Bearer',
    expiresAt: '2026-10-11T20:00:00Z',
  },
};

describe('OAuth Callback Reliability & Safety Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearAccessToken();
    clearReturnUrl();
    resetExchangeCoordinator();
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isInitialized: false,
    });
    // Default search
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        search: '?code=valid-one-time-code',
        pathname: '/auth/callback',
      },
    });
    // Default replaceState
    window.history.replaceState = vi.fn();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // Scenario 1 — Successful Exchange
  it('Scenario 1: Successful Exchange - calls API, stores in-memory token, synchronizes auth user, and safely redirects', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValueOnce({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-oauth-1',
    });

    rememberReturnUrl('/explore');

    render(<AuthCallbackPage />);

    expect(screen.getByText('Đang hoàn tất đăng nhập...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập thành công!')).toBeInTheDocument();
    });

    expect(authApi.exchangeOAuth2Code).toHaveBeenCalledWith({ code: 'valid-one-time-code' });
    expect(authApi.exchangeOAuth2Code).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBe('jwt-token-google-123');
    expect(useAuthStore.getState().user).toEqual(mockUser);
    expect(useAuthStore.getState().isAuthenticated).toBe(true);

    // URL code cleaned up
    expect(window.history.replaceState).toHaveBeenCalledWith({}, document.title, '/auth/callback');

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/explore');
    });
  });

  // Scenario 2 — Missing Code
  it('Scenario 2: Missing Code - does not call exchange, renders error UI, and offers login link', async () => {
    window.location.search = '';

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    expect(authApi.exchangeOAuth2Code).not.toHaveBeenCalled();
    expect(screen.getByText(/Thiếu mã đăng nhập OAuth2/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Quay lại trang Đăng nhập/i })).toHaveAttribute('href', '/login');
    expect(getAccessToken()).toBeNull();
  });

  // Scenario 3 — Empty or Invalid Code
  it('Scenario 3: Empty or Whitespace Code - rejects empty code and does not call API', async () => {
    window.location.search = '?code=   ';

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    expect(authApi.exchangeOAuth2Code).not.toHaveBeenCalled();
    expect(getAccessToken()).toBeNull();
  });

  // Scenario 4 — OAuth Error Parameter
  it('Scenario 4: OAuth Error Parameter - explicit provider error takes precedence over code', async () => {
    window.location.search = '?error=access_denied&code=ignored-code';

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    expect(authApi.exchangeOAuth2Code).not.toHaveBeenCalled();
    expect(screen.getByText('Bạn đã hủy yêu cầu đăng nhập bằng tài khoản Google.')).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });

  // Scenario 5 — Backend Exchange Failure
  it('Scenario 5: Backend Exchange Failure - renders error state, no authenticated redirect, no token stored', async () => {
    const error400 = new ApiError(400, {
      error: { code: 'VALIDATION_ERROR', message: 'Mã OAuth đã hết hạn hoặc đã được sử dụng.' },
    });
    vi.mocked(authApi.exchangeOAuth2Code).mockRejectedValueOnce(error400);

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    expect(screen.getByText('Mã OAuth đã hết hạn hoặc đã được sử dụng.')).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(mockReplace).not.toHaveBeenCalled();
  });

  // Scenario 6 — Network Failure During Exchange
  it('Scenario 6: Network Failure During Exchange - does not retry one-time code and lets user restart login', async () => {
    const networkError = new TypeError('Failed to fetch');
    vi.mocked(authApi.exchangeOAuth2Code).mockRejectedValueOnce(networkError);

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    expect(screen.getByText('Failed to fetch')).toBeInTheDocument();
    // Must NOT automatically retry one-time code
    expect(authApi.exchangeOAuth2Code).toHaveBeenCalledTimes(1);
    expect(mockReplace).not.toHaveBeenCalled();
  });

  // Scenario 7 — Normal Re-render
  it('Scenario 7: Normal Re-render - component re-render does not issue duplicate exchange requests', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValue({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-oauth-2',
    });

    const { rerender } = render(<AuthCallbackPage />);

    rerender(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập thành công!')).toBeInTheDocument();
    });

    expect(authApi.exchangeOAuth2Code).toHaveBeenCalledTimes(1);
  });

  // Scenario 8 — React Strict Mode Lifecycle
  it('Scenario 8: React Strict Mode - prevents duplicate exchange requests under StrictMode double-mounting', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValue({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-strict-1',
    });

    render(
      <StrictMode>
        <AuthCallbackPage />
      </StrictMode>
    );

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập thành công!')).toBeInTheDocument();
    });

    // In StrictMode with in-flight coordinator, exactly 1 exchange API call occurs
    expect(authApi.exchangeOAuth2Code).toHaveBeenCalledTimes(1);
  });

  // Scenario 9 — Safe Return Destination
  it('Scenario 9: Safe Return Destination - internal destination is used and consumed once', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValueOnce({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-1',
    });

    rememberReturnUrl('/profile?tab=security');

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/profile?tab=security');
    });

    // Check consumed
    expect(consumeReturnUrl('/default')).toBe('/default');
  });

  // Scenario 10 — Unsafe Return Destination
  it('Scenario 10: Unsafe Return Destination - rejects external or backslash destinations and falls back to /profile', () => {
    expect(isSafeReturnUrl('https://malicious.com')).toBe(false);
    expect(isSafeReturnUrl('//malicious.com')).toBe(false);
    expect(isSafeReturnUrl('\\malicious.com')).toBe(false);
    expect(isSafeReturnUrl('/\\malicious.com')).toBe(false);
    expect(isSafeReturnUrl('javascript:alert(1)')).toBe(false);

    // Consume with unsafe stored value
    window.sessionStorage.setItem('mentify:auth-return-url', 'https://malicious.com');
    expect(consumeReturnUrl('/profile')).toBe('/profile');
  });

  // Scenario 11 — Missing Return Destination
  it('Scenario 11: Missing Return Destination - defaults to /profile', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValueOnce({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-1',
    });

    clearReturnUrl();

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/profile');
    });
  });

  // Scenario 12 — Cleanup After Completion
  it('Scenario 12: Cleanup After Failure - clears return destination on callback failure', async () => {
    rememberReturnUrl('/secret-target');
    window.location.search = '?error=access_denied';

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    // Failure must clear any stored return URL so later logins are not poisoned
    expect(consumeReturnUrl('/profile')).toBe('/profile');
  });

  // Scenario 13 — Malformed Exchange Response
  it('Scenario 13: Malformed Exchange Response - rejects response without token or user ID and shows error', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValueOnce({
      success: true,
      data: { ...mockUser, token: null }, // Malformed!
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-bad',
    });

    render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập không thành công')).toBeInTheDocument();
    });

    expect(screen.getByText('Dữ liệu xác thực trả về từ máy chủ không hợp lệ.')).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(mockReplace).not.toHaveBeenCalled();
  });

  // Scenario 14 — Duplicate Completion
  it('Scenario 14: Duplicate Completion - does not trigger repeated redirects if already redirected', async () => {
    vi.mocked(authApi.exchangeOAuth2Code).mockResolvedValue({
      success: true,
      data: mockUser,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-1',
    });

    const { rerender } = render(<AuthCallbackPage />);

    await waitFor(() => {
      expect(screen.getByText('Đăng nhập thành công!')).toBeInTheDocument();
    });

    rerender(<AuthCallbackPage />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledTimes(1);
    });
  });
});
