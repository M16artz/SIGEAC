import {Routes} from '@angular/router';
import {ProcessCreatePage} from './pages/process-create.page';
import {ProcessDetailPage} from './pages/process-detail.page';
import {ProcessPlanningPage} from './pages/process-planning.page';
import {ProcessActivitiesPage} from './pages/process-activities.page';

export const PROCESS_ROUTES: Routes = [
  {path: '', pathMatch: 'full', component: ProcessCreatePage},
  {path: ':processId/planning', component: ProcessPlanningPage},
  {path: ':processId/activities', component: ProcessActivitiesPage},
  {path: ':processId', component: ProcessDetailPage}
];
