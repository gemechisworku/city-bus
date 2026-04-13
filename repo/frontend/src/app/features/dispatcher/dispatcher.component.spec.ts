import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { DispatcherComponent } from './dispatcher.component';
import { AuthService } from '../../auth/auth.service';

describe('DispatcherComponent', () => {
  let component: DispatcherComponent;
  let fixture: ComponentFixture<DispatcherComponent>;
  let httpCtrl: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    localStorage.clear();
    authSpy = jasmine.createSpyObj('AuthService', ['logout', 'hasAnyRole', 'isLoggedIn', 'getToken'], { user$: { subscribe: () => {} } });
    authSpy.hasAnyRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [DispatcherComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DispatcherComponent);
    component = fixture.componentInstance;
    httpCtrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpCtrl.verify();
    localStorage.clear();
  });

  function flushInitialRequests(): void {
    httpCtrl.expectOne('/api/v1/workflows').flush([]);
    httpCtrl.expectOne('/api/v1/tasks?status=PENDING').flush([]);
  }

  it('should create', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component).toBeTruthy();
  });

  it('should default to workflows tab', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component.tab).toBe('workflows');
  });

  it('should load workflows and tasks on init', () => {
    fixture.detectChanges();

    const wfReq = httpCtrl.expectOne('/api/v1/workflows');
    expect(wfReq.request.method).toBe('GET');
    wfReq.flush([{ id: 1, definitionName: 'RouteChange', title: 'WF1', status: 'OPEN', createdAt: '2026-01-01' }]);

    const taskReq = httpCtrl.expectOne('/api/v1/tasks?status=PENDING');
    expect(taskReq.request.method).toBe('GET');
    taskReq.flush([]);

    expect(component.workflows.length).toBe(1);
    expect(component.tasks).toEqual([]);
  });

  describe('createWorkflow', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should not POST when definitionId or title is missing', () => {
      component.wfDefinitionId = null;
      component.wfTitle = '';
      component.createWorkflow();
      httpCtrl.expectNone('/api/v1/workflows');
      expect(component.wfDefinitionId).toBeNull();
    });

    it('should not POST when only definitionId is set', () => {
      component.wfDefinitionId = 1;
      component.wfTitle = '';
      component.createWorkflow();
      httpCtrl.expectNone('/api/v1/workflows');
      expect(component.wfTitle).toBe('');
    });

    it('should POST and reload workflows/tasks on success', () => {
      component.wfDefinitionId = 2;
      component.wfTitle = 'Schedule Update';
      component.createWorkflow();

      const req = httpCtrl.expectOne('/api/v1/workflows');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ definitionId: 2, title: 'Schedule Update' });
      req.flush({ id: 10 });

      httpCtrl.expectOne('/api/v1/workflows').flush([]);
      httpCtrl.expectOne('/api/v1/tasks?status=PENDING').flush([]);

      expect(component.wfDefinitionId).toBeNull();
      expect(component.wfTitle).toBe('');
    });
  });

  describe('loadTasks', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should filter by PENDING status by default', () => {
      component.taskFilter = 'PENDING';
      component.loadTasks();

      const req = httpCtrl.expectOne('/api/v1/tasks?status=PENDING');
      expect(req.request.method).toBe('GET');
      req.flush([{ id: 1, taskName: 'Review', status: 'PENDING' }]);

      expect(component.tasks.length).toBe(1);
    });

    it('should fetch all tasks when filter is ALL', () => {
      component.taskFilter = 'ALL';
      component.loadTasks();

      const req = httpCtrl.expectOne('/api/v1/tasks');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('decideTask', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should POST approve action and reload', () => {
      component.decideTask(5, 'approve');

      const req = httpCtrl.expectOne('/api/v1/tasks/5/approve');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ note: 'approve by dispatcher' });
      req.flush({});

      httpCtrl.expectOne('/api/v1/tasks?status=PENDING').flush([]);
      httpCtrl.expectOne('/api/v1/workflows').flush([]);
    });

    it('should POST reject action', () => {
      component.decideTask(7, 'reject', 'Not valid');

      const req = httpCtrl.expectOne('/api/v1/tasks/7/reject');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ note: 'Not valid' });
      req.flush({});

      httpCtrl.expectOne('/api/v1/tasks?status=PENDING').flush([]);
      httpCtrl.expectOne('/api/v1/workflows').flush([]);
    });

    it('should POST return action', () => {
      component.decideTask(9, 'return');

      const req = httpCtrl.expectOne('/api/v1/tasks/9/return');
      expect(req.request.method).toBe('POST');
      req.flush({});

      httpCtrl.expectOne('/api/v1/tasks?status=PENDING').flush([]);
      httpCtrl.expectOne('/api/v1/workflows').flush([]);
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

    it('should display Dispatcher title', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.page__title')?.textContent).toContain('Dispatcher');
    });

    it('should have 2 tab buttons', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const tabs = el.querySelectorAll('.tabs button');
      expect(tabs.length).toBe(2);
    });

    it('should show workflows section by default', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Workflow instances');
    });

    it('should show empty state when no workflows', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.empty')?.textContent).toContain('No workflow instances');
    });

    it('should render workflow rows when data is present', () => {
      component.workflows = [
        { id: 1, definitionName: 'RouteChange', title: 'Test', status: 'OPEN', createdAt: '2026-01-01' },
      ];
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const rows = el.querySelectorAll('tbody tr');
      expect(rows.length).toBe(1);
    });

    it('should show tasks tab content when switched', () => {
      component.tab = 'tasks';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Task inbox');
    });
  });
});
