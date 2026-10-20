import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'access' },
  { path: 'access', loadChildren: () => import('./features/access/access.routes').then((m) => m.ACCESS_ROUTES) },
  { path: 'models', loadChildren: () => import('./features/models/models.routes').then((m) => m.MODEL_ROUTES) },
  { path: 'processes', loadChildren: () => import('./features/processes/processes.routes').then((m) => m.PROCESS_ROUTES) },
  { path: 'evidence', loadChildren: () => import('./features/evidence/evidence.routes').then((m) => m.EVIDENCE_ROUTES) },
  { path: 'reviews', loadChildren: () => import('./features/reviews/reviews.routes').then((m) => m.REVIEW_ROUTES) },
  { path: 'evaluations', loadChildren: () => import('./features/evaluations/evaluations.routes').then((m) => m.EVALUATION_ROUTES) },
  { path: 'reporting', loadChildren: () => import('./features/reporting/reporting.routes').then((m) => m.REPORTING_ROUTES) },
  { path: '**', redirectTo: 'access' }
];
