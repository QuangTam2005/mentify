const RETURN_URL_KEY = 'mentify:auth-return-url';

export function isSafeReturnUrl(value: string | null | undefined): value is string {
  if (!value || typeof value !== 'string') {
    return false;
  }
  // Must start with single forward slash, must not start with // or /\ or contain backslash or control characters
  if (!value.startsWith('/') || value.startsWith('//') || value.includes('\\')) {
    return false;
  }
  try {
    const url = new URL(value, 'http://localhost');
    return url.origin === 'http://localhost';
  } catch {
    return false;
  }
}

export function rememberReturnUrl(value: string | null | undefined): void {
  if (typeof window === 'undefined' || !isSafeReturnUrl(value)) {
    return;
  }
  window.sessionStorage.setItem(RETURN_URL_KEY, value);
}

export function consumeReturnUrl(fallback: string): string {
  if (typeof window === 'undefined') {
    return isSafeReturnUrl(fallback) ? fallback : '/profile';
  }

  const value = window.sessionStorage.getItem(RETURN_URL_KEY);
  window.sessionStorage.removeItem(RETURN_URL_KEY);
  const safeFallback = isSafeReturnUrl(fallback) ? fallback : '/profile';
  return isSafeReturnUrl(value) ? value : safeFallback;
}

export function clearReturnUrl(): void {
  if (typeof window !== 'undefined') {
    window.sessionStorage.removeItem(RETURN_URL_KEY);
  }
}
