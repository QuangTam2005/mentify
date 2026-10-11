import type { MajorGroupFilterParams, SchoolFilterParams } from './types/master-data.types';

export const masterDataKeys = {
  all: ['master-data'] as const,
  schools: (params?: SchoolFilterParams) =>
    params !== undefined
      ? ([...masterDataKeys.all, 'schools', params] as const)
      : ([...masterDataKeys.all, 'schools'] as const),
  majorGroups: (params?: MajorGroupFilterParams) =>
    params !== undefined
      ? ([...masterDataKeys.all, 'major-groups', params] as const)
      : ([...masterDataKeys.all, 'major-groups'] as const),
};
