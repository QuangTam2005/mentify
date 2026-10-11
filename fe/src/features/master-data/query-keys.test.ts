import { describe, it, expect } from 'vitest';
import { masterDataKeys } from './query-keys';

describe('masterDataKeys Factory', () => {
  it('Scenario 3: Master-data keys are deterministic', () => {
    expect(masterDataKeys.all).toEqual(['master-data']);
    expect(masterDataKeys.schools()).toEqual(['master-data', 'schools']);
    expect(masterDataKeys.majorGroups()).toEqual(['master-data', 'major-groups']);
    expect(masterDataKeys.schools()).toEqual(masterDataKeys.schools());
  });

  it('Scenario 5: Parameterized keys distinguish different inputs', () => {
    const northKey = masterDataKeys.schools({ region: 'NORTH' });
    const southKey = masterDataKeys.schools({ region: 'SOUTH' });
    const emptyKey = masterDataKeys.schools();

    expect(northKey).toEqual(['master-data', 'schools', { region: 'NORTH' }]);
    expect(southKey).toEqual(['master-data', 'schools', { region: 'SOUTH' }]);
    expect(emptyKey).toEqual(['master-data', 'schools']);

    expect(northKey).not.toEqual(southKey);
    expect(northKey).not.toEqual(emptyKey);

    const activeMajorKey = masterDataKeys.majorGroups({ isActive: true });
    const inactiveMajorKey = masterDataKeys.majorGroups({ isActive: false });
    expect(activeMajorKey).not.toEqual(inactiveMajorKey);
  });

  it('Scenario 6: No token or secret values appear in master-data keys', () => {
    const keysJson = JSON.stringify(masterDataKeys.schools({ search: 'Bách Khoa' }));
    expect(keysJson).not.toMatch(/token/i);
    expect(keysJson).not.toMatch(/secret/i);
  });
});
