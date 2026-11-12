/** Actual object types emitted by the API. Values remain the original query codes. */
export const AUDIT_OBJECT_TYPES = [
  'ACCESS_IDENTITY', 'ACCESS_SESSION', 'FUNCTIONAL_ASSIGNMENT', 'CONFLICT_EXCEPTION', 'AUDIT_QUERY',
  'ACADEMIC_UNIT', 'CAREER', 'EVALUATION_MODEL', 'MODEL_VERSION', 'NORMATIVE_REFERENCE',
  'SELF_ASSESSMENT_PROCESS', 'EVIDENCE_VERSION', 'ACADEMIC_EVALUATION', 'ACADEMIC_INPUT_SET',
  'ACADEMIC_CALCULATION', 'ACADEMIC_RECONCILIATION', 'DOCUMENTARY_CLOSURE', 'INTEGRAL_CLOSURE', 'ARCHIVE_RECORD',
  'DOCUMENTARY_SNAPSHOT', 'INDICATOR', 'ImprovementPlan', 'ImprovementAction', 'Finding', 'Consolidation',
  'ReportVersion', 'ReportObservation', 'ObservationReply', 'ReportSocialization', 'ApprovalDecision', 'ArchiveRecord'
] as const;
