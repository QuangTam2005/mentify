'use client';

import React from 'react';
import { useCurrentMentorApplication } from '../hooks/use-current-mentor-application';
import { MentorApplicationForm } from './mentor-application-form';
import { MentorApplicationStatus } from './mentor-application-status';
import { LoadingFeedback } from '@/shared/components/feedback/loading-feedback';
import { ErrorFeedback } from '@/shared/components/feedback/error-feedback';
import { ApiError } from '@/core/errors/api-error';

export function MentorApplyContainer() {
  const { data: application, isLoading, isError, error, refetch, isFetching } =
    useCurrentMentorApplication();

  // 1. Loading state: Fetching current application status
  if (isLoading) {
    return (
      <div className="py-12">
        <LoadingFeedback message="Đang kiểm tra hồ sơ ứng tuyển của bạn..." size="lg" />
      </div>
    );
  }

  // 2. Error handling with strict status distinction
  if (isError) {
    // 2a. Confirmed HTTP 404 (RESOURCE_NOT_FOUND): User has no prior mentor application
    if (error instanceof ApiError && error.status === 404) {
      return <MentorApplicationForm />;
    }

    // 2b. HTTP 403 (FORBIDDEN): Forbidden feedback without logging out
    if (error instanceof ApiError && error.status === 403) {
      return (
        <ErrorFeedback
          title="Truy cập bị từ chối"
          description={
            error.message ||
            'Tài khoản của bạn không có quyền truy cập hoặc thực hiện thao tác trên hệ thống ứng tuyển.'
          }
          navigationHref="/profile"
          navigationLabel="Về trang hồ sơ cá nhân"
        />
      );
    }

    // 2c. Network or Server (5xx / Timeout / Fetch error): Recoverable error with Retry
    const isNetworkOrServerError =
      (error instanceof ApiError && error.status >= 500) ||
      (error instanceof Error &&
        (error.name === 'AbortError' ||
          error.message.toLowerCase().includes('fetch') ||
          error.message.toLowerCase().includes('network') ||
          error.message.toLowerCase().includes('kết nối')));

    if (isNetworkOrServerError) {
      return (
        <ErrorFeedback
          title="Lỗi kết nối máy chủ"
          description="Không thể kết nối đến máy chủ để kiểm tra trạng thái hồ sơ ứng tuyển. Vui lòng kiểm tra mạng và thử lại."
          onRetry={() => void refetch()}
          retryLabel="Thử lại"
          navigationHref="/profile"
          navigationLabel="Về trang hồ sơ cá nhân"
        />
      );
    }

    // 2d. Unexpected / Generic error: Never treat unknown errors as 404!
    return (
      <ErrorFeedback
        title="Đã xảy ra sự cố"
        description={
          error instanceof Error
            ? error.message
            : 'Đã có lỗi không mong muốn xảy ra khi tải dữ liệu hồ sơ.'
        }
        onRetry={() => void refetch()}
        retryLabel="Thử lại"
        navigationHref="/"
        navigationLabel="Về trang chủ"
      />
    );
  }

  // 3. Success state: User has an existing application (HTTP 200)
  if (application) {
    return (
      <MentorApplicationStatus
        application={application}
        onRefresh={() => void refetch()}
        isRefreshing={isFetching}
      />
    );
  }

  // Fallback fallback: If no error and no application data, show recoverable state
  return (
    <ErrorFeedback
      title="Không tìm thấy dữ liệu"
      description="Không thể xác định trạng thái hồ sơ ứng tuyển."
      onRetry={() => void refetch()}
      retryLabel="Tải lại"
      navigationHref="/profile"
      navigationLabel="Về trang hồ sơ"
    />
  );
}
