import {ChangeDetectionStrategy, Component, OnInit, inject, signal} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ApiProblemError} from '../../../core/http/api-client';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {
  EvidenceVersionView,
  FileMetadataView,
  RequiredSourceState,
  SourceCoverageView,
  StructuredSourceCommand,
  StructuredSourceView
} from '../data-access/evidence.models';
import {CoverageDraft, EvidenceSourceCoverageComponent} from '../ui/evidence-source-coverage.component';
import {StructuredEvidenceComponent} from '../ui/structured-evidence.component';

@Component({
  selector: 'sec-evidence-version-page',
  standalone: true,
  imports: [EvidenceSourceCoverageComponent, StructuredEvidenceComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main>
      @if (version(); as current) {
        <h2>Versión {{current.revisionNumber}} · {{current.title}}</h2>
        <p><strong>Estado:</strong> {{current.status}} · <strong>Vigencia:</strong> {{current.coveredFrom}} → {{current.coveredTo}}</p>
        <dl>
          <dt>Procedencia</dt><dd>{{current.provenance}}</dd>
          <dt>Indicadores</dt><dd>{{current.indicatorIds.length}}</dd>
          <dt>Enviada</dt><dd>{{current.submittedAt ? (current.submittedAt) : 'Pendiente'}}</dd>
        </dl>
        <section aria-labelledby="version-files-heading">
          <h3 id="version-files-heading">Archivos</h3>
          @if (current.files.length === 0) {
            <p>Esta versión no tiene archivos adjuntos.</p>
          } @else {
            <ul>
              @for (file of current.files; track file.id) {
                <li>
                  <span>{{file.originalName}}</span>
                  <span> · {{file.sizeBytes}} bytes · SHA-256 {{file.sha256}}</span>
                  <button type="button" [attr.data-download-file]="file.id" [disabled]="working()"
                    (click)="download(file)">Descargar</button>
                </li>
              }
            </ul>
          }
        </section>
        <sec-evidence-source-coverage [sources]="sources()" [coverages]="coverages()"
          [disabled]="current.status === 'SUBMITTED' || working()"
          (declare)="declareCoverage($event)" (remove)="removeCoverage($event)" />
        <sec-structured-evidence [sources]="sources()" [snapshots]="snapshots()"
          [disabled]="current.status === 'SUBMITTED' || working()"
          (register)="registerSnapshot($event)" />
        @if (current.status !== 'SUBMITTED') {
          <button type="button" data-submit-version [disabled]="working()" (click)="submitVersion()">Remitir evidencia</button>
        }
        @if (message()) { <p role="status">{{message()}}</p> }
        @if (stale()) {
          <section role="alert">
            <p>El borrador cambió mientras se registraba la cobertura. Recargue y reintente.</p>
            <button type="button" data-stale-reload (click)="reload()">Recargar versión</button>
          </section>
        }
        @if (error()) { <p role="alert">{{error()}}</p> }
      } @else {
        <p role="status">Cargando versión de evidencia…</p>
        @if (error()) { <p role="alert">{{error()}}</p> }
      }
    </main>
  `
})
export class EvidenceVersionPage implements OnInit {
  private readonly api = inject(EvidenceApi);
  private readonly route = inject(ActivatedRoute);

  readonly version = signal<EvidenceVersionView | null>(null);
  readonly sources = signal<readonly RequiredSourceState[]>([]);
  readonly coverages = signal<readonly SourceCoverageView[]>([]);
  readonly snapshots = signal<readonly StructuredSourceView[]>([]);
  readonly etag = signal<string | null>(null);
  readonly stale = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly working = signal(false);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    const versionId = this.route.snapshot.paramMap.get('versionId');
    if (!versionId) {
      this.error.set('Versión no especificada.');
      return;
    }
    this.stale.set(false);
    this.error.set(null);
    this.working.set(true);
    this.api.getVersion(versionId).subscribe({
      next: resource => {
        this.version.set(resource.body);
        this.etag.set(resource.etag);
        this.api.requiredSources(versionId).subscribe({
          next: sources => this.sources.set(sources),
          error: () => this.error.set('No fue posible consultar las fuentes requeridas.')
        });
        this.api.coverages(versionId).subscribe({
          next: coverages => this.coverages.set(coverages),
          error: () => this.error.set('No fue posible consultar las coberturas.')
        });
        this.api.structuredSnapshots(versionId).subscribe({
          next: values => this.snapshots.set(values),
          error: () => this.error.set('No fue posible consultar los snapshots estructurados.')
        });
        this.working.set(false);
      },
      error: () => {
        this.error.set('No fue posible cargar la versión.');
        this.working.set(false);
      }
    });
  }

  declareCoverage(draft: CoverageDraft): void {
    const versionId = this.route.snapshot.paramMap.get('versionId');
    const etag = this.etag();
    if (!versionId || !etag) {
      this.error.set('No hay revisión vigente para registrar la cobertura.');
      return;
    }
    this.begin();
    this.api.declareCoverage(versionId, draft, etag).subscribe({
      next: () => {
        this.message.set('Cobertura registrada.');
        this.reload();
      },
      error: (failure: unknown) => this.handleFailure(failure)
    });
  }

  removeCoverage(coverageId: string): void {
    const versionId = this.route.snapshot.paramMap.get('versionId');
    const etag = this.etag();
    if (!versionId || !etag) {
      this.error.set('No hay revisión vigente para quitar la cobertura.');
      return;
    }
    this.begin();
    this.api.removeCoverage(versionId, coverageId, etag).subscribe({
      next: () => {
        this.message.set('Cobertura retirada.');
        this.reload();
      },
      error: (failure: unknown) => this.handleFailure(failure)
    });
  }

  registerSnapshot(command: StructuredSourceCommand): void {
    const versionId = this.route.snapshot.paramMap.get('versionId');
    const etag = this.etag();
    if (!versionId || !etag) {
      this.error.set('No hay revisión vigente para registrar el snapshot.');
      return;
    }
    this.begin();
    this.api.registerStructuredSource(versionId, command, etag).subscribe({
      next: () => {
        this.message.set('Snapshot registrado.');
        this.reload();
      },
      error: (failure: unknown) => this.handleFailure(failure)
    });
  }

  submitVersion(): void {
    const versionId = this.route.snapshot.paramMap.get('versionId');
    const etag = this.etag();
    if (!versionId || !etag) {
      this.error.set('No hay revisión vigente para remitir.');
      return;
    }
    this.begin();
    this.api.submit(versionId, etag).subscribe({
      next: () => {
        this.message.set('Evidencia remitida.');
        this.reload();
      },
      error: (failure: unknown) => this.handleFailure(failure)
    });
  }

  download(file: FileMetadataView): void {
    const versionId = this.route.snapshot.paramMap.get('versionId');
    if (!versionId) {
      this.error.set('Versión no especificada.');
      return;
    }
    this.error.set(null);
    this.message.set(null);
    this.working.set(true);
    this.api.downloadFile(versionId, file.id).subscribe({
      next: blob => {
        this.working.set(false);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = file.originalName;
        anchor.click();
        URL.revokeObjectURL(url);
        this.message.set(`Archivo descargado y verificado: ${file.originalName}.`);
      },
      error: (failure: unknown) => this.handleDownloadFailure(failure, file)
    });
  }

  private handleDownloadFailure(failure: unknown, file: FileMetadataView): void {
    this.working.set(false);
    if (failure instanceof ApiProblemError && failure.problem.code === 'FILE_INTEGRITY_FAILED') {
      this.error.set('El archivo no supera la verificación de integridad; no se descargó.');
      return;
    }
    this.error.set(`No fue posible descargar ${file.originalName}.`);
  }

  private begin(): void {
    this.error.set(null);
    this.message.set(null);
    this.stale.set(false);
    this.working.set(true);
  }

  private handleFailure(failure: unknown): void {
    this.working.set(false);
    if (failure instanceof ApiProblemError && failure.problem.code === 'STALE_DRAFT') {
      this.stale.set(true);
      return;
    }
    if (failure instanceof ApiProblemError && failure.problem.code === 'COVERAGE_REQUIRED') {
      this.error.set('Se requiere al menos una cobertura de fuente antes de remitir.');
      return;
    }
    if (failure instanceof ApiProblemError && failure.problem.code === 'STRUCTURED_SOURCE_NOT_COVERED') {
      this.error.set('La fuente requiere cobertura declarada antes del snapshot.');
      return;
    }
    this.error.set('La operación no se pudo completar.');
  }
}
