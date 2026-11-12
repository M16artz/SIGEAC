import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {finalize, forkJoin} from 'rxjs';
import {ProcessActivity, ProcessApi, ProcessView} from '../data-access/process-api.service';

@Component({
  selector: 'sec-process-activities',
  standalone: true,
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="sigeac-page">
    <header class="sigeac-page-header">
      <span class="sigeac-badge">Procesos</span>
      <h2>Actividades del proceso</h2>
      <p>Consulte las actividades y los plazos de sus ámbitos autorizados.</p>
    </header>
    <section class="sigeac-page-body">
    <p><a routerLink="/processes">Volver a procesos</a></p>
    @if (loading()) { <p role="status">Consultando actividades…</p> }
    @if (error()) { <p role="alert">{{error()}}</p> }
    @if (process(); as current) {
      <p>{{current.code}} · {{current.name}}</p>
      <p>Se muestran las actividades permitidas por sus asignaciones y permisos vigentes.</p>
      @if (current.state === 'SUSPENDED') {
        <p role="status">El proceso está suspendido. Las actuaciones ordinarias están bloqueadas.</p>
      }
      @if (activities().length) {
        <div class="sigeac-table-scroll" tabindex="0" role="region" aria-label="Actividades del proceso">
        <table class="sigeac-table-layout sigeac-table-layout--activities">
          <caption>Actividades visibles en su ámbito</caption>
          <thead><tr><th>Actividad</th><th>Indicador</th><th>Vencimiento (hora local)</th><th>Estado</th></tr></thead>
          <tbody>
            @for (activity of activities(); track activity.id) {
              <tr><td>{{activity.title}}</td><td>{{indicatorName(activity.indicatorId)}}</td>
                <td>{{activity.dueAt | date:'yyyy-MM-dd HH:mm'}}</td><td>Planificada</td></tr>
            }
          </tbody>
        </table>
        </div>
      } @else { <p>No hay actividades para sus asignaciones vigentes.</p> }
    }
    </section>
    </div>
  `
})
export class ProcessActivitiesPage implements OnInit {
  readonly process = signal<ProcessView | null>(null);
  readonly activities = signal<readonly ProcessActivity[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(private readonly api: ProcessApi, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('processId');
    if (!id) { this.loading.set(false); this.error.set('No se indicó el proceso.'); return; }
    forkJoin({process: this.api.get(id), activities: this.api.activities(id)})
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: value => { this.process.set(value.process); this.activities.set(value.activities); },
        error: () => this.error.set('No fue posible consultar las actividades del proceso.')
      });
  }

  indicatorName(id: string): string {
    const indicator = this.process()?.indicators.find(value => value.id === id);
    return indicator ? `${indicator.code} · ${indicator.name}` : id;
  }
}
