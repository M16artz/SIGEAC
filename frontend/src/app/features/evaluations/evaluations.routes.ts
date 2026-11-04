import { Routes } from '@angular/router';
import {IndicatorEvaluationPage} from './pages/indicator-evaluation.page';
import {QuantitativeEvaluationPage} from './pages/quantitative-evaluation.page';
import {AcademicDisagreementPage} from './pages/academic-disagreement.page';
export const EVALUATION_ROUTES: Routes = [{path:'',component:IndicatorEvaluationPage}, {path:'quantitative',component:QuantitativeEvaluationPage}, {path:'reconciliation',component:AcademicDisagreementPage}];
