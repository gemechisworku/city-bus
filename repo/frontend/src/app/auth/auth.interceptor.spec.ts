import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should add Authorization header when token exists', () => {
    localStorage.setItem('citybus.token', 'my-jwt');
    http.get('/api/v1/ping').subscribe();

    const req = httpMock.expectOne('/api/v1/ping');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt');
    req.flush({});
  });

  it('should not add Authorization header for login requests', () => {
    localStorage.setItem('citybus.token', 'my-jwt');
    http.post('/api/v1/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should not add Authorization when no token', () => {
    http.get('/api/v1/ping').subscribe();

    const req = httpMock.expectOne('/api/v1/ping');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
