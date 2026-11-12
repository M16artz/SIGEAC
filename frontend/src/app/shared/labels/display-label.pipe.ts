import {Pipe, PipeTransform} from '@angular/core';
import {displayLabel, LabelContext} from './display-labels';

@Pipe({name: 'displayLabel', standalone: true})
export class DisplayLabelPipe implements PipeTransform {
  transform(value: string | number | boolean | null | undefined, context: LabelContext = 'general'): string {
    return displayLabel(value, context);
  }
}
