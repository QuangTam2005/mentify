import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { LoadingFeedback } from './loading-feedback';

describe('LoadingFeedback', () => {
  it('renders an accessible loading status with role="status" and aria-live="polite"', () => {
    render(<LoadingFeedback />);

    const statusElement = screen.getByRole('status');
    expect(statusElement).toBeInTheDocument();
    expect(statusElement).toHaveAttribute('aria-live', 'polite');
  });

  it('renders a supplied loading message', () => {
    const customMessage = 'Đang đồng bộ hồ sơ cố vấn...';
    render(<LoadingFeedback message={customMessage} />);

    expect(screen.getByText(customMessage)).toBeInTheDocument();
  });

  it('uses a reasonable default message when none is supplied', () => {
    render(<LoadingFeedback />);

    expect(screen.getByText('Đang tải dữ liệu...')).toBeInTheDocument();
  });
});
