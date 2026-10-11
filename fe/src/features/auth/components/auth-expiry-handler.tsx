'use client';

import { useEffect, useRef } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { clearAccessToken } from '../store/access-token';
import { useAuthStore } from '../store/auth-store';
import { rememberReturnUrl } from '../auth-navigation';
import { AUTH_EXPIRED_EVENT } from '../auth-events';
import { clearPrivateUserCache } from '../auth-cache';

function isProtectedPath(pathname: string): boolean {
  return pathname === '/profile' || pathname.startsWith('/admin');
}

export function AuthExpiryHandler() {
  const router = useRouter();
  const pathname = usePathname();
  const queryClient = useQueryClient();
  const redirecting = useRef(false);

  useEffect(() => {
    const handleAuthExpired = () => {
      clearAccessToken();
      useAuthStore.getState().clearUser();
      void clearPrivateUserCache(queryClient);

      if (!isProtectedPath(pathname) || redirecting.current) {
        return;
      }

      redirecting.current = true;
      const currentUrl = `${window.location.pathname}${window.location.search}`;
      rememberReturnUrl(currentUrl);
      router.replace(`/login?returnUrl=${encodeURIComponent(currentUrl)}`);
    };

    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, [pathname, queryClient, router]);

  return null;
}
