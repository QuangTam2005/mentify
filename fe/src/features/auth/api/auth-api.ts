import { apiClient } from '@/core/http/api-client';
import type { ApiResponse } from '@/core/types/api';
import type {
  AuthMeResponse,
  DevLoginRequest,
  ForgotPasswordRequest,
  OAuth2CodeExchangeRequest,
  PasswordResetRequest,
} from '../types/auth.types';

export const authApi = {
  getMe: () => {
    return apiClient.get<AuthMeResponse>('/api/v1/auth/me');
  },

  devLogin: (payload: DevLoginRequest = {}) => {
    return apiClient.post<AuthMeResponse>('/api/v1/auth/dev-login', payload);
  },

  exchangeOAuth2Code: (payload: OAuth2CodeExchangeRequest) => {
    return apiClient.post<AuthMeResponse>('/api/v1/auth/oauth2/exchange', payload);
  },

  forgotPassword: (payload: ForgotPasswordRequest) => {
    return apiClient.post<null>('/api/v1/auth/password/forgot', payload);
  },

  resetPassword: (payload: PasswordResetRequest) => {
    return apiClient.post<null>('/api/v1/auth/password/reset', payload);
  },

  logout: () => {
    return apiClient.post<null>('/api/v1/auth/logout');
  },
};
