import {InjectionToken} from '@angular/core';
import {environment} from '../../../environments/environment';

/** Display metadata for existing policies, not authorization rules or new policies. */
export interface ConflictPolicyOption { readonly id: string; readonly label: string; }
export const CONFLICT_POLICY_OPTIONS = new InjectionToken<readonly ConflictPolicyOption[]>('Conflict policy display options', {
  providedIn: 'root', factory: () => environment.conflictPolicies
});
