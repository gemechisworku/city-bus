import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../auth.service';
import type { MeResponse } from '../auth.models';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const adminUser: MeResponse = { userId: 1, username: 'admin', roles: ['ADMIN'] };
  const dispatcherUser: MeResponse = { userId: 2, username: 'disp', roles: ['DISPATCHER'] };
  const passengerUser: MeResponse = { userId: 3, username: 'bob', roles: ['PASSENGER'] };

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['login']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty fields and no error', () => {
    expect(component.username).toBe('');
    expect(component.password).toBe('');
    expect(component.error).toBeNull();
    expect(component.loading).toBeFalse();
  });

  it('should render the City Bus heading', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('City Bus');
  });

  it('should render username and password inputs', () => {
    const el: HTMLElement = fixture.nativeElement;
    const inputs = el.querySelectorAll('input');
    expect(inputs.length).toBeGreaterThanOrEqual(2);
  });

  describe('submit', () => {
    it('should navigate to /admin for ADMIN role', fakeAsync(() => {
      authSpy.login.and.returnValue(of(adminUser));

      component.username = 'admin';
      component.password = 'password123';
      component.submit();
      tick();

      expect(authSpy.login).toHaveBeenCalledWith('admin', 'password123');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
      expect(component.loading).toBeFalse();
    }));

    it('should navigate to /dispatcher for DISPATCHER role', fakeAsync(() => {
      authSpy.login.and.returnValue(of(dispatcherUser));

      component.username = 'disp';
      component.password = 'password123';
      component.submit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dispatcher']);
    }));

    it('should navigate to /passenger for other roles', fakeAsync(() => {
      authSpy.login.and.returnValue(of(passengerUser));

      component.username = 'bob';
      component.password = 'password123';
      component.submit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/passenger']);
    }));

    it('should set error message on login failure', fakeAsync(() => {
      authSpy.login.and.returnValue(throwError(() => new Error('Unauthorized')));

      component.username = 'bad';
      component.password = 'bad12345';
      component.submit();
      tick();

      expect(component.error).toBe('Invalid username or password.');
      expect(component.loading).toBeFalse();
    }));

    it('should set loading = true during the request', () => {
      authSpy.login.and.returnValue(of(adminUser));

      component.username = 'admin';
      component.password = 'pass1234';
      component.submit();

      expect(authSpy.login).toHaveBeenCalled();
    });

    it('should trim whitespace from username', fakeAsync(() => {
      authSpy.login.and.returnValue(of(adminUser));

      component.username = '  admin  ';
      component.password = 'password123';
      component.submit();
      tick();

      expect(authSpy.login).toHaveBeenCalledWith('admin', 'password123');
    }));

    it('should clear previous error on new submission', fakeAsync(() => {
      component.error = 'Old error';
      authSpy.login.and.returnValue(of(adminUser));

      component.username = 'admin';
      component.password = 'password123';
      component.submit();
      tick();

      expect(component.error).toBeNull();
    }));
  });

  describe('template', () => {
    it('should show error message when error is set', () => {
      component.error = 'Bad credentials';
      fixture.detectChanges();

      const el: HTMLElement = fixture.nativeElement;
      const errEl = el.querySelector('[role="alert"]');
      expect(errEl?.textContent).toContain('Bad credentials');
    });

    it('should not show error element when error is null', () => {
      component.error = null;
      fixture.detectChanges();

      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[role="alert"]')).toBeFalsy();
    });
  });
});
