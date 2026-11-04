import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  ViewEncapsulation,
  inject,
  signal
} from '@angular/core';
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
  encapsulation: ViewEncapsulation.None,
  templateUrl: './identity-admin.page.html',
  styleUrl: './identity-admin.page.scss'
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
  readonly assignmentMessage = signal<string | null>(null);
  readonly revoking = signal(false);
  readonly revocationReason = this.fb.nonNullable.control('', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]);

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
    this.assignmentMessage.set(null);
    this.revocationReason.reset();
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
    this.assignments.set([]);
    this.api.listAssignments(user.id).subscribe({
      next: (assignments) => this.assignments.set(assignments),
      error: (error: unknown) => this.assignmentError.set(accessErrorMessage(error))
    });
  }

  revoke(assignment: AssignmentView): void {
    const user = this.selected();
    if (!user || this.revocationReason.invalid || this.revoking()) return;
    this.assignmentError.set(null); this.assignmentMessage.set(null); this.revoking.set(true);
    this.api.revokeAssignment(user.id, assignment.id, assignment.revision ?? 0, this.revocationReason.value)
      .pipe(finalize(() => this.revoking.set(false))).subscribe({
        next: () => {
          if (this.selected()?.id !== user.id) return;
          this.revocationReason.reset();
          this.assignmentMessage.set('Asignación revocada. El historial se conserva y el permiso deja de estar vigente.');
          this.reloadAssignments();
        },
        error: (error: unknown) => {
          if (this.selected()?.id === user.id) this.assignmentError.set(accessErrorMessage(error));
        }
      });
  }

  formatDate(value: string): string {
    const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
    return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
  }
}
