'use client';

import React, { useState } from 'react';
import { useAuth } from '@/features/auth/hooks/use-auth';
import { ProfileForm } from '@/features/user/components/profile-form';
import { ConsentModal } from '@/features/user/components/consent-modal';
import { Button } from '@/shared/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Badge } from '@/shared/components/ui/badge';
import { USER_TYPE_LABELS, MENTOR_STATUS_LABELS } from '@/shared/types/domain-enums';
import { ShieldCheck, UserCheck, AlertTriangle, LogIn } from 'lucide-react';
import Link from 'next/link';

export default function ProfilePage() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const [isConsentModalOpen, setIsConsentModalOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 text-center">
        <p className="text-sm text-slate-500">Đang nạp thông tin phiên đăng nhập...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return (
      <div className="mx-auto max-w-md px-4 py-20 text-center space-y-4">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400">
          <AlertTriangle className="h-6 w-6" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white">Yêu cầu Đăng nhập</h2>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Bạn cần đăng nhập bằng tài khoản Google hoặc sử dụng chế độ Dev Fast Login để truy cập
          hồ sơ cá nhân và quản lý pháp lý.
        </p>
        <div className="pt-2">
          <Link href="/login">
            <Button className="w-full">
              <LogIn className="mr-2 h-4 w-4" />
              Đến trang Đăng nhập / Dev Login
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8 space-y-8">
      {/* Top Banner: Profile Overview */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-primary-950 p-6 text-white shadow-xl">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black">{user.fullName || 'Người dùng Mentify'}</h1>
            <Badge variant="info">{USER_TYPE_LABELS[user.userType]}</Badge>
            {user.role === 'ADMIN' && <Badge variant="destructive">ADMIN</Badge>}
          </div>
          <p className="text-xs text-slate-300 font-mono">{user.email}</p>
          <div className="pt-1 flex items-center gap-2 text-xs text-slate-300">
            <span>Trạng thái Mentor:</span>
            <span className="font-semibold text-emerald-400">
              {MENTOR_STATUS_LABELS[user.mentorStatus]}
            </span>
            {user.acceptingBookings && (
              <Badge variant="success" className="text-[10px]">
                Đang nhận lịch hẹn
              </Badge>
            )}
          </div>
        </div>

        <Button
          variant="outline"
          onClick={() => setIsConsentModalOpen(true)}
          className="border-white/20 bg-white/10 text-white hover:bg-white/20 backdrop-blur-sm"
        >
          <ShieldCheck className="mr-2 h-4 w-4 text-emerald-400" />
          Điều khoản Pháp lý & Giám hộ
        </Button>
      </div>

      {/* Profile Form */}
      <ProfileForm />

      {/* Consent Modal */}
      <ConsentModal
        isOpen={isConsentModalOpen}
        onClose={() => setIsConsentModalOpen(false)}
        policyVersion="2026.1"
      />
    </div>
  );
}
