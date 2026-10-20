import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AccessSessionService } from '../data-access/access-session.service';

@Component({
  selector: 'sec-access-home',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="access-title">
      <h2 id="access-title">Acceso y ámbitos autorizados</h2>
      @if (session.identity(); as identity) {
        <p><strong>{{ identity.displayName }}</strong>{{ identity.technical ? ' · cuenta técnica' : '' }}</p>
        <h3>Asignaciones vigentes</h3>
        @if (identity.assignments.length === 0) {
          <p>No tiene asignaciones funcionales vigentes.</p>
        } @else {
          <table>
            <caption>Funciones y ámbitos vigentes de la sesión actual</caption>
            <thead>
              <tr><th scope="col">Función</th><th scope="col">Carrera</th><th scope="col">Proceso</th><th scope="col">Indicador</th><th scope="col">Desde</th><th scope="col">Hasta</th></tr>
            </thead>
            <tbody>
              @for (assignment of identity.assignments; track assignment.id) {
                <tr>
                  <td>{{ assignment.roleName }} ({{ assignment.roleCode }})</td>
                  <td>{{ assignment.careerId }}</td>
                  <td>{{ assignment.processId ?? 'Todos' }}</td>
                  <td>{{ assignment.indicatorId ?? 'Todos' }}</td>
                  <td>{{ assignment.validFrom }}</td>
                  <td>{{ assignment.validTo }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
        <nav aria-label="Administración de acceso">
          <ul>
            <li><a routerLink="/access/users">Usuarios y asignaciones</a></li>
            <li><a routerLink="/access/conflicts">Conflictos y excepciones</a></li>
            <li><a routerLink="/access/audit">Auditoría de acceso</a></li>
          </ul>
        </nav>
        <button type="button" (click)="logout()">Cerrar sesión</button>
      } @else {
        <p>Cargando identidad…</p>
      }
    </section>
  `
})
export class AccessHomePageComponent {
  readonly session = inject(AccessSessionService);
  private readonly router = inject(Router);

  logout(): void {
    this.session.logout().subscribe({
      next: () => void this.router.navigate(['/access/login']),
      error: () => {
        this.session.clear();
        void this.router.navigate(['/access/login']);
      }
    });
  }
}
