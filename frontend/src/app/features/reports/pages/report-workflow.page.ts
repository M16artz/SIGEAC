import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Observable, Subscription, catchError, forkJoin, of, switchMap} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {ApprovalEvent, Finding, FindingData, FindingPage, Observation, ReportApi, ReportVersion, Reply, Socialization, VersionPage} from '../data-access/report-api.service';

const DISCLAIMER_NOTICE = 'Este informe apoya la autoevaluación y no constituye acreditación.';
const EMPTY_CONSOLIDATION = '00000000-0000-0000-0000-000000000000';

@Component({
  selector: 'sec-report-workflow-page',
  standalone: true,
  imports: [FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    section { max-width: 80rem; }
    select, textarea, input[type="text"], input[type="datetime-local"] { display:block; margin-top:.2rem; padding:.3rem; }
    select { min-width: 16rem; }
    textarea { width:100%; min-height:5rem; box-sizing:border-box; }
    fieldset { margin:.8rem 0; } fieldset label { display:block; margin:.3rem 0; overflow-wrap:anywhere; }
    article { border:1px solid #ccc; border-radius:.4rem; padding:.6rem; margin:.8rem 0; overflow-wrap:anywhere; }
    article h4, article h5 { margin:.2rem 0; }
    form { margin:.8rem 0; }
    .muted { color:#555; }
  `],
  template: `
    <section aria-labelledby="reports-title">
      <h2 id="reports-title">Hallazgos, consolidación y ciclo del informe</h2>
      <p class="muted">{{notice}} Las mutaciones exigen revisión previa del recurso (If-Match) y el
        contenido emitido permanece inmutable; solo la tramitación y sus versiones finales avanzan.</p>
      <label for="reports-process">Proceso</label>
      <select id="reports-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="saving()">
        <option value="">Seleccione un proceso</option>
        @for (p of processes(); track p.id) { <option [value]="p.id">{{p.code}} · {{p.name}}</option> }
      </select>
      @if (loading()) { <p role="status">Consultando hallazgos e informe…</p> }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (message()) { <p role="status">{{message()}}</p> }

      @if (processId) {
        <h3>Paso P13: hallazgos y consolidación académica</h3>
        @if (consolidationId()) { <p class="muted">Consolidación previa disponible: {{consolidationId()}}</p> }
        @else { <p class="muted">Registre y consolide hallazgos antes de emitir versiones del informe.</p> }
        @for (finding of findings(); track finding.id) {
          <article>
            <label><input type="checkbox" [checked]="selectedFindings().includes(finding.id)"
                   (change)="toggleFinding(finding.id)" [disabled]="saving()" /> Seleccionar para consolidar</label>
            <strong>{{typeLabel(finding)}}</strong> · {{finding.data.title}}
            <p>{{finding.data.description}}</p>
            <p class="muted">Indicadores de soporte: {{finding.data.indicatorIds.length}} ·
              Registrado {{finding.createdAt | date:'medium'}}</p>
          </article>
        } @empty { <p>Sin hallazgos registrados para este proceso.</p> }

        <form (ngSubmit)="registerFinding()">
          <h4>Registrar hallazgo</h4>
          <label for="finding-type">Clase</label>
          <select id="finding-type" name="type" [(ngModel)]="findingType" required [disabled]="saving()">
            <option value="">Seleccione la clase</option>
            <option value="STRENGTH">Fortaleza</option>
            <option value="WEAKNESS">Debilidad</option>
            <option value="OBSERVATION">Observación</option>
          </select>
          <label for="finding-title">Título</label>
          <input id="finding-title" type="text" name="title" [(ngModel)]="findingTitle" required maxlength="300" [disabled]="saving()" />
          <label for="finding-description">Descripción sustentada</label>
          <textarea id="finding-description" name="description" [(ngModel)]="findingDescription" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
          <fieldset [disabled]="saving()">
            <legend>Indicadores que sustentan el hallazgo</legend>
            @for (indicator of indicators(); track indicator.id) {
              <label><input type="checkbox" [checked]="findingSupports().includes(indicator.id)"
                (change)="toggleSupport(indicator.id)" [disabled]="saving()" /> {{indicator.code}} · {{indicator.name}}</label>
            }
          </fieldset>
          <button type="submit" [disabled]="saving() || !findingValid()">Registrar hallazgo</button>
        </form>

        <form (ngSubmit)="consolidate()">
          <h4>Consolidar hallazgos</h4>
          <p class="muted">La revisión previa (If-Match) es la revisión de la última consolidación; use 0 para el
            histórico vacío y actualice el valor tras cada registro.</p>
          <label for="consolidation-revision">Revisión previa de la cadena</label>
          <input id="consolidation-revision" type="text" name="expected" [(ngModel)]="expectedConsolidation" required inputmode="numeric" [disabled]="saving()" />
          <label for="consolidation-rule">Regla validada (opcional)</label>
          <input id="consolidation-rule" type="text" name="rule" [(ngModel)]="aggregationRuleId" [disabled]="saving()" />
          <label for="consolidation-reason">Justificación</label>
          <textarea id="consolidation-reason" name="reason" [(ngModel)]="consolidationReason" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
          <button type="submit" [disabled]="saving() || selectedFindings().length === 0 || !consolidationValid()">Consolidar hallazgos</button>
        </form>

        @if (consolidationId()) {
          <h3>Paso P14: versiones y tramitación del informe</h3>
          <form (ngSubmit)="issue('PRELIMINARY')">
            <h4>Emitir versión preliminar</h4>
            <p class="muted">El preliminar exige plantilla validada y una consolidación previa; sus bytes no se editarán;</p>
            <label for="issue-template">Referencia de plantilla validada</label>
            <input id="issue-template" type="text" name="template" [(ngModel)]="templateReference" required maxlength="500" [disabled]="saving()" />
            <label for="issue-content">Contenido sustentado del informe</label>
            <textarea id="issue-content" name="content" [(ngModel)]="issueContent" required minlength="10" maxlength="200000" [disabled]="saving()"></textarea>
            <button type="submit" [disabled]="saving() || !issueValid()">Emitir preliminar</button>
          </form>
        }

        @for (item of versions(); track item.id) {
          <article>
            <strong>{{item.data.kind}} · ETag {{item.revision}}</strong>
            @if (item.state) { <span> · Estado: {{item.state}}</span> }
            <p class="muted">Emitido {{item.createdAt | date:'medium'}} · Plantilla: {{item.data.templateReference}}</p>
            @if (item.state !== 'APPROVED') {
              <button type="button" (click)="viewDetail(item)" [disabled]="saving()">Seleccionar para tramitar</button>
            }
          </article>
        } @empty { <p>Aún no hay versiones emitidas.</p> }

        @if (detail(); as d) {
          <article aria-label="Tramitación de la versión seleccionada">
            <h4>Versión en tramitación · ETag {{selectedRevision()}} · Estado {{d.state ?? d.data.kind}}</h4>
            <p>{{notice}}</p>
            <h5>Socializaciones</h5>
            @for (socialization of socializations(); track socialization.id) {
              <p class="muted">{{socialization.occurredAt | date:'medium'}} ·
                Participantes: {{socialization.participantAssignmentIds.length}} · {{socialization.recordReference}}</p>
            } @empty { <p class="muted">Sin socializaciones registradas.</p> }
            <form (ngSubmit)="socialize()">
              <label for="social-occurred">Fecha de la jornada</label>
              <input id="social-occurred" type="datetime-local" name="occurredAt" [(ngModel)]="socializationAt" required [disabled]="saving()" />
              <label for="social-assignments">Asignaciones participantes (separadas por coma)</label>
              <input id="social-assignments" type="text" name="participants" [(ngModel)]="socializationParticipants" required maxlength="500" [disabled]="saving()" />
              <label for="social-record">Referencia de la evidencia formal</label>
              <input id="social-record" type="text" name="record" [(ngModel)]="socializationRecord" required maxlength="500" [disabled]="saving()" />
              <button type="submit" [disabled]="saving()">Registrar socialización</button>
            </form>
            <h5>Observaciones</h5>
            @for (observation of observations(); track observation.id) {
              <p><strong>{{observation.sectionReference}}</strong> · {{observation.text}}</p>
              @for (reply of repliesFor(observation.id); track reply.id) {
                <p class="muted">Respuesta oficial: {{reply.response}}</p>
              }
            } @empty { <p class="muted">Sin observaciones.</p> }
            <form (ngSubmit)="observe()">
              <label for="observation-section">Sección observada</label>
              <input id="observation-section" type="text" name="section" [(ngModel)]="observationSection" required maxlength="200" [disabled]="saving()" />
              <label for="observation-text">Texto de la observación</label>
              <textarea id="observation-text" name="text" [(ngModel)]="observationText" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
              <button type="submit" [disabled]="saving()">Registrar observación</button>
            </form>
            @if (observations().length) {
              <form (ngSubmit)="reply()">
                <h5>Responder observación</h5>
                <label for="reply-observation">Observación</label>
                <select id="reply-observation" name="observation" [(ngModel)]="replyObservationId" [disabled]="saving()">
                  @for (o of observations(); track o.id) {
                    <option [value]="o.id">{{o.sectionReference}} · {{o.text}}</option>
                  }
                </select>
                <label for="reply-text">Respuesta oficial</label>
                <textarea id="reply-text" name="response" [(ngModel)]="replyText" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
                <label for="reply-correction">Versión de corrección (opcional)</label>
                <input id="reply-correction" type="text" name="correction" [(ngModel)]="replyCorrectionId" [disabled]="saving()" />
                <button type="submit" [disabled]="saving()">Registrar respuesta</button>
              </form>
            }
            @if (d.state !== 'APPROVED') {
              <form (ngSubmit)="issue('FINAL')">
                <h5>Emitir versión final (corrección)</h5>
                <label for="final-content">Contenido sustentado de la corrección</label>
                <textarea id="final-content" name="finalContent" [(ngModel)]="finalContent" required minlength="10" maxlength="200000" [disabled]="saving()"></textarea>
                <button type="submit" [disabled]="saving() || finalContent.trim().length < 10">Emitir final encadenado</button>
              </form>
            }
            <form (ngSubmit)="decide()">
              <h5>Decisión institucional</h5>
              <p class="muted">Requiere versión FINAL y asignación de autoridad competente; la decisión se registra
                una sola vez y cierra la cadena.</p>
              <label for="decision-value">Decisión</label>
              <select id="decision-value" name="decision" [(ngModel)]="decisionValue" required [disabled]="saving()">
                <option value="">Seleccione la decisión</option>
                <option value="APPROVED">APPROVED</option>
                <option value="RETURNED">RETURNED</option>
              </select>
              <label for="decision-assignment">Asignación de autoridad</label>
              <input id="decision-assignment" type="text" name="assignment" [(ngModel)]="decisionAssignmentId" required [disabled]="saving()" />
              <label for="decision-legal">Base legal</label>
              <input id="decision-legal" type="text" name="legal" [(ngModel)]="decisionLegalBasis" required maxlength="4000" [disabled]="saving()" />
              <label for="decision-document">Referencia documental</label>
              <input id="decision-document" type="text" name="document" [(ngModel)]="decisionDocumentReference" required maxlength="2000" [disabled]="saving()" />
              <label for="decision-reason">Justificación</label>
              <textarea id="decision-reason" name="reason" [(ngModel)]="decisionReason" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
              <button type="submit" [disabled]="saving() || !decisionValid()">Registrar decisión</button>
            </form>
          </article>
        }
      }
    </section>
  `
})
export class ReportWorkflowPage implements OnInit, OnDestroy {
  private readonly api = inject(ReportApi);
  private readonly processApi = inject(ProcessApi);
  private request?: Subscription;
  private listing?: Subscription;
  private consolidationMemory = '';

  readonly processes = signal<readonly ProcessView[]>([]);
  readonly findings = signal<readonly Finding[]>([]);
  readonly versions = signal<readonly ReportVersion[]>([]);
  readonly detail = signal<ReportVersion | null>(null);
  readonly socializations = signal<readonly Socialization[]>([]);
  readonly observations = signal<readonly Observation[]>([]);
  readonly replies = signal<readonly Reply[]>([]);
  readonly decisions = signal<readonly ApprovalEvent[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly selectedRevision = signal(0);
  readonly selectedFindings = signal<string[]>([]);
  readonly findingSupports = signal<string[]>([]);
  readonly consolidationId = signal('');

  processId = '';
  notice = DISCLAIMER_NOTICE;
  findingType = '';
  findingTitle = '';
  findingDescription = '';
  expectedConsolidation = '0';
  consolidationReason = '';
  aggregationRuleId = '';
  templateReference = '';
  issueContent = '';
  finalContent = '';
  socializationAt = '';
  socializationParticipants = '';
  socializationRecord = '';
  observationSection = '';
  observationText = '';
  replyObservationId = '';
  replyText = '';
  replyCorrectionId = '';
  decisionValue = '';
  decisionAssignmentId = '';
  decisionLegalBasis = '';
  decisionDocumentReference = '';
  decisionReason = '';

  ngOnInit(): void {
    this.listing = this.processApi.list().subscribe({
      next: rows => this.processes.set(rows),
      error: e => this.fail(e)
    });
  }

  ngOnDestroy(): void { this.request?.unsubscribe(); this.listing?.unsubscribe(); }

  typeLabel(finding: Finding): string {
    return finding.data.type === 'STRENGTH' ? 'Fortaleza'
      : finding.data.type === 'WEAKNESS' ? 'Debilidad' : 'Observación';
  }

  indicators(): readonly {id: string; code: string; name: string}[] {
    return this.processes().find(p => p.id === this.processId)?.indicators ?? [];
  }

  selectProcess(id: string): void {
    this.request?.unsubscribe();
    this.processId = id;
    this.consolidationMemory = '';
    this.consolidationId.set('');
    this.expectedConsolidation = '0';
    this.selectedFindings.set([]);
    this.findingSupports.set([]);
    this.detail.set(null);
    this.socializations.set([]);
    this.observations.set([]);
    this.replies.set([]);
    this.decisions.set([]);
    if (id) this.reload();
  }

  toggleFinding(id: string): void {
    this.selectedFindings.update(rows => rows.includes(id) ? rows.filter(v => v !== id) : [...rows, id]);
  }

  toggleSupport(id: string): void {
    const current = this.findingSupports();
    this.findingSupports.set(current.includes(id) ? current.filter(v => v !== id) : [...current, id]);
  }

  findingValid(): boolean {
    return this.findingType !== '' && this.findingTitle.trim().length >= 1
      && this.findingDescription.trim().length >= 10 && this.findingSupports().length > 0;
  }

  registerFinding(): void {
    if (!this.findingValid() || this.saving()) return;
    this.run(this.api.registerFinding(this.processId, {
      type: this.findingType as FindingData['type'], title: this.findingTitle.trim(),
      description: this.findingDescription.trim(), indicatorIds: this.findingSupports()
    }), 'Hallazgo registrado con soporte exacto y sin calificación.');
    this.findingType = ''; this.findingTitle = ''; this.findingDescription = '';
    this.findingSupports.set([]);
  }

  consolidationValid(): boolean {
    return this.consolidationReason.trim().length >= 10
      && Number(this.expectedConsolidation) >= 0 && !Number.isNaN(Number(this.expectedConsolidation));
  }

  consolidate(): void {
    if (this.selectedFindings().length === 0 || !this.consolidationValid() || this.saving()) return;
    const rule = this.aggregationRuleId.trim() === '' ? null : this.aggregationRuleId.trim();
    this.run(this.api.consolidate(this.processId, Number(this.expectedConsolidation), {
      findingIds: this.selectedFindings(), aggregationRuleId: rule, reason: this.consolidationReason.trim()
    }).pipe(switchMap(consolidation => {
      this.consolidationMemory = consolidation.id;
      this.consolidationId.set(consolidation.id);
      this.expectedConsolidation = String(consolidation.revision);
      this.selectedFindings.set([]);
      return this.api.findings(this.processId);
    })), 'Consolidación registrada con soportes y pendientes trazables.');
  }

  issueValid(): boolean {
    return this.templateReference.trim().length >= 1 && this.issueContent.trim().length >= 10;
  }

  issue(kind: 'PRELIMINARY' | 'FINAL'): void {
    if (this.saving()) return;
    const content = kind === 'PRELIMINARY' ? this.issueContent : this.finalContent;
    if (this.templateReference.trim().length < 1 || content.trim().length < 10) return;
    const expected = kind === 'PRELIMINARY' ? 0 : this.selectedRevision();
    this.run(this.api.issueVersion(this.processId, expected, {
      kind, consolidationId: this.consolidationId() || this.consolidationMemory || EMPTY_CONSOLIDATION,
      templateReference: this.templateReference.trim(), templateValidated: true, content: content.trim(),
      predecessorVersionId: kind === 'FINAL' ? this.detail()?.id ?? null : null
    }), 'Versión emitida; sus bytes permanecen inmutables.');
  }

  viewDetail(item: ReportVersion): void {
    this.request?.unsubscribe();
    this.error.set('');
    this.request = this.api.version(item.id).subscribe({
      next: detail => this.applyDetail(detail),
      error: e => this.fail(e)
    });
  }

  socialize(): void {
    const participants = this.socializationParticipants.split(',').map(v => v.trim()).filter(v => v.length > 0);
    if (participants.length < 1 || !this.socializationRecord.trim() || !this.socializationAt || this.saving()) return;
    this.run(this.api.socialize(this.selectedId(), this.selectedRevision(), {
      occurredAt: new Date(this.socializationAt).toISOString(),
      participantAssignmentIds: participants, recordReference: this.socializationRecord.trim()
    }), 'Socialización registrada con vigencia exacta.');
  }

  observe(): void {
    if (this.observationSection.trim().length < 1 || this.observationText.trim().length < 10 || this.saving()) return;
    this.run(this.api.observe(this.selectedId(), this.selectedRevision(), {
      sectionReference: this.observationSection.trim(), text: this.observationText.trim()
    }), 'Observación registrada; la versión queda bajo observación.');
  }

  reply(): void {
    if (this.replyText.trim().length < 10 || !this.replyObservationId || this.saving()) return;
    const correction = this.replyCorrectionId.trim() === '' ? null : this.replyCorrectionId.trim();
    this.run(this.api.reply(this.replyObservationId, this.selectedRevision(), {
      response: this.replyText.trim(), correctedVersionId: correction
    }), 'Respuesta registrada; el enlace de corrección queda trazado.');
  }

  decisionValid(): boolean {
    return this.decisionValue !== '' && this.decisionAssignmentId.trim().length > 0
      && this.decisionLegalBasis.trim().length > 0 && this.decisionDocumentReference.trim().length > 0
      && this.decisionReason.trim().length >= 10;
  }

  decide(): void {
    if (!this.decisionValid() || this.saving()) return;
    this.run(this.api.decide(this.selectedId(), this.selectedRevision(), {
      decision: this.decisionValue as ApprovalEvent['decision'],
      provenance: {
        authorityAssignmentId: this.decisionAssignmentId.trim(),
        legalBasis: this.decisionLegalBasis.trim(),
        documentReference: this.decisionDocumentReference.trim(),
        reason: this.decisionReason.trim()
      }
    }), 'Decisión institucional registrada con trazabilidad plena.');
  }

  repliesFor(observationId: string): readonly Reply[] {
    return this.replies().filter(reply => reply.observationId === observationId);
  }

  private run(work: Observable<unknown>, message: string): void {
    this.saving.set(true); this.error.set(''); this.message.set('');
    this.request?.unsubscribe();
    this.request = work.pipe(switchMap(() => this.observeData())).subscribe({
      next: result => {
        this.findings.set(result.findings?.items ?? []);
        this.versions.set(result.versions.items);
        this.saving.set(false);
        this.message.set(message);
      },
      error: e => this.fail(e)
    });
  }

  private reload(): void {
    this.request?.unsubscribe();
    this.error.set(''); this.loading.set(true);
    this.request = this.observeData().subscribe({
      next: result => {
        this.findings.set(result.findings?.items ?? []);
        this.versions.set(result.versions.items);
        this.loading.set(false);
      },
      error: e => this.fail(e)
    });
  }

  private observeData(): Observable<{findings: FindingPage | null; versions: VersionPage}> {
    const findings = this.api.findings(this.processId).pipe(catchError(() => of(null)));
    const versions = this.api.versions(this.processId);
    return forkJoin({findings, versions});
  }

  private applyDetail(detail: ReportVersion): void {
    this.detail.set(detail);
    this.selectedRevision.set(detail.revision);
    this.socializations.set(detail.socializations ?? []);
    this.observations.set(detail.observations ?? []);
    this.replies.set(detail.replies ?? []);
    this.decisions.set(detail.decisions ?? []);
    this.message.set(`Tramitando versión ${detail.state ?? detail.data.kind} · ETag ${detail.revision}`);
  }

  private selectedId(): string {
    return this.detail()?.id ?? '';
  }

  private fail(error: unknown): void {
    this.saving.set(false); this.loading.set(false);
    this.error.set(error instanceof ApiProblemError ? error.message
      : 'No se pudo completar la operación del ciclo del informe. Recargue antes de continuar.');
  }
}
