'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Badge } from '@/shared/components/ui/badge';
import { Button } from '@/shared/components/ui/button';
import { MENTOR_STATUS_LABELS } from '@/shared/types/domain-enums';
import type { CurrentMentorApplicationResponse, MentorApplicationStatus as AppStatusType } from '../types/mentor.types';
import {
  Clock,
  CheckCircle2,
  XCircle,
  Ban,
  AlertTriangle,
  RefreshCw,
  FileCheck2,
  Sparkles,
  User,
  GraduationCap,
  FileText,
  Calendar,
  RotateCcw,
} from 'lucide-react';

export interface MentorApplicationStatusProps {
  application: CurrentMentorApplicationResponse;
  onRefresh?: () => void;
  isRefreshing?: boolean;
  onResubmit?: () => void;
}

export function MentorApplicationStatus({
  application,
  onRefresh,
  isRefreshing = false,
  onResubmit,
}: MentorApplicationStatusProps) {
  const router = useRouter();
  const isSuspended = application.mentorStatus === 'SUSPENDED';

  const getStatusConfig = (status: AppStatusType) => {
    switch (status) {
      case 'PENDING':
        return {
          title: 'Hồ sơ đang chờ xét duyệt',
          description:
            'Đơn ứng tuyển của bạn đã được ghi nhận trên hệ thống và đang chờ Ban quản trị Mentify thẩm định.',
          badgeVariant: 'warning' as const,
          badgeText: 'Chờ xét duyệt (PENDING)',
          icon: Clock,
          iconBg: 'bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400',
          notice: (
            <div className="rounded-xl border border-amber-200/80 bg-amber-50/80 p-4 text-xs text-amber-900 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-200">
              <p className="font-semibold text-sm mb-1">Quy trình thẩm định đang diễn ra</p>
              <p className="leading-relaxed">
                Đội ngũ kiểm duyệt Mentify đang đánh giá tính xác thực của thông tin học vấn và kinh nghiệm thực tế.
                Thời gian xử lý thông thường từ 1 đến 3 ngày làm việc. Hệ thống không cho phép nộp thêm đơn mới trong thời gian này.
              </p>
            </div>
          ),
        };

      case 'APPROVED':
        return {
          title: 'Hồ sơ đã được chấp thuận',
          description:
            'Chúc mừng bạn! Hồ sơ ứng tuyển của bạn đã được Ban quản trị phê duyệt chính thức thành Mentor.',
          badgeVariant: 'success' as const,
          badgeText: 'Đã chấp thuận (APPROVED)',
          icon: CheckCircle2,
          iconBg: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400',
          notice: (
            <div className="rounded-xl border border-emerald-200/80 bg-emerald-50/80 p-4 text-xs text-emerald-900 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-200">
              <p className="font-semibold text-sm mb-1">Trạng thái cố vấn chính thức</p>
              <p className="leading-relaxed">
                {application.acceptingBookings
                  ? 'Tài khoản của bạn hiện đang ở trạng thái sẵn sàng nhận lịch hẹn hướng dẫn (accepting bookings).'
                  : 'Lưu ý: Việc đơn được duyệt không đồng nghĩa tài khoản tự động mở nhận lịch hẹn. Hiện bạn đang ở trạng thái chưa mở nhận lịch hẹn. Bạn có thể kích hoạt tại trang quản lý hồ sơ cá nhân.'}
              </p>
            </div>
          ),
        };

      case 'REJECTED':
        return {
          title: 'Hồ sơ đã bị từ chối',
          description:
            'Rất tiếc, đơn ứng tuyển Mentor của bạn chưa đáp ứng đủ tiêu chuẩn chuyên môn hoặc điều kiện của Mentify.',
          badgeVariant: 'destructive' as const,
          badgeText: 'Bị từ chối (REJECTED)',
          icon: XCircle,
          iconBg: 'bg-rose-100 text-rose-600 dark:bg-rose-950/60 dark:text-rose-400',
          notice: (
            <div className="rounded-xl border border-rose-200/80 bg-rose-50/80 p-4 text-xs text-rose-900 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-200">
              <p className="font-semibold text-sm mb-1">Thông tin nộp lại đơn</p>
              <p className="leading-relaxed">
                {onResubmit && !isSuspended
                  ? "Hồ sơ lần này chưa được duyệt. Bạn có thể chọn 'Nộp lại hồ sơ' bên dưới để cập nhật thông tin và gửi lại đơn ứng tuyển cho Ban quản trị xem xét."
                  : 'Hồ sơ lần này chưa được duyệt. Chức năng nộp lại hồ sơ (Resubmission) sẽ được hỗ trợ trong các phiên bản cập nhật tiếp theo. Quý ứng viên vui lòng theo dõi thông báo và quay lại sau.'}
              </p>
            </div>
          ),
        };

      case 'CANCELLED':
        return {
          title: 'Hồ sơ đã bị hủy',
          description: 'Đơn ứng tuyển Mentor này đã được hủy bỏ trên hệ thống.',
          badgeVariant: 'secondary' as const,
          badgeText: 'Đã hủy (CANCELLED)',
          icon: Ban,
          iconBg: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400',
          notice: (
            <div className="rounded-xl border border-slate-200/80 bg-slate-50/80 p-4 text-xs text-slate-700 dark:border-slate-800 dark:bg-slate-900/40 dark:text-slate-300">
              <p className="leading-relaxed">
                Đơn đăng ký đã dừng xử lý do yêu cầu hủy bỏ hoặc quá trình xác thực không hoàn tất.
              </p>
            </div>
          ),
        };
    }
  };

  const statusConfig = getStatusConfig(application.applicationStatus);
  const StatusIcon = statusConfig.icon;

  return (
    <Card className="border-border shadow-md">
      <CardHeader className="text-center pb-4">
        <div
          className={`mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-full ${statusConfig.iconBg}`}
        >
          <StatusIcon className="h-10 w-10" />
        </div>
        <CardTitle className="text-2xl font-bold text-foreground">
          {statusConfig.title}
        </CardTitle>
        <CardDescription className="text-sm text-muted-foreground mt-1 max-w-xl mx-auto">
          {statusConfig.description}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-6">
        {/* Suspended Warning if mentorStatus is SUSPENDED */}
        {isSuspended && (
          <div
            role="alert"
            className="rounded-xl border border-destructive/40 bg-destructive/10 p-4 text-xs text-destructive"
          >
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
              <div className="space-y-1">
                <p className="font-semibold text-sm">
                  Tài khoản Mentor đang bị Tạm ngưng hoạt động (SUSPENDED)
                </p>
                <p className="leading-relaxed">
                  Tài khoản Cố vấn của bạn hiện đang ở trạng thái bị tạm khóa hoặc giới hạn chức năng theo chính sách an toàn của Mentify. Mọi hoạt động tiếp nhận người học mới tạm thời bị dừng. Vui lòng liên hệ Ban quản trị qua email hỗ trợ để được giải đáp.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Status Specific Notice */}
        {statusConfig.notice}

        {/* Snapshot Details Card */}
        <div className="rounded-xl border border-border bg-muted/40 p-5 space-y-4">
          <h3 className="text-sm font-semibold text-foreground flex items-center gap-2 border-b border-border pb-2.5">
            <FileText className="h-4 w-4 text-primary" />
            Thông tin chi tiết hồ sơ ứng tuyển
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-3 gap-x-6 text-xs">
            <div className="flex items-center justify-between py-1 border-b border-border/50">
              <span className="text-muted-foreground">Trạng thái xét duyệt:</span>
              <Badge variant={statusConfig.badgeVariant}>{statusConfig.badgeText}</Badge>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-border/50">
              <span className="text-muted-foreground">Trạng thái Cố vấn:</span>
              <span className="font-medium text-foreground">
                {MENTOR_STATUS_LABELS[application.mentorStatus] || application.mentorStatus}
              </span>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-border/50">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <User className="h-3.5 w-3.5 text-muted-foreground" />
                Lần nộp hồ sơ:
              </span>
              <span className="font-medium text-foreground">
                Lần thứ {application.attemptNumber}
              </span>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-border/50">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
                Thời gian gửi:
              </span>
              <span className="font-medium text-foreground font-mono">
                {new Date(application.submittedAt).toLocaleString('vi-VN')}
              </span>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-border/50">
              <span className="text-muted-foreground">Mã đơn ứng tuyển:</span>
              <span className="font-mono text-[11px] text-foreground font-medium truncate max-w-[180px]">
                {application.mentorApplicationId}
              </span>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-border/50">
              <span className="text-muted-foreground">Mã hồ sơ Mentor:</span>
              <span className="font-mono text-[11px] text-foreground font-medium truncate max-w-[180px]">
                {application.mentorProfileId}
              </span>
            </div>

            {application.customMajorName && (
              <div className="flex items-center justify-between py-1 border-b border-border/50 sm:col-span-2">
                <span className="text-muted-foreground flex items-center gap-1.5">
                  <GraduationCap className="h-3.5 w-3.5 text-muted-foreground" />
                  Chuyên ngành đào tạo:
                </span>
                <span className="font-medium text-foreground">
                  {application.customMajorName}
                </span>
              </div>
            )}
          </div>

          {/* Bio Snippet if available */}
          {application.bio && (
            <div className="pt-2">
              <p className="text-xs font-semibold text-foreground mb-1">
                Tiểu sử & Giới thiệu đã nộp:
              </p>
              <div className="rounded-lg bg-background p-3 text-xs text-muted-foreground leading-relaxed border border-border/70 max-h-32 overflow-y-auto whitespace-pre-wrap">
                {application.bio}
              </div>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2 justify-center">
          {application.applicationStatus === 'REJECTED' && !isSuspended && onResubmit && (
            <Button
              type="button"
              onClick={onResubmit}
              className="w-full sm:w-auto"
            >
              <RotateCcw className="mr-2 h-4 w-4" />
              Nộp lại hồ sơ
            </Button>
          )}

          {onRefresh && (
            <Button
              type="button"
              variant="outline"
              onClick={onRefresh}
              isLoading={isRefreshing}
              disabled={isRefreshing}
              className="w-full sm:w-auto"
            >
              <RefreshCw className="mr-2 h-4 w-4" />
              Làm mới trạng thái
            </Button>
          )}

          <Button
            type="button"
            onClick={() => router.push('/profile')}
            className="w-full sm:w-auto"
          >
            <FileCheck2 className="mr-2 h-4 w-4" />
            Về trang hồ sơ cá nhân
          </Button>

          <Button
            type="button"
            variant="outline"
            onClick={() => router.push('/explore')}
            className="w-full sm:w-auto"
          >
            <Sparkles className="mr-2 h-4 w-4" />
            Khám phá Mentify
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
