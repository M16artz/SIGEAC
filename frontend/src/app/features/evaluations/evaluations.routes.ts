import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-evaluations-page',standalone:true,template:'<h2>Valoración académica</h2><p>Capacidad preparada para I5.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class EvaluationsPageComponent {}
export const EVALUATION_ROUTES: Routes = [{path:'',component:EvaluationsPageComponent}];
