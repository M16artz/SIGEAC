import {ChangeDetectionStrategy, Component, inject, input, output} from '@angular/core';
import {EntityNamesService} from './entity-names.service';
import {AccessSessionService} from '../../features/access/data-access/access-session.service';

/** Reuses authorized names; keeps manual references out of the ordinary workflow when a directory is unavailable. */
@Component({selector: 'sec-person-picker', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <select [id]="controlId()" [value]="value()" (change)="valueChange.emit($any($event.target).value)" [required]="!value()">
    <option value="">Seleccione una persona</option>
    @if (session.identity(); as identity) {<option [value]="identity.id">{{identity.displayName}}</option>}
    @for (person of names.users(); track person.id) {
      @if (person.id !== session.identity()?.id) {<option [value]="person.id">{{person.displayName}} ({{person.username}})</option>}
    }
  </select>
  @if (names.users().length === 0) {
    <details><summary>Usar una referencia autorizada</summary>
      <p>No se puede consultar el directorio de otras personas con esta sesión.</p>
      <label [for]="controlId() + '-reference'">Referencia de la persona</label>
      <input [id]="controlId() + '-reference'" [value]="value()" (input)="valueChange.emit($any($event.target).value)" autocomplete="off">
    </details>
  }
`})
export class PersonPickerComponent {
  readonly names = inject(EntityNamesService);
  readonly session = inject(AccessSessionService);
  readonly controlId = input.required<string>();
  readonly value = input('');
  readonly valueChange = output<string>();
}
