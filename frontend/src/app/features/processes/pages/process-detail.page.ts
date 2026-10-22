import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {ProcessApi, ProcessView} from '../data-access/process-api.service';
import {ProcessStateControls} from '../ui/process-state-controls.component';
import {ProcessAssignmentForm} from '../ui/process-assignment-form.component';

@Component({
  selector: 'sec-process-detail',
  standalone: true,
  imports: [FormsModule, RouterLink, ProcessStateControls, ProcessAssignmentForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p><a routerLink="/processes">Volver a procesos</a></p>
    <h2>Detalle del proceso</h2>
    @if (error()) { <p role="alert">{{error()}}</p> }
    @if (process(); as current) {
      <p><strong>{{current.code}} · {{current.name}}</strong></p>
      <p>Modelo fijado: {{current.modelVersionId}} · v{{current.modelVersionNumber}}</p>
      <ul>@for (indicator of current.indicators; track indicator.id) {
        <li>{{indicator.code}} · {{indicator.name}}</li>
      }</ul>
      <sec-process-state-controls [process]="current" (changed)="process.set($event)" />
      <sec-process-assignment-form [process]="current" (changed)="process.set($event)" />
    }
  `
})
export class ProcessDetailPage implements OnInit {
  readonly process = signal<ProcessView | null>(null);
  readonly error = signal('');

  constructor(private readonly api: ProcessApi, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('processId');
    if (!id) { this.error.set('No se indicó el proceso.'); return; }
    this.api.get(id).subscribe({
      next: process => this.process.set(process),
      error: () => this.error.set('No fue posible consultar el proceso.')
    });
  }
}
