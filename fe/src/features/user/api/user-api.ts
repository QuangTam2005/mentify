import { apiClient } from '@/core/http/api-client';
import type { ApiResponse } from '@/core/types/api';
import type {
  RecordConsentRequest,
  UpdateProfileRequest,
  UserConsentResponse,
  UserProfileResponse,
} from '../types/user.types';

export const userApi = {
  getProfile: () => {
    return apiClient.get<UserProfileResponse>('/api/v1/users/profile');
  },

  updateProfile: (payload: UpdateProfileRequest) => {
    return apiClient.put<UserProfileResponse>('/api/v1/users/profile', payload);
  },

  recordConsent: (payload: RecordConsentRequest) => {
    return apiClient.post<UserConsentResponse>('/api/v1/users/consent', payload);
  },
};
