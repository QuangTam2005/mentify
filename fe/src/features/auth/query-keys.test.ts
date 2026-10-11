import { describe, it, expect } from 'vitest';
import { authKeys } from './query-keys';
import { userKeys } from '@/features/user/query-keys';
import { masterDataKeys } from '@/features/master-data/query-keys';

describe('authKeys Factory', () => {
  it('Scenario 1: Auth keys are deterministic', () => {
    expect(authKeys.all).toEqual(['auth']);
    expect(authKeys.me()).toEqual(['auth', 'me']);
    // Repeated invocations produce identical tuples
    expect(authKeys.me()).toEqual(authKeys.me());
  });

  it('Scenario 4: Feature namespaces do not collide', () => {
    expect(authKeys.all[0]).toBe('auth');
    expect(userKeys.all[0]).toBe('user');
    expect(masterDataKeys.all[0]).toBe('master-data');

    expect(authKeys.all[0]).not.toEqual(userKeys.all[0]);
    expect(authKeys.all[0]).not.toEqual(masterDataKeys.all[0]);
  });

  it('Scenario 6: No token or secret values appear in keys', () => {
    const keysJson = JSON.stringify(authKeys.me());
    expect(keysJson).not.toMatch(/bearer/i);
    expect(keysJson).not.toMatch(/token/i);
    expect(keysJson).not.toMatch(/secret/i);
  });
});
