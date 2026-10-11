import type { MentorStatus } from '@/shared/types/domain-enums';

export type MentorApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export const APPLICATION_STATUS_LABELS: Record<MentorApplicationStatus, string> = {
  PENDING: 'Hồ sơ đang chờ xét duyệt',
  APPROVED: 'Hồ sơ đã được chấp thuận',
  REJECTED: 'Hồ sơ đã bị từ chối',
  CANCELLED: 'Hồ sơ đã bị hủy',
};

export interface SubmitMentorApplicationRequest {
  schoolId?: string;
  majorGroupCode?: string;
  customMajorName?: string;
  bio?: string;
  expertiseAreas?: string;
  defaultMeetingLink?: string;
}

export interface MentorApplicationSubmissionResponse {
  mentorProfileId: string;
  mentorApplicationId: string;
  userId: string;
  mentorStatus: MentorStatus;
  applicationStatus: MentorApplicationStatus;
  attemptNumber: number;
  appliedUserType: string;
  acceptingBookings: boolean;
  submittedAt: string;
}

export interface CurrentMentorApplicationResponse {
  mentorProfileId: string;
  mentorApplicationId: string;
  mentorStatus: MentorStatus;
  applicationStatus: MentorApplicationStatus;
  attemptNumber: number;
  appliedUserType: string;
  schoolId: string | null;
  majorGroupCode: string | null;
  customMajorName: string | null;
  bio: string | null;
  acceptingBookings: boolean;
  submittedAt: string;
}
