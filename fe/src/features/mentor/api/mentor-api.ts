import { apiClient } from '@/core/http/api-client';
import type {
  SubmitMentorApplicationRequest,
  MentorApplicationSubmissionResponse,
  CurrentMentorApplicationResponse,
} from '../types/mentor.types';

export const mentorApi = {
  submitApplication: (payload: SubmitMentorApplicationRequest) => {
    return apiClient.post<MentorApplicationSubmissionResponse>(
       '/api/v1/mentor/applications',
       payload
     );
   },
  resubmitApplication: (payload: SubmitMentorApplicationRequest) => {
    return apiClient.post<MentorApplicationSubmissionResponse>(
      '/api/v1/mentor/applications/resubmit',
      payload
    );
  },
  getCurrentApplication: () => {
    return apiClient.get<CurrentMentorApplicationResponse>(
      '/api/v1/mentor/applications/me'
    );
  },
};
