import type { MentorStatus } from '@/shared/types/domain-enums';

export type MentorApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

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
