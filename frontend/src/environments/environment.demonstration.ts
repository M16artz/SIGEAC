import type {ConflictPolicyOption} from '../app/core/config/conflict-policy-options';
import {PILOT_CONFLICT_POLICY_ID} from '../app/features/access/data-access/access.models';

export const environment = {
  demonstration: true,
  // Describes the RESPONSIBLE/REVIEWER pair seeded by SyntheticAccessData.
  conflictPolicies: [{id: PILOT_CONFLICT_POLICY_ID, label: 'Responsable y revisor'}] as readonly ConflictPolicyOption[]
} as const;
