'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Select } from '@/shared/components/ui/select';
import { Input } from '@/shared/components/ui/input';
import { Badge } from '@/shared/components/ui/badge';
import { UserRole, UserType, USER_TYPE_LABELS, ROLE_LABELS } from '@/shared/types/domain-enums';
import { useAuth } from '../hooks/use-auth';
import { parseError } from '@/core/errors/error-handler';
import { Sparkles, Terminal, CheckCircle2, AlertCircle } from 'lucide-react';
import { consumeReturnUrl, isSafeReturnUrl } from '../auth-navigation';
import { isDevLoginEnabled } from '@/core/config/env';

export function DevLoginPanel() {
  const router = useRouter();
  const { devLogin, isPendingLogin, user } = useAuth();

  const [email, setEmail] = useState('dev-tester@mentify.vn');
  const [fullName, setFullName] = useState('Trần Lập Trình');
  const [userType, setUserType] = useState<UserType>(UserType.UNIVERSITY_STUDENT);
  const [role, setRole] = useState<UserRole>(UserRole.USER);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  if (!isDevLoginEnabled()) {
    return null;
  }

  const handleDevLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatusMessage(null);
    try {
      const res = await devLogin({
        email,
        fullName,
        userType,
        role,
      });
      setStatusMessage({
        type: 'success',
        message: `Đăng nhập thử nghiệm thành công! Xin chào ${res.fullName || res.email}`,
      });
      if (window.location.pathname === '/login') {
        const queryReturnUrl = new URLSearchParams(window.location.search).get('returnUrl');
        router.replace(isSafeReturnUrl(queryReturnUrl) ? queryReturnUrl : consumeReturnUrl('/profile'));
      }
    } catch (err) {
      const parsed = parseError(err);
      setStatusMessage({
        type: 'error',
        message: `${parsed.title}: ${parsed.message}`,
      });
    }
  };

  return (
    <Card className="border-amber-200/60 bg-gradient-to-br from-amber-50/50 via-white to-orange-50/30 dark:border-amber-900/40 dark:from-slate-900 dark:to-amber-950/20 shadow-md">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:bg-amber-500/20">
              <Terminal className="h-4 w-4" />
            </span>
            <CardTitle className="text-base font-bold text-amber-950 dark:text-amber-300">
              Mô phỏng Đăng nhập Nhanh
            </CardTitle>
          </div>
          <Badge variant="warning" className="text-[10px]">
            Chế độ Thử nghiệm
          </Badge>
        </div>
        <CardDescription className="text-xs">
          Cấp mã xác thực JWT Bearer ngay lập tức cho bất kỳ Vai trò & Cấp bậc người dùng nào qua cổng{' '}
          <code className="rounded bg-amber-100 px-1 py-0.5 font-mono text-[11px] text-amber-800 dark:bg-amber-900/50 dark:text-amber-200">
            POST /api/v1/auth/dev-login
          </code>
        </CardDescription>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleDevLogin} className="space-y-3.5">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Input
              label="Địa chỉ Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="dev@mentify.vn"
              required
            />
            <Input
              label="Họ và tên"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn A"
              required
            />
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Select
              label="Cấp bậc người dùng"
              value={userType}
              onChange={(e) => setUserType(e.target.value as UserType)}
              options={[
                { value: UserType.SCHOOL_STUDENT, label: USER_TYPE_LABELS.SCHOOL_STUDENT },
                { value: UserType.UNIVERSITY_STUDENT, label: USER_TYPE_LABELS.UNIVERSITY_STUDENT },
                { value: UserType.WORKING_PROFESSIONAL, label: USER_TYPE_LABELS.WORKING_PROFESSIONAL },
              ]}
            />
            <Select
              label="Vai trò hệ thống"
              value={role}
              onChange={(e) => setRole(e.target.value as UserRole)}
              options={[
                { value: UserRole.USER, label: `${ROLE_LABELS.USER} (USER)` },
                { value: UserRole.ADMIN, label: `${ROLE_LABELS.ADMIN} (ADMIN)` },
              ]}
            />
          </div>

          {statusMessage && (
            <div
              className={`flex items-start gap-2 rounded-lg p-3 text-xs ${
                statusMessage.type === 'success'
                  ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300'
                  : 'bg-rose-50 text-rose-800 dark:bg-rose-950/40 dark:text-rose-300'
              }`}
            >
              {statusMessage.type === 'success' ? (
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
              ) : (
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
              )}
              <span>{statusMessage.message}</span>
            </div>
          )}

          <Button
            type="submit"
            isLoading={isPendingLogin}
            className="w-full bg-amber-600 hover:bg-amber-700 focus:ring-amber-500 shadow-amber-600/20 text-white"
          >
            <Sparkles className="mr-2 h-4 w-4" />
            Khởi tạo xác thực thử nghiệm ngay
          </Button>

          {user && (
            <div className="rounded-lg border border-slate-200 bg-white/60 p-2.5 text-xs dark:border-slate-800 dark:bg-slate-900/60">
              <span className="font-semibold text-slate-700 dark:text-slate-300">Tài khoản hiện hành: </span>
              <span className="font-mono text-primary-600">{user.email}</span>
              <span className="mx-1.5 text-slate-400">|</span>
              <Badge variant="info" className="text-[10px]">{USER_TYPE_LABELS[user.userType]}</Badge>
              <span className="mx-1 text-slate-400">|</span>
              <Badge variant="outline" className="text-[10px]">{ROLE_LABELS[user.role]}</Badge>
            </div>
          )}
        </form>
      </CardContent>
    </Card>
  );
}
