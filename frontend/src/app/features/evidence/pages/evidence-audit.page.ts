import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';

@Component({
  selector: 'sec-evidence-audit-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="evidence-audit" aria-labelledby="audit-title">
      <header>
        <h1 id="audit-title">Auditoría de evidencias</h1>
      </header>
      <form class="filters" aria-label="Filtros de auditoría">
        <label>
          Acción
          <input type="text" name="action" />
        </label>
        <label>
          Correlación
          <input type="text" name="correlationId" />
        </label>
      </form>
      <ul class="events" aria-live="polite">
        <li>Sin eventos registrados.</li>
      </ul>
    </section>
  `,
  styles: [':host{display:block;padding:1.5rem;}']
})
export class EvidenceAuditPage {}
