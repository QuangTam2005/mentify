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
});
