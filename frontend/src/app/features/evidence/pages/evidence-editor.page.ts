import {ChangeDetectionStrategy, Component, ElementRef, OnInit, ViewChild, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {EvidenceDraftCommand, ExternalReferenceView, FileMetadataView} from '../data-access/evidence.models';
import {EvidenceIndicatorOption, EvidenceMetadataFormComponent} from '../ui/evidence-metadata-form.component';
import {EvidenceSourcesComponent, ReferenceDraft} from '../ui/evidence-sources.component';

@Component({
  selector: 'sec-evidence-editor',
  standalone: true,
  imports: [ReactiveFormsModule, EvidenceMetadataFormComponent, EvidenceSourcesComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section>
      <h2>{{versionId ? 'Editar borrador de evidencia' : 'Nueva evidencia'}}</h2>
      <form [formGroup]="form" (ngSubmit)="save()" novalidate>
        <sec-evidence-metadata-form [form]="form" [indicators]="indicators()"
          (processSelected)="selectProcess($event)" (indicatorToggled)="toggleIndicator($event.id, $event.selected)" />
        <button type="submit">Guardar borrador</button>
      </form>
      @if (versionId) {
        <sec-evidence-sources [files]="files()" [references]="references()"
          [disabled]="working() || stale()" [progress]="uploadProgress()" [error]="sourcesError()"
          (upload)="uploadFile($event)" (removeFile)="removeFile($event)"
          (saveReference)="saveReference($event)" (removeReference)="removeReference($event)" />
      }
      @if (message()) { <p role="status">{{message()}}</p> }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (stale()) {
        <section role="alert">
          <p>El borrador cambió. Revise la versión actual antes de volver a guardar.</p>
          <button #staleReload data-stale-reload type="button" (click)="reload()">Recargar borrador</button>
        </section>
      }
    </section>
  `
})
export class EvidenceEditorPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(EvidenceApi);
  private readonly processes = inject(ProcessApi);
  private readonly route = inject(ActivatedRoute);
  @ViewChild('staleReload') private staleReload?: ElementRef<HTMLButtonElement>;

  versionId: string | null = null;
  readonly etag = signal<string | null>(null);
  readonly indicators = signal<readonly EvidenceIndicatorOption[]>([]);
  readonly stale = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly files = signal<readonly FileMetadataView[]>([]);
  readonly references = signal<readonly ExternalReferenceView[]>([]);
  readonly uploadProgress = signal<number | null>(null);
  readonly sourcesError = signal<string | null>(null);
  readonly working = signal(false);
  readonly form = this.fb.nonNullable.group({
    processId: ['', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(300)]],
    description: ['', [Validators.required, Validators.maxLength(8000)]],
    provenance: ['', [Validators.required, Validators.maxLength(4000)]],
    coveredFrom: ['', Validators.required],
    coveredTo: ['', Validators.required],
    indicatorIds: this.fb.nonNullable.control<readonly string[]>([], Validators.required)
  });

  ngOnInit(): void {
    this.versionId = this.route.snapshot.paramMap.get('versionId');
    if (this.versionId) {
      this.form.controls.processId.disable();
      this.loadVersion(this.versionId);
    } else {
      const processId = this.route.snapshot.queryParamMap.get('processId');
      if (processId) this.selectProcess(processId);
    }
  }

  selectProcess(processId: string): void {
    if (!processId) return;
    this.form.controls.processId.setValue(processId);
    this.processes.get(processId).subscribe({
      next: process => this.indicators.set(process.indicators),
      error: () => this.error.set('No fue posible consultar los indicadores del proceso.')
    });
  }

  toggleIndicator(indicatorId: string, selected: boolean): void {
    const current = this.form.controls.indicatorIds.value;
    const next = selected
      ? [...new Set([...current, indicatorId])]
      : current.filter(value => value !== indicatorId);
    this.form.controls.indicatorIds.setValue(next);
    this.form.controls.indicatorIds.markAsTouched();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.stale.set(false);
    const command: EvidenceDraftCommand = this.form.getRawValue();
    const request = this.versionId
      ? this.api.update(this.versionId, command, this.etag() ?? '')
      : this.api.create(command);
    request.subscribe({
      next: resource => {
        this.versionId = resource.body.id;
        this.etag.set(resource.etag);
        this.files.set(resource.body.files);
        this.references.set(resource.body.externalReferences);
        this.message.set('Borrador guardado.');
      },
      error: (failure: unknown) => this.handleFailure(failure)
    });
  }

  reload(): void {
    if (this.versionId) this.loadVersion(this.versionId);
  }

  private loadVersion(versionId: string): void {
    this.api.getVersion(versionId).subscribe({
      next: resource => {
        const version = resource.body;
        this.form.patchValue({
          title: version.title,
          description: version.description,
          provenance: version.provenance,
          coveredFrom: version.coveredFrom,
          coveredTo: version.coveredTo,
          indicatorIds: version.indicatorIds
        });
        this.etag.set(resource.etag);
        this.files.set(version.files);
        this.references.set(version.externalReferences);
        this.stale.set(false);
      },
      error: () => this.error.set('No fue posible cargar el borrador.')
    });
  }

  uploadFile(file: File): void {
    this.sourceRequest(versionId => this.api.uploadFile(versionId, file, this.etag() ?? ''),
      'Archivo adjuntado.', 'No fue posible adjuntar el archivo.');
  }

  removeFile(fileId: string): void {
    this.sourceRequest(versionId => this.api.removeFile(versionId, fileId, this.etag() ?? ''),
      'Archivo quitado del borrador.', 'No fue posible quitar el archivo.');
  }

  saveReference(draft: ReferenceDraft): void {
    const command = {uri: draft.uri, description: draft.description};
    this.sourceRequest(versionId => draft.id
        ? this.api.updateReference(versionId, draft.id, command, this.etag() ?? '')
        : this.api.addReference(versionId, command, this.etag() ?? ''),
      'Referencia guardada.', 'No fue posible guardar la referencia.');
  }

  removeReference(referenceId: string): void {
    this.sourceRequest(versionId => this.api.removeReference(versionId, referenceId, this.etag() ?? ''),
      'Referencia quitada del borrador.', 'No fue posible quitar la referencia.');
  }

  private sourceRequest(call: (versionId: string) => import('rxjs').Observable<unknown>,
                        success: string, failure: string): void {
    if (!this.versionId || !this.etag()) {
      this.sourcesError.set('Guarde el borrador antes de modificar sus fuentes.');
      return;
    }
    this.sourcesError.set(null);
    this.stale.set(false);
    this.working.set(true);
    this.uploadProgress.set(0);
    call(this.versionId).subscribe({
      next: () => {
        this.message.set(success);
        this.loadVersion(this.versionId!);
        this.working.set(false);
        this.uploadProgress.set(null);
      },
      error: (error: unknown) => {
        this.working.set(false);
        this.uploadProgress.set(null);
        this.handleSourceFailure(error, failure);
      }
    });
  }

  private handleSourceFailure(failure: unknown, fallback: string): void {
    if (failure instanceof ApiProblemError) {
      if (failure.problem.code === 'STALE_DRAFT') {
        this.stale.set(true);
        return;
      }
      if (failure.problem.code === 'FILE_TOO_LARGE') {
        this.sourcesError.set('El archivo supera el límite de 25 MiB.');
        return;
      }
      if (failure.problem.code === 'FILE_TYPE_NOT_ALLOWED' || failure.problem.code === 'FILE_TYPE_MISMATCH') {
        this.sourcesError.set('El tipo de archivo no está permitido o no coincide con el contenido.');
        return;
      }
    }
    this.sourcesError.set(fallback);
  }

  private handleFailure(failure: unknown): void {
    if (failure instanceof ApiProblemError && failure.problem.code === 'STALE_DRAFT') {
      this.stale.set(true);
      queueMicrotask(() => this.staleReload?.nativeElement.focus());
      return;
    }
    this.error.set('No fue posible guardar el borrador.');
  }
}
