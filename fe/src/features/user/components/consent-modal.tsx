'use client';

import React, { useState } from 'react';
import { Modal } from '@/shared/components/ui/modal';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { useUserProfile } from '../hooks/use-user-profile';
import { useAuth } from '@/features/auth/hooks/use-auth';
import { UserType } from '@/shared/types/domain-enums';
import { parseError } from '@/core/errors/error-handler';
import { ShieldCheck, CheckCircle2, AlertCircle } from 'lucide-react';

export interface ConsentModalProps {
  isOpen: boolean;
  onClose: () => void;
  policyVersion?: string;
}

export function ConsentModal({ isOpen, onClose, policyVersion = '2026.1' }: ConsentModalProps) {
  const { user } = useAuth();
  const { recordConsent, isSubmittingConsent } = useUserProfile();

  const isStudent = user?.userType === UserType.SCHOOL_STUDENT;

  const [guardianName, setGuardianName] = useState('');
  const [guardianContact, setGuardianContact] = useState('');
  const [isAgreed, setIsAgreed] = useState(false);
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatus(null);

    if (!isAgreed) {
      setStatus({
        type: 'error',
        message: 'Bạn phải đánh dấu đồng ý với các điều khoản để tiếp tục.',
      });
      return;
    }

    if (isStudent && (!guardianName.trim() || !guardianContact.trim())) {
      setStatus({
        type: 'error',
        message: 'Đối với học sinh THPT, vui lòng cung cấp đầy đủ thông tin người giám hộ.',
      });
      return;
    }

    try {
      await recordConsent({
        consentType: 'DATA_PROTECTION_AND_TOS',
        policyVersion,
        guardianName: isStudent ? guardianName.trim() : undefined,
        guardianContact: isStudent ? guardianContact.trim() : undefined,
      });

      setStatus({
        type: 'success',
        message: 'Ghi nhận chấp thuận pháp lý thành công!',
      });

      setTimeout(() => {
        onClose();
      }, 1200);
    } catch (err) {
      const parsed = parseError(err);
      setStatus({
        type: 'error',
        message: `${parsed.title}: ${parsed.message}`,
      });
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Chấp thuận Điều khoản & Bảo vệ Dữ liệu Cá nhân"
      description={`Phiên bản quy định pháp lý: v${policyVersion}`}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="max-h-48 overflow-y-auto rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs leading-relaxed text-slate-700 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-300">
          <p className="font-semibold mb-1">1. Cam kết phi thương mại & Bảo mật thông tin:</p>
          <p className="mb-2">
            Mentify là nền tảng kết nối cố vấn học tập và nghề nghiệp phi lợi nhuận. Mọi thông tin
            danh tính, hồ sơ học tập và dữ liệu liên lạc đều được bảo vệ nghiêm ngặt theo quy định của
            Nghị định 13/2023/NĐ-CP về Bảo vệ dữ liệu cá nhân.
          </p>
          <p className="font-semibold mb-1">2. Quy định dành cho Học sinh THPT:</p>
          <p>
            Người học dưới 18 tuổi cần có sự đồng thuận từ cha mẹ hoặc người giám hộ hợp pháp khi tham
            gia các buổi cố vấn định hướng 1:1.
          </p>
        </div>

        {isStudent && (
          <div className="rounded-lg border border-amber-200/70 bg-amber-50/60 p-3 space-y-3 dark:border-amber-900/40 dark:bg-amber-950/20">
            <p className="text-xs font-semibold text-amber-900 dark:text-amber-300">
              Thông tin Người giám hộ (Bắt buộc đối với Học sinh THPT):
            </p>
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              <Input
                label="Họ tên người giám hộ"
                value={guardianName}
                onChange={(e) => setGuardianName(e.target.value)}
                placeholder="Nguyễn Phụ Huynh"
                required
              />
              <Input
                label="Số điện thoại / Email giám hộ"
                value={guardianContact}
                onChange={(e) => setGuardianContact(e.target.value)}
                placeholder="0912345678"
                required
              />
            </div>
          </div>
        )}

        <label className="flex items-start gap-2.5 text-xs text-slate-700 dark:text-slate-300 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={isAgreed}
            onChange={(e) => setIsAgreed(e.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
          />
          <span>
            Tôi đã đọc kỹ, hiểu rõ và đồng ý với Điều khoản dịch vụ và Chính sách bảo vệ dữ liệu cá
            nhân của Mentify.
          </span>
        </label>

        {status && (
          <div
            className={`flex items-start gap-2 rounded-lg p-3 text-xs ${
              status.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300'
                : 'bg-rose-50 text-rose-800 dark:bg-rose-950/40 dark:text-rose-300'
            }`}
          >
            {status.type === 'success' ? (
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
            ) : (
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
            )}
            <span>{status.message}</span>
          </div>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Hủy bỏ
          </Button>
          <Button type="submit" isLoading={isSubmittingConsent}>
            <ShieldCheck className="mr-2 h-4 w-4" />
            Xác nhận đồng ý
          </Button>
        </div>
      </form>
    </Modal>
  );
}
