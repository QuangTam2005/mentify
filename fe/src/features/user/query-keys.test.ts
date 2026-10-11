import { describe, it, expect } from 'vitest';
import { userKeys } from './query-keys';

describe('userKeys Factory', () => {
  it('Scenario 2: User keys are deterministic', () => {
    expect(userKeys.all).toEqual(['user']);
    expect(userKeys.profile()).toEqual(['user', 'profile']);
    expect(userKeys.profile()).toEqual(userKeys.profile());
  });

  it('Scenario 6: No token, secret, or unnecessary user IDs appear in keys', () => {
    const keysJson = JSON.stringify(userKeys.profile());
    expect(keysJson).not.toMatch(/token/i);
    expect(keysJson).not.toMatch(/bearer/i);
    expect(keysJson).not.toMatch(/password/i);
    expect(keysJson).not.toMatch(/secret/i);
  });
});
