'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/features/auth/hooks/use-auth';
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import { USER_TYPE_LABELS, MENTOR_STATUS_LABELS, ROLE_LABELS } from '@/shared/types/domain-enums';
import { Compass, User, LogOut, Sparkles, BookOpen } from 'lucide-react';

export function Navbar() {
  const pathname = usePathname();
  const { user, isAuthenticated, logout, isPendingLogout } = useAuth();

  const navLinks = [
    { href: '/', label: 'Trang chủ', icon: Sparkles },
    { href: '/explore', label: 'Khám phá Danh mục', icon: Compass },
    ...(isAuthenticated ? [{ href: '/profile', label: 'Hồ sơ & Pháp lý', icon: User }] : []),
  ];

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200/80 bg-white/80 backdrop-blur-md dark:border-slate-800 dark:bg-slate-900/80">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        {/* Logo */}
        <div className="flex items-center gap-6">
          <Link href="/" className="flex items-center gap-2 group">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-primary-600 to-emerald-400 text-white shadow-md shadow-primary-500/20 group-hover:scale-105 transition-transform">
              <BookOpen className="h-5 w-5" />
            </div>
            <span className="text-xl font-black tracking-tight text-slate-900 dark:text-white">
              Mentify<span className="text-primary-600">.vn</span>
            </span>
          </Link>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map((link) => {
              const Icon = link.icon;
              const isActive = pathname === link.href;
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100 dark:text-slate-400 dark:hover:text-slate-200 dark:hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  {link.label}
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User Auth Actions */}
        <div className="flex items-center gap-3">
          {isAuthenticated && user ? (
            <div className="flex items-center gap-3">
              <div className="hidden sm:flex flex-col items-end text-right">
                <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                  {user.fullName || user.email}
                </span>
                <div className="flex items-center gap-1 mt-0.5">
                  <Badge variant="info" className="text-[10px]">
                    {USER_TYPE_LABELS[user.userType]}
                  </Badge>
                  {user.role === 'ADMIN' && (
                    <Badge variant="destructive" className="text-[10px]">
                      {ROLE_LABELS.ADMIN}
                    </Badge>
                  )}
                  {user.mentorStatus !== 'NOT_APPLIED' && (
                    <Badge variant="success" className="text-[10px]">
                      {MENTOR_STATUS_LABELS[user.mentorStatus]}
                    </Badge>
                  )}
                </div>
              </div>

              <Link href="/profile">
                <Button variant="outline" size="sm" className="h-9 px-3">
                  <User className="h-4 w-4 mr-1.5 text-slate-500" />
                  Hồ sơ
                </Button>
              </Link>

              <Button
                variant="ghost"
                size="sm"
                onClick={() => logout()}
                isLoading={isPendingLogout}
                className="h-9 px-2.5 text-rose-600 hover:bg-rose-50 hover:text-rose-700 dark:hover:bg-rose-950/50"
                title="Đăng xuất khỏi hệ thống"
              >
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link href="/login">
                <Button size="sm" variant="default">
                  Đăng nhập
                </Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
