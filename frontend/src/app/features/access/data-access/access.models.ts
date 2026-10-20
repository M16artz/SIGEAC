export interface SessionView {
  readonly userId: string;
  readonly expiresAt: string;
  readonly csrfToken: string;
}

export interface AssignmentView {
  readonly id: string;
  readonly roleCode: string;
  readonly roleName: string;
  readonly careerId: string;
  readonly processId: string | null;
  readonly indicatorId: string | null;
  readonly validFrom: string;
  readonly validTo: string;
}

export interface CurrentIdentity {
  readonly id: string;
  readonly displayName: string;
  readonly technical: boolean;
  readonly assignments: readonly AssignmentView[];
}

export interface UserView {
  readonly id: string;
  readonly username: string;
  readonly displayName: string;
  readonly technical: boolean;
  readonly active: boolean;
  readonly revision: number;
}

export interface PageView<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly total: number;
}

export interface ResourceView {
  readonly id: string;
  readonly version: number;
  readonly status: string;
}

export interface AuditEventView {
  readonly id: string;
  readonly occurredAt: string;
  readonly correlationId: string;
  readonly action: string;
  readonly objectType: string;
  readonly objectId: string;
  readonly outcome: string;
  readonly reasonCode: string | null;
  readonly scopeCareerId: string | null;
  readonly scopeProcessId: string | null;
}

export interface UserCommand {
  readonly username: string;
  readonly displayName: string;
  readonly initialPassword: string;
  readonly technical: boolean;
}

export interface AssignmentCommand {
  readonly roleCode: string;
  readonly careerId: string;
  readonly processId: string | null;
  readonly indicatorId: string | null;
  readonly validFrom: string;
  readonly validUntil: string | null;
  readonly exceptionReason: string | null;
}

export interface ConflictExceptionCommand {
  readonly policyId: string;
  readonly targetUserId: string;
  readonly careerId: string;
  readonly processId: string | null;
  readonly indicatorId: string | null;
  readonly justification: string;
  readonly validFrom: string;
  readonly validTo: string;
}

export interface AuditSearch {
  readonly objectType?: string;
  readonly objectId?: string;
  readonly page?: number;
  readonly size?: number;
}

export const PILOT_CAREER_ID = '11111111-1111-1111-1111-111111111111';
export const PILOT_CONFLICT_POLICY_ID = '30000000-0000-0000-0000-000000000001';
export const PILOT_ROLES = [
  {code: 'FUNCTIONAL_ADMIN', name: 'Administrador funcional'},
  {code: 'COORDINATOR', name: 'Coordinador de autoevaluación'},
  {code: 'RESPONSIBLE', name: 'Responsable de evidencia'},
  {code: 'REVIEWER', name: 'Revisor documental'},
  {code: 'EVALUATOR', name: 'Evaluador académico'}
] as const;
