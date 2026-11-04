import { describe, expect, it } from 'vitest';
import { isApiProblem } from './problem';

describe('isApiProblem', () => {
  it('accepts the stable RFC 9457 extension fields', () => {
    expect(isApiProblem({type:'urn:test',title:'Forbidden',status:403,detail:'Denied',instance:'/test',code:'ACCESS_DENIED',correlationId:'test-123',details:{}})).toBe(true);
  });

  it('rejects objects without safe correlation data', () => {
    expect(isApiProblem({type:'urn:test',title:'Forbidden',status:403,detail:'Denied',instance:'/test',code:'ACCESS_DENIED',correlationId:'short',details:{}})).toBe(false);
  });
});
