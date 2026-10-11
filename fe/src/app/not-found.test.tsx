import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import NotFound from './not-found';

describe('App Router Not-Found Boundary (not-found.tsx)', () => {
  it('renders a clear 404 message', () => {
    render(<NotFound />);

    expect(screen.getByText(/404 - Không tìm thấy trang/i)).toBeInTheDocument();
    expect(
      screen.getByText(/trang bạn đang tìm kiếm không tồn tại, đã bị gỡ bỏ hoặc tạm thời không thể truy cập/i)
    ).toBeInTheDocument();
  });

  it('provides a safe navigation destination such as / and /explore', () => {
    render(<NotFound />);

    const homeLink = screen.getByRole('link', { name: /về trang chủ/i });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute('href', '/');

    const exploreLink = screen.getByRole('link', { name: /khám phá cố vấn/i });
    expect(exploreLink).toBeInTheDocument();
    expect(exploreLink).toHaveAttribute('href', '/explore');
  });

  it('does not require an authenticated user to render successfully', () => {
    // Render independently without any authentication provider or mocked auth context
    const { container } = render(<NotFound />);

    expect(container).toBeInTheDocument();
    expect(screen.getByText(/404/i)).toBeInTheDocument();
  });
});
