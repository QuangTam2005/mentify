import React from 'react';
import Link from 'next/link';
import { FileQuestion, ArrowLeft, Compass } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/shared/components/ui/card';
import { Button } from '@/shared/components/ui/button';

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <Card className="w-full max-w-lg border-border bg-card text-card-foreground shadow-lg text-center">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-secondary/20 text-primary">
            <FileQuestion className="h-6 w-6" />
          </div>
          <CardTitle className="text-2xl font-bold text-foreground">
            404 - Không tìm thấy trang
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground">
            Trang bạn đang tìm kiếm không tồn tại, đã bị gỡ bỏ hoặc tạm thời không thể truy cập.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
          <Link href="/" className="w-full sm:w-auto">
            <Button className="w-full sm:w-auto">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Về Trang chủ
            </Button>
          </Link>
          <Link href="/explore" className="w-full sm:w-auto">
            <Button variant="outline" className="w-full sm:w-auto">
              <Compass className="mr-2 h-4 w-4" />
              Khám phá cố vấn
            </Button>
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}
