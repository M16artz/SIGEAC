import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-models-page',standalone:true,template:'<h2>Modelos de evaluación</h2><p>Capacidad preparada para I1.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class ModelsPageComponent {}
export const MODEL_ROUTES: Routes = [{path:'',component:ModelsPageComponent}];
