import { authApi } from '@/features/auth/api/auth-api';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { setAccessToken } from '@/features/auth/store/access-token';
import { consumeReturnUrl, clearReturnUrl } from '@/features/auth/auth-navigation';
import { ApiError } from '@/core/errors/api-error';
import type { AuthMeResponse } from '@/features/auth/types/auth.types';

export type CallbackState =
  | { status: 'processing' }
  | { status: 'success'; destination: string }
  | { status: 'error'; message: string };

// In-flight coordination map to prevent concurrent or StrictMode duplicate exchanges for the same code
const inFlightExchanges = new Map<string, Promise<AuthMeResponse>>();
const completedExchanges = new Map<string, { authData: AuthMeResponse; destination: string }>();
const failedExchanges = new Map<string, string>();

/**
 * Parses query params from URL search string or searchParams
 */
export function parseCallbackParams(search: string): {
  code: string | null;
  error: string | null;
  errorDescription: string | null;
} {
  const params = new URLSearchParams(search);
  const error = params.get('error')?.trim() || null;
  const errorDescription = params.get('error_description')?.trim() || null;
  const code = params.get('code')?.trim() || null;

  return { code, error, errorDescription };
}

/**
 * Resets the in-flight exchange and consumed code sets (for tests or memory bounds)
 */
export function resetExchangeCoordinator(): void {
  inFlightExchanges.clear();
  completedExchanges.clear();
  failedExchanges.clear();
}

/**
 * Coordinates exchange execution with single-flight and idempotency guarantees
 */
export async function processOAuthCallback(search: string): Promise<CallbackState> {
  const { code, error, errorDescription } = parseCallbackParams(search);

  // 1. OAuth Provider Error takes precedence
  if (error) {
    clearReturnUrl();
    const message =
      error === 'access_denied'
        ? 'Bạn đã hủy yêu cầu đăng nhập bằng tài khoản Google.'
        : errorDescription || 'Máy chủ xác thực Google từ chối yêu cầu đăng nhập.';
    return { status: 'error', message };
  }

  // 2. Missing or empty code
  if (!code) {
    clearReturnUrl();
    return {
      status: 'error',
      message: 'Thiếu mã đăng nhập OAuth2 hoặc liên kết không hợp lệ.',
    };
  }

  // 3. Check if already failed in this session (do not retry one-time code)
  const previousFailure = failedExchanges.get(code);
  if (previousFailure) {
    return {
      status: 'error',
      message: previousFailure,
    };
  }

  // 4. Check if already completed in this session
  const alreadyCompleted = completedExchanges.get(code);
  if (alreadyCompleted) {
    return {
      status: 'success',
      destination: alreadyCompleted.destination,
    };
  }

  // 4. Coordinate in-flight promise to avoid duplicate concurrent calls (e.g. Strict Mode)
  let exchangePromise = inFlightExchanges.get(code);

  if (!exchangePromise) {
    exchangePromise = (async () => {
      try {
        const exchangeRes = await authApi.exchangeOAuth2Code({ code });
        const data = exchangeRes?.data;
        if (!data || !data.token?.value || !data.id || !data.email) {
          throw new Error('Dữ liệu xác thực trả về từ máy chủ không hợp lệ.');
        }
        return data;
      } finally {
        inFlightExchanges.delete(code);
      }
    })();

    inFlightExchanges.set(code, exchangePromise);
  }

  try {
    const authData = await exchangePromise;

    // Commit authentication state
    setAccessToken(authData.token!.value);
    useAuthStore.getState().setUser(authData);

    // Clean up code from browser history if window is available
    if (typeof window !== 'undefined' && window.history && window.history.replaceState) {
      window.history.replaceState({}, document.title, '/auth/callback');
    }

    // Resolve safe return destination once
    const destination = consumeReturnUrl('/profile');

    // Record completed exchange
    completedExchanges.set(code, { authData, destination });

    return {
      status: 'success',
      destination,
    };
  } catch (err: unknown) {
    // If exchange failed, clean up return destination so it doesn't affect later unrelated login
    clearReturnUrl();

    let message = 'Xảy ra lỗi trong quá trình hoàn tất xác thực Google.';
    if (err instanceof ApiError) {
      message = err.message || 'Máy chủ từ chối mã xác thực.';
    } else if (err instanceof Error) {
      message = err.message;
    }

    failedExchanges.set(code, message);

    return {
      status: 'error',
      message,
    };
  }
}
