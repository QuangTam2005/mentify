import { apiClient } from '@/core/http/api-client';
import type { ApiResponse } from '@/core/types/api';
import type {
  MajorGroup,
  MajorGroupFilterParams,
  School,
  SchoolFilterParams,
} from '../types/master-data.types';

export const masterDataApi = {
  getSchools: (params?: SchoolFilterParams) => {
    return apiClient.get<School[]>('/api/v1/master-data/schools', {
      params: params as Record<string, string | number | boolean>,
    });
  },

  getMajorGroups: (params?: MajorGroupFilterParams) => {
    return apiClient.get<MajorGroup[]>('/api/v1/master-data/major-groups', {
      params: params as Record<string, string | number | boolean>,
    });
  },
};
