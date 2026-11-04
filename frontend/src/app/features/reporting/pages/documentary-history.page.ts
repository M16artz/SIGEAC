import {ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {DatePipe, DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Subscription, switchMap, throwError} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {DocumentarySnapshotPage, DocumentarySnapshotView, ReportingApi} from '../data-access/reporting-api.service';

@Component({
  selector: 'sec-documentary-history', standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="documentary-history-title">
      <h2 id="documentary-history-title">Cortes documentales e historial</h2>
      <p><a routerLink="/reporting">Volver al avance actual</a></p>
      <p>Guardar un corte conserva las fuentes y decisiones actuales, incluidos los pendientes. El proceso continúa activo.</p>
      <label for="snapshot-process">Proceso</label>
      <select id="snapshot-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="creating()">
        <option value="">Seleccione un proceso</option>
        @for (process of processes(); track process.id) {
          <option [value]="process.id">{{process.code}} · {{process.name}}</option>
        }
      </select>
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (message()) { <p role="status">{{message()}}</p> }
      @if (processId) {
        <form (ngSubmit)="create()">
          <label for="snapshot-reason">Motivo del corte</label>
          <textarea id="snapshot-reason" name="reason" [(ngModel)]="reason" minlength="10" maxlength="4000" required [disabled]="creating()"></textarea>
          <button type="submit" [disabled]="creating() || reason.trim().length < 10">Guardar corte actual</button>
        </form>
      }
      @if (history(); as page) {
        <table>
          <caption>Cortes guardados del proceso autorizado</caption>
          <thead><tr><th scope="col">Fecha</th><th scope="col">Motivo</th><th scope="col">Acción</th></tr></thead>
          <tbody>
            @for (item of page.items; track item.id) {
              <tr><td>{{item.createdAt | date:'medium'}}</td><td>{{item.reason}}</td>
                <td><button type="button" (click)="open(item.id)">Consultar corte</button></td></tr>
            }
          </tbody>
        </table>
        @if (page.items.length === 0) { <p>No hay cortes documentales en esta página.</p> }
        <nav aria-label="Páginas de cortes documentales">
          <button type="button" [disabled]="page.page === 0 || creating()" (click)="load(page.page - 1)">Anterior</button>
          <span>Página {{page.page + 1}}</span>
          <button type="button" [disabled]="!page.hasNext || creating()" (click)="load(page.page + 1)">Siguiente</button>
        </nav>
      }
      @if (selected(); as snapshot) {
        <section aria-labelledby="saved-snapshot-title">
          <h3 id="saved-snapshot-title">Corte guardado · {{snapshot.header.createdAt | date:'medium'}}</h3>
          <p>{{snapshot.header.reason}}</p>
          <p>Huella SHA-256: <code>{{snapshot.header.sha256}}</code></p>
          <p>Regla de cálculo: {{snapshot.data.progress.formulaVersion}}</p>
          @if (snapshot.data.progress.requiredSourceTotal > 0) {
            <p>Recopilación: {{(100 * snapshot.data.progress.registeredSourceTotal / snapshot.data.progress.requiredSourceTotal) | number:'1.2-2'}} %
              ({{snapshot.data.progress.registeredSourceTotal}} / {{snapshot.data.progress.requiredSourceTotal}}).</p>
            <p>Revisión documental: {{(100 * snapshot.data.progress.acceptedSourceTotal / snapshot.data.progress.requiredSourceTotal) | number:'1.2-2'}} %
              ({{snapshot.data.progress.acceptedSourceTotal}} / {{snapshot.data.progress.requiredSourceTotal}}).</p>
          } @else {
            <p>No calculable: {{snapshot.data.progress.status === 'NOT_APPLICABLE' ? 'fuentes excluidas' : 'fuentes obligatorias sin configurar'}}.</p>
          }
          <p>La aceptación documental no determina cumplimiento académico.</p>
          <ul>
            @for (source of snapshot.data.sources; track source.id) {
              <li>{{source.code}} · {{source.name}} · {{source.applicable ? (source.required ? 'Obligatoria' : 'Opcional') : 'Excluida'}}
                <ul>
                  @for (coverage of source.coverages; track coverage.versionId) {
                    <li>Versión {{coverage.versionId}} · Decisión {{coverage.decisionId ?? 'pendiente de revisión'}}</li>
                  }
                </ul>
              </li>
            }
          </ul>
        </section>
      }
    </section>
  `
})
export class DocumentaryHistoryPage implements OnInit, OnDestroy {
  private readonly api = inject(ReportingApi);
  private readonly processApi = inject(ProcessApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private historyRequest?: Subscription;
  private detailRequest?: Subscription;
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly history = signal<DocumentarySnapshotPage | null>(null);
  readonly selected = signal<DocumentarySnapshotView | null>(null);
  readonly error = signal('');
  readonly message = signal('');
  readonly creating = signal(false);
  processId = '';
  reason = '';

  ngOnInit(): void {
    this.processApi.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: processes => {
        this.processes.set(processes);
        if (processes.length === 1) this.selectProcess(processes[0].id);
        const snapshotId = this.route.snapshot.queryParamMap.get('snapshotId');
        if (snapshotId) this.open(snapshotId);
      },
      error: () => this.error.set('No fue posible consultar los procesos autorizados.')
    });
  }

  ngOnDestroy(): void { this.historyRequest?.unsubscribe(); this.detailRequest?.unsubscribe(); }

  selectProcess(id: string): void {
    this.detailRequest?.unsubscribe();
    this.processId = id;
    this.selected.set(null);
    this.message.set('');
    this.reason = '';
    this.load();
  }

  load(page = 0): void {
    this.historyRequest?.unsubscribe();
    this.history.set(null);
    this.error.set('');
    if (!this.processId) return;
    this.historyRequest = this.api.snapshots(this.processId, page).subscribe({
      next: history => this.history.set(history),
      error: () => { this.selected.set(null); this.error.set('No fue posible consultar el historial en el ámbito autorizado.'); }
    });
  }

  open(id: string): void {
    this.detailRequest?.unsubscribe();
    this.selected.set(null);
    this.error.set('');
    this.detailRequest = this.api.snapshot(id).subscribe({
      next: snapshot => {
        if (this.processId !== snapshot.header.processId) { this.processId = snapshot.header.processId; this.load(); }
        this.selected.set(snapshot);
      },
      error: () => {
        this.history.set(null);
        this.error.set('No fue posible verificar o consultar el corte en el ámbito autorizado.');
      }
    });
  }

  create(): void {
    if (this.creating() || !this.processId || this.reason.trim().length < 10 || this.reason.trim().length > 4000) return;
    const processId = this.processId;
    const reason = this.reason.trim();
    this.error.set('');
    this.message.set('');
    this.creating.set(true);
    this.processApi.get(processId).pipe(
      switchMap(process => process.state === 'ACTIVE'
        ? this.api.createSnapshot(processId, reason, '"' + process.revision + '"')
        : throwError(() => new Error('PROCESS_NOT_ACTIVE'))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: snapshot => {
        this.creating.set(false);
        if (this.processId !== processId) return;
        this.reason = '';
        this.load();
        this.selected.set(snapshot);
        this.message.set('Corte guardado. El proceso continúa activo.');
      },
      error: error => {
        this.creating.set(false);
        this.history.set(null);
        this.selected.set(null);
        this.error.set(error instanceof ApiProblemError && error.problem.status === 412
          ? 'El proceso cambió. Revise los datos y vuelva a guardar el corte.'
          : 'No fue posible guardar el corte. Compruebe que el proceso esté activo y tenga permiso sobre el proceso completo.');
      }
    });
  }
}
