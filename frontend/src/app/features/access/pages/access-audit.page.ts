import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, ViewEncapsulation, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { accessErrorMessage } from '../data-access/access.errors';
import { AuditEventView } from '../data-access/access.models';
import { DisplayLabelPipe } from '../../../shared/labels/display-label.pipe';
import { AUDIT_OBJECT_TYPES } from '../../../shared/labels/audit-labels';

@Component({
  selector: 'sec-access-audit',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DisplayLabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  templateUrl: './access-audit.page.html',
  styleUrl: './access-audit.page.scss'
})
export class AccessAuditPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccessApi);

  readonly events = signal<readonly AuditEventView[]>([]);
  readonly objectTypes = AUDIT_OBJECT_TYPES;
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
