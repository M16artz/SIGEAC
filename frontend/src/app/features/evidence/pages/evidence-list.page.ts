import {ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {EvidenceView} from '../data-access/evidence.models';

@Component({
  selector: 'sec-evidence-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section>
      <h2>Evidencias</h2>
      <label for="evidence-process">Proceso</label>
      <select id="evidence-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)">
        <option value="">Seleccione un proceso</option>
        @for (process of processes(); track process.id) {
          <option [value]="process.id">{{process.code}} · {{process.name}}</option>
        }
      </select>
      @if (processId) {
        <a routerLink="new" [queryParams]="{processId}">Nueva evidencia</a>
      }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (!processId) {
        <p role="status">Seleccione un proceso para consultar sus evidencias.</p>
      } @else if (!loading() && items().length === 0) {
        <p role="status">No hay evidencias en el proceso seleccionado.</p>
      } @else if (items().length > 0) {
        <table>
          <caption>Evidencias visibles en su ámbito vigente</caption>
          <thead><tr><th scope="col">Título</th><th scope="col">Versión</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td>{{item.latestVersion.title}}</td>
                <td>{{item.latestRevisionNumber}}</td>
                <td>{{item.latestVersion.status}}</td>
                <td><a [routerLink]="['versions', item.latestVersion.id, 'edit']">Editar</a>
                  <a [routerLink]="['versions', item.latestVersion.id]">Cobertura</a></td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `
})
export class EvidenceListPage implements OnInit, OnChanges {
  @Input() processId = '';
  private readonly api = inject(EvidenceApi);
  private readonly processApi = inject(ProcessApi);
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly items = signal<readonly EvidenceView[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.processApi.list().subscribe({
      next: processes => {
        this.processes.set(processes);
        if (!this.processId && processes.length === 1) this.processId = processes[0].id;
        this.load();
      },
      error: () => this.error.set('No fue posible consultar los procesos.')
    });
  }

  selectProcess(processId: string): void {
    this.processId = processId;
    this.items.set([]);
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['processId']?.firstChange) this.load();
  }

  private load(): void {
    if (!this.processId) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.processId).subscribe({
      next: page => {
        this.items.set(page.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No fue posible consultar las evidencias.');
        this.loading.set(false);
      }
    });
  }
}
