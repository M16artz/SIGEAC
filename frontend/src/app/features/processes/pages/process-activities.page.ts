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
    <p><a routerLink="/processes">Volver a procesos</a></p>
    <h2>Actividades del proceso</h2>
    @if (loading()) { <p role="status">Consultando actividades…</p> }
    @if (error()) { <p role="alert">{{error()}}</p> }
    @if (process(); as current) {
      <p>{{current.code}} · {{current.name}}</p>
      <p>Se muestran las actividades permitidas por sus asignaciones y permisos vigentes.</p>
      @if (current.state === 'SUSPENDED') {
        <p role="status">El proceso está suspendido. Las actuaciones ordinarias están bloqueadas.</p>
      }
      @if (activities().length) {
        <table>
          <caption>Actividades visibles en su ámbito</caption>
          <thead><tr><th>Actividad</th><th>Indicador</th><th>Vencimiento (hora local)</th><th>Estado</th></tr></thead>
          <tbody>
            @for (activity of activities(); track activity.id) {
              <tr><td>{{activity.title}}</td><td>{{indicatorName(activity.indicatorId)}}</td>
                <td>{{activity.dueAt | date:'yyyy-MM-dd HH:mm'}}</td><td>Planificada</td></tr>
            }
          </tbody>
        </table>
      } @else { <p>No hay actividades para sus asignaciones vigentes.</p> }
    }
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
