'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { AlertCircle, KeyRound } from 'lucide-react';
import { authApi } from '@/features/auth/api/auth-api';
import { parseError } from '@/core/errors/error-handler';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Input } from '@/shared/components/ui/input';

export default function ResetPasswordPage() {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setToken(new URLSearchParams(window.location.search).get('token'));
  }, []);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (!token) {
      setError('Liên kết đặt lại mật khẩu không hợp lệ hoặc đã thiếu token.');
      return;
    }
    if (newPassword.length < 12) {
      setError('Mật khẩu phải có từ 12 đến 128 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setIsPending(true);
    try {
      await authApi.resetPassword({ token, newPassword });
      router.replace('/login?reset=success');
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
            <KeyRound className="h-6 w-6" />
          </div>
          <CardTitle>Đặt lại mật khẩu</CardTitle>
          <CardDescription>Chọn mật khẩu mới có tối thiểu 12 ký tự.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Mật khẩu mới"
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              required
              minLength={12}
              maxLength={128}
              autoComplete="new-password"
            />
            <Input
              label="Xác nhận mật khẩu"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              minLength={12}
              maxLength={128}
              autoComplete="new-password"
            />
            {error && (
              <div className="flex items-start gap-2 rounded-md bg-rose-50 p-3 text-xs text-rose-700 dark:bg-rose-950/40 dark:text-rose-300">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}
            <Button type="submit" className="w-full" isLoading={isPending}>
              Cập nhật mật khẩu
            </Button>
            <Link href="/login" className="block text-center text-xs text-slate-500 hover:text-primary-600">
              Quay lại đăng nhập
            </Link>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
