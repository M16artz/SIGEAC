import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'sec-evidence-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="sigeac-page">
      <header class="sigeac-page-header">
        <span class="sigeac-badge">Evidencias</span>
        <h1 id="history-title">Historial de versiones</h1>
        <p>Consulte la secuencia de versiones de la evidencia.</p>
      </header>
    <section class="evidence-history sigeac-page-body" aria-labelledby="history-title">
      <ol class="timeline" aria-label="Línea de tiempo de versiones">
        <li class="version">
          <a routerLink="../..">Versión 1 (remitida)</a>
        </li>
        <li class="version">
          <a routerLink="../..">Versión 2 (borrador sucesor)</a>
        </li>
      </ol>
      <button type="button" data-create-successor>Crear sucesor</button>
    </section>
    </div>
  `,
  styles: [':host{display:block;padding:1.5rem;}']
})
export class EvidenceHistoryPage {}
