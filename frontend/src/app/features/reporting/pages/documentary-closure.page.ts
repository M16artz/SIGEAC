import {ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Subscription, switchMap, throwError} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {DocumentaryClosurePage, DocumentaryClosureView, ReportingApi} from '../data-access/reporting-api.service';

@Component({
  selector: 'sec-documentary-closure', standalone: true,
  imports: [DatePipe, FormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="documentary-closure-title">
      <h2 id="documentary-closure-title">Cierre de fase documental</h2>
      <p><a routerLink="/reporting">Consultar avance documental</a></p>
      <p>El cierre estricto exige aceptación suficiente de las fuentes obligatorias aplicables y archivos íntegros.
        Finaliza la recepción y revisión ordinarias. Las correcciones posteriores requieren reapertura autorizada.</p>
      <label for="closure-process">Proceso</label>
      <select id="closure-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="closing()">
        <option value="">Seleccione un proceso</option>
        @for (process of processes(); track process.id) {
          <option [value]="process.id">{{process.code}} · {{process.name}}</option>
        }
      </select>
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (blockers()) { <p>{{blockers()}}</p> }
      @if (message()) { <p role="status">{{message()}}</p> }
      @if (current(); as process) {
        @if (process.state === 'ACTIVE') {
          <form (ngSubmit)="close()">
            <label for="closure-reason">Motivo del cierre</label>
            <textarea id="closure-reason" name="reason" [(ngModel)]="reason" minlength="10" maxlength="2000" required [disabled]="closing()"></textarea>
            <button type="submit" [disabled]="closing() || reason.trim().length < 10">Finalizar fase documental</button>
          </form>
        } @else {
          <p>El proceso debe estar activo para registrar un nuevo cierre documental.</p>
        }
      }
      @if (history(); as page) {
        <table>
          <caption>Cierres documentales conservados</caption>
          <thead><tr><th scope="col">Fecha</th><th scope="col">Motivo</th><th scope="col">Acción</th></tr></thead>
          <tbody>
            @for (item of page.items; track item.id) {
              <tr><td>{{item.closedAt | date:'medium'}}</td><td>{{item.reason}}</td>
                <td><button type="button" (click)="open(item.id)">Consultar cierre</button></td></tr>
            }
          </tbody>
        </table>
        <nav aria-label="Páginas de cierres">
          <button type="button" [disabled]="page.page === 0 || closing()" (click)="load(page.page - 1)">Anterior</button>
          <span>Página {{page.page + 1}}</span>
          <button type="button" [disabled]="!page.hasNext || closing()" (click)="load(page.page + 1)">Siguiente</button>
        </nav>
      }
      @if (selected(); as closure) {
        <section aria-labelledby="saved-closure-title">
          <h3 id="saved-closure-title">Fase documental cerrada</h3>
          <p>{{closure.data.closedAt | date:'medium'}} · {{closure.data.reason}}</p>
          <p>Política: {{closure.data.policyVersion}}</p>
          <p>Huella SHA-256: <code>{{closure.sha256}}</code></p>
          <a routerLink="/reporting/history" [queryParams]="{snapshotId: closure.data.snapshotId}">Consultar el corte conservado del cierre</a>
          <p>La valoración académica y el cierre integral se gestionan por separado.</p>
        </section>
      }
    </section>
  `
})
export class DocumentaryClosurePageComponent implements OnInit, OnDestroy {
  private readonly api = inject(ReportingApi);
  private readonly processApi = inject(ProcessApi);
  private readonly destroyRef = inject(DestroyRef);
  private listRequest?: Subscription;
  private detailRequest?: Subscription;
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly current = signal<ProcessView | null>(null);
  readonly history = signal<DocumentaryClosurePage | null>(null);
  readonly selected = signal<DocumentaryClosureView | null>(null);
  readonly error = signal('');
  readonly blockers = signal('');
  readonly message = signal('');
  readonly closing = signal(false);
  processId = '';
  reason = '';
  ngOnInit(): void {
    this.processApi.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: processes => { this.processes.set(processes); if (processes.length === 1) this.selectProcess(processes[0].id); },
      error: () => this.error.set('No fue posible consultar los procesos autorizados.')
    });
  }
  ngOnDestroy(): void { this.listRequest?.unsubscribe(); this.detailRequest?.unsubscribe(); }
  selectProcess(id: string): void {
    this.processId = id; this.reason = ''; this.message.set(''); this.blockers.set('');
    this.detailRequest?.unsubscribe(); this.selected.set(null); this.current.set(null);
    this.load();
  }
  load(page = 0): void {
    this.listRequest?.unsubscribe(); this.history.set(null); this.error.set('');
    if (!this.processId) return;
    this.listRequest = this.processApi.get(this.processId).pipe(switchMap(process => {
      this.current.set(process);
      return this.api.closures(process.id, page);
    })).subscribe({
      next: history => this.history.set(history),
      error: () => { this.current.set(null); this.selected.set(null); this.error.set('No fue posible consultar los cierres en el ámbito autorizado.'); }
    });
  }
  open(id: string): void {
    this.detailRequest?.unsubscribe(); this.selected.set(null); this.error.set('');
    this.detailRequest = this.api.closure(id).subscribe({
      next: closure => this.selected.set(closure),
      error: () => { this.history.set(null); this.error.set('No fue posible verificar o consultar el cierre conservado.'); }
    });
  }
  close(): void {
    if (this.closing() || !this.processId || this.reason.trim().length < 10 || this.reason.trim().length > 2000) return;
    const id = this.processId;
    this.closing.set(true); this.error.set(''); this.blockers.set(''); this.message.set('');
    this.processApi.get(id).pipe(switchMap(process => process.state === 'ACTIVE'
      ? this.api.closeDocumentary(id, this.reason.trim(), '"' + process.revision + '"')
      : throwError(() => new Error('PROCESS_NOT_ACTIVE'))), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: closure => {
        this.closing.set(false);
        if (this.processId !== id) return;
        this.load(); this.selected.set(closure); this.reason = '';
        this.message.set('Fase documental cerrada. El corte y las verificaciones quedaron conservados.');
      },
      error: error => {
        this.closing.set(false); this.selected.set(null); this.history.set(null);
        this.error.set(error instanceof ApiProblemError && error.problem.status === 412
          ? 'El proceso cambió. Revise el avance antes de volver a cerrar.'
          : 'No fue posible cerrar. Revise las fuentes, los archivos, el estado activo y sus permisos.');
        if (error instanceof ApiProblemError) {
          const details = error.problem.details;
          this.blockers.set([details['sourceIds'] ? 'Fuentes pendientes: ' + details['sourceIds'] : '',
            details['fileIds'] ? 'Archivos por verificar: ' + details['fileIds'] : ''].filter(Boolean).join(' · '));
        }
      }
    });
  }
}
