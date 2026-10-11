import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { ProtectedRoute } from './protected-route';
import { useAuth } from '../hooks/use-auth';
import { getAccessToken, setAccessToken, clearAccessToken } from '../store/access-token';
import { ApiError } from '@/core/errors/api-error';
import { isSafeReturnUrl } from '../auth-navigation';
import type { AuthMeResponse } from '../types/auth.types';
import { UserRole, UserType, AccountStatus, MentorStatus } from '@/shared/types/domain-enums';

const mockPush = vi.fn();
const mockReplace = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
}));

vi.mock('../hooks/use-auth', () => ({
  useAuth: vi.fn(),
}));

const mockUser: AuthMeResponse = {
  id: 'user-1',
  email: 'test@mentify.vn',
  userType: UserType.UNIVERSITY_STUDENT,
  role: UserRole.USER,
  accountStatus: AccountStatus.ACTIVE,
  mentorStatus: MentorStatus.NOT_APPLIED,
  acceptingBookings: false,
  fullName: 'User One',
  avatarUrl: null,
  token: null,
};

describe('ProtectedRoute Guard Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearAccessToken();
    window.sessionStorage.clear();
    // Default window.location
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        pathname: '/profile',
        search: '',
      },
    });
  });

  // Scenario 1 — Authenticated User
  it('Scenario 1: Authenticated User - renders protected content without login redirect', () => {
    setAccessToken('valid-jwt-token');
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
      isAuthenticated: true,
      isLoading: false,
      isError: false,
      error: null,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  // Scenario 2 — Confirmed Guest
  it('Scenario 2: Confirmed Guest - hides protected content and navigates safely to login', async () => {
    clearAccessToken();
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isError: false,
      error: null,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Đang chuyển hướng đến trang đăng nhập...')).toBeInTheDocument();

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/login?returnUrl=%2Fprofile');
    });
  });

  // Scenario 3 — Authentication Loading
  it('Scenario 3: Authentication Loading - hides protected content and shows loading spinner with no premature redirect', () => {
    setAccessToken('valid-jwt-token');
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isError: false,
      error: null,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Đang xác thực thông tin tài khoản...')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  // Scenario 4 — HTTP 403 Forbidden
  it('Scenario 4: HTTP 403 - displays Access Denied, hides content, does not log out or redirect to login', () => {
    setAccessToken('valid-jwt-token');
    const forbiddenError = new ApiError(403, {
      error: { code: 'FORBIDDEN', message: 'Tài khoản chưa được phê duyệt quyền truy cập.' },
    });
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
      isAuthenticated: true,
      isLoading: false,
      isError: true,
      error: forbiddenError,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Truy cập bị từ chối (403)')).toBeInTheDocument();
    expect(
      screen.getByText('Tài khoản chưa được phê duyệt quyền truy cập.')
    ).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
    expect(getAccessToken()).toBe('valid-jwt-token');
  });

  // Scenario 5 — Network Failure
  it('Scenario 5: Network Failure - displays recoverable error with retry button and does not redirect to login', () => {
    setAccessToken('valid-jwt-token');
    const mockRefetch = vi.fn();
    const networkError = new TypeError('Failed to fetch');
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isError: true,
      error: networkError,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: mockRefetch,
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Lỗi kết nối hoặc hệ thống')).toBeInTheDocument();
    expect(screen.getByText('Failed to fetch')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();

    // Trigger retry
    screen.getByRole('button', { name: /Thử lại/i }).click();
    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });

  // Scenario 6 — HTTP 500
  it('Scenario 6: HTTP 500 - displays safe server-error state and preserves token without guest redirect', () => {
    setAccessToken('valid-jwt-token');
    const error500 = new ApiError(500, {
      error: { code: 'INTERNAL_ERROR', message: 'Lỗi máy chủ nội bộ' },
    });
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isError: true,
      error: error500,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Lỗi kết nối hoặc hệ thống')).toBeInTheDocument();
    expect(screen.getByText('Lỗi máy chủ nội bộ')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
    expect(getAccessToken()).toBe('valid-jwt-token');
  });

  // Scenario 7 — Existing Cached User Plus Blocking 403
  it('Scenario 7: Existing Cached User Plus Blocking 403 - does not unlock content with cached user when 403 occurs', () => {
    setAccessToken('valid-jwt-token');
    const forbiddenError = new ApiError(403, {
      error: { code: 'FORBIDDEN', message: 'Bị cấm truy cập' },
    });
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser, // cached user
      isAuthenticated: true,
      isLoading: false,
      isError: true,
      error: forbiddenError,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    // Guard must NOT render protected content despite cached user
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Truy cập bị từ chối (403)')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  // Scenario 8 — Missing Token Plus Stale User
  it('Scenario 8: Missing Token Plus Stale User - cannot unlock protected content solely from stale user object', async () => {
    clearAccessToken(); // Token is missing
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser, // stale user in store
      isAuthenticated: false,
      isLoading: false,
      isError: false,
      error: null,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/login?returnUrl=%2Fprofile');
    });
  });

  // Scenario 9 — Navigation Safety
  it('Scenario 9: Navigation Safety - verifies safe return destinations and rejects open redirects', () => {
    expect(isSafeReturnUrl('/profile')).toBe(true);
    expect(isSafeReturnUrl('/admin/dashboard')).toBe(true);
    expect(isSafeReturnUrl('/explore?tab=majors')).toBe(true);

    // Rejection of malicious URLs
    expect(isSafeReturnUrl('https://malicious.example')).toBe(false);
    expect(isSafeReturnUrl('http://malicious.example')).toBe(false);
    expect(isSafeReturnUrl('//malicious.example')).toBe(false);
    expect(isSafeReturnUrl('javascript:alert(1)')).toBe(false);
    expect(isSafeReturnUrl(null)).toBe(false);
    expect(isSafeReturnUrl(undefined)).toBe(false);
    expect(isSafeReturnUrl('')).toBe(false);
  });

  // Scenario 10 — No Redirect Loops
  it('Scenario 10: No Redirect Loops - does not trigger repeated redirects upon re-render', () => {
    clearAccessToken();
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isError: false,
      error: null,
      isPendingLogin: false,
      isPendingLogout: false,
      devLogin: vi.fn(),
      logout: vi.fn(),
      refetchMe: vi.fn(),
    });

    const { rerender } = render(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    expect(mockReplace).toHaveBeenCalledTimes(1);

    // Re-render component in same state
    rerender(
      <ProtectedRoute>
        <div data-testid="protected-content">Secret Profile Content</div>
      </ProtectedRoute>
    );

    // Ref redirecting prevents redundant replacements
    expect(mockReplace).toHaveBeenCalledTimes(1);
  });
});
