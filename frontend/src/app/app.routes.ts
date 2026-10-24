import { Routes } from '@angular/router';
import { accessGuard } from './features/access/access.guard';

export const routes: Routes = [
  { path: 'careers', canActivate: [accessGuard], loadChildren: () => import('./features/careers/careers.routes').then(m=>m.CAREER_ROUTES) },
  { path: '', pathMatch: 'full', redirectTo: 'access' },
  { path: 'access', loadChildren: () => import('./features/access/access.routes').then((m) => m.ACCESS_ROUTES) },
  { path: 'models', canActivate: [accessGuard], loadChildren: () => import('./features/models/models.routes').then((m) => m.MODEL_ROUTES) },
  { path: 'processes', canActivate: [accessGuard], loadChildren: () => import('./features/processes/processes.routes').then((m) => m.PROCESS_ROUTES) },
  { path: 'evidence', canActivate: [accessGuard], loadChildren: () => import('./features/evidence/evidence.routes').then((m) => m.EVIDENCE_ROUTES) },
  { path: 'reviews', canActivate: [accessGuard], loadChildren: () => import('./features/reviews/reviews.routes').then((m) => m.REVIEW_ROUTES) },
  { path: '**', redirectTo: 'access' }
];
