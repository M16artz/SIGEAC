import {ChangeDetectionStrategy, Component, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RequiredSourceState, SourceCoverageView} from '../data-access/evidence.models';

export interface CoverageDraft {
  readonly requiredSourceId: string;
  readonly coverageNote: string;
}

@Component({
  selector: 'sec-evidence-source-coverage',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="coverage-heading">
      <h3 id="coverage-heading">Fuentes requeridas y cobertura declarada</h3>
      @if (sources().length === 0) {
        <p role="status">La versión no tiene indicadores con fuentes requeridas.</p>
      } @else {
        <table>
          <caption>Fuentes del modelo fijo del proceso vigente</caption>
          <thead>
            <tr><th scope="col">Fuente</th><th scope="col">Indicador</th><th scope="col">Tipo</th>
              <th scope="col">Estado</th><th scope="col">Nota</th><th scope="col">Acciones</th></tr>
          </thead>
          <tbody>
            @for (source of sources(); track source.id) {
              <tr [attr.data-source-covered]="source.covered">
                <td>{{source.code}} · {{source.name}}</td>
                <td>{{source.indicatorCode}}</td>
                <td>{{source.sourceType}}</td>
                <td>{{source.covered ? 'Cubierta' : 'Sin cobertura'}}</td>
                <td>{{noteOf(source)}}</td>
                <td>
                  @if (coverageOf(source)) {
                    <button type="button" data-remove-coverage [disabled]="disabled()" (click)="remove.emit(coverageOf(source)!.id)">Quitar</button>
                  } @else {
                    <button type="button" data-declare-coverage [disabled]="disabled()" (click)="select(source)">Declarar</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
      @if (selected() && !disabled()) {
        <form (ngSubmit)="addCoverage()" novalidate>
          <label for="coverage-note">Nota de cobertura</label>
          <textarea id="coverage-note" name="coverageNote" required maxlength="4000"
            [ngModel]="note()" (ngModelChange)="note.set($event)"></textarea>
          <button type="submit" data-save-coverage>Guardar cobertura</button>
          <button type="button" (click)="cancel()">Cancelar</button>
          @if (invalid()) { <p role="alert">La nota de cobertura es obligatoria.</p> }
        </form>
      }
    </section>
  `
})
export class EvidenceSourceCoverageComponent {
  readonly sources = input<readonly RequiredSourceState[]>([]);
  readonly coverages = input<readonly SourceCoverageView[]>([]);
  readonly disabled = input(false);
  readonly declare = output<CoverageDraft>();
  readonly remove = output<string>();
  readonly selected = signal<RequiredSourceState | null>(null);
  readonly note = signal('');
  readonly invalid = signal(false);

  coverageOf(source: RequiredSourceState): SourceCoverageView | undefined {
    return this.coverages().find(coverage => coverage.requiredSourceId === source.id);
  }

  noteOf(source: RequiredSourceState): string {
    return this.coverageOf(source)?.coverageNote ?? '—';
  }

  select(source: RequiredSourceState): void {
    this.selected.set(source);
    this.note.set('');
    this.invalid.set(false);
  }

  cancel(): void {
    this.selected.set(null);
    this.note.set('');
    this.invalid.set(false);
  }

  addCoverage(): void {
    const source = this.selected();
    if (!source) return;
    const value = this.note().trim();
    if (!value) {
      this.invalid.set(true);
      return;
    }
    this.cancel();
    this.declare.emit({requiredSourceId: source.id, coverageNote: value});
  }
}
