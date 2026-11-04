/**
 * Navegación principal de SIGEAC.
 *
 * Cada módulo y cada opción apuntan a rutas reales declaradas en `app.routes.ts`
 * y en los `*.routes.ts` de cada feature. Los roles replican exactamente los
 * permisos que ya aplicaba el layout anterior.
 */

export interface NavItem {
  readonly label: string;
  readonly path: string;
  readonly icon: string;
  readonly roles?: readonly string[];
}

export interface NavModule {
  readonly id: string;
  readonly label: string;
  readonly icon: string;
  readonly roles?: readonly string[];
  readonly items: readonly NavItem[];
}

const ICONS = {
  shield: 'M12 3.5 19 6.3v5.2c0 4.2-2.9 7.7-7 9-4.1-1.3-7-4.8-7-9V6.3L12 3.5Z M9.2 12l2 2 3.8-4',
  home: 'M4 10.5 12 4l8 6.5V20h-5v-6H9v6H4Z',
  users: 'M8 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z M2.5 20.5c0-3 2.5-5.5 5.5-5.5s5.5 2.5 5.5 5.5 M16 4.5a3.5 3.5 0 0 1 0 7 M17 15.2c2.6.5 4.5 2.7 4.5 5.3',
  scale: 'M12 4v16 M5 8h14 M7 8l-3 6h6L7 8Z M17 8l-3 6h6l-3-6Z M8 20h8',
  audit: 'M8 6h12 M8 12h12 M8 18h12 M4 6h.01 M4 12h.01 M4 18h.01',
  graduation: 'M3 9.5 12 5l9 4.5-9 4.5-9-4.5Z M6.5 11.6V16c0 1.4 2.5 2.6 5.5 2.6s5.5-1.2 5.5-2.6v-4.4 M21 9.5V14',
  building: 'M4.5 20.5V5.5h9v15 M13.5 10h6v10.5 M7.5 9h3 M7.5 13h3 M7.5 17h3 M16.5 13h1 M16.5 17h1',
  layers: 'M12 3 20.5 7.5 12 12 3.5 7.5 12 3Z M3.5 12 12 16.5 20.5 12 M3.5 16.5 12 21l8.5-4.5',
  history: 'M4 12a8 8 0 1 0 2.5-5.8 M4 4.5V9h4.5 M12 8v4.5l3 1.8',
  compare: 'M8 4.5H4.5v15H8 M16 4.5h3.5v15H16 M10 12h4 M12 10l2 2-2 2',
  workflow: 'M4 4.5h6v6H4Z M14 13.5h6v6h-6Z M10 7.5h3.5a3 3 0 0 1 3 3v3 M7 10.5v6a3 3 0 0 0 3 3h1',
  file: 'M6.5 3.5h7l4 4v13h-11Z M13.5 3.5v4h4 M9.5 12h5 M9.5 15.5h5',
  plus: 'M12 5v14 M5 12h14',
  inbox: 'M4 4.5h16v15H4Z M4 13h4l2 3h4l2-3h4',
  clipboard: 'M9 4.5h6 M8 6H5.5v14.5h13V6H16 M8.5 12l2.5 2.5 4.5-5',
  chart: 'M4 20.5V13 M10 20.5V4.5 M16 20.5v-6 M3 20.5h18',
  document: 'M6.5 3.5h7l4 4v13h-11Z M13.5 3.5v4h4 M9.5 11.5h5 M9.5 15h5 M9.5 18h3',
  trend: 'M3.5 17.5 9 12l3.5 3.5L20 7 M15 7h5v5',
  box: 'M3.5 7.5h17v4h-17Z M5.5 11.5v9h13v-9 M9.5 15.5h5',
  check: 'M5 12.5l4.5 4.5L19 7.5'
} as const;

export const NAV_MODULES: readonly NavModule[] = [
  {
    id: 'access',
    label: 'Acceso',
    icon: ICONS.shield,
    items: [
      {label: 'Inicio', path: '/access', icon: ICONS.home},
      {label: 'Usuarios y asignaciones', path: '/access/users', icon: ICONS.users, roles: ['FUNCTIONAL_ADMIN']},
      {label: 'Conflictos y excepciones', path: '/access/conflicts', icon: ICONS.scale, roles: ['FUNCTIONAL_ADMIN']},
      {label: 'Auditoría de acceso', path: '/access/audit', icon: ICONS.audit, roles: ['FUNCTIONAL_ADMIN', 'COORDINATOR']}
    ]
  },
  {
    id: 'careers',
    label: 'Carreras',
    icon: ICONS.graduation,
    items: [
      {label: 'Catálogo de carreras', path: '/careers', icon: ICONS.building}
    ]
  },
  {
    id: 'models',
    label: 'Modelos',
    icon: ICONS.layers,
    roles: ['FUNCTIONAL_ADMIN', 'COORDINATOR'],
    items: [
      {label: 'Editor de modelo', path: '/models', icon: ICONS.layers},
      {label: 'Historial de versiones', path: '/models/history', icon: ICONS.history},
      {label: 'Comparar versiones', path: '/models/compare', icon: ICONS.compare},
      {label: 'Auditoría de modelos', path: '/models/audit', icon: ICONS.audit}
    ]
  },
  {
    id: 'processes',
    label: 'Procesos',
    icon: ICONS.workflow,
    roles: ['FUNCTIONAL_ADMIN', 'COORDINATOR', 'PROCESS_AUTHORITY'],
    items: [
      {label: 'Procesos de evaluación', path: '/processes', icon: ICONS.workflow}
    ]
  },
  {
    id: 'evidence',
    label: 'Evidencias',
    icon: ICONS.file,
    roles: ['RESPONSIBLE'],
    items: [
      {label: 'Bandeja de evidencias', path: '/evidence', icon: ICONS.file},
      {label: 'Nueva evidencia', path: '/evidence/new', icon: ICONS.plus},
      {label: 'Auditoría de evidencias', path: '/evidence/audit', icon: ICONS.audit}
    ]
  },
  {
    id: 'reviews',
    label: 'Revisión documental',
    icon: ICONS.inbox,
    roles: ['REVIEWER'],
    items: [
      {label: 'Bandeja de revisión', path: '/reviews', icon: ICONS.inbox}
    ]
  },
  {
    id: 'evaluations',
    label: 'Valoración académica',
    icon: ICONS.clipboard,
    roles: ['EVALUATOR', 'ACADEMIC_COMMITTEE'],
    items: [
      {label: 'Valoración por indicador', path: '/evaluations', icon: ICONS.clipboard},
      {label: 'Variables y cálculo', path: '/evaluations/quantitative', icon: ICONS.chart},
      {label: 'Discrepancias y conciliación', path: '/evaluations/reconciliation', icon: ICONS.scale}
    ]
  },
  {
    id: 'reporting',
    label: 'Seguimiento documental',
    icon: ICONS.chart,
    roles: ['COORDINATOR'],
    items: [
      {label: 'Tablero documental', path: '/reporting', icon: ICONS.chart},
      {label: 'Historial documental', path: '/reporting/history', icon: ICONS.history},
      {label: 'Cierre documental', path: '/reporting/closure', icon: ICONS.check}
    ]
  },
  {
    id: 'reports',
    label: 'Informes',
    icon: ICONS.document,
    roles: ['COORDINATOR', 'PROCESS_AUTHORITY'],
    items: [
      {label: 'Flujo de informes', path: '/reports', icon: ICONS.document}
    ]
  },
  {
    id: 'improvement',
    label: 'Mejora',
    icon: ICONS.trend,
    roles: ['COORDINATOR', 'PROCESS_AUTHORITY', 'RESPONSIBLE', 'REVIEWER'],
    items: [
      {label: 'Plan de mejora', path: '/improvement', icon: ICONS.trend}
    ]
  },
  {
    id: 'archive',
    label: 'Archivo',
    icon: ICONS.box,
    roles: ['COORDINATOR', 'PROCESS_AUTHORITY'],
    items: [
      {label: 'Archivo de procesos', path: '/archive', icon: ICONS.box}
    ]
  }
];
