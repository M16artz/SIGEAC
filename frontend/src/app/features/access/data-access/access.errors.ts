import { ApiProblemError } from '../../../core/http/api-client';

export function accessErrorMessage(error: unknown): string {
  if (error instanceof ApiProblemError) {
    return error.problem.detail ?? error.problem.title;
  }
  return 'No fue posible completar la operación. Intente nuevamente.';
}
