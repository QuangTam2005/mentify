import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { EmptyFeedback } from './empty-feedback';

describe('EmptyFeedback', () => {
  it('renders the expected title and explanatory text', () => {
    render(
      <EmptyFeedback
        title="Chưa có lịch hẹn nào"
        description="Bạn chưa đăng ký buổi cố vấn nào trong tuần này."
      />
    );

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText('Chưa có lịch hẹn nào')).toBeInTheDocument();
    expect(
      screen.getByText('Bạn chưa đăng ký buổi cố vấn nào trong tuần này.')
    ).toBeInTheDocument();
  });

  it('renders an optional action only when configured', async () => {
    const user = userEvent.setup();
    const handleAction = vi.fn();

    // 1. Without action configured
    const { rerender } = render(<EmptyFeedback title="Không có kết quả" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();

    // 2. With onAction configured
    rerender(
      <EmptyFeedback
        title="Không có kết quả"
        actionLabel="Tìm kiếm lại"
        onAction={handleAction}
      />
    );

    const actionButton = screen.getByRole('button', { name: /tìm kiếm lại/i });
    expect(actionButton).toBeInTheDocument();
    await user.click(actionButton);
    expect(handleAction).toHaveBeenCalledTimes(1);

    // 3. With safe actionHref configured
    rerender(
      <EmptyFeedback
        title="Không có kết quả"
        actionLabel="Về Trang chủ"
        actionHref="/"
      />
    );

    const actionLink = screen.getByRole('link', { name: /về trang chủ/i });
    expect(actionLink).toBeInTheDocument();
    expect(actionLink).toHaveAttribute('href', '/');

    // 4. With unsafe actionHref configured (must omit action)
    rerender(
      <EmptyFeedback
        title="Không có kết quả"
        actionLabel="Chuyển hướng ngoài"
        actionHref="https://attacker.example.com"
      />
    );

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
