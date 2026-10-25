import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';

@Component({
  selector: 'sec-indicator-evaluation-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section>
      <h1>Evaluación de indicadores</h1>
      <form>
        <label>Método <input type="text" /></label>
        <label>Limitaciones <textarea></textarea></label>
        <label>Justificación <textarea></textarea></label>
        <button type="button">Guardar</button>
      </form>
    </section>
  `
})
export class IndicatorEvaluationPage {}
