import { Routes } from '@angular/router';
import {EvidenceAuditPage} from './pages/evidence-audit.page';
import {EvidenceEditorPage} from './pages/evidence-editor.page';
import {EvidenceHistoryPage} from './pages/evidence-history.page';
import {EvidenceListPage} from './pages/evidence-list.page';
import {EvidenceVersionPage} from './pages/evidence-version.page';

export const EVIDENCE_ROUTES: Routes = [
  {path: '', component: EvidenceListPage, pathMatch: 'full'},
  {path: 'new', component: EvidenceEditorPage},
  {path: 'versions/:versionId', component: EvidenceVersionPage},
  {path: 'versions/:versionId/edit', component: EvidenceEditorPage},
  {path: 'versions/:versionId/history', component: EvidenceHistoryPage},
  {path: 'audit', component: EvidenceAuditPage}
];
