'use client';

import { useQuery } from '@tanstack/react-query';
import { masterDataApi } from '../api/master-data-api';
import type { MajorGroupFilterParams, SchoolFilterParams } from '../types/master-data.types';
import { masterDataKeys } from '../query-keys';

export const MASTER_DATA_QUERY_KEYS = masterDataKeys;

export function useSchools(params?: SchoolFilterParams) {
  return useQuery({
    queryKey: masterDataKeys.schools(params),
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
    queryKey: masterDataKeys.majorGroups(params),
    queryFn: async () => {
      const res = await masterDataApi.getMajorGroups(params);
      return res.data;
    },
    staleTime: 30 * 60 * 1000, // 30 phút cache
    gcTime: 60 * 60 * 1000,
  });
}
