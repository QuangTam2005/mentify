/**
 * TanStack Query Client Configuration
 * Optimized for Next.js App Router and Spring Boot Backend
 */

import { QueryClient, defaultShouldDehydrateQuery, isServer } from '@tanstack/react-query';
import { ApiError } from '../errors/api-error';

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000, // 1 phút mặc định
        gcTime: 5 * 60 * 1000, // 5 phút lưu trữ bộ nhớ đệm
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => {
          // Không retry nếu là các mã lỗi client-side xác định
          if (error instanceof ApiError) {
            if ([400, 401, 403, 404, 409].includes(error.status)) {
              return false;
            }
          }
          return failureCount < 2;
        },
      },
      mutations: {
        retry: false,
      },
      dehydrate: {
        // Chỉ serialize queries thành công hoặc đang nạp
        shouldDehydrateQuery: (query) =>
          defaultShouldDehydrateQuery(query) || query.state.status === 'pending',
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined = undefined;

export function getQueryClient() {
  if (isServer) {
    // Trên máy chủ SSR: luôn tạo mới một QueryClient cho mỗi request
    return makeQueryClient();
  } else {
    // Trên trình duyệt: giữ một instance duy nhất (singleton)
    if (!browserQueryClient) {
      browserQueryClient = makeQueryClient();
    }
    return browserQueryClient;
  }
}
