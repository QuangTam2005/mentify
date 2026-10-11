import type { AccountStatus, MentorStatus, UserRole, UserType } from '@/shared/types/domain-enums';

export interface AuthMeResponse {
  id: string;
  email: string;
  userType: UserType;
  role: UserRole;
  accountStatus: AccountStatus;
  mentorStatus: MentorStatus;
  acceptingBookings: boolean;
  fullName: string | null;
  avatarUrl: string | null;
  token: AuthTokenResponse | null;
}

export interface AuthTokenResponse {
  value: string;
  tokenType: 'Bearer';
  expiresAt: string;
}

export interface DevLoginRequest {
  email?: string;
  userType?: UserType;
  role?: UserRole;
  fullName?: string;
}

export interface OAuth2CodeExchangeRequest {
  code: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface PasswordResetRequest {
  token: string;
  newPassword: string;
}
