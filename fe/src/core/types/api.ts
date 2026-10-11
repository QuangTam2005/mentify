/**
 * Standard Envelope & API Types
 * Synchronized with Mentify Spring Boot Backend Foundation Contract
 */

export interface ApiResponse<T> {
  success: true;
  data: T;
  meta: PageMeta | null;
  timestamp: string; // ISO 8601 with Asia/Ho_Chi_Minh offset (+07:00)
  requestId: string;
}

export interface PageMeta {
  page: number; // 0-based
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ApiErrorDetail {
  field: string;
  message: string;
}

export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'RESOURCE_NOT_FOUND'
  | 'CONFLICT'
  | 'INTERNAL_ERROR'
  | (string & {});

export interface ApiErrorBody {
  code: ApiErrorCode;
  message: string;
  details?: ApiErrorDetail[] | null;
}

export interface ApiErrorResponse {
  success: false;
  error: ApiErrorBody;
  timestamp: string;
  requestId: string;
}

export interface PaginationParams {
  page?: number;
  size?: number;
  search?: string;
}
