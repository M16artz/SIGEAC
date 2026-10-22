import { Routes } from '@angular/router';
import {EvidenceEditorPage} from './pages/evidence-editor.page';
import {EvidenceListPage} from './pages/evidence-list.page';
import {EvidenceVersionPage} from './pages/evidence-version.page';

export const EVIDENCE_ROUTES: Routes = [
  {path: '', component: EvidenceListPage, pathMatch: 'full'},
  {path: 'new', component: EvidenceEditorPage},
  {path: 'versions/:versionId', component: EvidenceVersionPage},
  {path: 'versions/:versionId/edit', component: EvidenceEditorPage}
];
