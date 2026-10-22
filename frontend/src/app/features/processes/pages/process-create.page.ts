import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {ProcessApi, ProcessView} from '../data-access/process-api.service';
import {Career, CareerApi} from '../../careers/data-access/career-api.service';

@Component({
  selector: 'sec-process-create',
  standalone: true,
  imports: [FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Procesos de autoevaluación</h2>
    @if (processes().length > 0) {
      <table>
        <caption>Procesos visibles en su ámbito</caption>
        <thead><tr><th>Código</th><th>Nombre</th><th>Estado</th><th>Acción</th></tr></thead>
        <tbody>
          @for (item of processes(); track item.id) {
            <tr><td>{{item.code}}</td><td>{{item.name}}</td><td>{{item.state}}</td>
              <td><a [routerLink]="[item.id]">Abrir</a></td></tr>
          }
        </tbody>
      </table>
    } @else {
      <p>No hay procesos visibles.</p>
    }

    <h3>Crear proceso</h3>
    <p>La versión publicada se fijará al activar.</p>
    <form (ngSubmit)="create()">
      <label for="career-query">Buscar carrera activa</label><input id="career-query" [(ngModel)]="careerQuery" name="careerQuery" maxlength="300"><button type="button" (click)="loadCareers()">Buscar carreras activas</button>
      <label for="career">Carrera</label><select id="career" [(ngModel)]="careerId" name="career" required><option value="">Seleccione una carrera</option>@for(career of careers();track career.id){<option [value]="career.id">{{career.data.code}} · {{career.data.name}}</option>}</select>
      <label for="code">Código</label><input id="code" [(ngModel)]="code" name="code" required>
      <label for="name">Denominación</label><input id="name" [(ngModel)]="name" name="name" required>
      <label for="start">Inicio</label><input id="start" type="date" [(ngModel)]="start" name="start" required>
      <label for="end">Fin</label><input id="end" type="date" [(ngModel)]="end" name="end" required>
      <label for="model">Versión publicada</label><input id="model" [(ngModel)]="modelVersionId" name="model" required>
      <button [disabled]="!careerId">Crear proceso en preparación</button>
    </form>
    @if (created(); as process) {
      <p role="status">{{process.code}} · {{process.state}} · modelo v{{process.modelVersionNumber}}</p>
      <a [routerLink]="[process.id]">Administrar proceso</a>
    }
    @if (error()) { <p role="alert">{{error()}}</p> }
  `
})
export class ProcessCreatePage implements OnInit {
  careerId = '';
  careerQuery = '';
  readonly careers = signal<readonly Career[]>([]);
  code = 'PROC-SYN-1';
  name = 'Proceso sintético';
  start = '2026-01-01';
  end = '2026-12-31';
  modelVersionId = '';
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly created = signal<ProcessView | null>(null);
  readonly error = signal('');

  constructor(private readonly api: ProcessApi,private readonly catalog: CareerApi) {}

  ngOnInit(): void { this.load(); this.loadCareers(); }

  loadCareers(): void { this.catalog.careers(0,this.careerQuery,'ACTIVE',100).subscribe({next:result=>this.careers.set(result.items.filter(value=>!value.classificationPending)),error:()=>this.error.set('No fue posible consultar las carreras autorizadas.')}); }

  load(): void {
    this.api.list().subscribe({
      next: processes => this.processes.set(processes),
      error: () => this.error.set('No fue posible consultar los procesos.')
    });
  }

  create(): void {
    this.api.create({
      careerId: this.careerId, code: this.code, name: this.name,
      periodStart: this.start, periodEnd: this.end, modelVersionId: this.modelVersionId
    }).subscribe({
      next: process => { this.created.set(process); this.load(); },
      error: () => this.error.set('No fue posible crear el proceso.')
    });
  }
}
