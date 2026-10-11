'use client';

import React, { useEffect } from 'react';
import { ErrorFeedback } from '@/shared/components/feedback/error-feedback';

export interface AppErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function AppError({ error, reset }: AppErrorProps) {
  useEffect(() => {
    // Log unexpected errors for monitoring without leaking diagnostics to UI
    console.error('App error boundary caught error:', error);
  }, [error]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <ErrorFeedback
        title="Đã xảy ra sự cố"
        description="Đã có lỗi không mong muốn xảy ra trong quá trình xử lý yêu cầu. Vui lòng thử lại hoặc quay lại trang chủ."
        onRetry={reset}
        retryLabel="Thử lại"
        navigationHref="/"
        navigationLabel="Về Trang chủ"
      />
    </div>
  );
}
