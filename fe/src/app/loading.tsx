import React from 'react';
import { LoadingFeedback } from '@/shared/components/feedback/loading-feedback';

export default function Loading() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <LoadingFeedback message="Đang tải dữ liệu..." />
    </div>
  );
}
