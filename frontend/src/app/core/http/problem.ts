export interface ApiProblem {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly detail?: string;
  readonly code: string;
  readonly correlationId: string;
  readonly details?: Readonly<Record<string, string>>;
}

export function isApiProblem(value: unknown): value is ApiProblem {
  if (typeof value !== 'object' || value === null) return false;
  const candidate = value as Partial<ApiProblem>;
  return typeof candidate.type === 'string' && typeof candidate.title === 'string' &&
    typeof candidate.status === 'number' && typeof candidate.code === 'string' &&
    typeof candidate.correlationId === 'string';
}
