'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Select } from '@/shared/components/ui/select';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Badge } from '@/shared/components/ui/badge';
import { SchoolSelect } from '@/features/master-data/components/school-select';
import { MajorGroupSelect } from '@/features/master-data/components/major-group-select';
import { useUserProfile } from '../hooks/use-user-profile';
import { UserType, USER_TYPE_LABELS, USER_TYPE_ORDER } from '@/shared/types/domain-enums';
import { parseError } from '@/core/errors/error-handler';
import { CheckCircle2, AlertCircle, ShieldAlert, UserCheck } from 'lucide-react';

export function ProfileForm() {
  const { profile, isLoading, isUpdating, updateProfile } = useUserProfile();

  const [fullName, setFullName] = useState('');
  const [bio, setBio] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [schoolId, setSchoolId] = useState('');
  const [majorGroupCode, setMajorGroupCode] = useState('');
  const [customMajorName, setCustomMajorName] = useState('');
  const [newUserType, setNewUserType] = useState<UserType | ''>('');

  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string; fieldErrors?: Record<string, string> } | null>(null);

  // Sync state when profile is loaded
  useEffect(() => {
    if (profile) {
      setFullName(profile.fullName || '');
      setBio(profile.bio || '');
      setAvatarUrl(profile.avatarUrl || '');
      setSchoolId(profile.school?.id || '');
      setMajorGroupCode(profile.majorGroup?.code || '');
      setCustomMajorName(profile.customMajorName || '');
      setNewUserType(profile.userType);
    }
  }, [profile]);

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <p className="text-sm text-slate-500">Đang tải thông tin hồ sơ...</p>
      </div>
    );
  }

  const currentUserType = profile?.userType || UserType.SCHOOL_STUDENT;
  const currentOrder = USER_TYPE_ORDER[currentUserType];

  // Danh sách các lựa chọn UserType với validation cấm hạ cấp
  const userTypeOptions = [
    {
      value: UserType.SCHOOL_STUDENT,
      label: `${USER_TYPE_LABELS.SCHOOL_STUDENT} ${currentOrder > 0 ? '(Không thể hạ cấp)' : ''}`,
      disabled: currentOrder > 0,
    },
    {
      value: UserType.UNIVERSITY_STUDENT,
      label: `${USER_TYPE_LABELS.UNIVERSITY_STUDENT} ${currentOrder > 1 ? '(Không thể hạ cấp)' : ''}`,
      disabled: currentOrder > 1,
    },
    {
      value: UserType.WORKING_PROFESSIONAL,
      label: USER_TYPE_LABELS.WORKING_PROFESSIONAL,
      disabled: false,
    },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    try {
      await updateProfile({
        fullName: fullName.trim() || undefined,
        bio: bio.trim() || undefined,
        avatarUrl: avatarUrl.trim() || undefined,
        schoolId: schoolId || undefined,
        majorGroupCode: majorGroupCode || undefined,
        customMajorName: customMajorName.trim() || undefined,
        newUserType: newUserType ? (newUserType as UserType) : undefined,
      });

      setFeedback({
        type: 'success',
        message: 'Cập nhật thông tin hồ sơ thành công!',
      });
    } catch (err) {
      const parsed = parseError(err);
      setFeedback({
        type: 'error',
        message: `${parsed.title}: ${parsed.message}`,
        fieldErrors: parsed.fieldErrors,
      });
    }
  };

  return (
    <Card className="border-slate-200 dark:border-slate-800 shadow-sm">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-lg font-bold">Hồ sơ cá nhân & Thăng hạng UserType</CardTitle>
            <CardDescription className="text-xs mt-1">
              Đồng bộ dữ liệu trực tiếp với Spring Boot User Domain & Master Data (TT 74/2026)
            </CardDescription>
          </div>
          <Badge variant="info">
            {profile?.userType ? USER_TYPE_LABELS[profile.userType] : 'Chưa phân loại'}
          </Badge>
        </div>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="rounded-lg bg-blue-50/70 p-3 text-xs text-blue-900 dark:bg-blue-950/40 dark:text-blue-200 border border-blue-200/50">
            <div className="flex items-start gap-2">
              <ShieldAlert className="h-4 w-4 shrink-0 text-blue-600 dark:text-blue-400 mt-0.5" />
              <div>
                <span className="font-semibold">Quy tắc thăng hạng cấp bậc một chiều: </span>
                Hệ thống chỉ cho phép tăng cấp theo lộ trình: Học sinh THPT &rarr; Sinh viên Đại học &rarr; Người đi làm. Cấm hạ cấp ngược lại. Thăng hạng lên người đi làm không đồng nghĩa tự động được cấp quyền Mentor.
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="Họ và tên đầy đủ"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn An"
              error={feedback?.fieldErrors?.fullName}
            />

            <Select
              label="Cấp bậc người dùng (User Type)"
              value={newUserType}
              onChange={(e) => setNewUserType(e.target.value as UserType)}
              options={userTypeOptions}
              error={feedback?.fieldErrors?.newUserType}
              helperText="Chỉ có thể giữ nguyên hoặc thăng hạng."
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <SchoolSelect
              value={schoolId}
              onChange={setSchoolId}
              error={feedback?.fieldErrors?.schoolId}
            />

            <MajorGroupSelect
              value={majorGroupCode}
              onChange={setMajorGroupCode}
              error={feedback?.fieldErrors?.majorGroupCode}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="Chuyên ngành cụ thể (Tự nhập)"
              value={customMajorName}
              onChange={(e) => setCustomMajorName(e.target.value)}
              placeholder="Kỹ thuật phần mềm / Trí tuệ nhân tạo"
              error={feedback?.fieldErrors?.customMajorName}
              helperText="Tên chuyên ngành đào tạo cụ thể theo chương trình học của bạn."
            />

            <Input
              label="Đường dẫn ảnh đại diện (URL)"
              value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              placeholder="https://cdn.mentify.vn/avatars/user.png"
              error={feedback?.fieldErrors?.avatarUrl}
            />
          </div>

          <div className="w-full space-y-1.5">
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-200">
              Giới thiệu bản thân (Bio)
            </label>
            <textarea
              rows={3}
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Chia sẻ ngắn gọn về sở thích, định hướng học tập hoặc kinh nghiệm làm việc của bạn..."
              className="flex w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 transition-colors placeholder:text-slate-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            />
            {feedback?.fieldErrors?.bio && (
              <p className="text-xs text-rose-500">{feedback.fieldErrors.bio}</p>
            )}
          </div>

          {feedback && (
            <div
              className={`flex items-start gap-2 rounded-lg p-3 text-xs ${
                feedback.type === 'success'
                  ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300'
                  : 'bg-rose-50 text-rose-800 dark:bg-rose-950/40 dark:text-rose-300'
              }`}
            >
              {feedback.type === 'success' ? (
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
              ) : (
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
              )}
              <span>{feedback.message}</span>
            </div>
          )}

          <div className="flex justify-end pt-2">
            <Button type="submit" isLoading={isUpdating} className="w-full sm:w-auto">
              <UserCheck className="mr-2 h-4 w-4" />
              Lưu thay đổi hồ sơ
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
