import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Loading from './loading';
import { apiClient } from '@/core/http/api-client';

describe('App Router Loading Boundary (loading.tsx)', () => {
  it('delegates to the shared loading primitive with accessible status semantics', () => {
    render(<Loading />);

    const statusElement = screen.getByRole('status');
    expect(statusElement).toBeInTheDocument();
    expect(statusElement).toHaveAttribute('aria-live', 'polite');
    expect(screen.getByText('Đang tải dữ liệu...')).toBeInTheDocument();
  });

  it('does not trigger authentication or network API requests', () => {
    const apiSpy = vi.spyOn(apiClient, 'request');
    const fetchSpy = vi.fn();
    global.fetch = fetchSpy;

    render(<Loading />);

    expect(apiSpy).not.toHaveBeenCalled();
    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
