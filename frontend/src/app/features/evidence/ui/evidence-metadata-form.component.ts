import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {FormGroup, ReactiveFormsModule} from '@angular/forms';

export interface EvidenceIndicatorOption {
  readonly id: string;
  readonly code: string;
  readonly name: string;
}

@Component({
  selector: 'sec-evidence-metadata-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fieldset [formGroup]="form">
      <legend>Datos de la evidencia</legend>
      <label for="evidence-process">Proceso</label>
      <input id="evidence-process" formControlName="processId" (change)="processSelected.emit(processValue())" required>
      <label for="evidence-title">Título</label>
      <input id="evidence-title" formControlName="title" maxlength="300" required>
      <label for="evidence-description">Descripción</label>
      <textarea id="evidence-description" formControlName="description" maxlength="8000" required></textarea>
      <label for="evidence-provenance">Procedencia</label>
      <textarea id="evidence-provenance" formControlName="provenance" maxlength="4000" required></textarea>
      <label for="evidence-from">Desde</label>
      <input id="evidence-from" type="date" formControlName="coveredFrom" required>
      <label for="evidence-to">Hasta</label>
      <input id="evidence-to" type="date" formControlName="coveredTo" required>
    </fieldset>
    <fieldset>
      <legend>Indicadores</legend>
      @for (indicator of indicators; track indicator.id) {
        <label>
          <input type="checkbox" [checked]="selected(indicator.id)"
                 (change)="indicatorToggled.emit({id: indicator.id, selected: checkbox($event).checked})">
          {{indicator.code}} — {{indicator.name}}
        </label>
      }
      @if (form.controls['indicatorIds'].touched && form.controls['indicatorIds'].invalid) {
        <p role="alert">Seleccione al menos un indicador.</p>
      }
    </fieldset>
  `
})
export class EvidenceMetadataFormComponent {
  @Input({required: true}) form!: FormGroup;
  @Input() indicators: readonly EvidenceIndicatorOption[] = [];
  @Output() readonly processSelected = new EventEmitter<string>();
  @Output() readonly indicatorToggled = new EventEmitter<{id: string; selected: boolean}>();

  processValue(): string {
    return String(this.form.controls['processId'].value ?? '');
  }

  selected(id: string): boolean {
    return (this.form.controls['indicatorIds'].value as readonly string[]).includes(id);
  }

  checkbox(event: Event): HTMLInputElement {
    return event.target as HTMLInputElement;
  }
}
