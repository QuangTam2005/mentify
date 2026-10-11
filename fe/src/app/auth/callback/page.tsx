'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { authApi } from '@/features/auth/api/auth-api';
import { useAuthStore } from '@/features/auth/store/auth-store';
import { setAccessToken } from '@/features/auth/store/access-token';
import { Spinner } from '@/shared/components/ui/spinner';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent } from '@/shared/components/ui/card';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import Link from 'next/link';
import { consumeReturnUrl } from '@/features/auth/auth-navigation';

/**
 * Route /auth/callback
 * Matches Spring Boot OAuth2LoginSuccessHandler redirect URI:
 * @Value("${app.security.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
 */
export default function AuthCallbackPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    let mounted = true;

    async function handleHandshake() {
      try {
        const code = new URLSearchParams(window.location.search).get('code');
        if (!code) {
          throw new Error('Thiếu mã đăng nhập OAuth2.');
        }

        const exchangeRes = await authApi.exchangeOAuth2Code({ code });
        if (!exchangeRes.data.token) {
          throw new Error('BE không trả về access token.');
        }
        setAccessToken(exchangeRes.data.token.value);
        useAuthStore.getState().setUser(exchangeRes.data);
        window.history.replaceState({}, document.title, '/auth/callback');
        if (exchangeRes.data) {
          if (mounted) {
            setIsSuccess(true);
            setTimeout(() => {
              router.replace(consumeReturnUrl('/profile'));
            }, 1000);
          }
        } else {
          if (mounted) {
          setError('Không thể hoàn tất đăng nhập OAuth2. Vui lòng thử lại.');
          }
        }
      } catch (err: unknown) {
        if (mounted) {
          setError(
            err instanceof Error
              ? err.message
              : 'Xảy ra lỗi trong quá trình hoàn tất xác thực Google.'
          );
        }
      }
    }

    handleHandshake();

    return () => {
      mounted = false;
    };
  }, [router]);

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-slate-50 dark:bg-slate-950">
      <Card className="w-full max-w-md shadow-xl border-slate-200 dark:border-slate-800 text-center">
        <CardContent className="pt-8 pb-8 space-y-4">
          {!error && !isSuccess && (
            <div className="space-y-3">
              <Spinner size="lg" />
              <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                Đang hoàn tất đăng nhập...
              </h2>
              <p className="text-xs text-slate-500">
                Hệ thống đang đồng bộ phiên làm việc bảo mật với máy chủ Mentify.
              </p>
            </div>
          )}

          {isSuccess && (
            <div className="space-y-3">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-400">
                <CheckCircle2 className="h-6 w-6" />
              </div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                Đăng nhập thành công!
              </h2>
              <p className="text-xs text-slate-500">Đang chuyển hướng bạn tới trang cá nhân...</p>
            </div>
          )}

          {error && (
            <div className="space-y-4">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 text-rose-600 dark:bg-rose-950 dark:text-rose-400">
                <AlertCircle className="h-6 w-6" />
              </div>
              <h2 className="text-lg font-bold text-rose-600">Đăng nhập không thành công</h2>
              <p className="text-xs text-slate-600 dark:text-slate-400">{error}</p>
              <div className="pt-2">
                <Link href="/login">
                  <Button variant="outline" className="w-full">
                    Quay lại trang Đăng nhập
                  </Button>
                </Link>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
