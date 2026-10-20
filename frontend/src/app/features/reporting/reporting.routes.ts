import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-reporting-page',standalone:true,template:'<h2>Seguimiento y cierre</h2><p>Capacidad preparada para I6.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class ReportingPageComponent {}
export const REPORTING_ROUTES: Routes = [{path:'',component:ReportingPageComponent}];
