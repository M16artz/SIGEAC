import {describe, expect, it} from 'vitest';
import {displayLabel} from './display-labels';

describe('displayLabel', () => {
  it('translates roles without changing the technical code', () => {
    expect(displayLabel('FUNCTIONAL_ADMIN')).toBe('Administrador funcional');
    expect(displayLabel('COORDINATOR')).toBe('Coordinador');
    expect(displayLabel('READ_ONLY')).toBe('Solo lectura');
  });

  it('translates process states and audit values', () => {
    expect(displayLabel('IN_PROGRESS')).toBe('En proceso');
    expect(displayLabel('PENDING_REVIEW')).toBe('Pendiente de revisión');
    expect(displayLabel('SESSION_LOGIN')).toBe('Inicio de sesión');
    expect(displayLabel('SESSION_LOGOUT')).toBe('Cierre de sesión');
    expect(displayLabel('ACCESS_SESSION')).toBe('Sesión de usuario');
    expect(displayLabel('ACCESS_DENIED')).toBe('Acceso denegado');
    expect(displayLabel('CAREER_CREATED')).toBe('Carrera creada');
    expect(displayLabel('UNIT_STATE_CHANGED')).toBe('Estado de la unidad académica actualizado');
    expect(displayLabel('REPORT_EMISSION_REJECTED')).toBe('Emisión de la versión de informe rechazada');
    expect(displayLabel('ImprovementAction')).toBe('Acción de mejora');
    expect(displayLabel('ApprovalDecision')).toBe('Decisión de aprobación del informe');
    expect(displayLabel('STALE_WORKFLOW')).toBe('Hubo cambios concurrentes; recargue antes de continuar.');
    expect(displayLabel('MODEL_VERSION_NOT_FOUND')).toBe('La versión del modelo no existe o no está visible.');
  });

  it('respects module-specific meanings', () => {
    expect(displayLabel('IN_PROGRESS', 'improvement')).toBe('En ejecución');
    expect(displayLabel('PENDING_VERIFICATION', 'improvement')).toBe('Pendiente de verificación');
    expect(displayLabel('ACCEPTED', 'coverage')).toBe('Aceptada');
    expect(displayLabel('REJECTED', 'coverage')).toBe('Rechazada');
  });

  it('does not invent meanings for unknown codes or rewrite free text', () => {
    expect(displayLabel('UNKNOWN_CODE')).toBe('Etiqueta pendiente de definición');
    expect(displayLabel('Texto institucional')).toBe('Texto institucional');
    expect(displayLabel(null)).toBe('—');
    expect(displayLabel(undefined)).toBe('—');
    expect(displayLabel('')).toBe('—');
  });

  it('presents booleans without exposing technical values', () => {
    expect(displayLabel(true)).toBe('Sí');
    expect(displayLabel(false)).toBe('No');
  });
});
