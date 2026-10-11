'use client';

import React from 'react';
import { DevLoginPanel } from '@/features/auth/components/dev-login-panel';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { BookOpen, ShieldCheck } from 'lucide-react';
import Link from 'next/link';
import { rememberReturnUrl } from '@/features/auth/auth-navigation';

export default function LoginPage() {
  const handleGoogleLogin = () => {
    // Chuyển hướng trình duyệt tới cổng đăng nhập Google OAuth của Backend Spring Boot
    const googleAuthUrl =
      process.env.NEXT_PUBLIC_GOOGLE_AUTH_URL || 'http://localhost:8080/oauth2/authorization/google';
    rememberReturnUrl(new URLSearchParams(window.location.search).get('returnUrl'));
    window.location.href = googleAuthUrl;
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-4 bg-slate-50 dark:bg-slate-950">
      <div className="w-full max-w-lg space-y-6">
        {/* Tiêu đề thương hiệu */}
        <div className="text-center space-y-2">
          <Link href="/" className="inline-flex items-center gap-2 group">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-600 text-white shadow-md shadow-primary-500/20">
              <BookOpen className="h-6 w-6" />
            </div>
            <span className="text-2xl font-black text-slate-900 dark:text-white">
              Mentify<span className="text-primary-600">.vn</span>
            </span>
          </Link>
          <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
            Đăng nhập vào Hệ thống Mentify
          </h2>
          <p className="text-xs text-slate-500">
            Nền tảng cố vấn học thuật phi thương mại & Định hướng nghề nghiệp 2026
          </p>
        </div>

        {/* Khối Đăng nhập Google OAuth */}
        <Card className="shadow-lg border-slate-200 dark:border-slate-800">
          <CardHeader className="pb-3 text-center">
            <CardTitle className="text-base font-bold">Xác thực bằng Tài khoản Google</CardTitle>
            <CardDescription className="text-xs">
              Đăng nhập an toàn và bảo mật với tài khoản Google cá nhân của bạn
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button
              onClick={handleGoogleLogin}
              variant="outline"
              size="lg"
              className="w-full border-slate-300 font-medium hover:bg-slate-50 dark:hover:bg-slate-800 shadow-sm"
            >
              <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
              Tiếp tục với Google
            </Button>

            <div className="flex items-center gap-2 text-center text-xs text-slate-400">
              <span className="flex-1 border-t border-slate-200 dark:border-slate-800" />
              <span>HOẶC CHẾ ĐỘ THỬ NGHIỆM CHO LẬP TRÌNH VIÊN</span>
              <span className="flex-1 border-t border-slate-200 dark:border-slate-800" />
            </div>
          </CardContent>
        </Card>

        <div className="text-center text-xs">
          <Link href="/forgot-password" className="text-primary-600 hover:underline">
            Quên mật khẩu?
          </Link>
        </div>

        {/* Khối Đăng nhập Nhanh Dev */}
        <DevLoginPanel />

        <div className="text-center text-xs text-slate-400 flex items-center justify-center gap-1">
          <ShieldCheck className="h-3.5 w-3.5 text-primary-600" />
          <span>Phiên làm việc bảo mật qua Cookie HttpOnly và Token bảo vệ kép</span>
        </div>
      </div>
    </div>
  );
}
