import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AccessSessionService } from '../../features/access/data-access/access-session.service';

@Component({selector:'sec-app-layout',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./app-layout.component.html',changeDetection:ChangeDetectionStrategy.OnPush})
export class AppLayoutComponent {
  readonly session = inject(AccessSessionService);

  hasRole(...roles: string[]): boolean {
    return this.session.identity()?.assignments.some(assignment => roles.includes(assignment.roleCode)) ?? false;
  }
}
