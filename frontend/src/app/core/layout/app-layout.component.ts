import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({selector:'sec-app-layout',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./app-layout.component.html',changeDetection:ChangeDetectionStrategy.OnPush})
export class AppLayoutComponent {}
