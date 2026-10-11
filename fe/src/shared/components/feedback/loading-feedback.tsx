import React from 'react';
import { Spinner } from '@/shared/components/ui/spinner';
import { cn } from '@/shared/lib/utils';

export interface LoadingFeedbackProps extends React.HTMLAttributes<HTMLDivElement> {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
}

export function LoadingFeedback({
  message = 'Đang tải dữ liệu...',
  size = 'md',
  className,
  ...props
}: LoadingFeedbackProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn('flex flex-col items-center justify-center p-6 text-center', className)}
      {...props}
    >
      <Spinner size={size} />
      {message ? (
        <p className="mt-3 text-sm font-medium text-muted-foreground">{message}</p>
      ) : null}
    </div>
  );
}
