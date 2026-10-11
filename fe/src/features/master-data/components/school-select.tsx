'use client';

import React from 'react';
import { Select } from '@/shared/components/ui/select';
import { useSchools } from '../hooks/use-master-data';

export interface SchoolSelectProps {
  value?: string;
  onChange: (value: string) => void;
  label?: string;
  error?: string;
  helperText?: string;
  required?: boolean;
}

export function SchoolSelect({
  value,
  onChange,
  label = 'Trường Đại học / Học viện',
  error,
  helperText,
  required = false,
}: SchoolSelectProps) {
  const { data, isLoading } = useSchools({ size: 100 });

  const schools = data?.data || [];

  const options = [
    { value: '', label: isLoading ? 'Đang tải danh mục trường...' : '-- Chọn trường đại học --' },
    ...schools.map((s) => ({
      value: s.id,
      label: `${s.name} (${s.commonAbbreviation || s.admissionCode}) - ${s.region}`,
    })),
  ];

  return (
    <Select
      label={label}
      value={value || ''}
      onChange={(e) => onChange(e.target.value)}
      options={options}
      error={error}
      helperText={helperText}
      required={required}
      disabled={isLoading}
    />
  );
}
