'use client';

import React, { useEffect, useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { Spinner } from '@/shared/components/ui/spinner';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent } from '@/shared/components/ui/card';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import Link from 'next/link';
import { processOAuthCallback, type CallbackState } from '@/features/auth/oauth-callback';

/**
 * Route /auth/callback
 * Matches Spring Boot OAuth2LoginSuccessHandler redirect URI:
 * @Value("${app.security.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
 */
export default function AuthCallbackPage() {
  const router = useRouter();
  const [state, setState] = useState<CallbackState>({ status: 'processing' });
  const hasRedirected = useRef(false);

  useEffect(() => {
    let mounted = true;

    async function handleExchange() {
      const search = typeof window !== 'undefined' ? window.location.search : '';
      const result = await processOAuthCallback(search);

      if (!mounted) {
        return;
      }

      setState(result);

      if (result.status === 'success' && !hasRedirected.current) {
        hasRedirected.current = true;
        router.replace(result.destination);
      }
    }

    handleExchange();

    return () => {
      mounted = false;
    };
  }, [router]);

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-background text-foreground">
      <Card className="w-full max-w-md shadow-xl border-border bg-card text-card-foreground text-center">
        <CardContent className="pt-8 pb-8 space-y-4">
          {state.status === 'processing' && (
            <div role="status" aria-live="polite" className="space-y-3">
              <Spinner size="lg" />
              <h2 className="text-lg font-bold text-foreground">
                Đang hoàn tất đăng nhập...
              </h2>
              <p className="text-xs text-muted-foreground">
                Hệ thống đang đồng bộ xác thực JWT bảo mật với máy chủ Mentify.
              </p>
            </div>
          )}

          {state.status === 'success' && (
            <div role="status" aria-live="polite" className="space-y-3">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-400">
                <CheckCircle2 className="h-6 w-6" />
              </div>
              <h2 className="text-lg font-bold text-foreground">
                Đăng nhập thành công!
              </h2>
              <p className="text-xs text-muted-foreground">Đang chuyển hướng bạn tới trang cá nhân...</p>
            </div>
          )}

          {state.status === 'error' && (
            <div role="alert" aria-live="assertive" className="space-y-4">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
                <AlertCircle className="h-6 w-6" />
              </div>
              <h2 className="text-lg font-bold text-destructive">Đăng nhập không thành công</h2>
              <p className="text-xs text-muted-foreground">{state.message}</p>
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
