import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MentorApplyContainer } from './mentor-apply-container';
import { mentorApi } from '../api/mentor-api';
import { ApiError } from '@/core/errors/api-error';
import { MentorStatus } from '@/shared/types/domain-enums';
import type { CurrentMentorApplicationResponse } from '../types/mentor.types';

// Mock router
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

// Mock auth hook
vi.mock('@/features/auth/hooks/use-auth', () => ({
  useAuth: () => ({
    user: {
      id: 'usr-1',
      email: 'applicant@mentify.vn',
      userType: 'UNIVERSITY_STUDENT',
      role: 'USER',
      mentorStatus: 'NOT_APPLIED',
    },
    isAuthenticated: true,
  }),
}));

// Mock access token
vi.mock('@/features/auth/store/access-token', () => ({
  getAccessToken: () => 'valid-mock-token',
  setAccessToken: vi.fn(),
  clearAccessToken: vi.fn(),
}));

// Mock master data API
vi.mock('@/features/master-data/api/master-data-api', () => ({
  masterDataApi: {
    getSchools: vi.fn().mockResolvedValue({
      data: [{ id: 'sch-1', name: 'Đại học Bách Khoa', region: 'MIEN_BAC' }],
    }),
    getMajorGroups: vi.fn().mockResolvedValue({
      data: [{ code: '4801', name: 'Máy tính', description: 'CNTT' }],
    }),
  },
}));

// Mock mentor API
vi.mock('../api/mentor-api', () => ({
  mentorApi: {
    submitApplication: vi.fn(),
    resubmitApplication: vi.fn(),
    getCurrentApplication: vi.fn(),
  },
}));

const mockExistingApp: CurrentMentorApplicationResponse = {
  mentorProfileId: 'prof-existing-1',
  mentorApplicationId: 'app-existing-1',
  mentorStatus: MentorStatus.PENDING,
  applicationStatus: 'PENDING',
  attemptNumber: 1,
  appliedUserType: 'UNIVERSITY_STUDENT',
  schoolId: 'sch-1',
  majorGroupCode: '4801',
  customMajorName: 'Khoa học máy tính',
  bio: 'Tiểu sử ứng viên',
  acceptingBookings: false,
  submittedAt: '2026-10-11T12:00:00Z',
};

describe('MentorApplyContainer Integration Suite', () => {
  let queryClient: QueryClient;

  function renderWithClient(ui: React.ReactElement) {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
  }

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('Scenario 7: Initial loading hides the submission form and displays LoadingFeedback', () => {
    // Unresolved promise simulates in-flight loading
    vi.mocked(mentorApi.getCurrentApplication).mockReturnValue(new Promise(() => {}));

    renderWithClient(<MentorApplyContainer />);

    expect(screen.getByText(/Đang kiểm tra hồ sơ ứng tuyển của bạn.../i)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /Đăng ký trở thành Mentor/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Gửi đơn ứng tuyển/i })).not.toBeInTheDocument();
  });

  it('Scenario 8: Confirmed HTTP 404 (RESOURCE_NOT_FOUND) displays the initial submission form', async () => {
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(
      new ApiError(404, {
        error: { code: 'RESOURCE_NOT_FOUND', message: 'No application exists' },
      })
    );

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByText('Đăng ký trở thành Mentor')).toBeInTheDocument();
    });

    // Form fields are visible
    expect(screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i })).toBeInTheDocument();
  });

  it('Scenario 9 & 10: HTTP 200 with PENDING application displays status card and prevents duplicate submission', async () => {
    vi.mocked(mentorApi.getCurrentApplication).mockResolvedValueOnce({
      success: true,
      data: mockExistingApp,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-200',
    });

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByText('Hồ sơ đang chờ xét duyệt')).toBeInTheDocument();
    });

    // Read-only status is visible
    expect(screen.getByText('Chờ xét duyệt (PENDING)')).toBeInTheDocument();
    expect(screen.getByText('app-existing-1')).toBeInTheDocument();

    // Submission form is NOT rendered
    expect(screen.queryByRole('button', { name: /Gửi đơn ứng tuyển/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Lĩnh vực chuyên môn & Kỹ năng/i)).not.toBeInTheDocument();
  });

  it('Scenario 16: HTTP 403 displays forbidden feedback without logging out or rendering form', async () => {
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(
      new ApiError(403, {
        error: { code: 'FORBIDDEN', message: 'Tài khoản của bạn đã bị khóa tính năng ứng tuyển' },
      })
    );

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByText('Truy cập bị từ chối')).toBeInTheDocument();
    });

    expect(screen.getByText(/Tài khoản của bạn đã bị khóa tính năng ứng tuyển/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Gửi đơn ứng tuyển/i })).not.toBeInTheDocument();
  });

  it('Scenario 17: Network error displays recoverable error UI with Retry button and does not show form', async () => {
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(
      new Error('Failed to fetch')
    );

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByText('Lỗi kết nối máy chủ')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /Thử lại/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Gửi đơn ứng tuyển/i })).not.toBeInTheDocument();

    // Test Retry: Next attempt succeeds with 404 -> displays form
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(
      new ApiError(404, null)
    );

    fireEvent.click(screen.getByRole('button', { name: /Thử lại/i }));

    await waitFor(() => {
      expect(screen.getByText('Đăng ký trở thành Mentor')).toBeInTheDocument();
    });
  });

  it('Scenario 18: HTTP 500 displays recoverable error UI and does not expose form', async () => {
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(
      new ApiError(500, {
        error: { code: 'INTERNAL_ERROR', message: 'Máy chủ gặp sự cố' },
      })
    );

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByText('Lỗi kết nối máy chủ')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /Thử lại/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Gửi đơn ứng tuyển/i })).not.toBeInTheDocument();
  });

  it('Scenario 19: Unknown error is NOT treated as 404 (renders safe error)', async () => {
    vi.mocked(mentorApi.getCurrentApplication).mockRejectedValueOnce(
      new Error('Custom unusual error occurred')
    );

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByText('Đã xảy ra sự cố')).toBeInTheDocument();
    });

    expect(screen.getByText('Custom unusual error occurred')).toBeInTheDocument();
    // Form is NEVER shown for unknown errors!
    expect(screen.queryByRole('button', { name: /Gửi đơn ứng tuyển/i })).not.toBeInTheDocument();
  });

  it('allows REJECTED applicant to enter resubmission mode and renders prefilled form', async () => {
    const mockRejectedApp: CurrentMentorApplicationResponse = {
      ...mockExistingApp,
      applicationStatus: 'REJECTED',
      mentorStatus: MentorStatus.REJECTED,
      customMajorName: 'Công nghệ thông tin',
      bio: 'Tiểu sử trước đó đã nộp',
    };

    vi.mocked(mentorApi.getCurrentApplication).mockResolvedValueOnce({
      success: true,
      data: mockRejectedApp,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-rej',
    });

    renderWithClient(<MentorApplyContainer />);

    // Initially displays status card
    await waitFor(() => {
      expect(screen.getByText('Hồ sơ đã bị từ chối')).toBeInTheDocument();
    });

    const resubmitBtn = screen.getByRole('button', { name: /Nộp lại hồ sơ/i });
    expect(resubmitBtn).toBeInTheDocument();

    // Click Nộp lại hồ sơ to enter resubmission mode
    fireEvent.click(resubmitBtn);

    // Form is displayed in resubmission mode
    expect(screen.getByText('Nộp lại hồ sơ ứng tuyển Mentor')).toBeInTheDocument();
    expect(screen.getByText('Giai đoạn nộp lại hồ sơ')).toBeInTheDocument();

    // Prefilled fields
    expect(screen.getByDisplayValue('Công nghệ thông tin')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Tiểu sử trước đó đã nộp')).toBeInTheDocument();

    // Missing fields from GET response are NOT fabricated
    const expertiseInput = screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i);
    expect((expertiseInput as HTMLTextAreaElement).value).toBe('');

    // Cancel action is available and returns to status view
    const cancelBtn = screen.getByRole('button', { name: /Hủy bỏ/i });
    expect(cancelBtn).toBeInTheDocument();

    fireEvent.click(cancelBtn);
    expect(screen.getByText('Hồ sơ đã bị từ chối')).toBeInTheDocument();
    expect(screen.queryByText('Nộp lại hồ sơ ứng tuyển Mentor')).not.toBeInTheDocument();
  });

  it('submits resubmission to POST /api/v1/mentor/applications/resubmit and displays success view', async () => {
    const mockRejectedApp: CurrentMentorApplicationResponse = {
      ...mockExistingApp,
      applicationStatus: 'REJECTED',
      mentorStatus: MentorStatus.REJECTED,
      customMajorName: 'Khoa học máy tính',
      bio: 'Tiểu sử đã có',
    };

    vi.mocked(mentorApi.getCurrentApplication).mockResolvedValueOnce({
      success: true,
      data: mockRejectedApp,
      meta: null,
      timestamp: '2026-10-11T12:00:00Z',
      requestId: 'req-rej-2',
    });

    vi.mocked(mentorApi.resubmitApplication).mockResolvedValueOnce({
      success: true,
      data: {
        mentorProfileId: 'prof-existing-1',
        mentorApplicationId: 'app-resubmit-new',
        userId: 'usr-1',
        mentorStatus: MentorStatus.PENDING,
        applicationStatus: 'PENDING',
        attemptNumber: 2,
        appliedUserType: 'UNIVERSITY_STUDENT',
        acceptingBookings: false,
        submittedAt: '2026-10-11T16:00:00Z',
      },
      meta: null,
      timestamp: '2026-10-11T16:00:00Z',
      requestId: 'req-resubmit-ok',
    });

    renderWithClient(<MentorApplyContainer />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Nộp lại hồ sơ/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Nộp lại hồ sơ/i }));

    // Fill in required expertise
    fireEvent.change(screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i), {
      target: { value: 'Lập trình frontend với React và Next.js' },
    });

    // Submit resubmission
    const submitBtn = screen.getByRole('button', { name: /Nộp lại hồ sơ/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mentorApi.resubmitApplication).toHaveBeenCalledTimes(1);
    });

    // Success view shows new attempt details
    await waitFor(() => {
      expect(screen.getByText('Hồ sơ nộp lại đã được tiếp nhận!')).toBeInTheDocument();
      expect(screen.getByText(/Lần thứ 2/i)).toBeInTheDocument();
      expect(screen.getByText('app-resubmit-new')).toBeInTheDocument();
    });
  });
});
