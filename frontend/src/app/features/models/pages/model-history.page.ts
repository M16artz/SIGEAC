import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ModelApi} from '../data-access/model-api.service';
import {ModelVersion} from '../data-access/model.models';

@Component({selector: 'sec-model-history', standalone: true, imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="sigeac-page">
    <header class="sigeac-page-header">
      <span class="sigeac-badge">Modelos</span>
      <h2>Historial de versiones</h2>
      <p>Consulte las versiones conservadas de cada modelo de evaluación.</p>
    </header>
    <section class="sigeac-page-body">
    <div class="sigeac-form-search">
    <div class="sigeac-form-field"><label for="history-model">Modelo</label><input id="history-model" [(ngModel)]="modelId"></div>
    <button (click)="load()">Consultar</button>
    </div>
    @if (error()) { <p role="alert">{{error()}}</p> }
    <ol>@for (v of versions(); track v.id) {
      <li>v{{v.versionNumber}} · {{v.status}} · fuente: {{v.source}}
        @if (v.status !== 'DRAFT') { <button (click)="successor(v)">Crear sucesora</button> }
      </li>
    }</ol>
    </section>
    </div>`})
export class ModelHistoryPage {
  modelId = '';
  readonly versions = signal<readonly ModelVersion[]>([]);
  readonly error = signal('');
  constructor(private readonly api: ModelApi) {}
  load(): void {
    this.error.set(''); this.versions.set([]);
    this.api.configuredVersions(this.modelId).subscribe({next: versions => this.versions.set(versions),
      error: () => this.error.set('No fue posible consultar el historial autorizado.')});
  }
  successor(version: ModelVersion): void {
    this.error.set('');
    const request = version.configuration ? this.api.configuredSuccessor(version.id) : this.api.successor(version.id);
    request.subscribe({next: () => this.load(),
      error: () => this.error.set('No fue posible crear la sucesora. Revise permisos y estado de la versión.')});
  }
}
