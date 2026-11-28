import {ChangeDetectionStrategy, Component, inject, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {ModelApi} from '../data-access/model-api.service';
import {ModelVersion} from '../data-access/model.models';
import {NotificationService} from '../../../shared/notifications/notification.service';
import {PersonPickerComponent} from '../../../shared/labels/person-picker.component';

@Component({selector: 'sec-publication-panel', standalone: true, imports: [FormsModule, PersonPickerComponent], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <section aria-labelledby="publish-title">
    <h3 id="publish-title">Publicar versión</h3>
    <div class="sigeac-form-search">
      <div class="sigeac-form-field"><label for="approver">Persona aprobadora</label><sec-person-picker controlId="approver" [(value)]="approverId" /></div>
      <button type="button" (click)="publish()" [disabled]="busy()">Publicar instantánea</button>
    </div>
  </section>`})
export class PublicationPanel {
  readonly version = input.required<ModelVersion>();
  readonly configured = input(false);
  readonly published = output<ModelVersion>();
  readonly busy = signal(false);
  approverId = '';
  private readonly notifications = inject(NotificationService);
  constructor(private readonly api: ModelApi) {}
  publish(): void {
    if (!this.approverId) return;
    this.busy.set(true);
    const version = this.version();
    const request = this.configured() ? this.api.publishConfigured(version.id, version.revision, this.approverId)
      : this.api.publish(version.id, version.revision, this.approverId);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: value => { this.notifications.success('Versión publicada', 'La instantánea publicada permanece inmutable.'); this.published.emit(value); },
      error: () => this.notifications.error('No se pudo publicar la versión', 'Revise todas las condiciones indicadas y sus permisos.')
    });
  }
}
