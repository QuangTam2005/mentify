import type { Metadata } from 'next';
import { MentorApplyContainer } from '@/features/mentor/components/mentor-apply-container';

export const metadata: Metadata = {
  title: 'Đăng ký trở thành Mentor | Mentify',
  description: 'Gửi đơn đăng ký và kiểm tra trạng thái ứng tuyển Mentor trên nền tảng Mentify',
};

export default function MentorApplyPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8 space-y-6">
      <MentorApplyContainer />
    </div>
  );
}
