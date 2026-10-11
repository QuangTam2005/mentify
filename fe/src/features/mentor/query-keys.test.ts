import { describe, it, expect } from 'vitest';
import { mentorKeys } from './query-keys';

describe('mentorKeys factory', () => {
  it('returns canonical root key', () => {
    expect(mentorKeys.all).toEqual(['mentor']);
  });

  it('returns canonical myApplication query key', () => {
    expect(mentorKeys.myApplication()).toEqual(['mentor', 'applications', 'me']);
  });
});
