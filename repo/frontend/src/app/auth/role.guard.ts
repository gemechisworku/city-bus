import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { AuthService } from './auth.service';

export function roleGuard(allowedRoles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isLoggedIn()) {
      return router.createUrlTree(['/login']);
    }
    return auth.ensureUser().pipe(
      map((u) => {
        const ok = allowedRoles.some((r) => u.roles.includes(r));
        return ok ? true : router.createUrlTree(['/login']);
      }),
      catchError(() => of(router.createUrlTree(['/login'])))
    );
  };
}
