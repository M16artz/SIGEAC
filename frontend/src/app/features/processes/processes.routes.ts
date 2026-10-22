import {Routes} from '@angular/router';
import {ProcessCreatePage} from './pages/process-create.page';
import {ProcessDetailPage} from './pages/process-detail.page';

export const PROCESS_ROUTES: Routes = [
  {path: '', pathMatch: 'full', component: ProcessCreatePage},
  {path: ':processId', component: ProcessDetailPage}
];
