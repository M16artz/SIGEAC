import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AppLayoutComponent } from './core/layout/app-layout.component';

@Component({selector:'sec-root',standalone:true,imports:[AppLayoutComponent],template:'<sec-app-layout />',changeDetection:ChangeDetectionStrategy.OnPush})
export class AppComponent {}
