import {Routes} from '@angular/router';
import {Component} from '@angular/core';

@Component({selector:'sec-archive-placeholder',standalone:true,template:'<section><h1>Archivo</h1></section>'})
class ArchivePlaceholder {}

export const ARCHIVE_ROUTES: Routes = [{path:'', component: ArchivePlaceholder}];
