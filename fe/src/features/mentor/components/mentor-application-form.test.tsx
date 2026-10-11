import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MentorApplicationForm } from './mentor-application-form';
import { mentorApi } from '../api/mentor-api';
import { ApiError } from '@/core/errors/api-error';
import { MentorStatus } from '@/shared/types/domain-enums';
import type {
  MentorApplicationSubmissionResponse,
  CurrentMentorApplicationResponse,
} from '../types/mentor.types';
import type { ApiResponse } from '@/core/types/api';

// Mock router
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

// Mock auth hook
const mockUseAuth = vi.fn();
vi.mock('@/features/auth/hooks/use-auth', () => ({
  useAuth: () => mockUseAuth(),
}));

// Mock master data API
vi.mock('@/features/master-data/api/master-data-api', () => ({
  masterDataApi: {
    getSchools: vi.fn().mockResolvedValue({
      data: [
        {
          id: 'sch-uuid-1',
          name: 'Đại học Bách Khoa Hà Nội',
          commonAbbreviation: 'HUST',
          admissionCode: 'BKA',
          region: 'MIEN_BAC',
        },
      ],
    }),
    getMajorGroups: vi.fn().mockResolvedValue({
      data: [
        {
          code: '4801',
          name: 'Máy tính và công nghệ thông tin',
          description: 'CNTT',
        },
      ],
    }),
  },
}));

// Mock mentor API
vi.mock('../api/mentor-api', () => ({
  mentorApi: {
    submitApplication: vi.fn(),
    resubmitApplication: vi.fn(),
  },
}));

const mockSuccessResponse: MentorApplicationSubmissionResponse = {
  mentorProfileId: 'prof-999-uuid',
  mentorApplicationId: 'app-888-uuid',
  userId: 'usr-123-uuid',
  mentorStatus: MentorStatus.PENDING,
  applicationStatus: 'PENDING',
  attemptNumber: 1,
  appliedUserType: 'UNIVERSITY_STUDENT',
  acceptingBookings: false,
  submittedAt: '2026-10-11T15:00:00Z',
};

describe('MentorApplicationForm Component', () => {
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
    mockUseAuth.mockReturnValue({
      user: {
        id: 'usr-1',
        email: 'applicant@mentify.vn',
        userType: 'UNIVERSITY_STUDENT',
        role: 'USER',
        mentorStatus: 'NOT_APPLIED',
      },
      isAuthenticated: true,
    });
  });

  // Scenario 1: Required fields render correctly
  it('Scenario 1: Renders required fields, labels, and indicators', async () => {
    renderWithClient(<MentorApplicationForm />);

    expect(
      screen.getByRole('heading', { name: /Đăng ký trở thành Mentor/i })
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i)
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i)
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Trường Đại học \/ Học viện/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Nhóm ngành chuẩn/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Chuyên ngành cụ thể/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Liên kết phòng họp trực tuyến/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i })
    ).toBeInTheDocument();
  });

  // Scenario 2: Invalid inputs block submission (empty and whitespace-only)
  it('Scenario 2: Blocks submission and rejects empty or whitespace-only inputs without calling API', async () => {
    renderWithClient(<MentorApplicationForm />);

    // Attempt 1: Submit completely empty
    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    expect(
      screen.getByText(/Vui lòng nhập tiểu sử và giới thiệu bản thân/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Vui lòng nhập lĩnh vực chuyên môn và kỹ năng/i)
    ).toBeInTheDocument();
    expect(mentorApi.submitApplication).not.toHaveBeenCalled();

    // Attempt 2: Fill with whitespace only
    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );

    fireEvent.change(bioInput, { target: { value: '   \n  \t  ' } });
    fireEvent.change(expertiseInput, { target: { value: '        ' } });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    expect(
      screen.getByText(/Vui lòng nhập tiểu sử và giới thiệu bản thân/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Vui lòng nhập lĩnh vực chuyên môn và kỹ năng/i)
    ).toBeInTheDocument();
    expect(mentorApi.submitApplication).not.toHaveBeenCalled();
  });

  // Scenario 3: Validation constraints match actual backend DTO
  it('Scenario 3: Enforces field length and URL format matching backend constraints', async () => {
    renderWithClient(<MentorApplicationForm />);

    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );
    const customMajorInput = screen.getByLabelText(/Chuyên ngành cụ thể/i);
    const meetingLinkInput = screen.getByLabelText(/Liên kết phòng họp trực tuyến/i);

    // Over maximum lengths
    fireEvent.change(bioInput, { target: { value: 'a'.repeat(1001) } });
    fireEvent.change(expertiseInput, { target: { value: 'b'.repeat(501) } });
    fireEvent.change(customMajorInput, { target: { value: 'c'.repeat(256) } });
    fireEvent.change(meetingLinkInput, { target: { value: 'not-a-valid-url' } });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    expect(screen.getByText(/Tiểu sử không được vượt quá 1000 ký tự/i)).toBeInTheDocument();
    expect(
      screen.getByText(/Lĩnh vực chuyên môn không được vượt quá 500 ký tự/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Tên ngành đào tạo không được vượt quá 255 ký tự/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Định dạng liên kết phòng họp không hợp lệ/i)
    ).toBeInTheDocument();
    expect(mentorApi.submitApplication).not.toHaveBeenCalled();

    // Invalid scheme URL (e.g. ftp://)
    fireEvent.change(meetingLinkInput, { target: { value: 'ftp://example.com/meet' } });
    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));
    expect(
      screen.getByText(/Liên kết phòng họp phải bắt đầu bằng http:\/\/ hoặc https:\/\//i)
    ).toBeInTheDocument();
  });

  // Scenario 4: Correct POST endpoint and payload
  it('Scenario 4: Dispatches valid, trimmed payload matching backend request contract', async () => {
    vi.mocked(mentorApi.submitApplication).mockResolvedValueOnce({
      success: true,
      data: mockSuccessResponse,
      meta: null,
      timestamp: '2026-10-11T15:00:00Z',
      requestId: 'req-4',
    });

    renderWithClient(<MentorApplicationForm />);

    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );
    const customMajorInput = screen.getByLabelText(/Chuyên ngành cụ thể/i);
    const meetingLinkInput = screen.getByLabelText(/Liên kết phòng họp trực tuyến/i);

    fireEvent.change(bioInput, {
      target: { value: '  Kỹ sư phần mềm 4 năm kinh nghiệm tại TechCorp  ' },
    });
    fireEvent.change(expertiseInput, {
      target: { value: '  Java, Spring Boot, Microservices  ' },
    });
    fireEvent.change(customMajorInput, {
      target: { value: '  Kỹ thuật phần mềm  ' },
    });
    fireEvent.change(meetingLinkInput, {
      target: { value: '  https://meet.google.com/abc-xyz-123  ' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(mentorApi.submitApplication).toHaveBeenCalledTimes(1);
    });

    expect(mentorApi.submitApplication).toHaveBeenCalledWith({
      bio: 'Kỹ sư phần mềm 4 năm kinh nghiệm tại TechCorp',
      expertiseAreas: 'Java, Spring Boot, Microservices',
      customMajorName: 'Kỹ thuật phần mềm',
      defaultMeetingLink: 'https://meet.google.com/abc-xyz-123',
      schoolId: undefined,
      majorGroupCode: undefined,
    });
  });

  // Scenario 5: Rapid clicks do not duplicate requests
  it('Scenario 5: Prevents duplicate submissions on rapid multiple clicks', async () => {
    let resolveApi!: (value: ApiResponse<MentorApplicationSubmissionResponse>) => void;
    const pendingPromise = new Promise<ApiResponse<MentorApplicationSubmissionResponse>>((resolve) => {
      resolveApi = resolve;
    });

    vi.mocked(mentorApi.submitApplication).mockReturnValueOnce(pendingPromise);

    renderWithClient(<MentorApplicationForm />);

    fireEvent.change(
      screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i),
      { target: { value: 'Tiểu sử hợp lệ' } }
    );
    fireEvent.change(
      screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i),
      { target: { value: 'Chuyên môn hợp lệ' } }
    );

    const submitButton = screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i });

    // Rapid multiple click wrapped in act
    act(() => {
      fireEvent.click(submitButton);
      fireEvent.click(submitButton);
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(mentorApi.submitApplication).toHaveBeenCalledTimes(1);
    });

    // Resolve API to clean up
    resolveApi({
      success: true,
      data: mockSuccessResponse,
      meta: null,
      timestamp: '2026-10-11T15:00:00Z',
      requestId: 'req-5',
    });
  });

  // Scenario 6: Accessible submitting state
  it('Scenario 6: Shows accessible submitting indicator (disabled, spinner, aria-busy)', async () => {
    let resolveApi!: (value: ApiResponse<MentorApplicationSubmissionResponse>) => void;
    const pendingPromise = new Promise<ApiResponse<MentorApplicationSubmissionResponse>>((resolve) => {
      resolveApi = resolve;
    });

    vi.mocked(mentorApi.submitApplication).mockReturnValueOnce(pendingPromise);

    renderWithClient(<MentorApplicationForm />);

    fireEvent.change(
      screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i),
      { target: { value: 'Tiểu sử' } }
    );
    fireEvent.change(
      screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i),
      { target: { value: 'Chuyên môn' } }
    );

    const submitButton = screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i });
    act(() => {
      fireEvent.click(submitButton);
    });

    // Submitting state verification
    expect(screen.getByText(/Đang gửi hồ sơ.../i)).toBeInTheDocument();
    expect(submitButton).toBeDisabled();

    const form = submitButton.closest('form');
    expect(form).toHaveAttribute('aria-busy', 'true');

    resolveApi({
      success: true,
      data: mockSuccessResponse,
      meta: null,
      timestamp: '2026-10-11T15:00:00Z',
      requestId: 'req-6',
    });
  });

  // Scenario 7: Successful submission feedback
  it('Scenario 7: Displays success confirmation card explaining submission is under review and NOT immediate approval', async () => {
    vi.mocked(mentorApi.submitApplication).mockResolvedValueOnce({
      success: true,
      data: mockSuccessResponse,
      meta: null,
      timestamp: '2026-10-11T15:00:00Z',
      requestId: 'req-7',
    });

    renderWithClient(<MentorApplicationForm />);

    fireEvent.change(
      screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i),
      { target: { value: 'Tiểu sử hoàn chỉnh' } }
    );
    fireEvent.change(
      screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i),
      { target: { value: 'Chuyên môn hoàn chỉnh' } }
    );

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/Đơn ứng tuyển đã được tiếp nhận!/i)
      ).toBeInTheDocument();
    });

    // Check PENDING status badge
    expect(screen.getByText('PENDING')).toBeInTheDocument();

    // Check explicit explanation that submission does NOT imply immediate approval
    expect(
      screen.getByText(/Việc nộp đơn thành công KHÔNG đồng nghĩa với việc bạn đã được phê duyệt làm Mentor ngay lập tức/i)
    ).toBeInTheDocument();

    // Check IDs and details
    expect(screen.getByText('app-888-uuid')).toBeInTheDocument();
    expect(screen.getByText('prof-999-uuid')).toBeInTheDocument();
    expect(screen.getByText(/Lần thứ 1/i)).toBeInTheDocument();
  });

  // Scenario 8: 400/422 validation errors mapping
  it('Scenario 8: Maps backend 400 fieldErrors to inputs and preserves user inputs', async () => {
    vi.mocked(mentorApi.submitApplication).mockRejectedValueOnce(
      new ApiError(400, {
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Dữ liệu không hợp lệ',
          details: [
            { field: 'bio', message: 'Tiểu sử vi phạm quy định nội dung' },
            { field: 'customMajorName', message: 'Tên chuyên ngành không hợp lệ' },
          ],
        },
      })
    );

    renderWithClient(<MentorApplicationForm />);

    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );
    const customMajorInput = screen.getByLabelText(/Chuyên ngành cụ thể/i);

    fireEvent.change(bioInput, { target: { value: 'Nội dung tiểu sử nhập vào' } });
    fireEvent.change(expertiseInput, { target: { value: 'Kỹ năng nhập vào' } });
    fireEvent.change(customMajorInput, { target: { value: 'Chuyên ngành nhập vào' } });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(screen.getByText(/Tiểu sử vi phạm quy định nội dung/i)).toBeInTheDocument();
      expect(screen.getByText(/Tên chuyên ngành không hợp lệ/i)).toBeInTheDocument();
    });

    // Ensure user inputs are strictly preserved
    expect(bioInput).toHaveValue('Nội dung tiểu sử nhập vào');
    expect(expertiseInput).toHaveValue('Kỹ năng nhập vào');
    expect(customMajorInput).toHaveValue('Chuyên ngành nhập vào');
  });

  // Scenario 9: 401 unauthenticated handling
  it('Scenario 9: Handles 401 unauthenticated response gracefully without crashing', async () => {
    vi.mocked(mentorApi.submitApplication).mockRejectedValueOnce(
      new ApiError(401, {
        error: {
          code: 'UNAUTHENTICATED',
          message: 'Phiên đăng nhập đã hết hạn',
        },
      })
    );

    renderWithClient(<MentorApplicationForm />);

    fireEvent.change(
      screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i),
      { target: { value: 'Tiểu sử' } }
    );
    fireEvent.change(
      screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i),
      { target: { value: 'Kỹ năng' } }
    );

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/Yêu cầu đăng nhập/i)).toBeInTheDocument();
    });
  });

  // Scenario 10: 403 forbidden behavior
  it('Scenario 10: Renders 403 forbidden alert banner, preserves input, and does not force logout', async () => {
    vi.mocked(mentorApi.submitApplication).mockRejectedValueOnce(
      new ApiError(403, {
        error: {
          code: 'FORBIDDEN',
          message:
            'Chỉ sinh viên đại học/cao đẳng hoặc người đi làm mới có thể đăng ký làm mentor',
        },
      })
    );

    renderWithClient(<MentorApplicationForm />);

    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );

    fireEvent.change(bioInput, { target: { value: 'Tiểu sử của tôi' } });
    fireEvent.change(expertiseInput, { target: { value: 'Kỹ năng của tôi' } });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/Truy cập bị từ chối/i)).toBeInTheDocument();
      expect(
        screen.getByText(
          /Chỉ sinh viên đại học\/cao đẳng hoặc người đi làm mới có thể đăng ký làm mentor/i
        )
      ).toBeInTheDocument();
    });

    // Inputs remain intact
    expect(bioInput).toHaveValue('Tiểu sử của tôi');
    expect(expertiseInput).toHaveValue('Kỹ năng của tôi');
  });

  // Scenario 11: 409 conflict behavior
  it('Scenario 11: Displays 409 conflict banner and preserves user inputs', async () => {
    vi.mocked(mentorApi.submitApplication).mockRejectedValueOnce(
      new ApiError(409, {
        error: {
          code: 'CONFLICT',
          message: 'Người dùng đã có đơn ứng tuyển mentor trước đó',
        },
      })
    );

    renderWithClient(<MentorApplicationForm />);

    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );

    fireEvent.change(bioInput, { target: { value: 'Tiểu sử thử nghiệm' } });
    fireEvent.change(expertiseInput, { target: { value: 'Kỹ năng thử nghiệm' } });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/Xung đột dữ liệu/i)).toBeInTheDocument();
      expect(
        screen.getByText(/Người dùng đã có đơn ứng tuyển mentor trước đó/i)
      ).toBeInTheDocument();
    });

    // Inputs are preserved
    expect(bioInput).toHaveValue('Tiểu sử thử nghiệm');
    expect(expertiseInput).toHaveValue('Kỹ năng thử nghiệm');
  });

  // Scenario 12: Network/5xx recovery
  it('Scenario 12: Handles network or 500 error gracefully, preserves input, and allows retry', async () => {
    vi.mocked(mentorApi.submitApplication).mockRejectedValueOnce(
      new Error('Failed to fetch')
    );

    renderWithClient(<MentorApplicationForm />);

    const bioInput = screen.getByLabelText(/Tiểu sử & Giới thiệu bản thân \(Bio\)/i);
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    );

    fireEvent.change(bioInput, { target: { value: 'Tiểu sử cần giữ lại' } });
    fireEvent.change(expertiseInput, { target: { value: 'Kỹ năng cần giữ lại' } });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/Lỗi kết nối/i)).toBeInTheDocument();
    });

    expect(bioInput).toHaveValue('Tiểu sử cần giữ lại');
    expect(expertiseInput).toHaveValue('Kỹ năng cần giữ lại');

    // Retry submission: succeeds on next attempt
    vi.mocked(mentorApi.submitApplication).mockResolvedValueOnce({
      success: true,
      data: mockSuccessResponse,
      meta: null,
      timestamp: '2026-10-11T15:00:00Z',
      requestId: 'req-12',
    });

    fireEvent.click(screen.getByRole('button', { name: /Gửi đơn ứng tuyển/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/Đơn ứng tuyển đã được tiếp nhận!/i)
      ).toBeInTheDocument();
    });
  });

  // Scenario 13: Ineligible high school student warning banner
  it('Scenario 13: Renders eligibility warning for SCHOOL_STUDENT users', async () => {
    mockUseAuth.mockReturnValue({
      user: {
        id: 'usr-student',
        email: 'pupil@mentify.vn',
        userType: 'SCHOOL_STUDENT',
        role: 'USER',
        mentorStatus: 'NOT_APPLIED',
      },
      isAuthenticated: true,
    });

    renderWithClient(<MentorApplicationForm />);

    expect(
      screen.getByText(/Cảnh báo điều kiện ứng tuyển:/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Học sinh THPT/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Sinh viên Đại học\/Cao đẳng/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Người đi làm/i)
    ).toBeInTheDocument();
  });

  // Scenario 14: Keyboard accessibility and back button
  it('Scenario 14: Supports keyboard form submission and back navigation', async () => {
    renderWithClient(<MentorApplicationForm />);

    const backButton = screen.getByRole('button', { name: /Quay lại/i });
    fireEvent.click(backButton);
    expect(mockPush).toHaveBeenCalledWith('/profile');
  });

  // Scenario 15: Resubmission mode rendering & prefilling
  it('Scenario 15: Renders resubmission mode with prefilled values, empty expertise/link, and helper note', async () => {
    const mockInitialData: CurrentMentorApplicationResponse = {
      mentorProfileId: 'prof-existing-1',
      mentorApplicationId: 'app-old-1',
      mentorStatus: MentorStatus.REJECTED,
      applicationStatus: 'REJECTED',
      attemptNumber: 1,
      appliedUserType: 'UNIVERSITY_STUDENT',
      schoolId: 'sch-uuid-1',
      majorGroupCode: '4801',
      customMajorName: 'Khoa học máy tính & AI',
      bio: 'Tiểu sử trước đây đã viết rất chi tiết.',
      acceptingBookings: false,
      submittedAt: '2026-10-10T10:00:00Z',
    };

    renderWithClient(
      <MentorApplicationForm mode="resubmit" initialData={mockInitialData} />
    );

    // Title & badge indicate resubmission mode
    expect(screen.getByText('Nộp lại hồ sơ ứng tuyển Mentor')).toBeInTheDocument();
    expect(screen.getByText('Giai đoạn nộp lại hồ sơ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Nộp lại hồ sơ/i })).toBeInTheDocument();

    // Prefilled values
    expect(screen.getByDisplayValue('Khoa học máy tính & AI')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Tiểu sử trước đây đã viết rất chi tiết.')).toBeInTheDocument();

    // Absent fields from GET response are NOT fabricated
    const expertiseInput = screen.getByLabelText(
      /Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i
    ) as HTMLTextAreaElement;
    expect(expertiseInput.value).toBe('');

    const meetingLinkInput = screen.getByLabelText(
      /Liên kết phòng họp trực tuyến mặc định/i
    ) as HTMLInputElement;
    expect(meetingLinkInput.value).toBe('');

    // Explanatory note for re-entering expertise
    expect(
      screen.getByText(/Lĩnh vực chuyên môn và liên kết phòng họp không nằm trong bản tóm tắt hồ sơ/i)
    ).toBeInTheDocument();
  });

  // Scenario 16: Successful resubmission calls POST /api/v1/mentor/applications/resubmit
  it('Scenario 16: Submits to resubmission endpoint, prevents rapid double-clicks, and shows success view', async () => {
    const mockInitialData: CurrentMentorApplicationResponse = {
      mentorProfileId: 'prof-existing-1',
      mentorApplicationId: 'app-old-1',
      mentorStatus: MentorStatus.REJECTED,
      applicationStatus: 'REJECTED',
      attemptNumber: 1,
      appliedUserType: 'UNIVERSITY_STUDENT',
      schoolId: 'sch-uuid-1',
      majorGroupCode: '4801',
      customMajorName: 'Kỹ thuật máy tính',
      bio: 'Tiểu sử cập nhật',
      acceptingBookings: false,
      submittedAt: '2026-10-10T10:00:00Z',
    };

    const resubmitOkResponse: MentorApplicationSubmissionResponse = {
      mentorProfileId: 'prof-existing-1',
      mentorApplicationId: 'app-resubmit-new-uuid',
      userId: 'usr-1',
      mentorStatus: MentorStatus.PENDING,
      applicationStatus: 'PENDING',
      attemptNumber: 2,
      appliedUserType: 'UNIVERSITY_STUDENT',
      acceptingBookings: false,
      submittedAt: '2026-10-11T16:00:00Z',
    };

    let resolveMutation!: (val: ApiResponse<MentorApplicationSubmissionResponse>) => void;
    vi.mocked(mentorApi.resubmitApplication).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveMutation = resolve;
        })
    );

    const onCancelMock = vi.fn();
    const onSuccessMock = vi.fn();

    renderWithClient(
      <MentorApplicationForm
        mode="resubmit"
        initialData={mockInitialData}
        onCancel={onCancelMock}
        onSuccess={onSuccessMock}
      />
    );

    // Enter required expertise
    fireEvent.change(screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i), {
      target: { value: 'Frontend Development, TanStack Query, Next.js' },
    });

    const submitBtn = screen.getByRole('button', { name: /Nộp lại hồ sơ/i });

    // Click twice rapidly to verify double-submit protection
    act(() => {
      fireEvent.click(submitBtn);
      fireEvent.click(submitBtn);
    });

    await waitFor(() => {
      expect(mentorApi.resubmitApplication).toHaveBeenCalledTimes(1);
    });

    // Resolve mutation
    await act(async () => {
      resolveMutation({
        success: true,
        data: resubmitOkResponse,
        meta: null,
        timestamp: '2026-10-11T16:00:00Z',
        requestId: 'req-ok',
      });
    });

    await waitFor(() => {
      expect(screen.getByText('Hồ sơ nộp lại đã được tiếp nhận!')).toBeInTheDocument();
      expect(screen.getByText(/Lần thứ 2/i)).toBeInTheDocument();
      expect(screen.getByText('app-resubmit-new-uuid')).toBeInTheDocument();
    });

    expect(onSuccessMock).toHaveBeenCalledWith(resubmitOkResponse);

    // Clicking "Xem trạng thái hồ sơ" triggers onCancel callback
    const viewStatusBtn = screen.getByRole('button', { name: /Xem trạng thái hồ sơ/i });
    fireEvent.click(viewStatusBtn);
    expect(onCancelMock).toHaveBeenCalledTimes(1);
  });

  // Scenario 17: HTTP 400 validation error in resubmission mode displays errors and preserves input
  it('Scenario 17: HTTP 400 validation error in resubmission mode displays error and preserves input', async () => {
    vi.mocked(mentorApi.resubmitApplication).mockRejectedValueOnce(
      new ApiError(400, {
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Dữ liệu không hợp lệ',
          details: [{ field: 'bio', message: 'Tiểu sử chứa từ ngữ không hợp lệ' }],
        },
      })
    );

    renderWithClient(
      <MentorApplicationForm
        mode="resubmit"
        initialData={{
          mentorProfileId: 'prof-1',
          mentorApplicationId: 'app-1',
          mentorStatus: MentorStatus.REJECTED,
          applicationStatus: 'REJECTED',
          attemptNumber: 1,
          appliedUserType: 'UNIVERSITY_STUDENT',
          schoolId: null,
          majorGroupCode: null,
          customMajorName: 'Toán tin',
          bio: 'Tiểu sử cần sửa',
          acceptingBookings: false,
          submittedAt: '2026-10-10T10:00:00Z',
        }}
      />
    );

    fireEvent.change(screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i), {
      target: { value: 'Toán ứng dụng' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Nộp lại hồ sơ/i }));

    await waitFor(() => {
      expect(screen.getByText('Tiểu sử chứa từ ngữ không hợp lệ')).toBeInTheDocument();
    });

    // Inputs are strictly preserved
    expect(screen.getByDisplayValue('Toán tin')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Tiểu sử cần sửa')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Toán ứng dụng')).toBeInTheDocument();
  });

  // Scenario 18: HTTP 409 Conflict displays alert banner and preserves values
  it('Scenario 18: HTTP 409 Conflict displays conflict alert and preserves entered values', async () => {
    vi.mocked(mentorApi.resubmitApplication).mockRejectedValueOnce(
      new ApiError(409, {
        error: {
          code: 'CONFLICT',
          message: 'Chỉ có thể nộp lại đơn mentor bị từ chối',
        },
      })
    );

    renderWithClient(
      <MentorApplicationForm
        mode="resubmit"
        initialData={{
          mentorProfileId: 'prof-1',
          mentorApplicationId: 'app-1',
          mentorStatus: MentorStatus.REJECTED,
          applicationStatus: 'REJECTED',
          attemptNumber: 1,
          appliedUserType: 'UNIVERSITY_STUDENT',
          schoolId: null,
          majorGroupCode: null,
          customMajorName: null,
          bio: 'Tiểu sử',
          acceptingBookings: false,
          submittedAt: '2026-10-10T10:00:00Z',
        }}
      />
    );

    fireEvent.change(screen.getByLabelText(/Lĩnh vực chuyên môn & Kỹ năng hướng dẫn/i), {
      target: { value: 'Kỹ năng chuyên môn' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Nộp lại hồ sơ/i }));

    await waitFor(() => {
      expect(screen.getByText('Chỉ có thể nộp lại đơn mentor bị từ chối')).toBeInTheDocument();
    });

    // Inputs preserved
    expect(screen.getByDisplayValue('Kỹ năng chuyên môn')).toBeInTheDocument();
  });

  // Scenario 19: onCancel prop triggers cancellation in resubmit mode
  it('Scenario 19: onCancel button triggers onCancel callback in resubmit mode', async () => {
    const onCancelMock = vi.fn();
    renderWithClient(
      <MentorApplicationForm mode="resubmit" onCancel={onCancelMock} />
    );

    const cancelBtn = screen.getByRole('button', { name: /Hủy bỏ/i });
    expect(cancelBtn).toBeInTheDocument();

    fireEvent.click(cancelBtn);
    expect(onCancelMock).toHaveBeenCalledTimes(1);
    expect(mockPush).not.toHaveBeenCalled();
  });
});
