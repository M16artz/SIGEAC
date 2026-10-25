import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';

@Component({
  selector: 'sec-academic-disagreement-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section>
      <h1>Desacuerdos académicos</h1>
      <textarea placeholder="Nota"></textarea>
      <button>Reconciliar</button>
    </section>
  `
})
export class AcademicDisagreementPage {}
