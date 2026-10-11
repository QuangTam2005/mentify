import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Input } from './input';

describe('Input component', () => {
  it('renders with label and accepts user input', async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();

    render(
      <Input
        label="Họ và tên"
        placeholder="Nhập họ và tên"
        onChange={handleChange}
      />
    );

    const input = screen.getByLabelText(/họ và tên/i);
    expect(input).toBeInTheDocument();

    await user.type(input, 'Nguyễn Văn A');
    expect(input).toHaveValue('Nguyễn Văn A');
    expect(handleChange).toHaveBeenCalled();
  });

  it('displays error message when provided', () => {
    render(
      <Input
        label="Email"
        error="Địa chỉ email không hợp lệ"
      />
    );

    expect(screen.getByText('Địa chỉ email không hợp lệ')).toBeInTheDocument();
  });

  it('prevents user typing when disabled', async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();

    render(
      <Input
        label="Tài khoản"
        disabled
        onChange={handleChange}
      />
    );

    const input = screen.getByLabelText(/tài khoản/i);
    expect(input).toBeDisabled();

    await user.type(input, 'test');
    expect(input).toHaveValue('');
    expect(handleChange).not.toHaveBeenCalled();
  });
});
