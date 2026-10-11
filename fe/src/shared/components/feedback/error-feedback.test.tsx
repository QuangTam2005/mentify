import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { ErrorFeedback } from './error-feedback';

describe('ErrorFeedback', () => {
  it('renders a controlled error title and description', () => {
    render(
      <ErrorFeedback
        title="Lỗi tải danh mục"
        description="Không thể tải danh sách ngành nghề vào lúc này."
      />
    );

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Lỗi tải danh mục')).toBeInTheDocument();
    expect(
      screen.getByText('Không thể tải danh sách ngành nghề vào lúc này.')
    ).toBeInTheDocument();
  });

  it('calls the Retry callback when the user activates Retry', async () => {
    const user = userEvent.setup();
    const handleRetry = vi.fn();

    render(<ErrorFeedback onRetry={handleRetry} retryLabel="Thử lại ngay" />);

    const retryButton = screen.getByRole('button', { name: /thử lại ngay/i });
    expect(retryButton).toBeInTheDocument();

    await user.click(retryButton);
    expect(handleRetry).toHaveBeenCalledTimes(1);
  });

  it('does not render Retry when no callback is supplied', () => {
    render(<ErrorFeedback title="Lỗi không thể thử lại" />);

    expect(screen.queryByRole('button', { name: /thử lại/i })).not.toBeInTheDocument();
  });

  it('renders a safe internal navigation action when configured', () => {
    render(
      <ErrorFeedback
        navigationHref="/explore"
        navigationLabel="Khám phá cố vấn"
      />
    );

    const link = screen.getByRole('link', { name: /khám phá cố vấn/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/explore');
  });

  it('rejects or omits unsafe external navigation destinations', () => {
    const { rerender } = render(
      <ErrorFeedback
        navigationHref="https://malicious-site.example.com"
        navigationLabel="Chuyển trang ngoài"
      />
    );

    expect(
      screen.queryByRole('link', { name: /chuyển trang ngoài/i })
    ).not.toBeInTheDocument();

    rerender(
      <ErrorFeedback
        navigationHref="//protocol-relative-exploit.com"
        navigationLabel="Chuyển trang ngoài"
      />
    );

    expect(
      screen.queryByRole('link', { name: /chuyển trang ngoài/i })
    ).not.toBeInTheDocument();

    rerender(
      <ErrorFeedback
        navigationHref="javascript:alert(1)"
        navigationLabel="Chuyển trang ngoài"
      />
    );

    expect(
      screen.queryByRole('link', { name: /chuyển trang ngoài/i })
    ).not.toBeInTheDocument();
  });
});
