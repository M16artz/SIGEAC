import {CommonModule} from '@angular/common';
import {Component, inject} from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'sec-evidence-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="evidence-history" aria-labelledby="history-title">
      <header>
        <h1 id="history-title">Historial de versiones</h1>
      </header>
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
  `,
  styles: [':host{display:block;padding:1.5rem;}']
})
export class EvidenceHistoryPage {}
