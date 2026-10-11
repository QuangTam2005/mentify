import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useSchools, useMajorGroups } from './use-master-data';
import { masterDataApi } from '../api/master-data-api';
import { masterDataKeys } from '../query-keys';
import { clearAccessToken } from '@/features/auth/store/access-token';
import { CampusType } from '@/shared/types/domain-enums';
import type { School, MajorGroup } from '../types/master-data.types';

vi.mock('../api/master-data-api', () => ({
  masterDataApi: {
    getSchools: vi.fn(),
    getMajorGroups: vi.fn(),
  },
}));

const mockSchools: School[] = [
  {
    id: 'sch-1',
    code: 'BKA',
    admissionCode: 'BKA',
    name: 'Đại học Bách Khoa Hà Nội',
    shortName: 'Bách Khoa',
    commonAbbreviation: 'HUST',
    campusType: CampusType.MAIN_CAMPUS,
    parentSchoolId: null,
    region: 'NORTH',
    status: 'ACTIVE',
    dataVersion: '1.0',
  },
];

const mockMajorGroups: MajorGroup[] = [
  {
    id: 'mg-1',
    code: '4801',
    name: 'Máy tính và Công nghệ thông tin',
    fieldCode: '48',
    regulationVersion: 'TT_74_2026',
    description: 'Khối ngành CNTT',
    isActive: true,
  },
];

describe('useMasterData Hooks - Public Reference Queries', () => {
  let queryClient: QueryClient;

  function createWrapper() {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Infinity },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
  }

  beforeEach(() => {
    vi.clearAllMocks();
    clearAccessToken(); // Master data must be public and never require a token
  });

  it('Scenario 7 & 10: useSchools uses masterDataKeys.schools and executes publicly without token', async () => {
    vi.mocked(masterDataApi.getSchools).mockResolvedValueOnce({
      success: true,
      data: mockSchools,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-schools-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useSchools({ region: 'NORTH' }), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(masterDataApi.getSchools).toHaveBeenCalledWith({ region: 'NORTH' });
    const cachedData = queryClient.getQueryData(masterDataKeys.schools({ region: 'NORTH' }));
    expect(cachedData).toEqual({
      success: true,
      data: mockSchools,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-schools-1',
    });
  });

  it('Scenario 7 & 10: useMajorGroups uses masterDataKeys.majorGroups and executes publicly', async () => {
    vi.mocked(masterDataApi.getMajorGroups).mockResolvedValueOnce({
      success: true,
      data: mockMajorGroups,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-majors-1',
    });

    const wrapper = createWrapper();
    const { result } = renderHook(() => useMajorGroups({ isActive: true }), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(masterDataApi.getMajorGroups).toHaveBeenCalledWith({ isActive: true });
    expect(queryClient.getQueryData(masterDataKeys.majorGroups({ isActive: true }))).toEqual(
      mockMajorGroups
    );
  });
});
