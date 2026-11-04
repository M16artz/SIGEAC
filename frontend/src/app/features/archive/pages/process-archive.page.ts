import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Observable, Subscription, catchError, forkJoin, of} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {
  ArchiveApi, ClosurePage, ClosurePreview, ClosureRecord, ProvenanceCommand
} from '../data-access/archive-api.service';

@Component({
  selector: 'sec-process-archive-page',
  standalone: true,
  imports: [FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    section { max-width: 80rem; }
    select, input, textarea { display:block; margin-top:.2rem; padding:.3rem; }
    select { min-width:18rem; } textarea { width:100%; min-height:4rem; box-sizing:border-box; }
    article { border:1px solid #ccc; border-radius:.4rem; padding:.6rem; margin:.8rem 0; overflow-wrap:anywhere; }
    .blocked { color:#a40000; font-weight:600; } .muted { color:#555; }
    code { overflow-wrap:anywhere; }
  `],
  template: `
    <section aria-labelledby="archive-title">
      <h2 id="archive-title">Archivo y cierre integral</h2>
      <p class="muted">La previsualizaci��n informa los bloqueadores vigentes. El cierre integral conserva una
        instant��nea inmutable y el archivo verifica su integridad antes de registrarlo.</p>
      <label for="archive-process">Proceso</label>
      <select id="archive-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="saving()">
        <option value="">Seleccione un proceso</option>
        @for (process of processes(); track process.id) {
          <option [value]="process.id">{{process.code}} �� {{process.name}}</option>
        }
      </select>
      <button type="button" (click)="refresh()" [disabled]="!processId || loading() || saving()">Actualizar</button>
      @if (loading()) { <p role="status">Consultando cierres y bloqueadores...</p> }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (message()) { <p role="status">{{message()}}</p> }

      @if (preview(); as p) {
        <article aria-label="Vista previa del cierre integral">
          <h3>Paso P16: cierre integral</h3>
          <p><strong>{{p.eligible ? 'Elegible para cierre' : 'No elegible para cierre'}}</strong> ��
            Pol��tica {{p.policyVersion}} �� ETag {{p.revision}}</p>
          @for (blocker of p.blockers; track blocker) { <p class="blocked">{{blocker}}</p> }
          @for (warning of p.warnings; track warning) { <p class="muted">{{warning}}</p> }
          @if (p.blockers.length === 0) { <p class="muted">No hay bloqueadores vigentes.</p> }

          <form (ngSubmit)="closeIntegral()">
            <h4>Cerrar integralmente</h4>
            <label for="closure-policy">Pol��tica de cierre</label>
            <input id="closure-policy" name="closurePolicy" [(ngModel)]="policyVersion" required maxlength="100" [disabled]="saving()" />
            <label for="closure-assignment">Asignaci��n con competencia</label>
            <input id="closure-assignment" name="closureAssignment" [(ngModel)]="authorityAssignmentId" required [disabled]="saving()" />
            <label for="closure-legal">Fundamento normativo</label>
            <input id="closure-legal" name="closureLegal" [(ngModel)]="legalBasis" required maxlength="4000" [disabled]="saving()" />
            <label for="closure-document">Documento de respaldo</label>
            <input id="closure-document" name="closureDocument" [(ngModel)]="documentReference" required maxlength="2000" [disabled]="saving()" />
            <label for="closure-reason">Motivo</label>
            <textarea id="closure-reason" name="closureReason" [(ngModel)]="reason" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
            <label for="closure-exceptions">Decisiones de excepci��n (separadas por coma)</label>
            <input id="closure-exceptions" name="closureExceptions" [(ngModel)]="exceptionText" [disabled]="saving()" />
            <button type="submit" [disabled]="saving() || !closureValid()">Cerrar integralmente</button>
          </form>
        </article>
      }

      <h3>Historial de cierres</h3>
      @if (history(); as page) {
        <p class="muted">{{page.totalElements}} cierre(s) conservado(s).</p>
        @for (closure of page.items; track closure.id) {
          <article [attr.aria-label]="'Cierre ' + closure.type">
            <strong>{{closure.type === 'INTEGRAL_CLOSURE' ? 'Cierre integral' : 'Cierre documental'}}</strong>
            �� {{closure.policyVersion}} �� {{closure.createdAt | date:'medium'}}
            <p class="muted">SHA-256 <code>{{closure.sha256}}</code></p>
            <p class="muted">Excepciones {{closure.exceptionDecisionIds.length}} �� Pendientes {{closure.pendingIds.length}}</p>
            @if (closure.type === 'INTEGRAL_CLOSURE') {
              <button type="button" (click)="selectClosure(closure)" [disabled]="saving()">Seleccionar para archivar</button>
            }
          </article>
        } @empty { <p>No hay cierres registrados para este proceso.</p> }
      }

      @if (selectedClosure(); as closure) {
        <form (ngSubmit)="archiveClosure()">
          <h4>Archivar cierre seleccionado</h4>
          <p class="muted"><code>{{closure.id}}</code></p>
          <label for="archive-retention">Pol��tica de retenci��n</label>
          <input id="archive-retention" name="archiveRetention" [(ngModel)]="retentionPolicyVersion" required maxlength="100" [disabled]="saving()" />
          <button type="submit" [disabled]="saving() || retentionPolicyVersion.trim().length === 0">Archivar cierre</button>
        </form>
      }
    </section>
  `
})
export class ProcessArchivePage implements OnInit, OnDestroy {
  private readonly processesApi = inject(ProcessApi);
  private readonly api = inject(ArchiveApi);
  private request?: Subscription;

  readonly processes = signal<readonly ProcessView[]>([]);
  readonly preview = signal<ClosurePreview | null>(null);
  readonly history = signal<ClosurePage | null>(null);
  readonly selectedClosure = signal<ClosureRecord | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');

  processId = '';
  policyVersion = 'INTEGRAL_STRICT_V1';
  authorityAssignmentId = '';
  legalBasis = '';
  documentReference = '';
  reason = '';
  exceptionText = '';
  retentionPolicyVersion = 'RETENTION_10Y';

  ngOnInit(): void {
    this.request = this.processesApi.list().pipe(catchError(() => of<readonly ProcessView[]>([])))
      .subscribe(rows => this.processes.set(rows));
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
  }

  selectProcess(id: string): void {
    this.processId = id;
    this.preview.set(null);
    this.history.set(null);
    this.selectedClosure.set(null);
    this.error.set('');
    this.message.set('');
    if (id) this.refresh();
  }

  refresh(): void {
    if (!this.processId || this.loading()) return;
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set('');
    this.request = forkJoin({
      preview: this.api.preview(this.processId),
      history: this.api.history(this.processId)
    }).subscribe({
      next: ({preview, history}) => {
        this.preview.set(preview);
        this.history.set(history);
        this.selectedClosure.set(history.items.find(item => item.type === 'INTEGRAL_CLOSURE') ?? null);
        this.loading.set(false);
      },
      error: (error: unknown) => this.fail(error)
    });
  }

  selectClosure(closure: ClosureRecord): void {
    this.selectedClosure.set(closure);
  }

  closureValid(): boolean {
    return this.policyVersion.trim().length > 0 && this.authorityAssignmentId.trim().length > 0
      && this.legalBasis.trim().length > 0 && this.documentReference.trim().length > 0
      && this.reason.trim().length >= 10;
  }

  closeIntegral(): void {
    const preview = this.preview();
    if (!preview || !this.closureValid() || this.saving()) return;
    this.save(this.api.close(this.processId, preview.revision, {
      closurePolicyVersion: this.policyVersion.trim(),
      provenance: this.provenance(),
      exceptionDecisionIds: this.exceptionIds()
    }), 'Cierre integral registrado con su instant��nea inmutable.');
  }

  archiveClosure(): void {
    const closure = this.selectedClosure();
    if (!closure || this.saving() || this.retentionPolicyVersion.trim().length === 0) return;
    this.save(this.api.archive(this.processId, {
      closureSnapshotId: closure.id,
      retentionPolicyVersion: this.retentionPolicyVersion.trim(),
      provenance: this.provenance()
    }), 'Cierre archivado; la integridad SHA-256 fue verificada.');
  }

  private provenance(): ProvenanceCommand {
    return {
      authorityAssignmentId: this.authorityAssignmentId.trim(),
      legalBasis: this.legalBasis.trim(),
      documentReference: this.documentReference.trim(),
      reason: this.reason.trim()
    };
  }

  private exceptionIds(): readonly string[] {
    return this.exceptionText.split(',').map(value => value.trim()).filter(value => value.length > 0);
  }

  private save(work: Observable<unknown>, message: string): void {
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    this.request?.unsubscribe();
    this.request = work.subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set(message);
        this.refresh();
      },
      error: (error: unknown) => this.fail(error)
    });
  }

  private fail(error: unknown): void {
    this.loading.set(false);
    this.saving.set(false);
    this.error.set(error instanceof ApiProblemError ? error.message
      : 'No se pudo completar la operaci��n de cierre o archivo. Recargue antes de continuar.');
  }
}
