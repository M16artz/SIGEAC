import {CommonModule} from '@angular/common';
import {Component, Input} from '@angular/core';
import {RouterLink} from '@angular/router';
import {DisplayLabelPipe} from '../../../shared/labels/display-label.pipe';

@Component({
  selector: 'sec-evidence-history-timeline',
  standalone: true,
  imports: [CommonModule, RouterLink, DisplayLabelPipe],
  template: `
    <ol class="timeline" aria-label="Línea de tiempo">
      @for (version of versions; track version.id) {
        <li>
          <a [routerLink]="['/evidences/versions', version.id]">v{{version.revisionNumber}} - {{version.status | displayLabel}}</a>
        </li>
      }
    </ol>
  `
})
export class EvidenceHistoryTimelineComponent {
  @Input() versions: Array<{id: string; revisionNumber: number; status: string}> = [];
}
