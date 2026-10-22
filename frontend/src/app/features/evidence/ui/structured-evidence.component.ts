import {ChangeDetectionStrategy, Component, computed, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {
  RequiredSourceState,
  StructuredSourceCommand,
  StructuredSourceView
} from '../data-access/evidence.models';

/**
 * AC-X16 / FR-069: editor for structured-data and institutional-reference
 * snapshots; conserves typed value, unit and period, never demands files.
 */
@Component({
  selector: 'sec-structured-evidence',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="structured-heading">
      <h3 id="structured-heading">Datos estructurados y referencias institucionales</h3>
      @if (snapshots().length === 0 && candidates().length === 0) {
        <p role="status">No hay fuentes de datos estructurados ni referencias institucionales con cobertura declarada.</p>
      }
      @if (snapshots().length > 0) {
        <table>
          <caption>Snapshots registrados en la versión</caption>
          <thead>
            <tr><th scope="col">Fuente</th><th scope="col">Modalidad</th><th scope="col">Valor</th>
              <th scope="col">Unidad</th><th scope="col">Período</th><th scope="col">Referencia</th></tr>
          </thead>
          <tbody>
            @for (snapshot of snapshots(); track snapshot.id) {
              <tr>
                <td>{{sourceName(snapshot.requiredSourceId)}}</td>
                <td>{{snapshot.kind}}</td>
                <td>{{snapshot.value}}</td>
                <td>{{snapshot.unit ?? '—'}}</td>
                <td>{{snapshot.periodStart}} → {{snapshot.periodEnd}}</td>
                <td>{{snapshot.reference ?? '—'}}</td>
              </tr>
            }
          </tbody>
        </table>
      }
      @if (candidates().length > 0 && !disabled()) {
        <form (ngSubmit)="registerSnapshot()" novalidate>
          <label for="structured-source">Fuente</label>
          <select id="structured-source" name="selectedSourceId" [ngModel]="selectedSourceId()"
            (ngModelChange)="selectedSourceId.set($event)">
            <option value="">Seleccione una fuente</option>
            @for (source of candidates(); track source.id) {
              <option [value]="source.id">{{source.code}} · {{source.name}} ({{source.sourceType}})</option>
            }
          </select>
          <label for="value-type">Tipo de valor</label>
          <select id="value-type" name="valueType" [ngModel]="valueType()"
            (ngModelChange)="valueType.set($event)">
            <option value="NUMBER">Número</option>
            <option value="STRING">Texto</option>
            <option value="BOOLEAN">Booleano</option>
          </select>
          @switch (valueType()) {
            @case ('NUMBER') {
              <label for="structured-number">Valor</label>
              <input id="structured-number" name="numberValue" type="number" [ngModel]="numberValue()"
                (ngModelChange)="numberValue.set($event)" />
            }
            @case ('BOOLEAN') {
              <label for="structured-boolean">Valor</label>
              <select id="structured-boolean" name="booleanValue" [ngModel]="booleanValue()"
                (ngModelChange)="booleanValue.set($event)">
                <option [ngValue]="true">Verdadero</option>
                <option [ngValue]="false">Falso</option>
              </select>
            }
            @default {
              <label for="structured-text">Valor</label>
              <textarea id="structured-text" name="textValue" maxlength="4000" [ngModel]="textValue()"
                (ngModelChange)="textValue.set($event)"></textarea>
            }
          }
          <label for="structured-unit">Unidad (opcional)</label>
          <input id="structured-unit" name="unit" maxlength="100" [ngModel]="unit()" (ngModelChange)="unit.set($event)" />
          <label for="structured-period-start">Inicio del período</label>
          <input id="structured-period-start" name="periodStart" type="date"
            [ngModel]="periodStart()" (ngModelChange)="periodStart.set($event)" />
          <label for="structured-period-end">Fin del período</label>
          <input id="structured-period-end" name="periodEnd" type="date"
            [ngModel]="periodEnd()" (ngModelChange)="periodEnd.set($event)" />
          <label for="structured-reference">Referencia (opcional)</label>
          <input id="structured-reference" name="reference" maxlength="2000"
            [ngModel]="reference()" (ngModelChange)="reference.set($event)" />
          <button type="submit" data-register-snapshot>Registrar snapshot</button>
          @if (formError()) { <p role="alert">{{formError()}}</p> }
        </form>
      }
    </section>
  `
})
export class StructuredEvidenceComponent {
  readonly sources = input<readonly RequiredSourceState[]>([]);
  readonly snapshots = input<readonly StructuredSourceView[]>([]);
  readonly disabled = input(false);
  readonly register = output<StructuredSourceCommand>();

  readonly selectedSourceId = signal('');
  readonly valueType = signal<'NUMBER' | 'STRING' | 'BOOLEAN'>('STRING');
  readonly numberValue = signal<number | null>(null);
  readonly textValue = signal('');
  readonly booleanValue = signal(true);
  readonly unit = signal('');
  readonly periodStart = signal('');
  readonly periodEnd = signal('');
  readonly reference = signal('');
  readonly formError = signal<string | null>(null);

  readonly eligible = computed(() =>
    this.sources().filter(source =>
      source.sourceType === 'STRUCTURED_DATA' || source.sourceType === 'INSTITUTIONAL_REFERENCE'));
  readonly candidates = computed(() => this.eligible().filter(source =>
    !this.snapshots().some(snapshot => snapshot.requiredSourceId === source.id)));

  sourceName(sourceId: string): string {
    const source = this.sources().find(item => item.id === sourceId);
    return source ? `${source.code} · ${source.name}` : sourceId;
  }

  registerSnapshot(): void {
    const source = this.sources().find(item => item.id === this.selectedSourceId());
    if (!source) {
      this.formError.set('Seleccione una fuente requerida.');
      return;
    }
    const start = this.periodStart();
    const end = this.periodEnd();
    if (!start || !end || start > end) {
      this.formError.set('El período del snapshot es inválido.');
      return;
    }
    let value: string | number | boolean;
    if (this.valueType() === 'NUMBER') {
      const numeric = this.numberValue();
      if (numeric === null || Number.isNaN(numeric)) {
        this.formError.set('El valor numérico es obligatorio.');
        return;
      }
      value = numeric;
    } else if (this.valueType() === 'BOOLEAN') {
      value = this.booleanValue();
    } else {
      const text = this.textValue().trim();
      if (!text) {
        this.formError.set('El valor del snapshot no puede estar vacío.');
        return;
      }
      value = text;
    }
    this.formError.set(null);
    this.register.emit({
      requiredSourceId: source.id,
      kind: source.sourceType === 'INSTITUTIONAL_REFERENCE' ? 'INSTITUTIONAL_REFERENCE' : 'STRUCTURED_DATA',
      value,
      unit: this.unit().trim() || null,
      periodStart: start,
      periodEnd: end,
      reference: this.reference().trim() || null
    });
  }
}
