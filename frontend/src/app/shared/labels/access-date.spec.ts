import {describe, expect, it} from 'vitest';
import {accessDate} from './access-date';
describe('accessDate', () => {
  it('only treats the documented access sentinel or absent end as indefinite', () => {
    expect(accessDate('9999-12-31T00:00:00Z')).toBe('Sin fecha de finalización');
    expect(accessDate('9999-12-31T00:00:00Z', 'Indefinida')).toBe('Indefinida');
    expect(accessDate('9999-12-31T00:00:00.000Z')).toBe('Sin fecha de finalización');
    expect(accessDate(null)).toBe('Sin fecha de finalización');
    expect(accessDate('9999-12-30T00:00:00Z')).toBe('30/12/9999');
    expect(accessDate('2030-01-01T00:00:00Z')).toBe('01/01/2030');
    expect(accessDate('2030-01-01T00:00:00Z', 'Indefinida')).toBe('01/01/2030');
  });
  it('does not shift calendar dates through UTC parsing or expose malformed internal values', () => {
    expect(accessDate('2026-12-01T00:00:00Z')).toBe('01/12/2026');
    expect(accessDate('INTERNAL_VALUE')).toBe('Fecha no disponible');
  });
});
