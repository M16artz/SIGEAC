import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Routes } from '@angular/router';
@Component({selector:'sec-evidence-page',standalone:true,template:'<h2>Evidencias</h2><p>Capacidad preparada para I3.</p>',changeDetection:ChangeDetectionStrategy.OnPush})
export class EvidencePageComponent {}
export const EVIDENCE_ROUTES: Routes = [{path:'',component:EvidencePageComponent}];
