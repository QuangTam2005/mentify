import { describe, it, expect } from 'vitest';
import { cn } from './cn';

describe('cn utility', () => {
  it('merges static class names correctly', () => {
    const result = cn('text-sm', 'font-medium', 'text-slate-900');
    expect(result).toBe('text-sm font-medium text-slate-900');
  });

  it('resolves conflicting Tailwind CSS utility classes using twMerge precedence', () => {
    const result = cn('px-2 py-1', 'px-4');
    expect(result).toBe('py-1 px-4');
  });

  it('handles falsy, conditional, and nullish inputs gracefully', () => {
    const isActive = false;
    const isPrimary = true;
    const result = cn(
      'btn',
      isActive && 'btn-active',
      isPrimary && 'btn-primary',
      null,
      undefined,
      false
    );
    expect(result).toBe('btn btn-primary');
  });
});
