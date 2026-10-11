'use client';

import React, { useState } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { getQueryClient } from '@/core/http/query-client';
import { AuthExpiryHandler } from '@/features/auth/components/auth-expiry-handler';

export function Providers({ children }: { children: React.ReactNode }) {
  // Đảm bảo QueryClient singleton trên client side
  const [queryClient] = useState(() => getQueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      <AuthExpiryHandler />
      {children}
      <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-right" />
    </QueryClientProvider>
  );
}
