import {Routes} from '@angular/router';
import {Component} from '@angular/core';

@Component({selector:'sec-improvement-placeholder',standalone:true,template:'<section><h1>Mejora continua</h1></section>'})
class ImprovementPlaceholder {}

export const IMPROVEMENT_ROUTES: Routes = [{path:'', component: ImprovementPlaceholder}];
