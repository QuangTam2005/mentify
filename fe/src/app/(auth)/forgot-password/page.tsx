'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { AlertCircle, ArrowLeft, CheckCircle2, Mail } from 'lucide-react';
import { authApi } from '@/features/auth/api/auth-api';
import { parseError } from '@/core/errors/error-handler';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Input } from '@/shared/components/ui/input';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isPending, setIsPending] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setIsPending(true);
    try {
      await authApi.forgotPassword({ email });
      setSubmitted(true);
    } catch (requestError) {
      const parsed = parseError(requestError);
      setError(`${parsed.title}: ${parsed.message}`);
    } finally {
      setIsPending(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4 dark:bg-slate-950">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary-100 text-primary-700 dark:bg-primary-950 dark:text-primary-300">
            <Mail className="h-6 w-6" />
          </div>
          <CardTitle>Quên mật khẩu</CardTitle>
          <CardDescription>
            Nhập email tài khoản. Nếu tài khoản hợp lệ, hệ thống sẽ gửi hướng dẫn đặt lại mật khẩu.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {submitted ? (
            <div className="space-y-4 text-center">
              <CheckCircle2 className="mx-auto h-8 w-8 text-emerald-600" />
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.
              </p>
              <Link href="/login">
                <Button variant="outline" className="w-full">Quay lại đăng nhập</Button>
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="Email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
                required
                autoComplete="email"
              />
              {error && (
                <div className="flex items-start gap-2 rounded-md bg-rose-50 p-3 text-xs text-rose-700 dark:bg-rose-950/40 dark:text-rose-300">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}
              <Button type="submit" className="w-full" isLoading={isPending}>
                Gửi hướng dẫn đặt lại mật khẩu
              </Button>
              <Link href="/login" className="flex items-center justify-center gap-1 text-xs text-slate-500 hover:text-primary-600">
                <ArrowLeft className="h-3.5 w-3.5" /> Quay lại đăng nhập
              </Link>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
