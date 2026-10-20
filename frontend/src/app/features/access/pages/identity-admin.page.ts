import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { accessErrorMessage } from '../data-access/access.errors';
import { AssignmentView, UserView } from '../data-access/access.models';
import { AssignmentFormComponent } from '../ui/assignment-form.component';

@Component({
  selector: 'sec-identity-admin',
  standalone: true,
  imports: [ReactiveFormsModule, AssignmentFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="users-title">
      <h2 id="users-title">Usuarios y asignaciones</h2>
      <p>Habilite identidades sintéticas y asigne funciones con ámbito y vigencia explícitos.</p>

      <form [formGroup]="createForm" (ngSubmit)="createUser()" novalidate>
        <h3>Registrar usuario</h3>
        <div>
          <label for="newUsername">Usuario</label>
          <input id="newUsername" type="text" formControlName="username" autocomplete="off" required>
        </div>
        <div>
          <label for="newDisplayName">Nombre visible</label>
          <input id="newDisplayName" type="text" formControlName="displayName" required>
        </div>
        <div>
          <label for="newPassword">Contraseña inicial (mínimo 12 caracteres)</label>
          <input id="newPassword" type="password" formControlName="initialPassword" autocomplete="new-password" required>
        </div>
        <div>
          <label><input type="checkbox" formControlName="technical"> Cuenta técnica</label>
        </div>
        @if (createError()) {
          <p role="alert">{{ createError() }}</p>
        }
        @if (createMessage()) {
          <p role="status">{{ createMessage() }}</p>
        }
        <button type="submit" [disabled]="creating()">{{ creating() ? 'Registrando…' : 'Registrar usuario' }}</button>
      </form>

      <h3>Usuarios registrados</h3>
      @if (listError()) {
        <p role="alert">{{ listError() }}</p>
      }
      @if (loading()) {
        <p>Cargando usuarios…</p>
      } @else {
        <table>
          <caption>Usuarios visibles para la administración funcional</caption>
          <thead>
            <tr><th scope="col">Usuario</th><th scope="col">Nombre</th><th scope="col">Tipo</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr>
          </thead>
          <tbody>
            @for (user of users(); track user.id) {
              <tr [attr.data-user-id]="user.id">
                <td>{{ user.username }}</td>
                <td>{{ user.displayName }}</td>
                <td>{{ user.technical ? 'Técnica' : 'Humana' }}</td>
                <td>{{ user.active ? 'Activa' : 'Desactivada' }}</td>
                <td>
                  <button type="button" (click)="select(user)" [attr.aria-pressed]="selected()?.id === user.id">
                    Asignaciones
                  </button>
                  <button type="button" (click)="toggle(user)">{{ user.active ? 'Desactivar' : 'Activar' }}</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
        <p>Página {{ page() + 1 }} · {{ total() }} usuarios</p>
        <button type="button" (click)="load(page() - 1)" [disabled]="page() === 0">Anterior</button>
        <button type="button" (click)="load(page() + 1)" [disabled]="(page() + 1) * size() >= total()">Siguiente</button>
      }

      @if (selected(); as user) {
        <section aria-labelledby="assignments-title">
          <h3 id="assignments-title">Asignaciones de {{ user.displayName }}</h3>
          @if (assignmentError()) {
            <p role="alert">{{ assignmentError() }}</p>
          }
          @if (assignments().length === 0) {
            <p>Sin asignaciones registradas.</p>
          } @else {
            <table>
              <caption>Asignaciones del usuario seleccionado</caption>
              <thead>
                <tr><th scope="col">Función</th><th scope="col">Carrera</th><th scope="col">Proceso</th><th scope="col">Indicador</th><th scope="col">Vigencia</th></tr>
              </thead>
              <tbody>
                @for (assignment of assignments(); track assignment.id) {
                  <tr>
                    <td>{{ assignment.roleName }} ({{ assignment.roleCode }})</td>
                    <td>{{ assignment.careerId }}</td>
                    <td>{{ assignment.processId ?? 'Todos' }}</td>
                    <td>{{ assignment.indicatorId ?? 'Todos' }}</td>
                    <td>{{ assignment.validFrom }} — {{ assignment.validTo }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }
          @if (user.active && !user.technical) {
            <sec-assignment-form [userId]="user.id" (saved)="reloadAssignments()" />
          } @else {
            <p>Las cuentas técnicas o desactivadas no admiten nuevas asignaciones funcionales.</p>
          }
        </section>
      }
    </section>
  `
})
export class IdentityAdminPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccessApi);

  readonly users = signal<readonly UserView[]>([]);
  readonly assignments = signal<readonly AssignmentView[]>([]);
  readonly selected = signal<UserView | null>(null);
  readonly page = signal(0);
  readonly size = signal(20);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly listError = signal<string | null>(null);
  readonly createError = signal<string | null>(null);
  readonly createMessage = signal<string | null>(null);
  readonly assignmentError = signal<string | null>(null);

  readonly createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    displayName: ['', [Validators.required, Validators.maxLength(200)]],
    initialPassword: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(256)]],
    technical: [false]
  });

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    if (page < 0) return;
    this.loading.set(true);
    this.listError.set(null);
    this.api.listUsers(page, this.size()).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (result) => {
        this.users.set(result.items);
        this.page.set(result.page);
        this.total.set(result.total);
        const current = this.selected();
        if (current) {
          const refreshed = result.items.find((user) => user.id === current.id);
          if (refreshed) this.selected.set(refreshed);
        }
      },
      error: (error: unknown) => this.listError.set(accessErrorMessage(error))
    });
  }

  createUser(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.createError.set(null);
    this.createMessage.set(null);
    this.api.createUser(this.createForm.getRawValue()).pipe(finalize(() => this.creating.set(false))).subscribe({
      next: () => {
        this.createMessage.set('Usuario registrado correctamente.');
        this.createForm.reset({username: '', displayName: '', initialPassword: '', technical: false});
        this.load(0);
      },
      error: (error: unknown) => this.createError.set(accessErrorMessage(error))
    });
  }

  toggle(user: UserView): void {
    this.listError.set(null);
    this.api.updateUserState(user.id, !user.active, user.revision).subscribe({
      next: () => this.load(this.page()),
      error: (error: unknown) => this.listError.set(accessErrorMessage(error))
    });
  }

  select(user: UserView): void {
    if (this.selected()?.id === user.id) {
      this.selected.set(null);
      this.assignments.set([]);
      return;
    }
    this.selected.set(user);
    this.reloadAssignments();
  }

  reloadAssignments(): void {
    const user = this.selected();
    if (!user) return;
    this.assignmentError.set(null);
    this.api.listAssignments(user.id).subscribe({
      next: (assignments) => this.assignments.set(assignments),
      error: (error: unknown) => this.assignmentError.set(accessErrorMessage(error))
    });
  }
}
