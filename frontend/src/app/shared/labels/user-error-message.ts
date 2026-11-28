import {ApiProblemError} from '../../core/http/api-client';
import {DISPLAY_LABELS} from './display-labels';

/** Use local, reviewed copy rather than backend diagnostics or resource identifiers. */
export function userErrorMessage(error: unknown, fallback = 'No fue posible completar la operación. Intente nuevamente.'): string {
  if (!(error instanceof ApiProblemError)) return fallback;
  const code = error.problem.code;
  if (code === 'AUTHENTICATION_FAILED') return 'Las credenciales no fueron aceptadas.';
  if (Object.hasOwn(DISPLAY_LABELS, code)) return DISPLAY_LABELS[code];
  if (error.problem.status === 401) return 'Su sesión no está disponible. Inicie sesión nuevamente.';
  if (error.problem.status === 403) return 'No dispone de permiso para esta consulta u operación.';
  if (error.problem.status === 412) return 'La información cambió. Recargue antes de continuar.';
  return fallback;
}
