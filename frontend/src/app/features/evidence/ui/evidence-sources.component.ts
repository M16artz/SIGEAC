import {ChangeDetectionStrategy, Component, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ExternalReferenceView, FileMetadataView} from '../data-access/evidence.models';

export interface ReferenceDraft {
  readonly id: string | null;
  readonly uri: string;
  readonly description: string;
}

@Component({
  selector: 'sec-evidence-sources',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="files-heading">
      <h3 id="files-heading">Archivos adjuntos</h3>
      @if (files().length === 0) {
        <p role="status">El borrador no tiene archivos.</p>
      } @else {
        <ul>
          @for (file of files(); track file.id) {
            <li [attr.data-file-id]="file.id">
              <span>{{file.originalName}}</span>
              <span> · {{file.detectedMediaType}} · {{file.sizeBytes}} bytes</span>
              <span> · SHA-256 {{file.sha256.slice(0, 12)}}…</span>
              <span> · {{file.status}}</span>
              <button type="button" data-remove-file [disabled]="disabled()" (click)="removeFile.emit(file.id)">Quitar</button>
            </li>
          }
        </ul>
      }
      <label for="evidence-file">Añadir archivo (máximo 25 MiB)</label>
      <input id="evidence-file" type="file" [disabled]="disabled()" (change)="selectFile($event)" />
      @if (progress() !== null) {
        <div role="progressbar" aria-valuemin="0" aria-valuemax="100" [attr.aria-valuenow]="progress()">
          Cargando {{progress()}}%
        </div>
      }
    </section>

    <section aria-labelledby="references-heading">
      <h3 id="references-heading">Referencias externas declaradas</h3>
      <p>Una referencia declara una ubicación; no se comprueba su disponibilidad en red.</p>
      @if (references().length === 0) {
        <p role="status">El borrador no tiene referencias externas.</p>
      } @else {
        <ul>
          @for (reference of references(); track reference.id) {
            <li [attr.data-reference-id]="reference.id">
              <span>{{reference.uri}}</span>
              <span> · {{reference.description}}</span>
              <button type="button" data-edit-reference [disabled]="disabled()" (click)="edit(reference)">Editar</button>
              <button type="button" data-remove-reference [disabled]="disabled()" (click)="removeReference.emit(reference.id)">Quitar</button>
            </li>
          }
        </ul>
      }
      @if (!disabled()) {
        <form (ngSubmit)="submitReference()" novalidate>
          <label for="reference-uri">URI absoluta</label>
          <input id="reference-uri" name="uri" maxlength="2048" [ngModel]="uri()" (ngModelChange)="uri.set($event)" />
          <label for="reference-description">Descripción</label>
          <input id="reference-description" name="description" maxlength="1000"
            [ngModel]="description()" (ngModelChange)="description.set($event)" />
          @if (invalid()) { <p role="alert">Indique una URI absoluta y una descripción de hasta 1000 caracteres.</p> }
          <button type="submit" data-save-reference>{{editing() ? 'Guardar referencia' : 'Añadir referencia'}}</button>
          @if (editing()) { <button type="button" (click)="reset()">Cancelar</button> }
        </form>
      }
      @if (error()) { <p role="alert">{{error()}}</p> }
    </section>
  `
})
export class EvidenceSourcesComponent {
  readonly files = input<readonly FileMetadataView[]>([]);
  readonly references = input<readonly ExternalReferenceView[]>([]);
  readonly disabled = input(false);
  readonly progress = input<number | null>(null);
  readonly error = input<string | null>(null);
  readonly upload = output<File>();
  readonly removeFile = output<string>();
  readonly saveReference = output<ReferenceDraft>();
  readonly removeReference = output<string>();

  readonly editing = signal<string | null>(null);
  readonly uri = signal('');
  readonly description = signal('');
  readonly invalid = signal(false);

  selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.upload.emit(file);
    input.value = '';
  }

  edit(reference: ExternalReferenceView): void {
    this.editing.set(reference.id);
    this.uri.set(reference.uri);
    this.description.set(reference.description);
    this.invalid.set(false);
  }

  reset(): void {
    this.editing.set(null);
    this.uri.set('');
    this.description.set('');
    this.invalid.set(false);
  }

  submitReference(): void {
    const uri = this.uri().trim();
    const description = this.description().trim();
    if (!uri || !description || description.length > 1000 || !/^[a-z][a-z0-9+.-]*:/i.test(uri)) {
      this.invalid.set(true);
      return;
    }
    const draft = {id: this.editing(), uri, description};
    this.reset();
    this.saveReference.emit(draft);
  }
}
