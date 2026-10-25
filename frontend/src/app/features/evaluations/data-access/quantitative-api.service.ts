import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';
import {EvaluationSupport} from './academic-evaluation-api.service';

export interface QuantitativeVariable {code: string; value: string; unit: string; supports: readonly EvaluationSupport[]}
export interface QuantitativeInputs {id: string; processId: string; indicatorId: string; modelVersionId: string; ruleId: string; capturedBy: string;
  capturedAt: string; revision: number; predecessorId: string | null; periodStart: string; periodEnd: string; variables: readonly QuantitativeVariable[]}
export interface VariableVerification {inputSetId: string; verifiedBy: string; verifiedAt: string; reason: string}
export interface QuantitativeRule {id: string; methodDefinition: string; formulaDefinition: string; strategyKey: string; strategyVersion: string;
  variables: readonly {code: string; name: string; unit: string; dataType: string; validationDefinition: string}[]}
export interface QuantitativeRun {id: string; inputs: QuantitativeInputs; verification: VariableVerification; rule: QuantitativeRule;
  result: string; ratingLabel: string; executedAt: string; executedBy: string; arithmetic: string; justification: string; limitations: string;
  threshold: {lowerBound: number | null; upperBound: number | null; lowerInclusive: boolean; upperInclusive: boolean}}
export interface QuantitativeView {processId: string; indicatorId: string; processState: string; periodStart: string; periodEnd: string;
  rule: QuantitativeRule; acceptedSupports: readonly EvaluationSupport[]; ownInputs: QuantitativeInputs | null;
  verification: VariableVerification | null; items: readonly QuantitativeRun[]; page: number; size: number; hasNext: boolean}
export interface CaptureVariables {periodStart: string; periodEnd: string; predecessorId: string | null;
  variables: Record<string, {value: string; unit: string; evidenceVersionIds: readonly string[]}>}
@Injectable({providedIn: 'root'})
export class QuantitativeApi {
  private readonly api = inject(ApiClient);
  view(p: string, i: string, page = 0): Observable<QuantitativeView> {return this.api.get(`${this.path(p, i)}/quantitative-evaluations`, {page, size: 20});}
  capture(v: QuantitativeView, command: CaptureVariables): Observable<QuantitativeInputs> {
    return this.api.post(`${this.path(v.processId, v.indicatorId)}/quantitative-inputs`, command, this.match(v.ownInputs?.revision ?? 0));
  }
  verify(v: QuantitativeView, reason: string): Observable<VariableVerification> {
    const inputs = v.ownInputs;
    if (!inputs) throw new Error('No hay variables capturadas.');
    return this.api.post(`${this.path(v.processId, v.indicatorId)}/quantitative-inputs/${inputs.id}/verification`, {reason}, this.match(inputs.revision));
  }
  calculate(v: QuantitativeView, justification: string, limitations: string): Observable<QuantitativeRun> {
    const inputs = v.ownInputs;
    if (!inputs) throw new Error('No hay variables capturadas.');
    return this.api.post(`${this.path(v.processId, v.indicatorId)}/quantitative-calculations`, {inputSetId: inputs.id, justification, limitations}, this.match(inputs.revision));
  }
  private path(p: string, i: string): string {return `processes/${p}/indicators/${i}`;}
  private match(revision: number): Record<string, string> {return {'If-Match': `"${revision}"`};}
}
