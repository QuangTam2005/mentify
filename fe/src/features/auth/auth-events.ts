export const AUTH_EXPIRED_EVENT = 'mentify:auth-expired';

export function notifyAuthExpired(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
  }
}
