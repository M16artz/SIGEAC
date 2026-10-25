import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';

@Component({
  selector: 'sec-report-workflow-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section>
      <h1>Informe y hallazgos</h1>
      <ol>
        <li>Preliminar</li>
        <li>Observaciones</li>
        <li>Final</li>
        <li>Aprobado</li>
      </ol>
    </section>
  `
})
export class ReportWorkflowPage {}
