'use client';

import React, { useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../hooks/use-auth';
import { getAccessToken } from '../store/access-token';
import { isSafeReturnUrl, rememberReturnUrl } from '../auth-navigation';
import { ApiError } from '@/core/errors/api-error';
import { Spinner } from '@/shared/components/ui/spinner';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { AlertCircle, AlertTriangle, ArrowLeft, RefreshCw, ShieldAlert } from 'lucide-react';
import Link from 'next/link';

export interface ProtectedRouteProps {
  children: React.ReactNode;
  fallbackUrl?: string;
}

export function ProtectedRoute({ children, fallbackUrl = '/login' }: ProtectedRouteProps) {
  const router = useRouter();
  const { user, isAuthenticated, isLoading, isError, error, refetchMe } = useAuth();
  const hasToken = typeof window !== 'undefined' ? !!getAccessToken() : true;
  const redirecting = useRef(false);

  // Authentication Decision Model
  // Priority order:
  // 1. Loading: while authentication resolution is in progress
  // 2. HTTP 403 Forbidden: authorization denial
  // 3. Network or HTTP 5xx Error: recoverable system failure
  // 4. Confirmed Authenticated: valid token + user + authenticated flag
  // 5. Confirmed Guest: missing token or unauthenticated state without blocking error

  const isForbidden = isError && error instanceof ApiError && error.isForbidden();
  const isServerOrNetworkError = isError && (!error || !(error instanceof ApiError) || !error.isUnauthenticated());
  const isConfirmedAuth = hasToken && isAuthenticated && !!user && !isForbidden && !isError;
  const isConfirmedGuest = (!hasToken || (!isAuthenticated && !user)) && !isLoading && !isError;

  useEffect(() => {
    if (isConfirmedGuest && !redirecting.current) {
      redirecting.current = true;
      if (typeof window !== 'undefined') {
        const currentUrl = `${window.location.pathname}${window.location.search}`;
        if (isSafeReturnUrl(currentUrl)) {
          rememberReturnUrl(currentUrl);
        }
        const destination = isSafeReturnUrl(currentUrl)
          ? `${fallbackUrl}?returnUrl=${encodeURIComponent(currentUrl)}`
          : fallbackUrl;
        router.replace(destination);
      }
    }
  }, [isConfirmedGuest, fallbackUrl, router]);

  // A. Loading State
  if (isLoading || (!isConfirmedAuth && !isForbidden && !isServerOrNetworkError && !isConfirmedGuest)) {
    return (
      <div
        role="status"
        aria-live="polite"
        className="flex min-h-[50vh] flex-col items-center justify-center p-6 text-center"
      >
        <Spinner size="lg" />
        <p className="mt-4 text-sm font-medium text-muted-foreground">
          Đang xác thực thông tin tài khoản...
        </p>
      </div>
    );
  }

  // D. HTTP 403 — Forbidden State
  if (isForbidden) {
    return (
      <div
        role="alert"
        aria-live="assertive"
        className="mx-auto flex min-h-[50vh] max-w-lg items-center justify-center p-6"
      >
        <Card className="w-full border-border bg-card shadow-lg">
          <CardHeader className="text-center">
            <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
              <ShieldAlert className="h-6 w-6" />
            </div>
            <CardTitle className="text-xl font-bold text-foreground">
              Truy cập bị từ chối (403)
            </CardTitle>
            <CardDescription className="text-sm text-muted-foreground">
              {error instanceof ApiError && error.message
                ? error.message
                : 'Bạn không có quyền truy cập vào khu vực này với tài khoản hiện tại.'}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3 pt-2">
            <Link href="/" className="w-full">
              <Button variant="outline" className="w-full">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Về Trang chủ
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  // E. Network / Server Error (5xx / Connection issue)
  if (isServerOrNetworkError) {
    const errorMessage =
      error instanceof ApiError
        ? error.message || 'Lỗi hệ thống máy chủ'
        : error instanceof Error
        ? error.message
        : 'Không thể kết nối đến máy chủ';

    return (
      <div
        role="alert"
        aria-live="assertive"
        className="mx-auto flex min-h-[50vh] max-w-lg items-center justify-center p-6"
      >
        <Card className="w-full border-border bg-card shadow-lg">
          <CardHeader className="text-center">
            <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
              <AlertCircle className="h-6 w-6" />
            </div>
            <CardTitle className="text-xl font-bold text-foreground">
              Lỗi kết nối hoặc hệ thống
            </CardTitle>
            <CardDescription className="text-sm text-muted-foreground">
              {errorMessage}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3 pt-2">
            <Button
              onClick={() => {
                refetchMe();
              }}
              className="w-full"
            >
              <RefreshCw className="mr-2 h-4 w-4" />
              Thử lại
            </Button>
            <Link href="/" className="w-full">
              <Button variant="outline" className="w-full">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Về Trang chủ
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  // B. Confirmed Authenticated
  if (isConfirmedAuth) {
    return <>{children}</>;
  }

  // C. Confirmed Guest while redirecting
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex min-h-[50vh] flex-col items-center justify-center p-6 text-center"
    >
      <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400">
        <AlertTriangle className="h-6 w-6" />
      </div>
      <p className="mt-2 text-sm font-medium text-muted-foreground">
        Đang chuyển hướng đến trang đăng nhập...
      </p>
    </div>
  );
}
