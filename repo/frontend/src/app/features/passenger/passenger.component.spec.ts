import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { PassengerComponent } from './passenger.component';
import { AuthService } from '../../auth/auth.service';

describe('PassengerComponent', () => {
  let component: PassengerComponent;
  let fixture: ComponentFixture<PassengerComponent>;
  let httpCtrl: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    localStorage.clear();
    authSpy = jasmine.createSpyObj('AuthService', ['logout', 'hasAnyRole', 'isLoggedIn', 'getToken'], { user$: { subscribe: () => {} } });
    authSpy.hasAnyRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [PassengerComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PassengerComponent);
    component = fixture.componentInstance;
    httpCtrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpCtrl.verify();
    localStorage.clear();
  });

  function flushInitialRequests(): void {
    httpCtrl.expectOne('/api/v1/passenger/reservations').flush([]);
    httpCtrl.expectOne('/api/v1/passenger/checkins').flush([]);
    httpCtrl.expectOne('/api/v1/messages').flush([]);
    httpCtrl.expectOne('/api/v1/passenger/reminder-preferences').flush({ enabled: true, minutesBefore: 10, channel: 'IN_APP' });
    httpCtrl.expectOne('/api/v1/passenger/dnd-windows').flush([]);
  }

  it('should create', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component).toBeTruthy();
  });

  it('should default to search tab', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component.tab).toBe('search');
  });

  it('should load reservations, checkins, messages, and prefs on init', () => {
    fixture.detectChanges();
    flushInitialRequests();

    expect(component.reservations).toEqual([]);
    expect(component.checkins).toEqual([]);
    expect(component.messages).toEqual([]);
  });

  describe('search', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should not search when query is less than 2 characters', () => {
      component.searchQuery = 'a';
      component.search();
      httpCtrl.expectNone(req => req.url.includes('/api/v1/search/results'));
      expect(component.searchResults).toEqual([]);
    });

    it('should call API with encoded query', () => {
      component.searchQuery = 'main st';
      component.search();

      const req = httpCtrl.expectOne(r => r.url.includes('/api/v1/search/results'));
      expect(req.request.url).toContain('q=main%20st');
      expect(req.request.url).toContain('limit=10');
      req.flush([{ id: 1, kind: 'STOP', code: 'S1', name: 'Main St', score: 5.0 }]);

      expect(component.searchResults.length).toBe(1);
    });

    it('should clear selectedRoute on new search', () => {
      component.selectedRoute = { id: 99 };
      component.searchQuery = 'test';
      component.search();

      httpCtrl.expectOne(r => r.url.includes('/api/v1/search/results')).flush([]);
      expect(component.selectedRoute).toBeNull();
    });

    it('should set searchError on failure', () => {
      component.searchQuery = 'broken';
      component.search();

      httpCtrl.expectOne(r => r.url.includes('/api/v1/search/results'))
        .flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });

      expect(component.searchError).toBeTruthy();
    });
  });

  describe('loadRouteSchedules', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should fetch route details and set selectedRoute', () => {
      const mockRoute = { id: 5, name: 'Route 5', code: 'R5', stops: [], schedules: [] };
      component.loadRouteSchedules(5);

      httpCtrl.expectOne('/api/v1/routes/5').flush(mockRoute);
      expect(component.selectedRoute).toEqual(mockRoute);
      expect(component.selectedRouteStopId).toBeNull();
    });

    it('should set searchError on failure', () => {
      component.loadRouteSchedules(999);
      httpCtrl.expectOne('/api/v1/routes/999').flush('', { status: 404, statusText: 'Not Found' });
      expect(component.searchError).toBe('Failed to load route details');
    });
  });

  describe('reserveFromSchedule', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should not send request without selectedRouteStopId', () => {
      component.selectedRouteStopId = null;
      component.reserveFromSchedule(10);
      httpCtrl.expectNone('/api/v1/passenger/reservations');
      expect(component.selectedRouteStopId).toBeNull();
    });

    it('should post reservation and switch to reservations tab', fakeAsync(() => {
      component.selectedRouteStopId = 3;
      component.reserveFromSchedule(10);

      const req = httpCtrl.expectOne('/api/v1/passenger/reservations');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ scheduleId: 10, stopId: 3 });
      req.flush({});

      httpCtrl.expectOne('/api/v1/passenger/reservations').flush([]);
      httpCtrl.expectOne('/api/v1/messages').flush([]);

      expect(component.tab).toBe('reservations');
      expect(component.selectedRoute).toBeNull();
      expect(component.selectedRouteStopId).toBeNull();
    }));
  });

  describe('useStopForCheckin', () => {
    it('should set checkinStopId and switch to checkins tab', () => {
      fixture.detectChanges();
      flushInitialRequests();

      component.useStopForCheckin(42);
      expect(component.checkinStopId).toBe(42);
      expect(component.tab).toBe('checkins');
    });
  });

  describe('reservations', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('createReservation should not POST without scheduleId/stopId', () => {
      component.resScheduleId = null;
      component.resStopId = null;
      component.createReservation();
      httpCtrl.expectNone('/api/v1/passenger/reservations');
      expect(component.resScheduleId).toBeNull();
    });

    it('createReservation should POST and reload', () => {
      component.resScheduleId = 1;
      component.resStopId = 2;
      component.createReservation();

      const req = httpCtrl.expectOne('/api/v1/passenger/reservations');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ scheduleId: 1, stopId: 2 });
      req.flush({});

      httpCtrl.expectOne('/api/v1/passenger/reservations').flush([]);
      httpCtrl.expectOne('/api/v1/messages').flush([]);

      expect(component.resScheduleId).toBeNull();
      expect(component.resStopId).toBeNull();
    });

    it('cancelReservation should PUT with CANCELLED status', () => {
      component.cancelReservation(5);

      const req = httpCtrl.expectOne('/api/v1/passenger/reservations/5');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ status: 'CANCELLED' });
      req.flush({});

      httpCtrl.expectOne('/api/v1/passenger/reservations').flush([]);
      httpCtrl.expectOne('/api/v1/messages').flush([]);
    });

    it('confirmReservation should PUT with CONFIRMED status', () => {
      component.confirmReservation(7);

      const req = httpCtrl.expectOne('/api/v1/passenger/reservations/7');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ status: 'CONFIRMED' });
      req.flush({});

      httpCtrl.expectOne('/api/v1/passenger/reservations').flush([]);
    });
  });

  describe('checkins', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('createCheckin should not POST without stopId', () => {
      component.checkinStopId = null;
      component.createCheckin();
      httpCtrl.expectNone('/api/v1/passenger/checkins');
      expect(component.checkinStopId).toBeNull();
    });

    it('createCheckin should POST and reload', () => {
      component.checkinStopId = 10;
      component.createCheckin();

      const req = httpCtrl.expectOne('/api/v1/passenger/checkins');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ stopId: 10 });
      req.flush({});

      httpCtrl.expectOne('/api/v1/passenger/checkins').flush([]);
      expect(component.checkinStopId).toBeNull();
    });
  });

  describe('messages', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('markRead should POST to message read endpoint', () => {
      component.markRead(3);

      const req = httpCtrl.expectOne('/api/v1/messages/3/read');
      expect(req.request.method).toBe('POST');
      req.flush({});

      httpCtrl.expectOne('/api/v1/messages').flush([]);
    });

    it('sendMessage should not POST without subject or body', () => {
      component.msgSubject = '';
      component.msgBody = 'hello';
      component.sendMessage();
      httpCtrl.expectNone('/api/v1/messages');
      expect(component.msgSubject).toBe('');
    });

    it('sendMessage should POST and reload', () => {
      component.msgSubject = 'Test';
      component.msgBody = 'Body text';
      component.sendMessage();

      const req = httpCtrl.expectOne('/api/v1/messages');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ subject: 'Test', body: 'Body text' });
      req.flush({});

      httpCtrl.expectOne('/api/v1/messages').flush([]);
      expect(component.msgSubject).toBe('');
      expect(component.msgBody).toBe('');
    });
  });

  describe('prefs', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('savePrefs should PUT preferences', () => {
      component.prefs = { enabled: false, minutesBefore: 30, channel: 'EMAIL' };
      component.savePrefs();

      const req = httpCtrl.expectOne('/api/v1/passenger/reminder-preferences');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ enabled: false, minutesBefore: 30, channel: 'EMAIL' });
      req.flush({ enabled: false, minutesBefore: 30, channel: 'EMAIL' });

      expect(component.prefs.channel).toBe('EMAIL');
    });
  });

  describe('dnd-windows', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('createDndWindow should POST and reload', () => {
      component.dndDay = 0;
      component.dndStart = '22:00';
      component.dndEnd = '07:00';
      component.createDndWindow();

      const req = httpCtrl.expectOne('/api/v1/passenger/dnd-windows');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ dayOfWeek: 0, startTime: '22:00', endTime: '07:00' });
      req.flush({});

      httpCtrl.expectOne('/api/v1/passenger/dnd-windows').flush([]);
    });

    it('deleteDndWindow should DELETE and reload', () => {
      component.deleteDndWindow(5);

      const req = httpCtrl.expectOne('/api/v1/passenger/dnd-windows/5');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      httpCtrl.expectOne('/api/v1/passenger/dnd-windows').flush([]);
    });
  });

  describe('logout', () => {
    it('should call auth.logout', () => {
      fixture.detectChanges();
      flushInitialRequests();

      component.logout();
      expect(authSpy.logout).toHaveBeenCalled();
    });
  });

  describe('template rendering', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should display Passenger title', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.page__title')?.textContent).toContain('Passenger');
    });

    it('should have 6 tab buttons', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const tabs = el.querySelectorAll('.tabs button');
      expect(tabs.length).toBe(6);
    });

    it('should show search section by default', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Search stops');
    });
  });
});
