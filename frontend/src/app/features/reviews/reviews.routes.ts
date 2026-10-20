import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-reviews-page',standalone:true,template:'<h2>Revisión documental</h2><p>Capacidad preparada para I4.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class ReviewsPageComponent {}
export const REVIEW_ROUTES: Routes = [{path:'',component:ReviewsPageComponent}];
