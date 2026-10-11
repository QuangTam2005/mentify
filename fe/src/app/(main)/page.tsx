import React from 'react';
import Link from 'next/link';
import { DevLoginPanel } from '@/features/auth/components/dev-login-panel';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import {
  Sparkles,
  ShieldCheck,
  GraduationCap,
  Layers,
  ArrowRight,
  Database,
  Lock,
  Clock,
  Compass,
} from 'lucide-react';

export default function HomePage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8 space-y-12">
      {/* Phần Tiêu đề chính */}
      <div className="text-center space-y-4 max-w-3xl mx-auto">
        <div className="inline-flex items-center gap-2 rounded-full border border-primary-200 bg-primary-50/80 px-3.5 py-1 text-xs font-semibold text-primary-800 dark:border-primary-900/50 dark:bg-primary-950/40 dark:text-primary-300">
          <Sparkles className="h-3.5 w-3.5 text-primary-600" />
          <span>Kiến trúc Hiện đại 2026 &bull; Đồng bộ Toàn diện với Spring Boot 3</span>
        </div>

        <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-5xl">
          Nền tảng Cố vấn Học thuật & Hướng nghiệp <span className="text-primary-600">Mentify</span>
        </h1>

        <p className="text-base text-slate-600 dark:text-slate-400 leading-relaxed">
          Kết nối học sinh THPT, sinh viên đại học với mạng lưới cố vấn giàu kinh nghiệm theo danh
          mục đào tạo chuẩn <strong>Thông tư 74/2026/TT-BGDĐT</strong>, bảo mật phiên làm việc và bảo
          vệ dữ liệu cá nhân theo quy định pháp luật.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
          <Link href="/explore">
            <Button size="lg" className="shadow-lg shadow-primary-600/20">
              <Compass className="mr-2 h-5 w-5" />
              Khám phá Danh mục Đào tạo
            </Button>
          </Link>
          <Link href="/profile">
            <Button size="lg" variant="outline">
              Xem Hồ sơ & Pháp lý
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </Link>
        </div>
      </div>

      {/* Lưới tính năng: Bảng thử nghiệm đăng nhập & Các trụ cột kiến trúc */}
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12 items-start">
        {/* Cột trái: Khung thử nghiệm dành cho Lập trình viên */}
        <div className="lg:col-span-5 space-y-6">
          <DevLoginPanel />

          <Card className="border-slate-200 dark:border-slate-800 bg-white/70 dark:bg-slate-900/40">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <Clock className="h-4 w-4 text-primary-600" />
                Múi giờ & Định dạng Chuẩn
              </CardTitle>
            </CardHeader>
            <CardContent className="text-xs text-slate-600 dark:text-slate-400 space-y-1.5">
              <p>
                <strong>Múi giờ nghiệp vụ:</strong>{' '}
                <code className="text-primary-600 font-mono">Asia/Ho_Chi_Minh (+07:00)</code>
              </p>
              <p>
                Mọi mốc thời gian từ máy chủ đều có độ lệch rõ ràng. Giao diện cam kết hiển thị chính
                xác giờ Việt Nam và không tự ý dịch chuyển thời gian của các phiên hẹn.
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Cột phải: Các trụ cột nền tảng */}
        <div className="lg:col-span-7 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <Layers className="h-5 w-5 text-primary-600" />
              Nền tảng Kỹ thuật Cốt lõi
            </h2>
            <Badge variant="success">Sẵn sàng Vận hành</Badge>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Card>
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2 text-primary-600">
                  <Lock className="h-4 w-4" />
                  <CardTitle className="text-sm">Bảo mật Phiên & CSRF</CardTitle>
                </div>
              </CardHeader>
              <CardContent className="text-xs text-slate-600 dark:text-slate-400">
                Phiên làm việc bảo mật qua Cookie an toàn kết hợp cơ chế kiểm soát mã xác thực kép
                chống giả mạo request, tự động xác thực trước mọi thao tác lưu trữ dữ liệu.
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2 text-emerald-600">
                  <Database className="h-4 w-4" />
                  <CardTitle className="text-sm">Chuẩn hóa Gói Dữ liệu API</CardTitle>
                </div>
              </CardHeader>
              <CardContent className="text-xs text-slate-600 dark:text-slate-400">
                Đồng bộ chặt chẽ cấu trúc phản hồi thành công và thông báo lỗi có định danh truy vết
                mã yêu cầu nhằm hỗ trợ kỹ thuật nhanh chóng khi gặp sự cố.
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2 text-blue-600">
                  <GraduationCap className="h-4 w-4" />
                  <CardTitle className="text-sm">Danh mục Thông tư 74/2026</CardTitle>
                </div>
              </CardHeader>
              <CardContent className="text-xs text-slate-600 dark:text-slate-400">
                Tích hợp dữ liệu danh mục Trường Đại học toàn quốc và các Nhóm ngành đào tạo mới nhất
                theo quy định của Bộ Giáo dục & Đào tạo với cơ chế đệm dữ liệu thông minh.
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2 text-indigo-600">
                  <ShieldCheck className="h-4 w-4" />
                  <CardTitle className="text-sm">Tuân thủ Quy định Pháp lý</CardTitle>
                </div>
              </CardHeader>
              <CardContent className="text-xs text-slate-600 dark:text-slate-400">
                Thực hiện quy trình thăng hạng cấp bậc một chiều, ghi nhận lịch sử kiểm toán và cơ
                chế lấy ý kiến người giám hộ hợp pháp đối với người học là học sinh THPT.
              </CardContent>
            </Card>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 dark:border-slate-800 dark:bg-slate-900/40 text-xs text-slate-600 dark:text-slate-400 flex items-center justify-between">
            <div className="space-y-1">
              <span className="font-bold text-slate-800 dark:text-slate-200">
                Tài liệu kỹ thuật giao tiếp:
              </span>
              <p>
                Tham khảo tài liệu hướng dẫn tích hợp tại thư mục{' '}
                <code className="font-mono text-primary-600">fe/docs/be-communication.md</code>
              </p>
            </div>
            <Link href="/explore">
              <Button size="sm" variant="outline">
                Xem Danh mục
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
