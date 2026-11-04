import {
  ChangeDetectionStrategy,
  Component,
  ViewEncapsulation,
  computed,
  inject
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccessSessionService } from '../data-access/access-session.service';
import { AssignmentView } from '../data-access/access.models';

interface QuickLink {
  readonly path: string;
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

const QUICK_LINKS: readonly QuickLink[] = [
  {
    path: '/access/users',
    title: 'Usuarios y asignaciones',
    description: 'Habilite identidades y asigne funciones con ámbito y vigencia.',
    icon: 'M15 19.5a6 6 0 0 0-12 0 M9 11.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z M17 11.5a2.8 2.8 0 1 0 0-5.6 M19 19.5a5 5 0 0 0-3.2-4.6'
  },
  {
    path: '/access/conflicts',
    title: 'Conflictos y excepciones',
    description: 'Revise incompatibilidades y excepciones temporales del acceso.',
    icon: 'M12 4.5 21 20H3L12 4.5Z M12 10v4 M12 17h.01'
  },
  {
    path: '/access/audit',
    title: 'Auditoría de acceso',
    description: 'Consulte la trazabilidad de cambios y decisiones registradas.',
    icon: 'M12 3.5 20 7v5c0 4.5-3.2 8.2-8 9.5-4.8-1.3-8-5-8-9.5V7l8-3.5Z M9 12l2 2 4-4.5'
  }
];

@Component({
  selector: 'sec-access-home',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  templateUrl: './access-home.page.html',
  styleUrl: './access-home.page.scss'
})
export class AccessHomePageComponent {
  readonly session = inject(AccessSessionService);

  readonly identity = this.session.identity;
  readonly assignments = computed<readonly AssignmentView[]>(
    () => this.identity()?.assignments ?? []
  );

  readonly roleCount = computed(
    () => new Set(this.assignments().map((assignment) => assignment.roleCode)).size
  );

  readonly scopeCount = computed(
    () =>
      new Set(
        this.assignments().map(
          (assignment) =>
            `${assignment.careerId}|${assignment.processId ?? ''}|${assignment.indicatorId ?? ''}`
        )
      ).size
  );

  readonly careerCount = computed(
    () => new Set(this.assignments().map((assignment) => assignment.careerId)).size
  );

  readonly roleSummary = computed(() => {
    const roles = [...new Set(this.assignments().map((assignment) => assignment.roleName))];
    return roles.length > 0 ? roles.join(' · ') : 'Sin funciones vigentes';
  });

  readonly initials = computed(() => {
    const name = this.identity()?.displayName?.trim() ?? '';
    if (!name) {
      return '—';
    }
    const parts = name.split(/\s+/);
    const chosen = parts.length > 1 ? [parts[0], parts[parts.length - 1]] : parts;
    return chosen
      .map((part) => part.charAt(0))
      .join('')
      .toUpperCase();
  });

  readonly quickLinks = computed<readonly QuickLink[]>(() => {
    const roles = new Set(this.assignments().map((assignment) => assignment.roleCode));
    if (roles.has('FUNCTIONAL_ADMIN')) {
      return QUICK_LINKS;
    }
    if (roles.has('COORDINATOR')) {
      return QUICK_LINKS.filter((link) => link.path === '/access/audit');
    }
    return [];
  });

  formatDate(value: string): string {
    const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
    return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
  }
}
