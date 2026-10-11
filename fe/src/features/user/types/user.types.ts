import type { AccountStatus, UserRole, UserType } from '@/shared/types/domain-enums';
import type { MajorGroup, School } from '@/features/master-data/types/master-data.types';

export interface UserProfileResponse {
  id: string;
  email: string;
  userType: UserType;
  role: UserRole;
  accountStatus: AccountStatus;
  fullName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  school: School | null;
  majorGroup: MajorGroup | null;
  customMajorName: string | null;
}

export interface UpdateProfileRequest {
  fullName?: string;
  bio?: string;
  avatarUrl?: string;
  schoolId?: string;
  majorGroupCode?: string;
  customMajorName?: string;
  newUserType?: UserType;
}

export interface RecordConsentRequest {
  consentType: string;
  policyVersion: string;
  guardianName?: string;
  guardianContact?: string;
}

export interface UserConsentResponse {
  id: string;
  consentType: string;
  policyVersion: string;
  acceptedAt: string;
  guardianName: string | null;
  guardianContact: string | null;
}
