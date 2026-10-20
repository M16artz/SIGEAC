import { describe, expect, it } from 'vitest';
import { isApiProblem } from './problem';

describe('isApiProblem', () => {
  it('accepts the stable RFC 9457 extension fields', () => {
    expect(isApiProblem({type:'urn:test',title:'Forbidden',status:403,code:'ACCESS_DENIED',correlationId:'test-123'})).toBe(true);
  });

  it('rejects objects without safe correlation data', () => {
    expect(isApiProblem({title:'Forbidden',status:403,code:'ACCESS_DENIED'})).toBe(false);
  });
});
