import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { MentorApplicationStatus } from './mentor-application-status';
import { MentorStatus } from '@/shared/types/domain-enums';
import type { CurrentMentorApplicationResponse } from '../types/mentor.types';

const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

const baseApplication: CurrentMentorApplicationResponse = {
  mentorProfileId: 'prof-111-uuid',
  mentorApplicationId: 'app-222-uuid',
  mentorStatus: MentorStatus.PENDING,
  applicationStatus: 'PENDING',
  attemptNumber: 1,
  appliedUserType: 'UNIVERSITY_STUDENT',
  schoolId: 'sch-uuid',
  majorGroupCode: '7480',
  customMajorName: 'Khoa học máy tính',
  bio: 'Tôi mong muốn đồng hành cùng các bạn mentee.',
  acceptingBookings: false,
  submittedAt: '2026-10-11T12:00:00Z',
};

describe('MentorApplicationStatus Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders PENDING application with review notice and snapshot details', () => {
    render(<MentorApplicationStatus application={baseApplication} />);

    expect(screen.getByText('Hồ sơ đang chờ xét duyệt')).toBeInTheDocument();
    expect(screen.getByText('Chờ xét duyệt (PENDING)')).toBeInTheDocument();
    expect(screen.getByText(/Quy trình thẩm định đang diễn ra/i)).toBeInTheDocument();
    expect(screen.getByText(/Lần thứ 1/i)).toBeInTheDocument();
    expect(screen.getByText('Khoa học máy tính')).toBeInTheDocument();
    expect(screen.getByText('app-222-uuid')).toBeInTheDocument();
    expect(screen.getByText(/Tôi mong muốn đồng hành cùng các bạn mentee/i)).toBeInTheDocument();
  });

  it('renders APPROVED application distinguishing acceptingBookings status', () => {
    const approvedApp: CurrentMentorApplicationResponse = {
      ...baseApplication,
      applicationStatus: 'APPROVED',
      mentorStatus: MentorStatus.APPROVED,
      acceptingBookings: true,
    };

    render(<MentorApplicationStatus application={approvedApp} />);

    expect(screen.getByText('Hồ sơ đã được chấp thuận')).toBeInTheDocument();
    expect(screen.getByText('Đã chấp thuận (APPROVED)')).toBeInTheDocument();
    expect(screen.getByText(/sẵn sàng nhận lịch hẹn hướng dẫn/i)).toBeInTheDocument();
  });

  it('renders APPROVED application with unactivated booking status correctly', () => {
    const approvedNotBooking: CurrentMentorApplicationResponse = {
      ...baseApplication,
      applicationStatus: 'APPROVED',
      mentorStatus: MentorStatus.APPROVED,
      acceptingBookings: false,
    };

    render(<MentorApplicationStatus application={approvedNotBooking} />);

    expect(
      screen.getByText(/Việc đơn được duyệt không đồng nghĩa tài khoản tự động mở nhận lịch hẹn/i)
    ).toBeInTheDocument();
  });

  it('renders REJECTED application with future resubmission note and no fake resubmit button', () => {
    const rejectedApp: CurrentMentorApplicationResponse = {
      ...baseApplication,
      applicationStatus: 'REJECTED',
      mentorStatus: MentorStatus.REJECTED,
    };

    render(<MentorApplicationStatus application={rejectedApp} />);

    expect(screen.getByText('Hồ sơ đã bị từ chối')).toBeInTheDocument();
    expect(screen.getByText('Bị từ chối (REJECTED)')).toBeInTheDocument();
    expect(screen.getByText(/Chức năng nộp lại hồ sơ \(Resubmission\) sẽ được hỗ trợ/i)).toBeInTheDocument();

    // Ensure no fake/disabled resubmit action is present
    expect(screen.queryByRole('button', { name: /Nộp lại/i })).not.toBeInTheDocument();
  });

  it('renders CANCELLED application status correctly', () => {
    const cancelledApp: CurrentMentorApplicationResponse = {
      ...baseApplication,
      applicationStatus: 'CANCELLED',
      mentorStatus: MentorStatus.NOT_APPLIED,
    };

    render(<MentorApplicationStatus application={cancelledApp} />);

    expect(screen.getByText('Hồ sơ đã bị hủy')).toBeInTheDocument();
    expect(screen.getByText('Đã hủy (CANCELLED)')).toBeInTheDocument();
  });

  it('renders dedicated restricted warning banner when mentorStatus is SUSPENDED', () => {
    const suspendedApp: CurrentMentorApplicationResponse = {
      ...baseApplication,
      applicationStatus: 'APPROVED',
      mentorStatus: MentorStatus.SUSPENDED,
    };

    render(<MentorApplicationStatus application={suspendedApp} />);

    expect(
      screen.getByText(/Tài khoản Mentor đang bị Tạm ngưng hoạt động \(SUSPENDED\)/i)
    ).toBeInTheDocument();
  });

  it('supports navigation to /profile, /explore, and optional onRefresh', () => {
    const onRefreshMock = vi.fn();
    render(<MentorApplicationStatus application={baseApplication} onRefresh={onRefreshMock} />);

    const profileButton = screen.getByRole('button', { name: /Về trang hồ sơ cá nhân/i });
    fireEvent.click(profileButton);
    expect(mockPush).toHaveBeenCalledWith('/profile');

    const exploreButton = screen.getByRole('button', { name: /Khám phá Mentify/i });
    fireEvent.click(exploreButton);
    expect(mockPush).toHaveBeenCalledWith('/explore');

    const refreshButton = screen.getByRole('button', { name: /Làm mới trạng thái/i });
    fireEvent.click(refreshButton);
    expect(onRefreshMock).toHaveBeenCalledTimes(1);
  });
});
