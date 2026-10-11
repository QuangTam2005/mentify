import React from 'react';
import Link from 'next/link';
import { Shield, Clock, Heart } from 'lucide-react';

export function Footer() {
  return (
    <footer className="border-t border-slate-200/80 bg-slate-50/60 dark:border-slate-800 dark:bg-slate-950/40">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
            <span className="font-semibold text-slate-700 dark:text-slate-300">Mentify.vn</span>
            <span>&copy; 2026 Nền tảng kết nối cố vấn học tập & nghề nghiệp.</span>
          </div>

          <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
            <div className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5 text-primary-600" />
              <span>Múi giờ chuẩn: Asia/Ho_Chi_Minh (+07:00)</span>
            </div>
            <div className="flex items-center gap-1">
              <Shield className="h-3.5 w-3.5 text-primary-600" />
              <span>Bảo vệ Dữ liệu Cá nhân & Thông tư 74/2026</span>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
