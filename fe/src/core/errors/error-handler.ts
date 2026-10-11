import { ApiError } from './api-error';

export interface FriendlyError {
  title: string;
  message: string;
  requestId?: string;
  fieldErrors?: Record<string, string>;
}

export function parseError(error: unknown): FriendlyError {
  if (error instanceof ApiError) {
    const fieldErrors: Record<string, string> = {};
    if (error.details && error.details.length > 0) {
      error.details.forEach((d) => {
        fieldErrors[d.field] = d.message;
      });
    }

    switch (error.code) {
      case 'UNAUTHENTICATED':
        return {
          title: 'Yêu cầu đăng nhập',
          message: 'Phiên làm việc của bạn đã kết thúc hoặc bạn chưa đăng nhập.',
          requestId: error.requestId,
        };
      case 'FORBIDDEN':
        return {
          title: 'Truy cập bị từ chối',
          message: error.message || 'Bạn không có quyền thực hiện thao tác này.',
          requestId: error.requestId,
        };
      case 'RESOURCE_NOT_FOUND':
        return {
          title: 'Không tìm thấy dữ liệu',
          message: error.message || 'Tài nguyên bạn yêu cầu không tồn tại trên hệ thống.',
          requestId: error.requestId,
        };
      case 'CONFLICT':
        return {
          title: 'Xung đột dữ liệu',
          message:
            error.message ||
            'Thao tác không thể thực hiện do xung đột trạng thái dữ liệu (ví dụ: cấm hạ cấp UserType hoặc trùng lịch).',
          requestId: error.requestId,
        };
      case 'VALIDATION_ERROR':
        return {
          title: 'Dữ liệu không hợp lệ',
          message: error.message || 'Vui lòng kiểm tra lại các trường thông tin bên dưới.',
          requestId: error.requestId,
          fieldErrors,
        };
      case 'INTERNAL_ERROR':
      default:
        return {
          title: 'Lỗi hệ thống',
          message:
            error.message ||
            'Đã có sự cố xảy ra từ máy chủ. Vui lòng thử lại sau hoặc liên hệ quản trị viên.',
          requestId: error.requestId,
        };
    }
  }

  if (error instanceof Error) {
    if (error.name === 'AbortError') {
      return {
        title: 'Hủy yêu cầu',
        message: 'Yêu cầu mạng đã bị hủy do quá thời gian chờ (timeout).',
      };
    }

    return {
      title: 'Lỗi kết nối',
      message: 'Không thể kết nối đến máy chủ Backend. Vui lòng kiểm tra lại kết nối mạng.',
    };
  }

  return {
    title: 'Lỗi không xác định',
    message: 'Đã có sự cố không lường trước xảy ra.',
  };
}
