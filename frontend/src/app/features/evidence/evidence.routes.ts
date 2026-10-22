import { Routes } from '@angular/router';
import {EvidenceEditorPage} from './pages/evidence-editor.page';
import {EvidenceListPage} from './pages/evidence-list.page';

export const EVIDENCE_ROUTES: Routes = [
  {path: '', component: EvidenceListPage, pathMatch: 'full'},
  {path: 'new', component: EvidenceEditorPage},
  {path: 'versions/:versionId/edit', component: EvidenceEditorPage}
];
