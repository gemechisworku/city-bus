import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

describe('authGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
  });

  afterEach(() => localStorage.clear());

  it('should redirect to login when not authenticated', () => {
    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/passenger' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBeInstanceOf(UrlTree);
  });

  it('should allow access when authenticated and user loads', (done) => {
    localStorage.setItem('citybus.token', 'fake-token');
    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/passenger' } as RouterStateSnapshot;
    const httpMock = TestBed.inject(HttpTestingController);

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBeInstanceOf(Observable);

    (result as Observable<boolean | UrlTree>).subscribe((value) => {
      expect(value).toBe(true);
      done();
    });

    const meReq = httpMock.expectOne('/api/v1/auth/me');
    meReq.flush({ userId: 1, username: 'test', roles: ['PASSENGER'] });
  });
});
