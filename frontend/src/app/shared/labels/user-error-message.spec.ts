import {describe, expect, it} from 'vitest';
import {ApiProblemError} from '../../core/http/api-client';
import {userErrorMessage} from './user-error-message';
describe('userErrorMessage', () => {
  it('uses reviewed local labels without displaying backend diagnostics', () => {
    const problem = new ApiProblemError({code: 'STALE_REVISION', status: 412, detail: 'SQLSTATE secret UUID', title: 'Exception', type: '', instance: '', correlationId: 'private', details: {}});
    expect(userErrorMessage(problem)).toBe('Revisión desactualizada');
    expect(userErrorMessage(new ApiProblemError({...problem.problem, code: 'NEW_INTERNAL_ERROR', status: 500}), 'No se pudo consultar.')).toBe('No se pudo consultar.');
    expect(userErrorMessage(new ApiProblemError({...problem.problem, code: 'NEW_AUTHORIZATION_ERROR', status: 403}))).toContain('No dispone de permiso');
  });
});
