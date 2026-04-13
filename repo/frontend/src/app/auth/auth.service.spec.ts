import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isLoggedIn returns false when no token', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  it('isLoggedIn returns true when token exists', () => {
    localStorage.setItem('citybus.token', 'fake-token');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('getToken returns stored token', () => {
    localStorage.setItem('citybus.token', 'abc123');
    expect(service.getToken()).toBe('abc123');
  });

  it('login stores token and fetches user', () => {
    service.login('admin', 'ChangeMe123!').subscribe((user) => {
      expect(user.username).toBe('admin');
    });

    const loginReq = httpMock.expectOne('/api/v1/auth/login');
    expect(loginReq.request.method).toBe('POST');
    expect(loginReq.request.body).toEqual({ username: 'admin', password: 'ChangeMe123!' });
    loginReq.flush({ accessToken: 'jwt-token-here', tokenType: 'Bearer', expiresInSeconds: 3600 });

    const meReq = httpMock.expectOne('/api/v1/auth/me');
    expect(meReq.request.method).toBe('GET');
    meReq.flush({ userId: 1, username: 'admin', roles: ['ADMIN'] });

    expect(localStorage.getItem('citybus.token')).toBe('jwt-token-here');
  });

  it('hasAnyRole returns false when no user loaded', () => {
    expect(service.hasAnyRole(['ADMIN'])).toBe(false);
  });

  it('logout clears token', () => {
    localStorage.setItem('citybus.token', 'test-token');
    service.logout();
    expect(localStorage.getItem('citybus.token')).toBeNull();
    httpMock.expectOne('/api/v1/auth/logout');
  });
});
