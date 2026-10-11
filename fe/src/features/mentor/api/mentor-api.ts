import { apiClient } from '@/core/http/api-client';
import type {
  SubmitMentorApplicationRequest,
  MentorApplicationSubmissionResponse,
} from '../types/mentor.types';

export const mentorApi = {
  submitApplication: (payload: SubmitMentorApplicationRequest) => {
    return apiClient.post<MentorApplicationSubmissionResponse>(
      '/api/v1/mentor/applications',
      payload
    );
  },
};
