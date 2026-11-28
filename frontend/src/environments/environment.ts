import type {ConflictPolicyOption} from '../app/core/config/conflict-policy-options';

export const environment = {
  demonstration: false,
  // Supply verified IDs and names for policies already configured in the deployment.
  // The current API has no policy-list endpoint. Never guess a production policy.
  conflictPolicies: [] as readonly ConflictPolicyOption[]
} as const;
