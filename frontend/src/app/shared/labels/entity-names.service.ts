import {Injectable, computed, effect, inject, signal} from '@angular/core';
import {EMPTY, Subscription, expand, reduce, switchMap} from 'rxjs';
import {Career, CareerApi} from '../../features/careers/data-access/career-api.service';
import {ProcessApi, ProcessView} from '../../features/processes/data-access/process-api.service';
import {AccessApi} from '../../features/access/data-access/access-api.service';
import {AccessSessionService} from '../../features/access/data-access/access-session.service';
import {UserView} from '../../features/access/data-access/access.models';
import {displayLabel} from './display-labels';

/** Read-only presentation catalogue. IDs remain the values of forms and HTTP commands. */
@Injectable({providedIn: 'root'})
export class EntityNamesService {
  private readonly careerApi = inject(CareerApi);
  private readonly processApi = inject(ProcessApi);
  private readonly accessApi = inject(AccessApi);
  private readonly session = inject(AccessSessionService);
  readonly careers = signal<readonly Career[]>([]);
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly users = signal<readonly UserView[]>([]);
  readonly loading = signal(false);
  readonly indicators = computed(() => [...new Map(this.processes().flatMap(process =>
    process.indicators.map(indicator => [indicator.id, indicator] as const))).values()]);

  constructor() {
    effect(onCleanup => {
      const identity = this.session.identity();
      const requests = new Subscription();
      this.careers.set([]); this.processes.set([]); this.users.set([]);
      this.loading.set(!!identity && !identity.technical);
      if (identity && !identity.technical) {
        // Both catalogue visibility and record-level scope are enforced by existing APIs.
        requests.add(this.careerApi.access().pipe(switchMap(() => this.careerApi.careers(0, '', undefined, 100)),
          expand(page => (page.page + 1) * page.size < page.totalElements
            ? this.careerApi.careers(page.page + 1, '', undefined, 100) : EMPTY, 1),
          reduce((all, page) => [...all, ...page.items], [] as readonly Career[])
        ).subscribe({next: items => {this.careers.set(items); this.loading.set(false);}, error: () => this.loading.set(false)}));
        requests.add(this.processApi.list().subscribe({next: items => this.processes.set(items), error: () => {}}));
        if (identity.assignments.some(assignment => assignment.roleCode === 'FUNCTIONAL_ADMIN')) {
          requests.add(this.accessApi.listUsers(0, 100).pipe(
            expand(page => (page.page + 1) * page.size < page.total
              ? this.accessApi.listUsers(page.page + 1, 100) : EMPTY, 1),
            reduce((all, page) => [...all, ...page.items], [] as readonly UserView[])
          ).subscribe({next: items => this.users.set(items), error: () => {}}));
        }
      }
      // A logout, identity/permission change or app destruction cancels requests and clears old names.
      onCleanup(() => requests.unsubscribe());
    });
  }

  career(id: string): string { return this.careers().find(item => item.id === id)?.data.name ?? (this.loading() ? 'Consultando carrera…' : 'Carrera no disponible'); }
  process(id: string): string { return this.processes().find(item => item.id === id)?.name ?? 'Proceso no disponible'; }
  indicator(id: string): string { return this.indicators().find(item => item.id === id)?.name ?? 'Indicador no disponible'; }
  user(id: string): string {
    const identity = this.session.identity();
    return (identity?.id === id ? identity.displayName : this.users().find(item => item.id === id)?.displayName) ?? 'Usuario no disponible';
  }
  assignment(id: string): string {
    const assignment = this.session.identity()?.assignments.find(item => item.id === id);
    return assignment ? `${displayLabel(assignment.roleName || assignment.roleCode)} (${this.career(assignment.careerId)})` : 'Asignación no disponible';
  }
}
