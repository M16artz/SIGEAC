import {Routes} from '@angular/router';
import {DocumentaryDashboardPage} from './pages/documentary-dashboard.page';
import {DocumentaryHistoryPage} from './pages/documentary-history.page';
import {DocumentaryClosurePageComponent} from './pages/documentary-closure.page';

export const REPORTING_ROUTES: Routes = [
  {path: '', pathMatch: 'full', component: DocumentaryDashboardPage},
  {path: 'history', component: DocumentaryHistoryPage},
  {path: 'closure', component: DocumentaryClosurePageComponent}
];
