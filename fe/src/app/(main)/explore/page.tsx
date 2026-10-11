'use client';

import React, { useState } from 'react';
import { useSchools, useMajorGroups } from '@/features/master-data/hooks/use-master-data';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Input } from '@/shared/components/ui/input';
import { Select } from '@/shared/components/ui/select';
import { Badge } from '@/shared/components/ui/badge';
import { Button } from '@/shared/components/ui/button';
import { Spinner } from '@/shared/components/ui/spinner';
import { CampusType, CAMPUS_TYPE_LABELS } from '@/shared/types/domain-enums';
import { GraduationCap, BookOpen, Layers, ChevronLeft, ChevronRight } from 'lucide-react';

export default function ExplorePage() {
  const [activeTab, setActiveTab] = useState<'schools' | 'majors'>('schools');

  // Bộ lọc trường học
  const [schoolSearch, setSchoolSearch] = useState('');
  const [schoolRegion, setSchoolRegion] = useState('');
  const [schoolCampusType, setSchoolCampusType] = useState<string>('');
  const [schoolPage, setSchoolPage] = useState(0);

  // Bộ lọc nhóm ngành
  const [majorSearch, setMajorSearch] = useState('');

  const { data: schoolsData, isLoading: isLoadingSchools } = useSchools({
    search: schoolSearch || undefined,
    region: schoolRegion || undefined,
    campusType: schoolCampusType ? (schoolCampusType as CampusType) : undefined,
    page: schoolPage,
    size: 20,
  });

  const { data: majorGroups = [], isLoading: isLoadingMajors } = useMajorGroups({
    search: majorSearch || undefined,
    isActive: true,
  });

  const schools = schoolsData?.data || [];
  const meta = schoolsData?.meta;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8 space-y-8">
      {/* Tiêu đề trang */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <GraduationCap className="h-8 w-8 text-primary-600" />
            Khám phá Danh mục Đào tạo Chuẩn
          </h1>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            Tra cứu danh mục các Trường Đại học và Nhóm ngành theo quy định mới nhất của Thông tư 74/2026/TT-BGDĐT.
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
          <button
            onClick={() => setActiveTab('schools')}
            className={`flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'schools'
                ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            <BookOpen className="h-4 w-4" />
            Trường Đại học ({meta?.totalItems ?? '...'})
          </button>
          <button
            onClick={() => setActiveTab('majors')}
            className={`flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'majors'
                ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-900 dark:text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            <Layers className="h-4 w-4" />
            Nhóm ngành Thông tư 74/2026 ({majorGroups.length})
          </button>
        </div>
      </div>

      {/* Tab 1: Danh sách Trường Đại học */}
      {activeTab === 'schools' && (
        <div className="space-y-6">
          <Card>
            <CardContent className="p-4">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <Input
                  placeholder="Tìm theo tên trường, mã tuyển sinh, viết tắt (VD: Bách khoa, HCMUT)..."
                  value={schoolSearch}
                  onChange={(e) => {
                    setSchoolSearch(e.target.value);
                    setSchoolPage(0);
                  }}
                />

                <Select
                  value={schoolRegion}
                  onChange={(e) => {
                    setSchoolRegion(e.target.value);
                    setSchoolPage(0);
                  }}
                  options={[
                    { value: '', label: '-- Tất cả Vùng miền --' },
                    { value: 'TP.HCM', label: 'Thành phố Hồ Chí Minh' },
                    { value: 'Hà Nội', label: 'Thủ đô Hà Nội' },
                    { value: 'Cần Thơ', label: 'Khu vực Cần Thơ - ĐBSCL' },
                    { value: 'Đà Nẵng - Huế', label: 'Khu vực Đà Nẵng - Huế' },
                    { value: 'Miền Bắc ngoài Hà Nội', label: 'Miền Bắc (ngoài Hà Nội)' },
                  ]}
                />

                <Select
                  value={schoolCampusType}
                  onChange={(e) => {
                    setSchoolCampusType(e.target.value);
                    setSchoolPage(0);
                  }}
                  options={[
                    { value: '', label: '-- Tất cả Loại hình Cơ sở --' },
                    { value: CampusType.MAIN_CAMPUS, label: CAMPUS_TYPE_LABELS.MAIN_CAMPUS },
                    { value: CampusType.BRANCH_CAMPUS, label: CAMPUS_TYPE_LABELS.BRANCH_CAMPUS },
                  ]}
                />
              </div>
            </CardContent>
          </Card>

          {isLoadingSchools ? (
            <div className="py-20 text-center">
              <Spinner size="lg" />
              <p className="mt-3 text-xs text-slate-500">Đang tra cứu danh mục trường từ hệ thống...</p>
            </div>
          ) : schools.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 p-12 text-center text-sm text-slate-500">
              Không tìm thấy trường đại học nào phù hợp với bộ lọc tìm kiếm hiện tại.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {schools.map((school) => (
                <Card key={school.id} className="hover:border-primary-300 transition-colors">
                  <CardHeader className="pb-2">
                    <div className="flex items-start justify-between gap-2">
                      <Badge variant="outline" className="font-mono text-[11px] font-bold">
                        {school.code}
                      </Badge>
                      <Badge variant={school.campusType === 'MAIN_CAMPUS' ? 'info' : 'secondary'} className="text-[10px]">
                        {CAMPUS_TYPE_LABELS[school.campusType] || school.campusType}
                      </Badge>
                    </div>
                    <CardTitle className="text-base font-bold leading-snug pt-1">
                      {school.name}
                    </CardTitle>
                    <CardDescription className="text-xs">
                      {school.shortName} {school.commonAbbreviation ? `(${school.commonAbbreviation})` : ''}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="text-xs text-slate-500 space-y-1">
                    <div className="flex items-center justify-between">
                      <span>Vùng miền:</span>
                      <span className="font-semibold text-slate-700 dark:text-slate-300">{school.region}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Mã tuyển sinh:</span>
                      <span className="font-mono font-semibold text-primary-600">{school.admissionCode}</span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}

          {/* Phân trang */}
          {meta && meta.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-slate-200 pt-4 dark:border-slate-800">
              <span className="text-xs text-slate-500">
                Trang {meta.page + 1} trên {meta.totalPages} (Tổng cộng {meta.totalItems} trường)
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={schoolPage <= 0}
                  onClick={() => setSchoolPage((p) => Math.max(0, p - 1))}
                >
                  <ChevronLeft className="h-4 w-4 mr-1" /> Trang trước
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={schoolPage >= meta.totalPages - 1}
                  onClick={() => setSchoolPage((p) => p + 1)}
                >
                  Trang sau <ChevronRight className="h-4 w-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Danh mục Nhóm ngành Thông tư 74/2026 */}
      {activeTab === 'majors' && (
        <div className="space-y-6">
          <Card>
            <CardContent className="p-4">
              <Input
                placeholder="Tìm kiếm theo mã 4 số, tên nhóm ngành đào tạo (VD: 4801, Máy tính, Công nghệ thông tin)..."
                value={majorSearch}
                onChange={(e) => setMajorSearch(e.target.value)}
              />
            </CardContent>
          </Card>

          {isLoadingMajors ? (
            <div className="py-20 text-center">
              <Spinner size="lg" />
              <p className="mt-3 text-xs text-slate-500">Đang tải danh mục Thông tư 74/2026...</p>
            </div>
          ) : majorGroups.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 p-12 text-center text-sm text-slate-500">
              Không tìm thấy nhóm ngành nào phù hợp với từ khóa tìm kiếm.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {majorGroups.map((major) => (
                <Card key={major.id} className="hover:border-primary-300 transition-colors">
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                      <span className="rounded bg-primary-100 px-2 py-0.5 font-mono text-xs font-black text-primary-800 dark:bg-primary-950 dark:text-primary-300">
                        Mã chuẩn: {major.code}
                      </span>
                      <Badge variant="success" className="text-[10px]">
                        {major.regulationVersion}
                      </Badge>
                    </div>
                    <CardTitle className="text-base font-bold pt-2">{major.name}</CardTitle>
                    <CardDescription className="text-xs">{major.description}</CardDescription>
                  </CardHeader>
                  <CardContent className="text-xs text-slate-500">
                    <div className="flex items-center justify-between">
                      <span>Lĩnh vực đào tạo:</span>
                      <span className="font-semibold text-slate-700 dark:text-slate-300">
                        Mã lĩnh vực {major.fieldCode}
                      </span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
