'use client';

import React from 'react';
import { Select } from '@/shared/components/ui/select';
import { useMajorGroups } from '../hooks/use-master-data';

export interface MajorGroupSelectProps {
  value?: string; // 4-digit code e.g. "4801"
  onChange: (value: string) => void;
  label?: string;
  error?: string;
  helperText?: string;
  required?: boolean;
}

export function MajorGroupSelect({
  value,
  onChange,
  label = 'Nhóm ngành chuẩn (Thông tư 74/2026/TT-BGDĐT)',
  error,
  helperText,
  required = false,
}: MajorGroupSelectProps) {
  const { data: majorGroups = [], isLoading } = useMajorGroups({ isActive: true });

  const options = [
    { value: '', label: isLoading ? 'Đang tải nhóm ngành...' : '-- Chọn nhóm ngành chuẩn --' },
    ...majorGroups.map((m) => ({
      value: m.code,
      label: `[Mã ${m.code}] ${m.name} (${m.description})`,
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
