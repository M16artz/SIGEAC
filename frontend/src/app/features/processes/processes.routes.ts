import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-processes-page',standalone:true,template:'<h2>Procesos de autoevaluación</h2><p>Capacidad preparada para I2.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class ProcessesPageComponent {}
export const PROCESS_ROUTES: Routes = [{path:'',component:ProcessesPageComponent}];
