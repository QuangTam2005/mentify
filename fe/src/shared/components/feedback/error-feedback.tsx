import React from 'react';
import Link from 'next/link';
import { AlertCircle, RefreshCw, ArrowLeft } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/shared/components/ui/card';
import { Button } from '@/shared/components/ui/button';
import { isSafeReturnUrl } from '@/features/auth/auth-navigation';
import { cn } from '@/shared/lib/utils';

export interface ErrorFeedbackProps extends React.HTMLAttributes<HTMLDivElement> {
  title?: string;
  description?: string;
  onRetry?: () => void;
  retryLabel?: string;
  navigationHref?: string;
  navigationLabel?: string;
}

export function ErrorFeedback({
  title = 'Đã xảy ra sự cố',
  description = 'Đã có lỗi không mong muốn xảy ra. Vui lòng thử lại sau.',
  onRetry,
  retryLabel = 'Thử lại',
  navigationHref,
  navigationLabel = 'Về Trang chủ',
  className,
  ...props
}: ErrorFeedbackProps) {
  const isSafeNav = navigationHref ? isSafeReturnUrl(navigationHref) : false;

  return (
    <div
      role="alert"
      aria-live="assertive"
      className={cn('mx-auto flex w-full max-w-lg items-center justify-center p-4', className)}
      {...props}
    >
      <Card className="w-full border-border bg-card text-card-foreground shadow-lg">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
            <AlertCircle className="h-6 w-6" />
          </div>
          <CardTitle className="text-xl font-bold text-foreground">
            {title}
          </CardTitle>
          {description ? (
            <CardDescription className="text-sm text-muted-foreground">
              {description}
            </CardDescription>
          ) : null}
        </CardHeader>
        {(onRetry || isSafeNav) && (
          <CardContent className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            {onRetry ? (
              <Button onClick={onRetry} className="w-full sm:w-auto">
                <RefreshCw className="mr-2 h-4 w-4" />
                {retryLabel}
              </Button>
            ) : null}
            {isSafeNav ? (
              <Link href={navigationHref!} className="w-full sm:w-auto">
                <Button variant="outline" className="w-full sm:w-auto">
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  {navigationLabel}
                </Button>
              </Link>
            ) : null}
          </CardContent>
        )}
      </Card>
    </div>
  );
}
