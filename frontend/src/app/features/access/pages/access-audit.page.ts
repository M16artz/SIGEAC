import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { accessErrorMessage } from '../data-access/access.errors';
import { AuditEventView } from '../data-access/access.models';

@Component({
  selector: 'sec-access-audit',
  standalone: true,
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="audit-title">
      <h2 id="audit-title">Auditoría de acceso</h2>
      <p>Consulta inmutable de eventos dentro de los ámbitos autorizados de la sesión.</p>
      <form [formGroup]="filter" (ngSubmit)="search(0)" novalidate>
        <div>
          <label for="objectType">Tipo de objeto (opcional)</label>
          <input id="objectType" type="text" formControlName="objectType" placeholder="ACCESS_IDENTITY">
        </div>
        <div>
          <label for="objectId">Identificador de objeto (opcional)</label>
          <input id="objectId" type="text" formControlName="objectId">
        </div>
        <button type="submit" [disabled]="loading()">{{ loading() ? 'Consultando…' : 'Buscar' }}</button>
      </form>
      @if (error()) {
        <p role="alert">{{ error() }}</p>
      }
      @if (!loading() && events().length === 0) {
        <p>No hay eventos visibles para el filtro seleccionado.</p>
      } @else {
        <table>
          <caption>Eventos de auditoría ordenados por fecha y correlación</caption>
          <thead>
            <tr><th scope="col">Fecha</th><th scope="col">Acción</th><th scope="col">Objeto</th><th scope="col">Resultado</th><th scope="col">Motivo</th><th scope="col">Correlación</th></tr>
          </thead>
          <tbody>
            @for (event of events(); track event.id) {
              <tr>
                <td>{{ event.occurredAt }}</td>
                <td>{{ event.action }}</td>
                <td>{{ event.objectType }} · {{ event.objectId }}</td>
                <td>{{ event.outcome }}</td>
                <td>{{ event.reasonCode ?? '—' }}</td>
                <td>{{ event.correlationId }}</td>
              </tr>
            }
          </tbody>
        </table>
        <p>Página {{ page() + 1 }} · {{ total() }} eventos</p>
        <button type="button" (click)="search(page() - 1)" [disabled]="page() === 0 || loading()">Anterior</button>
        <button type="button" (click)="search(page() + 1)" [disabled]="(page() + 1) * size() >= total() || loading()">Siguiente</button>
      }
    </section>
  `
})
export class AccessAuditPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccessApi);

  readonly events = signal<readonly AuditEventView[]>([]);
  readonly page = signal(0);
  readonly size = signal(20);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly filter = this.fb.nonNullable.group({objectType: [''], objectId: ['']});

  ngOnInit(): void {
    this.search(0);
  }

  search(page: number): void {
    if (page < 0) return;
    const raw = this.filter.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.api.searchAudit({
      objectType: raw.objectType.trim() || undefined,
      objectId: raw.objectId.trim() || undefined,
      page,
      size: this.size()
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (result) => {
        this.events.set(result.items);
        this.page.set(result.page);
        this.total.set(result.total);
      },
      error: (error: unknown) => this.error.set(accessErrorMessage(error))
    });
  }
}
