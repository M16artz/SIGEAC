import {Routes} from '@angular/router';
import {ModelEditorPage} from './pages/model-editor.page';
import {ModelHistoryPage} from './pages/model-history.page';
import {ModelComparePage} from './pages/model-compare.page';
import {ModelAuditPage} from './pages/model-audit.page';
export const MODEL_ROUTES:Routes=[{path:'',component:ModelEditorPage},{path:'history',component:ModelHistoryPage},{path:'compare',component:ModelComparePage},{path:'audit',component:ModelAuditPage}];
