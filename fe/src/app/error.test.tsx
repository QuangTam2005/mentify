import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import AppError from './error';
import * as tokenStore from '@/features/auth/store/access-token';
import * as authEvents from '@/features/auth/auth-events';

describe('App Router Error Boundary (error.tsx)', () => {
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    vi.restoreAllMocks();
  });

  it('renders a safe generic error state', () => {
    const mockError = new Error('Database connection failed') as Error & { digest?: string };
    const mockReset = vi.fn();

    render(<AppError error={mockError} reset={mockReset} />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Đã xảy ra sự cố')).toBeInTheDocument();
    expect(
      screen.getByText(/đã có lỗi không mong muốn xảy ra trong quá trình xử lý yêu cầu/i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /thử lại/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /về trang chủ/i })).toBeInTheDocument();
  });

  it('calls reset() when Retry is activated', async () => {
    const user = userEvent.setup();
    const mockError = new Error('Network timeout') as Error & { digest?: string };
    const mockReset = vi.fn();

    render(<AppError error={mockError} reset={mockReset} />);

    const retryBtn = screen.getByRole('button', { name: /thử lại/i });
    await user.click(retryBtn);

    expect(mockReset).toHaveBeenCalledTimes(1);
  });

  it('does not expose raw exception messages, stack traces, or digests', () => {
    const sensitiveError = new Error('FATAL_POSTGRES_EXCHANGE_CODE_SECRET_XYZ') as Error & {
      digest?: string;
    };
    sensitiveError.stack = 'Error at DatabaseEngine.execute (file:///secret/db.ts:99:1)';
    sensitiveError.digest = 'SECRET_DIGEST_HASH_778899';

    const mockReset = vi.fn();
    render(<AppError error={sensitiveError} reset={mockReset} />);

    expect(
      screen.queryByText(/FATAL_POSTGRES_EXCHANGE_CODE_SECRET_XYZ/i)
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/DatabaseEngine/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/SECRET_DIGEST_HASH_778899/i)).not.toBeInTheDocument();
  });

  it('does not trigger logout, token clearing, or auth-expiry events', () => {
    const clearTokenSpy = vi.spyOn(tokenStore, 'clearAccessToken');
    const notifyAuthExpiredSpy = vi.spyOn(authEvents, 'notifyAuthExpired');

    const mockError = new Error('Unexpected rendering exception') as Error & { digest?: string };
    const mockReset = vi.fn();

    render(<AppError error={mockError} reset={mockReset} />);

    expect(clearTokenSpy).not.toHaveBeenCalled();
    expect(notifyAuthExpiredSpy).not.toHaveBeenCalled();
  });
});
