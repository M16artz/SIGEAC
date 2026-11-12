import {ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {EvidenceView} from '../data-access/evidence.models';
import {DisplayLabelPipe} from '../../../shared/labels/display-label.pipe';

@Component({
  selector: 'sec-evidence-list',
  standalone: true,
  imports: [FormsModule, RouterLink, DisplayLabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="sigeac-page sigeac-page--balanced">
    <header class="sigeac-page-header">
      <span class="sigeac-badge">Evidencias</span>
      <h2>Evidencias</h2>
      <p>Consulte las versiones y coberturas documentales de su ámbito vigente.</p>
    </header>
    <section class="sigeac-page-body">
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
        <div class="sigeac-table-scroll" tabindex="0" role="region" aria-label="Evidencias del proceso">
        <table class="sigeac-table-layout sigeac-table-layout--evidence">
          <caption>Evidencias visibles en su ámbito vigente</caption>
          <thead><tr><th scope="col">Título</th><th scope="col">Versión</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td>{{item.latestVersion.title}}</td>
                <td>{{item.latestRevisionNumber}}</td>
                <td>{{item.latestVersion.status | displayLabel}}</td>
                <td><div class="sigeac-table-actions"><a [routerLink]="['versions', item.latestVersion.id, 'edit']">Editar</a>
                  <a [routerLink]="['versions', item.latestVersion.id]">Cobertura</a></div></td>
              </tr>
            }
          </tbody>
        </table>
        </div>
      }
    </section>
    </div>
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
