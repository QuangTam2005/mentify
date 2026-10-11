/**
 * Core Type-Safe API Client
 * Features:
 * - Bearer JWT credential inclusion
 * - X-Request-Id tracking per request
 * - Unified ApiResponse<T> deserialization & ApiError throw
 */

import { ApiError } from '../errors/api-error';
import type { ApiResponse, ApiErrorResponse } from '../types/api';
import { clearAccessToken, getAccessToken } from '@/features/auth/store/access-token';
import { notifyAuthExpired } from '@/features/auth/auth-events';

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  params?: Record<string, string | number | boolean | null | undefined>;
  timeoutMs?: number;
}

class ApiClient {
  private defaultBaseUrl: string;

  constructor() {
    this.defaultBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || '';
  }

  private buildUrl(path: string, params?: RequestOptions['params']): string {
    const isAbsolute = path.startsWith('http://') || path.startsWith('https://');
    let urlString = isAbsolute ? path : `${this.defaultBaseUrl}${path}`;

    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          searchParams.append(key, String(value));
        }
      });
      const queryString = searchParams.toString();
      if (queryString) {
        urlString += (urlString.includes('?') ? '&' : '?') + queryString;
      }
    }

    return urlString;
  }

  private generateRequestId(): string {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    return 'req-' + Math.random().toString(36).substring(2, 11) + Date.now().toString(36);
  }

  public async request<T>(
    path: string,
    options: RequestOptions & { body?: unknown; method: string }
  ): Promise<ApiResponse<T>> {
    const { method, params, body, headers = {}, timeoutMs = 30000, ...customConfig } = options;

    const fullUrl = this.buildUrl(path, params);
    const requestHeaders = new Headers(headers);

    // Chuẩn hóa Accept & Content-Type
    if (!requestHeaders.has('Accept')) {
      requestHeaders.set('Accept', 'application/json');
    }

    if (body !== undefined && !(body instanceof FormData) && !requestHeaders.has('Content-Type')) {
      requestHeaders.set('Content-Type', 'application/json');
    }

    // Gắn X-Request-Id phục vụ distributed tracing
    if (!requestHeaders.has('X-Request-Id')) {
      requestHeaders.set('X-Request-Id', this.generateRequestId());
    }

    const accessToken = getAccessToken();
    if (accessToken && !requestHeaders.has('Authorization')) {
      requestHeaders.set('Authorization', `Bearer ${accessToken}`);
    }

    const upperMethod = method.toUpperCase();

    // Timeout controller
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(fullUrl, {
        ...customConfig,
        method: upperMethod,
        headers: requestHeaders,
        body:
          body === undefined
            ? undefined
            : body instanceof FormData
            ? body
            : JSON.stringify(body),
        credentials: 'omit',
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      // Xử lý status 204 No Content
      if (response.status === 204) {
        return {
          success: true,
          data: null as unknown as T,
          meta: null,
          timestamp: new Date().toISOString(),
          requestId: requestHeaders.get('X-Request-Id') || '',
        };
      }

      const contentType = response.headers.get('content-type');
      const isJson = contentType && contentType.includes('application/json');

      if (!response.ok) {
        if (response.status === 401) {
          clearAccessToken();
          notifyAuthExpired();
        }
        let errorData: Partial<ApiErrorResponse> | null = null;
        if (isJson) {
          try {
            errorData = await response.json();
          } catch {
            errorData = null;
          }
        }
        throw new ApiError(response.status, errorData, response.statusText);
      }

      if (!isJson) {
        throw new ApiError(
          response.status,
          null,
          `Expected JSON response but received ${contentType}`
        );
      }

      const result: ApiResponse<T> = await response.json();
      return result;
    } catch (err: unknown) {
      clearTimeout(timeoutId);

      if (err instanceof ApiError) {
        throw err;
      }

      // Xử lý Abort timeout
      if (err instanceof Error && err.name === 'AbortError') {
        throw new ApiError(408, {
          error: {
            code: 'INTERNAL_ERROR',
            message: 'Yêu cầu hết thời gian chờ (Timeout). Vui lòng thử lại.',
          },
        });
      }

      throw err;
    }
  }

  public get<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'GET' });
  }

  public post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'POST', body });
  }

  public put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'PUT', body });
  }

  public patch<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'PATCH', body });
  }

  public delete<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
