import type { CampusType } from '@/shared/types/domain-enums';

export interface School {
  id: string;
  code: string;
  admissionCode: string;
  name: string;
  shortName: string;
  commonAbbreviation: string;
  campusType: CampusType;
  parentSchoolId: string | null;
  region: string;
  status: string;
  dataVersion: string;
}

export interface MajorGroup {
  id: string;
  code: string; // 4-digit code e.g. "4801"
  name: string;
  fieldCode: string;
  regulationVersion: string; // "TT_74_2026"
  description: string;
  isActive: boolean;
}

export interface SchoolFilterParams {
  search?: string;
  region?: string;
  campusType?: CampusType;
  page?: number;
  size?: number;
}

export interface MajorGroupFilterParams {
  search?: string;
  isActive?: boolean;
}
