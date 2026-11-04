import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AccessSessionService } from '../../features/access/data-access/access-session.service';
import type { CurrentIdentity } from '../../features/access/data-access/access.models';
import { NAV_MODULES, NavModule } from './app-layout.navigation';

@Component({
  selector: 'sec-app-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-layout.component.html',
  styleUrl: './app-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppLayoutComponent {
  private readonly router = inject(Router);
  readonly session = inject(AccessSessionService);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects)
    ),
    {initialValue: this.router.url}
  );

  readonly isLogin = computed(() => this.currentUrl().startsWith('/access/login'));

  readonly modules = computed<readonly NavModule[]>(() => {
    const identity = this.session.identity();
    return NAV_MODULES
      .filter((module) => this.allowed(module.roles, identity))
      .map((module) => ({
        ...module,
        items: module.items.filter((item) => this.allowed(item.roles, identity))
      }));
  });

  readonly activeModule = computed(() => {
    const segment = this.currentUrl().split('?')[0].split('/')[1] ?? '';
    return this.modules().find((module) => module.id === segment) ?? this.modules().at(0) ?? null;
  });

  readonly roleLabel = computed(() => {
    const identity = this.session.identity();
    if (!identity) {
      return '';
    }
    if (identity.assignments.length === 0) {
      return identity.technical ? 'Cuenta técnica' : 'Sin rol asignado';
    }
    const names = [...new Set(identity.assignments.map((assignment) => assignment.roleName))];
    return names.length > 1 ? `${names[0]} +${names.length - 1}` : names[0];
  });

  readonly initials = computed(() => {
    const name = this.session.identity()?.displayName?.trim();
    if (!name) {
      return '?';
    }
    const parts = name.split(/\s+/).filter(Boolean);
    const first = parts[0]?.charAt(0) ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1]?.charAt(0) ?? '' : '';
    return `${first}${last}`.toUpperCase();
  });

  readonly mobileOpen = signal(false);
  readonly profileOpen = signal(false);

  openModule(module: NavModule): void {
    this.mobileOpen.set(false);
    this.profileOpen.set(false);
    const target = module.items[0]?.path;
    if (target) {
      void this.router.navigateByUrl(target);
    }
  }

  logout(): void {
    this.profileOpen.set(false);
    this.session.logout().subscribe({
      next: () => void this.router.navigate(['/access/login']),
      error: () => {
        this.session.clear();
        void this.router.navigate(['/access/login']);
      }
    });
  }

  private allowed(roles: readonly string[] | undefined, identity: CurrentIdentity | null): boolean {
    return !roles || (identity?.assignments.some((assignment) => roles.includes(assignment.roleCode)) ?? false);
  }
}
