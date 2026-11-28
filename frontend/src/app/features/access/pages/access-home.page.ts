import {
  ChangeDetectionStrategy,
  Component,
  ViewEncapsulation,
  computed,
  inject,
  effect,
  DestroyRef,
  signal
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccessSessionService } from '../data-access/access-session.service';
import { AssignmentView } from '../data-access/access.models';
import { DisplayLabelPipe } from '../../../shared/labels/display-label.pipe';
import { AccessApi } from '../data-access/access-api.service';
import { CareerApi } from '../../careers/data-access/career-api.service';
import { Subscription } from 'rxjs';
import {EntityNamesService} from '../../../shared/labels/entity-names.service';
import {accessDate} from '../../../shared/labels/access-date';

interface DashboardCount {
  readonly value: number | null;
  readonly state: 'loading' | 'ready' | 'error' | 'unavailable';
}

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
  providers: [EntityNamesService],
  imports: [RouterLink, DisplayLabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  templateUrl: './access-home.page.html',
  styleUrls: ['./access-home.page.scss', './access-home-calendar.scss', './access-tables.scss']
})
export class AccessHomePageComponent {
  readonly names = inject(EntityNamesService);
  readonly session = inject(AccessSessionService);
  private readonly api = inject(AccessApi);
  private readonly careers = inject(CareerApi);
  private readonly reload = signal(0);
  readonly usersCount = signal<DashboardCount>({value: null, state: 'unavailable'});
  readonly configuredCareers = signal<DashboardCount>({value: null, state: 'unavailable'});

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

  readonly today = signal(new Date());
  readonly shownMonth = signal(this.today().getFullYear() * 12 + this.today().getMonth());
  readonly weekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
  readonly shortWeekdays = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
  readonly weekdayNames = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
  readonly monthLabel = computed(() => new Intl.DateTimeFormat('es-EC', {month: 'long', year: 'numeric'})
    .format(this.monthDate()));
  readonly todayLabel = computed(() => new Intl.DateTimeFormat('es-EC', {day: 'numeric', month: 'long', year: 'numeric'})
    .format(this.today()));
  readonly calendarWeeks = computed(() => {
    const date = this.monthDate();
    const firstWeekday = (date.getDay() + 6) % 7;
    const year = date.getFullYear(), month = date.getMonth();
    const days = new Date(year, month + 1, 0, 12).getDate();
    const today = this.today();
    return Array.from({length: 6}, (_, week) => Array.from({length: 7}, (_, weekday) => {
      const day = week * 7 + weekday - firstWeekday + 1;
      return day < 1 || day > days ? null : {
        day,
        iso: `${String(year).padStart(4, '0')}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`,
        current: today.getFullYear() === year && today.getMonth() === month && today.getDate() === day
      };
    }));
  });

  constructor() {
    // Counts are independent: a forbidden/unavailable catalogue must not hide the session ledger.
    effect(onCleanup => {
      const identity = this.identity();
      this.reload();
      const requests = new Subscription();
      this.usersCount.set({value: null, state: 'unavailable'});
      this.configuredCareers.set({value: null, state: 'unavailable'});
      if (identity && !identity.technical) {
        if (identity.assignments.some(item => item.roleCode === 'FUNCTIONAL_ADMIN')) {
          this.usersCount.set({value: null, state: 'loading'});
          requests.add(this.api.listUsers(0, 1).subscribe({
            next: page => this.usersCount.set({value: page.total, state: 'ready'}),
            error: () => this.usersCount.set({value: null, state: 'error'})
          }));
        }
        this.configuredCareers.set({value: null, state: 'loading'});
        requests.add(this.careers.access().subscribe({
          next: () => requests.add(this.careers.careers(0, '', undefined, 1).subscribe({
            next: page => this.configuredCareers.set({value: page.totalElements, state: 'ready'}),
            error: () => this.configuredCareers.set({value: null, state: 'error'})
          })),
          error: () => this.configuredCareers.set({value: null, state: 'unavailable'})
        }));
      }
      onCleanup(() => requests.unsubscribe());
    });
    // Read local calendar fields; never parse a date-only ISO string as a UTC instant.
    const timer = setInterval(() => {
      const previous = this.today();
      const now = new Date();
      if (previous.toDateString() !== now.toDateString()) {
        if (this.shownMonth() === previous.getFullYear() * 12 + previous.getMonth()) {
          this.shownMonth.set(now.getFullYear() * 12 + now.getMonth());
        }
        this.today.set(now);
      }
    }, 60000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  retryCounts(): void { this.reload.update(value => value + 1); }
  changeMonth(delta: number): void {
    this.shownMonth.update(value => Math.max(1200, Math.min(119999, value + delta)));
  }
  private monthDate(): Date {
    return new Date(Math.floor(this.shownMonth() / 12), this.shownMonth() % 12, 1, 12);
  }

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
    return accessDate(value, 'Indefinida');
  }
}
