import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-access-page',standalone:true,template:'<h2>Acceso y ámbitos</h2><p>Capacidad preparada para I0.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class AccessPageComponent {}
export const ACCESS_ROUTES: Routes = [{path:'',component:AccessPageComponent}];
