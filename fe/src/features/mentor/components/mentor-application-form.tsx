'use client';

import React, { useState, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/components/ui/card';
import { Badge } from '@/shared/components/ui/badge';
import { SchoolSelect } from '@/features/master-data/components/school-select';
import { MajorGroupSelect } from '@/features/master-data/components/major-group-select';
import { useAuth } from '@/features/auth/hooks/use-auth';
import { useSubmitMentorApplication } from '../hooks/use-mentor-application';
import type {
  SubmitMentorApplicationRequest,
  MentorApplicationSubmissionResponse,
} from '../types/mentor.types';
import { parseError } from '@/core/errors/error-handler';
import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Info,
  Send,
  ArrowLeft,
  FileCheck2,
  Clock,
  Sparkles,
} from 'lucide-react';

export function MentorApplicationForm() {
  const router = useRouter();
  const { user } = useAuth();
  const submitMutation = useSubmitMentorApplication();

  // Form input states
  const [schoolId, setSchoolId] = useState('');
  const [majorGroupCode, setMajorGroupCode] = useState('');
  const [customMajorName, setCustomMajorName] = useState('');
  const [bio, setBio] = useState('');
  const [expertiseAreas, setExpertiseAreas] = useState('');
  const [defaultMeetingLink, setDefaultMeetingLink] = useState('');

  // Validation & error states
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<{ title: string; message: string } | null>(null);
  const [submissionSuccess, setSubmissionSuccess] =
    useState<MentorApplicationSubmissionResponse | null>(null);

  // Validate form inputs
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    const trimmedBio = bio.trim();
    if (!trimmedBio) {
      errors.bio = 'Vui lòng nhập tiểu sử và giới thiệu bản thân.';
    } else if (trimmedBio.length > 1000) {
      errors.bio = 'Tiểu sử không được vượt quá 1000 ký tự.';
    }

    const trimmedExpertise = expertiseAreas.trim();
    if (!trimmedExpertise) {
      errors.expertiseAreas = 'Vui lòng nhập lĩnh vực chuyên môn và kỹ năng hướng dẫn.';
    } else if (trimmedExpertise.length > 500) {
      errors.expertiseAreas = 'Lĩnh vực chuyên môn không được vượt quá 500 ký tự.';
    }

    if (customMajorName.trim().length > 255) {
      errors.customMajorName = 'Tên ngành đào tạo không được vượt quá 255 ký tự.';
    }

    if (majorGroupCode.trim().length > 4) {
      errors.majorGroupCode = 'Mã nhóm ngành không được vượt quá 4 ký tự.';
    }

    const trimmedLink = defaultMeetingLink.trim();
    if (trimmedLink) {
      if (trimmedLink.length > 512) {
        errors.defaultMeetingLink = 'Liên kết phòng họp không được vượt quá 512 ký tự.';
      } else {
        try {
          const parsedUrl = new URL(trimmedLink);
          if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
            errors.defaultMeetingLink = 'Liên kết phòng họp phải bắt đầu bằng http:// hoặc https://';
          }
        } catch {
          errors.defaultMeetingLink =
            'Định dạng liên kết phòng họp không hợp lệ (ví dụ: https://meet.google.com/abc-xyz).';
        }
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const isSubmittingRef = useRef(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitMutation.isPending || isSubmittingRef.current) {
      return;
    }
    setGeneralError(null);

    if (!validateForm()) {
      return;
    }

    const payload: SubmitMentorApplicationRequest = {
      schoolId: schoolId.trim() || undefined,
      majorGroupCode: majorGroupCode.trim() || undefined,
      customMajorName: customMajorName.trim() || undefined,
      bio: bio.trim(),
      expertiseAreas: expertiseAreas.trim(),
      defaultMeetingLink: defaultMeetingLink.trim() || undefined,
    };

    isSubmittingRef.current = true;
    try {
      const response = await submitMutation.mutateAsync(payload);
      setSubmissionSuccess(response);
    } catch (err: unknown) {
      const parsed = parseError(err);
      if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
        setFieldErrors((prev) => ({ ...prev, ...parsed.fieldErrors }));
      }
      setGeneralError({
        title: parsed.title,
        message: parsed.message,
      });
      // Important: Input values are strictly preserved on error
    } finally {
      isSubmittingRef.current = false;
    }
  };

  // SUCCESS STATE VIEW
  if (submissionSuccess) {
    return (
      <Card className="border-border shadow-md">
        <CardHeader className="text-center pb-4">
          <div className="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400">
            <CheckCircle2 className="h-10 w-10" />
          </div>
          <CardTitle className="text-2xl font-bold text-foreground">
            Đơn ứng tuyển đã được tiếp nhận!
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground mt-1">
            Hồ sơ của bạn đã được ghi nhận thành công và chuyển vào quy trình xét duyệt của Mentify.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {/* Important Status Notice */}
          <div className="rounded-xl border border-amber-200/80 bg-amber-50/80 p-4 text-xs text-amber-900 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-200">
            <div className="flex items-start gap-3">
              <Clock className="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
              <div className="space-y-1">
                <p className="font-semibold text-sm">Trạng thái hồ sơ: Chờ xét duyệt (PENDING)</p>
                <p>
                  Đơn ứng tuyển của bạn hiện đang chờ thẩm định từ Ban quản trị Mentify. Thời gian xem
                  xét thường từ 1 đến 3 ngày làm việc.
                </p>
                <p className="font-medium text-amber-800 dark:text-amber-300 pt-1">
                  * Lưu ý quan trọng: Việc nộp đơn thành công KHÔNG đồng nghĩa với việc bạn đã được phê
                  duyệt làm Mentor ngay lập tức. Sau khi hồ sơ được duyệt, tài khoản của bạn mới được kích
                  hoạt quyền cố vấn và nhận lịch hẹn.
                </p>
              </div>
            </div>
          </div>

          {/* Submission Details */}
          <div className="rounded-xl border border-border bg-muted/40 p-4 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-border text-xs">
              <span className="text-muted-foreground">Trạng thái đơn:</span>
              <Badge variant="warning">{submissionSuccess.applicationStatus}</Badge>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-border text-xs">
              <span className="text-muted-foreground">Mã đơn ứng tuyển:</span>
              <span className="font-mono text-foreground font-medium">
                {submissionSuccess.mentorApplicationId}
              </span>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-border text-xs">
              <span className="text-muted-foreground">Mã hồ sơ Mentor:</span>
              <span className="font-mono text-foreground font-medium">
                {submissionSuccess.mentorProfileId}
              </span>
            </div>
            <div className="flex items-center justify-between pb-2 border-b border-border text-xs">
              <span className="text-muted-foreground">Lần nộp hồ sơ:</span>
              <span className="text-foreground font-medium">
                Lần thứ {submissionSuccess.attemptNumber}
              </span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted-foreground">Thời gian nộp:</span>
              <span className="text-foreground font-medium">
                {new Date(submissionSuccess.submittedAt).toLocaleString('vi-VN')}
              </span>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-col sm:flex-row gap-3 pt-2 justify-center">
            <Button
              type="button"
              onClick={() => router.push('/profile')}
              className="w-full sm:w-auto"
            >
              <FileCheck2 className="mr-2 h-4 w-4" />
              Về trang hồ sơ cá nhân
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push('/explore')}
              className="w-full sm:w-auto"
            >
              <Sparkles className="mr-2 h-4 w-4" />
              Khám phá Mentify
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Check if current user is ineligible high school student
  const isSchoolStudent = user?.userType === 'SCHOOL_STUDENT';

  return (
    <Card className="border-border shadow-sm">
      <CardHeader>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-xl font-bold text-foreground">
              Đăng ký trở thành Mentor
            </h2>
            <CardDescription className="text-xs text-muted-foreground mt-1">
              Chia sẻ kinh nghiệm học tập, định hướng sự nghiệp và đồng hành cùng các bạn mentee trên Mentify
            </CardDescription>
          </div>
          <Badge variant="outline" className="w-fit">
            Giai đoạn nộp đơn
          </Badge>
        </div>
      </CardHeader>

      <CardContent>
        {/* Policy and workflow notice */}
        <div className="mb-6 rounded-xl border border-primary/20 bg-primary/5 p-4 text-xs text-foreground space-y-2">
          <div className="flex items-start gap-2.5">
            <Info className="h-4 w-4 shrink-0 text-primary mt-0.5" />
            <div>
              <p className="font-semibold text-primary">Quy trình đăng ký và xét duyệt:</p>
              <p className="text-muted-foreground mt-0.5 leading-relaxed">
                Đơn đăng ký của bạn sẽ được chuyển đến Ban quản trị Mentify để xét duyệt. Quá trình xét
                duyệt sẽ đánh giá sự phù hợp về lĩnh vực chuyên môn và kinh nghiệm thực tế.
                Sau khi được chấp thuận, hồ sơ của bạn sẽ xuất hiện trên trang Danh sách Mentor.
              </p>
            </div>
          </div>
        </div>

        {/* Warning if user is SCHOOL_STUDENT */}
        {isSchoolStudent && (
          <div
            role="status"
            className="mb-6 rounded-xl border border-amber-300 bg-amber-50 p-4 text-xs text-amber-900 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-200"
          >
            <div className="flex items-start gap-2.5">
              <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
              <div>
                <p className="font-semibold">Cảnh báo điều kiện ứng tuyển:</p>
                <p className="mt-0.5 leading-relaxed">
                  Tài khoản của bạn hiện là <strong>Học sinh THPT</strong>. Theo quy định hệ thống,
                  chỉ <strong>Sinh viên Đại học/Cao đẳng</strong> hoặc <strong>Người đi làm</strong> mới có thể
                  đăng ký làm Mentor. Bạn có thể cập nhật thông tin cấp bậc tại{' '}
                  <Link href="/profile" className="underline font-semibold hover:text-amber-950 dark:hover:text-amber-100">
                    Hồ sơ cá nhân
                  </Link>{' '}
                  nếu đã tốt nghiệp hoặc đi làm.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Top General Error Alert */}
        {generalError && (
          <div
            role="alert"
            className="mb-6 flex items-start gap-3 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-xs text-destructive animate-in fade-in-50"
          >
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
            <div className="space-y-0.5">
              <p className="font-semibold">{generalError.title}</p>
              <p>{generalError.message}</p>
            </div>
          </div>
        )}

        <form
          noValidate
          onSubmit={handleSubmit}
          className="space-y-6"
          aria-busy={submitMutation.isPending}
        >
          {/* Education & Master Data Section */}
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-foreground border-b border-border pb-2">
              1. Thông tin học vấn & Chuyên môn đào tạo
            </h3>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <SchoolSelect
                value={schoolId}
                onChange={setSchoolId}
                error={fieldErrors.schoolId}
                label="Trường Đại học / Học viện"
                helperText="Chọn cơ sở đào tạo của bạn (nếu có)"
              />

              <MajorGroupSelect
                value={majorGroupCode}
                onChange={setMajorGroupCode}
                error={fieldErrors.majorGroupCode}
                label="Nhóm ngành chuẩn (TT 74/2026/TT-BGDĐT)"
                helperText="Chọn nhóm ngành chuẩn quốc gia phù hợp nhất"
              />
            </div>

            <div>
              <Input
                id="custom-major-name"
                label="Chuyên ngành cụ thể (Tùy chọn)"
                value={customMajorName}
                onChange={(e) => setCustomMajorName(e.target.value)}
                maxLength={255}
                placeholder="Ví dụ: Khoa học máy tính, Kỹ thuật phần mềm, Marketing..."
                error={fieldErrors.customMajorName}
                helperText="Tên chuyên ngành đào tạo cụ thể theo chương trình của bạn (tối đa 255 ký tự)."
              />
            </div>
          </div>

          {/* Mentoring Capabilities Section */}
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-foreground border-b border-border pb-2">
              2. Kỹ năng & Giới thiệu bản thân
            </h3>

            {/* Expertise Areas */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="mentor-expertise"
                  className="block text-xs font-semibold text-foreground"
                >
                  Lĩnh vực chuyên môn & Kỹ năng hướng dẫn <span className="text-destructive">*</span>
                </label>
                <span
                  className={`text-[11px] font-mono ${
                    expertiseAreas.length > 500 ? 'text-destructive font-bold' : 'text-muted-foreground'
                  }`}
                >
                  {expertiseAreas.length}/500 ký tự
                </span>
              </div>
              <textarea
                id="mentor-expertise"
                rows={3}
                aria-required="true"
                value={expertiseAreas}
                onChange={(e) => setExpertiseAreas(e.target.value)}
                placeholder="Ví dụ: Lập trình Java/Spring Boot, Thiết kế hệ thống, Ôn thi Đại học khối A00, Định hướng nghề nghiệp ngành IT, Chuẩn bị CV & Phỏng vấn..."
                className={`flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 ${
                  fieldErrors.expertiseAreas ? 'border-destructive focus-visible:ring-destructive' : ''
                }`}
                aria-invalid={!!fieldErrors.expertiseAreas}
                aria-describedby={fieldErrors.expertiseAreas ? 'expertise-error' : undefined}
                disabled={submitMutation.isPending}
              />
              {fieldErrors.expertiseAreas ? (
                <p id="expertise-error" className="text-xs text-destructive animate-in fade-in-50">
                  {fieldErrors.expertiseAreas}
                </p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Liệt kê các kỹ năng thế mạnh và nội dung bạn có thể hỗ trợ học viên (tối đa 500 ký tự).
                </p>
              )}
            </div>

            {/* Bio */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="mentor-bio"
                  className="block text-xs font-semibold text-foreground"
                >
                  Tiểu sử & Giới thiệu bản thân (Bio) <span className="text-destructive">*</span>
                </label>
                <span
                  className={`text-[11px] font-mono ${
                    bio.length > 1000 ? 'text-destructive font-bold' : 'text-muted-foreground'
                  }`}
                >
                  {bio.length}/1000 ký tự
                </span>
              </div>
              <textarea
                id="mentor-bio"
                rows={5}
                aria-required="true"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Giới thiệu về quá trình học tập, kinh nghiệm làm việc, phong cách cố vấn và lý do bạn muốn đồng hành cùng các mentee..."
                className={`flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 ${
                  fieldErrors.bio ? 'border-destructive focus-visible:ring-destructive' : ''
                }`}
                aria-invalid={!!fieldErrors.bio}
                aria-describedby={fieldErrors.bio ? 'bio-error' : undefined}
                disabled={submitMutation.isPending}
              />
              {fieldErrors.bio ? (
                <p id="bio-error" className="text-xs text-destructive animate-in fade-in-50">
                  {fieldErrors.bio}
                </p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Chia sẻ chi tiết giúp học viên hiểu rõ hơn về bạn (tối đa 1000 ký tự).
                </p>
              )}
            </div>
          </div>

          {/* Meeting link */}
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-foreground border-b border-border pb-2">
              3. Thiết lập kết nối
            </h3>

            <div>
              <Input
                id="default-meeting-link"
                label="Liên kết phòng họp trực tuyến mặc định (Tùy chọn)"
                value={defaultMeetingLink}
                onChange={(e) => setDefaultMeetingLink(e.target.value)}
                maxLength={512}
                placeholder="https://meet.google.com/abc-xyz-123"
                error={fieldErrors.defaultMeetingLink}
                helperText="Google Meet, Zoom, MS Teams... dùng cho các buổi trao đổi định hướng với học viên."
                disabled={submitMutation.isPending}
              />
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex flex-col-reverse sm:flex-row items-center justify-end gap-3 pt-4 border-t border-border">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push('/profile')}
              disabled={submitMutation.isPending}
              className="w-full sm:w-auto"
            >
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại
            </Button>
            <Button
              type="submit"
              isLoading={submitMutation.isPending}
              disabled={submitMutation.isPending}
              className="w-full sm:w-auto"
            >
              <Send className="mr-2 h-4 w-4" />
              {submitMutation.isPending ? 'Đang gửi hồ sơ...' : 'Gửi đơn ứng tuyển'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
