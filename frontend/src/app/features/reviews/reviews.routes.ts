import { Routes } from '@angular/router';
import { DocumentReviewInboxPage } from './pages/document-review-inbox.page';

export const REVIEW_ROUTES: Routes = [
  {path: '', component: DocumentReviewInboxPage, pathMatch: 'full'}
];
