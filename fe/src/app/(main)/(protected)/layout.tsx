import React from 'react';
import { ProtectedRoute } from '@/features/auth/components/protected-route';

export default function ProtectedLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute>{children}</ProtectedRoute>;
}
