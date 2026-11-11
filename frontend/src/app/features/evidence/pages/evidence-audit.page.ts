import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';

@Component({
  selector: 'sec-evidence-audit-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="sigeac-page">
      <header class="sigeac-page-header">
        <span class="sigeac-badge">Evidencias</span>
        <h1 id="audit-title">Auditoría de evidencias</h1>
        <p>Consulte la trazabilidad de las evidencias documentales.</p>
      </header>
    <section class="evidence-audit sigeac-page-body" aria-labelledby="audit-title">
      <form class="filters sigeac-form-layout" aria-label="Filtros de auditoría"><div class="sigeac-form-grid">
        <div class="sigeac-form-field"><label>
          Acción
          <input type="text" name="action" />
        </label></div>
        <div class="sigeac-form-field"><label>
          Correlación
          <input type="text" name="correlationId" />
        </label></div>
        </div>
      </form>
      <ul class="events" aria-live="polite">
        <li>Sin eventos registrados.</li>
      </ul>
    </section>
    </div>
  `,
  styles: [':host{display:block;padding:1.5rem;}']
})
export class EvidenceAuditPage {}
