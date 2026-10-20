import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AccessSessionService } from './data-access/access-session.service';

export const accessGuard: CanActivateFn = (_route, state) => {
  const session = inject(AccessSessionService);
  const router = inject(Router);
  return session.ensure().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/access/login'], {queryParams: {returnUrl: state.url}})))
  );
};
