import {ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {DatePipe, DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Subscription} from 'rxjs';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {DocumentaryProgressView, ReportingApi} from '../data-access/reporting-api.service';

@Component({
  selector: 'sec-documentary-dashboard',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="documentary-title" [attr.aria-busy]="loading()">
      <h2 id="documentary-title">Seguimiento documental</h2>
      <p><a routerLink="/reporting/history">Guardar y consultar cortes documentales</a></p>
      <p><a routerLink="/reporting/closure">Cerrar fase documental y consultar cierres</a></p>
      <label for="progress-process">Proceso</label>
      <select id="progress-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)">
        <option value="">Seleccione un proceso</option>
        @for (process of processes(); track process.id) {
          <option [value]="process.id">{{process.code}} · {{process.name}}</option>
        }
      </select>
      @if (loading()) { <p role="status">Consultando avance…</p> }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (view(); as current) {
        <p>{{current.scopeDescription}}</p>
        <p>Consulta actualizada: {{current.calculatedAt | date:'medium'}}</p>
        @if (current.state === 'NOT_CONFIGURED') {
          <p role="status">No hay fuentes obligatorias configuradas en este ámbito.</p>
        } @else if (current.state === 'NOT_APPLICABLE') {
          <p role="status">Todas las fuentes obligatorias están excluidas por decisiones de aplicabilidad.</p>
        }
        <dl>
          <dt>Recopilación</dt>
          <dd>{{current.collectionPercent === null ? 'No calculable' : (current.collectionPercent | number:'1.2-2') + ' %'}}
            ({{current.registeredSourceTotal}} / {{current.requiredSourceTotal}} fuentes)</dd>
          <dt>Revisión documental</dt>
          <dd>{{current.documentReviewPercent === null ? 'No calculable' : (current.documentReviewPercent | number:'1.2-2') + ' %'}}
            ({{current.acceptedSourceTotal}} / {{current.requiredSourceTotal}} fuentes)</dd>
          <dt>Valoración académica</dt>
          <dd>Se consulta por separado. La aceptación documental no determina cumplimiento académico.</dd>
        </dl>
        <p>Recopilación = fuentes remitidas / fuentes obligatorias aplicables × 100.
          Revisión = fuentes con cardinalidad aceptada / fuentes obligatorias aplicables × 100.</p>
        @if (sourceId) { <button type="button" (click)="focusSource('')">Ver todas las fuentes</button> }
        <table>
          <caption>Fuentes del ámbito autorizado</caption>
          <thead><tr><th scope="col">Fuente</th><th scope="col">Aplicabilidad</th>
            <th scope="col">Cardinalidad mínima</th><th scope="col">Coberturas vigentes</th></tr></thead>
          <tbody>
            @for (source of current.sources; track source.id) {
              <tr>
                <th scope="row"><button type="button" (click)="focusSource(source.id)">{{source.code}} · {{source.name}}</button></th>
                <td>{{!source.applicable ? 'Excluida' : source.required ? 'Obligatoria' : 'Opcional'}}</td>
                <td>{{source.cardinalityMin}}</td>
                <td>
                  @if (source.coverages.length === 0) { Sin remisiones }
                  @for (coverage of source.coverages; track coverage.versionId) {
                    <p>Versión {{coverage.versionId}} · {{outcomeLabel(coverage.outcome)}}</p>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
        <nav aria-label="Páginas de fuentes">
          <button type="button" [disabled]="loading() || current.page === 0" (click)="load(current.page - 1)">Anterior</button>
          <span>Página {{current.page + 1}} · {{current.totalSources}} fuentes</span>
          <button type="button" [disabled]="loading() || (current.page + 1) * current.size >= current.totalSources"
            (click)="load(current.page + 1)">Siguiente</button>
        </nav>
      }
    </section>
  `
})
export class DocumentaryDashboardPage implements OnInit, OnDestroy {
  private readonly api = inject(ReportingApi);
  private readonly processApi = inject(ProcessApi);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly view = signal<DocumentaryProgressView | null>(null);
  readonly error = signal('');
  readonly loading = signal(false);
  processId = '';
  sourceId = '';

  ngOnInit(): void {
    this.processApi.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: processes => {
        this.processes.set(processes);
        if (processes.length === 1) this.selectProcess(processes[0].id);
      },
      error: () => this.error.set('No fue posible consultar los procesos autorizados.')
    });
  }

  ngOnDestroy(): void { this.request?.unsubscribe(); }

  selectProcess(id: string): void {
    this.processId = id;
    this.sourceId = '';
    this.load();
  }

  focusSource(id: string): void {
    this.sourceId = id;
    this.load();
  }

  load(page = 0): void {
    this.request?.unsubscribe();
    this.view.set(null);
    this.error.set('');
    this.loading.set(false);
    if (!this.processId) return;
    this.loading.set(true);
    this.request = this.api.progress(this.processId, page, this.sourceId).subscribe({
      next: view => { this.view.set(view); this.loading.set(false); },
      error: () => {
        this.loading.set(false);
        this.error.set('No fue posible consultar el avance en el ámbito autorizado.');
      }
    });
  }

  outcomeLabel(outcome: string): string {
    const labels: Record<string, string> = {
      ACCEPTED: 'Aceptada', OBSERVED: 'Observada', REJECTED: 'Rechazada', PENDING_REVIEW: 'Pendiente de revisión'
    };
    return labels[outcome] ?? 'Pendiente de revisión';
  }
}
