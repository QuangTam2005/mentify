'use client';

import React, { useState } from 'react';
import { useAuth } from '@/features/auth/hooks/use-auth';
import { ProfileForm } from '@/features/user/components/profile-form';
import { ConsentModal } from '@/features/user/components/consent-modal';
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import { USER_TYPE_LABELS, MENTOR_STATUS_LABELS } from '@/shared/types/domain-enums';
import { ShieldCheck } from 'lucide-react';

export default function ProfilePage() {
  const { user } = useAuth();
  const [isConsentModalOpen, setIsConsentModalOpen] = useState(false);

  if (!user) {
    return null;
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
