import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from './button';

describe('Button component', () => {
  it('renders button with label and responds to clicks', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    render(<Button onClick={handleClick}>Xác nhận</Button>);

    const button = screen.getByRole('button', { name: /xác nhận/i });
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();

    await user.click(button);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('prevents click events when disabled', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    render(
      <Button disabled onClick={handleClick}>
        Không khả dụng
      </Button>
    );

    const button = screen.getByRole('button', { name: /không khả dụng/i });
    expect(button).toBeDisabled();

    await user.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('disables the button when isLoading is true', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    render(
      <Button isLoading onClick={handleClick}>
        Đang xử lý
      </Button>
    );

    const button = screen.getByRole('button', { name: /đang xử lý/i });
    expect(button).toBeDisabled();

    await user.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });
});
