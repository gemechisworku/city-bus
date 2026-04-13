import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { of, throwError } from 'rxjs';
import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';
import type { MeResponse } from './auth.models';

describe('roleGuard', () => {
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const adminUser: MeResponse = { userId: 1, username: 'admin', roles: ['ADMIN'] };
  const passengerUser: MeResponse = { userId: 2, username: 'bob', roles: ['PASSENGER'] };

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'ensureUser']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: { createUrlTree: jasmine.createSpy('createUrlTree').and.callFake((cmds: string[]) => ({ toString: () => cmds.join('/'), __loginRedirect: true }) as unknown as UrlTree) } },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('should redirect to /login when not logged in', () => {
    authSpy.isLoggedIn.and.returnValue(false);
    const guard = roleGuard(['ADMIN']);

    TestBed.runInInjectionContext(() => {
      const result = guard({} as any, {} as any);
      expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
    });
  });

  it('should return true when user has a matching role', (done) => {
    authSpy.isLoggedIn.and.returnValue(true);
    authSpy.ensureUser.and.returnValue(of(adminUser));
    const guard = roleGuard(['ADMIN', 'DISPATCHER']);

    TestBed.runInInjectionContext(() => {
      const result = guard({} as any, {} as any);
      (result as any).subscribe((val: boolean | UrlTree) => {
        expect(val).toBeTrue();
        done();
      });
    });
  });

  it('should redirect when user lacks all allowed roles', (done) => {
    authSpy.isLoggedIn.and.returnValue(true);
    authSpy.ensureUser.and.returnValue(of(passengerUser));
    const guard = roleGuard(['ADMIN']);

    TestBed.runInInjectionContext(() => {
      const result = guard({} as any, {} as any);
      (result as any).subscribe((val: boolean | UrlTree) => {
        expect(val).not.toBeTrue();
        done();
      });
    });
  });

  it('should redirect to /login when ensureUser fails', (done) => {
    authSpy.isLoggedIn.and.returnValue(true);
    authSpy.ensureUser.and.returnValue(throwError(() => new Error('session expired')));
    const guard = roleGuard(['ADMIN']);

    TestBed.runInInjectionContext(() => {
      const result = guard({} as any, {} as any);
      (result as any).subscribe((val: boolean | UrlTree) => {
        expect(val).not.toBeTrue();
        done();
      });
    });
  });

  it('should allow access when user has one of multiple allowed roles', (done) => {
    authSpy.isLoggedIn.and.returnValue(true);
    authSpy.ensureUser.and.returnValue(of(passengerUser));
    const guard = roleGuard(['PASSENGER', 'DISPATCHER']);

    TestBed.runInInjectionContext(() => {
      const result = guard({} as any, {} as any);
      (result as any).subscribe((val: boolean | UrlTree) => {
        expect(val).toBeTrue();
        done();
      });
    });
  });
});
