import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';

@Component({
  selector: 'sec-process-archive-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section>
      <h1>Archivo y cierre integral</h1>
      <button>Previsualizar bloqueadores</button>
      <button>Cerrar integralmente</button>
    </section>
  `
})
export class ProcessArchivePage {}
