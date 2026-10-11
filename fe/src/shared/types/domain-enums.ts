/**
 * Domain Enums & Value Constants
 * Directly aligned with Mentify Product Rules & Spring Boot Entities
 * 100% Tiếng Việt cho toàn bộ nhãn hiển thị người dùng
 */

export const UserType = {
  SCHOOL_STUDENT: 'SCHOOL_STUDENT',
  UNIVERSITY_STUDENT: 'UNIVERSITY_STUDENT',
  WORKING_PROFESSIONAL: 'WORKING_PROFESSIONAL',
} as const;

export type UserType = (typeof UserType)[keyof typeof UserType];

/**
 * Phân cấp UserType theo chiều tăng tiến một chiều:
 * SCHOOL_STUDENT (0) -> UNIVERSITY_STUDENT (1) -> WORKING_PROFESSIONAL (2)
 * Cấm hạ cấp ngược lại.
 */
export const USER_TYPE_ORDER: Record<UserType, number> = {
  SCHOOL_STUDENT: 0,
  UNIVERSITY_STUDENT: 1,
  WORKING_PROFESSIONAL: 2,
};

export const USER_TYPE_LABELS: Record<UserType, string> = {
  SCHOOL_STUDENT: 'Học sinh THPT',
  UNIVERSITY_STUDENT: 'Sinh viên Đại học/Cao đẳng',
  WORKING_PROFESSIONAL: 'Người đi làm / Chuyên gia',
};

export const UserRole = {
  USER: 'USER',
  ADMIN: 'ADMIN',
} as const;

export type UserRole = (typeof UserRole)[keyof typeof UserRole];

export const ROLE_LABELS: Record<UserRole, string> = {
  USER: 'Thành viên',
  ADMIN: 'Quản trị viên',
};

export const AccountStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  SUSPENDED: 'SUSPENDED',
  DELETED: 'DELETED',
} as const;

export type AccountStatus = (typeof AccountStatus)[keyof typeof AccountStatus];

export const ACCOUNT_STATUS_LABELS: Record<AccountStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Chưa kích hoạt',
  SUSPENDED: 'Đang bị khóa',
  DELETED: 'Đã xóa tài khoản',
};

export const MentorStatus = {
  NOT_APPLIED: 'NOT_APPLIED',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  SUSPENDED: 'SUSPENDED',
} as const;

export type MentorStatus = (typeof MentorStatus)[keyof typeof MentorStatus];

export const MENTOR_STATUS_LABELS: Record<MentorStatus, string> = {
  NOT_APPLIED: 'Chưa đăng ký Cố vấn',
  PENDING: 'Đang chờ xét duyệt',
  APPROVED: 'Cố vấn chính thức',
  REJECTED: 'Hồ sơ chưa đạt',
  SUSPENDED: 'Tạm ngưng nhận hẹn',
};

export const CampusType = {
  MAIN_CAMPUS: 'MAIN_CAMPUS',
  BRANCH_CAMPUS: 'BRANCH_CAMPUS',
} as const;

export type CampusType = (typeof CampusType)[keyof typeof CampusType];

export const CAMPUS_TYPE_LABELS: Record<CampusType, string> = {
  MAIN_CAMPUS: 'Cơ sở chính',
  BRANCH_CAMPUS: 'Phân hiệu',
};

export const BookingStatus = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  DECLINED: 'DECLINED',
  CANCELLED_BY_LEARNER: 'CANCELLED_BY_LEARNER',
  CANCELLED_BY_MENTOR: 'CANCELLED_BY_MENTOR',
  RESCHEDULE_REQUESTED: 'RESCHEDULE_REQUESTED',
  COMPLETED: 'COMPLETED',
  NO_SHOW: 'NO_SHOW',
  EXPIRED: 'EXPIRED',
} as const;

export type BookingStatus = (typeof BookingStatus)[keyof typeof BookingStatus];

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  DECLINED: 'Đã từ chối',
  CANCELLED_BY_LEARNER: 'Người học đã hủy',
  CANCELLED_BY_MENTOR: 'Cố vấn đã hủy',
  RESCHEDULE_REQUESTED: 'Yêu cầu dời lịch',
  COMPLETED: 'Đã hoàn thành',
  NO_SHOW: 'Vắng mặt',
  EXPIRED: 'Hết hạn',
};

export const PostType = {
  QUESTION: 'QUESTION',
  SHARING: 'SHARING',
} as const;

export type PostType = (typeof PostType)[keyof typeof PostType];

export const POST_TYPE_LABELS: Record<PostType, string> = {
  QUESTION: 'Câu hỏi',
  SHARING: 'Chia sẻ kinh nghiệm',
};

export const ConsentType = {
  TOS: 'TOS',
  PRIVACY_POLICY: 'PRIVACY_POLICY',
  DATA_PROTECTION_AND_TOS: 'DATA_PROTECTION_AND_TOS',
} as const;

export type ConsentType = (typeof ConsentType)[keyof typeof ConsentType];
