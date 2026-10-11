const RETURN_URL_KEY = 'mentify:auth-return-url';

export function isSafeReturnUrl(value: string | null | undefined): value is string {
  return !!value && value.startsWith('/') && !value.startsWith('//');
}

export function rememberReturnUrl(value: string | null | undefined): void {
  if (typeof window === 'undefined' || !isSafeReturnUrl(value)) {
    return;
  }
  window.sessionStorage.setItem(RETURN_URL_KEY, value);
}

export function consumeReturnUrl(fallback: string): string {
  if (typeof window === 'undefined') {
    return fallback;
  }

  const value = window.sessionStorage.getItem(RETURN_URL_KEY);
  window.sessionStorage.removeItem(RETURN_URL_KEY);
  return isSafeReturnUrl(value) ? value : fallback;
}
