import React from 'react';
import Link from 'next/link';
import { Inbox } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/shared/components/ui/card';
import { Button } from '@/shared/components/ui/button';
import { isSafeReturnUrl } from '@/features/auth/auth-navigation';
import { cn } from '@/shared/lib/utils';

export interface EmptyFeedbackProps extends React.HTMLAttributes<HTMLDivElement> {
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  actionHref?: string;
}

export function EmptyFeedback({
  title,
  description,
  actionLabel,
  onAction,
  actionHref,
  className,
  ...props
}: EmptyFeedbackProps) {
  const isSafeActionHref = actionHref ? isSafeReturnUrl(actionHref) : false;
  const hasAction = Boolean(actionLabel && (onAction || isSafeActionHref));

  return (
    <div
      role="status"
      aria-live="polite"
      className={cn('mx-auto flex w-full max-w-lg items-center justify-center p-4', className)}
      {...props}
    >
      <Card className="w-full border-border bg-card text-card-foreground shadow-sm text-center">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <Inbox className="h-6 w-6" />
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
        {hasAction ? (
          <CardContent className="flex items-center justify-center pt-2">
            {onAction ? (
              <Button onClick={onAction}>
                {actionLabel}
              </Button>
            ) : isSafeActionHref ? (
              <Link href={actionHref!}>
                <Button>
                  {actionLabel}
                </Button>
              </Link>
            ) : null}
          </CardContent>
        ) : null}
      </Card>
    </div>
  );
}
