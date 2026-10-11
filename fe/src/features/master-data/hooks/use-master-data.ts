'use client';

import { useQuery } from '@tanstack/react-query';
import { masterDataApi } from '../api/master-data-api';
import type { MajorGroupFilterParams, SchoolFilterParams } from '../types/master-data.types';

export const MASTER_DATA_QUERY_KEYS = {
  schools: (params?: SchoolFilterParams) => ['master-data', 'schools', params] as const,
  majorGroups: (params?: MajorGroupFilterParams) => ['master-data', 'major-groups', params] as const,
};

export function useSchools(params?: SchoolFilterParams) {
  return useQuery({
    queryKey: MASTER_DATA_QUERY_KEYS.schools(params),
    queryFn: async () => {
      const res = await masterDataApi.getSchools(params);
      return res;
    },
    staleTime: 30 * 60 * 1000, // 30 phút cache
    gcTime: 60 * 60 * 1000, // 1 giờ trong garbage collector
  });
}

export function useMajorGroups(params?: MajorGroupFilterParams) {
  return useQuery({
    queryKey: MASTER_DATA_QUERY_KEYS.majorGroups(params),
    queryFn: async () => {
      const res = await masterDataApi.getMajorGroups(params);
      return res.data;
    },
    staleTime: 30 * 60 * 1000, // 30 phút cache
    gcTime: 60 * 60 * 1000,
  });
}
