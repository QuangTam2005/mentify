import type { ApiErrorCode, ApiErrorDetail, ApiErrorResponse } from '../types/api';

export class ApiError extends Error {
  public readonly code: ApiErrorCode;
  public readonly status: number;
  public readonly details: ApiErrorDetail[] | null;
  public readonly requestId?: string;
  public readonly timestamp?: string;

  constructor(status: number, errorResponse?: Partial<ApiErrorResponse> | null, defaultMessage?: string) {
    const errorBody = errorResponse?.error;
    const message = errorBody?.message || defaultMessage || `Request failed with status ${status}`;

    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = (errorBody?.code || (status === 401 ? 'UNAUTHENTICATED' : status === 403 ? 'FORBIDDEN' : status === 404 ? 'RESOURCE_NOT_FOUND' : status === 409 ? 'CONFLICT' : 'INTERNAL_ERROR')) as ApiErrorCode;
    this.details = errorBody?.details || null;
    this.requestId = errorResponse?.requestId;
    this.timestamp = errorResponse?.timestamp;

    // Giữ stack trace chuẩn
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, ApiError);
    }
  }

  public isUnauthenticated(): boolean {
    return this.status === 401 || this.code === 'UNAUTHENTICATED';
  }

  public isForbidden(): boolean {
    return this.status === 403 || this.code === 'FORBIDDEN';
  }

  public isNotFound(): boolean {
    return this.status === 404 || this.code === 'RESOURCE_NOT_FOUND';
  }

  public isConflict(): boolean {
    return this.status === 409 || this.code === 'CONFLICT';
  }

  public isValidation(): boolean {
    return this.status === 400 || this.code === 'VALIDATION_ERROR';
  }

  public getFieldError(fieldName: string): string | undefined {
    return this.details?.find((d) => d.field === fieldName)?.message;
  }
}
