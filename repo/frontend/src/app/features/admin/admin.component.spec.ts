import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AdminComponent } from './admin.component';
import { AuthService } from '../../auth/auth.service';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;
  let httpCtrl: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    localStorage.clear();
    authSpy = jasmine.createSpyObj('AuthService', ['logout', 'hasAnyRole', 'isLoggedIn', 'getToken'], { user$: { subscribe: () => {} } });
    authSpy.hasAnyRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    httpCtrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpCtrl.verify();
    localStorage.clear();
  });

  function flushInitialRequests(): void {
    httpCtrl.expectOne('/api/v1/admin/ranking-config').flush({ routeWeight: 1, stopWeight: 1, popularityWeight: 0.5, maxSuggestions: 5, maxResults: 20 });
    httpCtrl.expectOne('/api/v1/admin/cleaning-rules').flush([]);
    httpCtrl.expectOne('/api/v1/admin/dictionaries').flush([]);
    httpCtrl.expectOne('/api/v1/admin/users').flush([]);
    httpCtrl.expectOne('/api/v1/admin/alerts').flush([]);
    httpCtrl.expectOne('/api/v1/admin/audit').flush([]);
  }

  it('should create', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component).toBeTruthy();
  });

  it('should default to import tab', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component.tab).toBe('import');
  });

  it('should load all data on init', () => {
    fixture.detectChanges();
    flushInitialRequests();
    expect(component.rules).toEqual([]);
    expect(component.dicts).toEqual([]);
    expect(component.users).toEqual([]);
    expect(component.alerts).toEqual([]);
    expect(component.auditLogs).toEqual([]);
  });

  describe('import', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('should not trigger import without a file', () => {
      component.importFile = null;
      component.triggerImport();
      httpCtrl.expectNone('/api/v1/admin/imports/run');
      expect(component.importStatus).toBe('');
    });

    it('onFileSelected should set importFile', () => {
      const file = new File(['{}'], 'test.json', { type: 'application/json' });
      const event = { target: { files: [file] } } as unknown as Event;
      component.onFileSelected(event);
      expect(component.importFile).toBe(file);
    });

    it('onFileSelected with no files should set null', () => {
      const event = { target: { files: null } } as unknown as Event;
      component.onFileSelected(event);
      expect(component.importFile).toBeNull();
    });
  });

  describe('ranking', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('loadRanking should GET ranking config', () => {
      component.loadRanking();
      const req = httpCtrl.expectOne('/api/v1/admin/ranking-config');
      expect(req.request.method).toBe('GET');
      req.flush({ routeWeight: 2, stopWeight: 1, popularityWeight: 0.3, maxSuggestions: 10, maxResults: 50 });
      expect(component.ranking.routeWeight).toBe(2);
    });

    it('saveRanking should PUT ranking config', () => {
      component.ranking = { routeWeight: 3, stopWeight: 2, popularityWeight: 0.5, maxSuggestions: 5, maxResults: 20 };
      component.saveRanking();

      const req = httpCtrl.expectOne('/api/v1/admin/ranking-config');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body.routeWeight).toBe(3);
      req.flush(component.ranking);
    });
  });

  describe('cleaning rules', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('loadRules should GET cleaning rules', () => {
      component.loadRules();
      const req = httpCtrl.expectOne('/api/v1/admin/cleaning-rules');
      expect(req.request.method).toBe('GET');
      req.flush([{ id: 1, name: 'Trim', pattern: '\\s+', replacement: ' ' }]);
      expect(component.rules.length).toBe(1);
    });

    it('createRule should not POST without name', () => {
      component.ruleName = '';
      component.createRule();
      httpCtrl.expectNone('/api/v1/admin/cleaning-rules');
      expect(component.ruleName).toBe('');
    });

    it('createRule should POST and reload', () => {
      component.ruleName = 'Trim';
      component.ruleFieldTarget = 'name';
      component.rulePattern = '\\s+';
      component.ruleReplacement = ' ';
      component.createRule();

      const req = httpCtrl.expectOne('/api/v1/admin/cleaning-rules');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        name: 'Trim',
        fieldTarget: 'name',
        pattern: '\\s+',
        replacement: ' ',
        enabled: true,
      });
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/cleaning-rules').flush([]);
      expect(component.ruleName).toBe('');
      expect(component.rulePattern).toBe('');
    });

    it('createRule should send null fieldTarget when empty', () => {
      component.ruleName = 'Remove';
      component.ruleFieldTarget = '';
      component.rulePattern = 'x';
      component.ruleReplacement = '';
      component.createRule();

      const req = httpCtrl.expectOne('/api/v1/admin/cleaning-rules');
      expect(req.request.body.fieldTarget).toBeNull();
      req.flush({});
      httpCtrl.expectOne('/api/v1/admin/cleaning-rules').flush([]);
    });

    it('deleteRule should DELETE and reload', () => {
      component.deleteRule(3);

      const req = httpCtrl.expectOne('/api/v1/admin/cleaning-rules/3');
      expect(req.request.method).toBe('DELETE');
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/cleaning-rules').flush([]);
    });
  });

  describe('dictionaries', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('loadDicts should GET dictionaries', () => {
      component.loadDicts();
      const req = httpCtrl.expectOne('/api/v1/admin/dictionaries');
      req.flush([{ id: 1, fieldName: 'direction', canonicalValue: 'North', aliases: ['N'] }]);
      expect(component.dicts.length).toBe(1);
    });

    it('createDict should not POST without fieldName or canonicalValue', () => {
      component.dictFieldName = '';
      component.dictCanonicalValue = '';
      component.createDict();
      httpCtrl.expectNone('/api/v1/admin/dictionaries');
      expect(component.dictFieldName).toBe('');
    });

    it('createDict should POST with parsed aliases', () => {
      component.dictFieldName = 'direction';
      component.dictCanonicalValue = 'North';
      component.dictAliases = 'N, NB, Northbound';
      component.createDict();

      const req = httpCtrl.expectOne('/api/v1/admin/dictionaries');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        fieldName: 'direction',
        canonicalValue: 'North',
        aliases: ['N', 'NB', 'Northbound'],
        enabled: true,
      });
      req.flush({});
      httpCtrl.expectOne('/api/v1/admin/dictionaries').flush([]);

      expect(component.dictFieldName).toBe('');
      expect(component.dictAliases).toBe('');
    });

    it('createDict should handle empty aliases string', () => {
      component.dictFieldName = 'type';
      component.dictCanonicalValue = 'Bus';
      component.dictAliases = '';
      component.createDict();

      const req = httpCtrl.expectOne('/api/v1/admin/dictionaries');
      expect(req.request.body.aliases).toEqual([]);
      req.flush({});
      httpCtrl.expectOne('/api/v1/admin/dictionaries').flush([]);
    });

    it('deleteDict should DELETE and reload', () => {
      component.deleteDict(5);

      const req = httpCtrl.expectOne('/api/v1/admin/dictionaries/5');
      expect(req.request.method).toBe('DELETE');
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/dictionaries').flush([]);
    });
  });

  describe('users', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('loadUsers should GET users list', () => {
      component.loadUsers();
      const req = httpCtrl.expectOne('/api/v1/admin/users');
      req.flush([{ id: 1, username: 'alice', enabled: true }]);
      expect(component.users.length).toBe(1);
    });

    it('toggleUser should PUT enabled status and reload', () => {
      component.toggleUser(2, false);

      const req = httpCtrl.expectOne('/api/v1/admin/users/2');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ enabled: false });
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/users').flush([]);
    });

    it('toggleUser should enable a disabled user', () => {
      component.toggleUser(3, true);

      const req = httpCtrl.expectOne('/api/v1/admin/users/3');
      expect(req.request.body).toEqual({ enabled: true });
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/users').flush([]);
    });
  });

  describe('alerts', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('loadAlerts should GET alerts', () => {
      component.loadAlerts();
      const req = httpCtrl.expectOne('/api/v1/admin/alerts');
      req.flush([{ id: 1, severity: 'WARNING', source: 'MANUAL', title: 'Test', acknowledged: false }]);
      expect(component.alerts.length).toBe(1);
    });

    it('createAlert should not POST without title', () => {
      component.alertTitle = '';
      component.createAlert();
      httpCtrl.expectNone('/api/v1/admin/alerts');
      expect(component.alertTitle).toBe('');
    });

    it('createAlert should POST and reload', () => {
      component.alertSeverity = 'CRITICAL';
      component.alertSource = 'SYSTEM';
      component.alertTitle = 'DB down';
      component.alertDetail = 'Connection refused';
      component.createAlert();

      const req = httpCtrl.expectOne('/api/v1/admin/alerts');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        severity: 'CRITICAL',
        source: 'SYSTEM',
        title: 'DB down',
        detail: 'Connection refused',
      });
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/alerts').flush([]);
      expect(component.alertTitle).toBe('');
      expect(component.alertDetail).toBe('');
    });

    it('createAlert should send null detail when empty', () => {
      component.alertTitle = 'Alert';
      component.alertDetail = '';
      component.createAlert();

      const req = httpCtrl.expectOne('/api/v1/admin/alerts');
      expect(req.request.body.detail).toBeNull();
      req.flush({});
      httpCtrl.expectOne('/api/v1/admin/alerts').flush([]);
    });

    it('ackAlert should POST acknowledge and reload', () => {
      component.ackAlert(4);

      const req = httpCtrl.expectOne('/api/v1/admin/alerts/4/acknowledge');
      expect(req.request.method).toBe('POST');
      req.flush({});

      httpCtrl.expectOne('/api/v1/admin/alerts').flush([]);
    });
  });

  describe('diagnostics', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('runDiag should POST diagnostics request', () => {
      component.diagType = 'DB_HEALTH';
      component.runDiag();

      const req = httpCtrl.expectOne('/api/v1/admin/diagnostics');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reportType: 'DB_HEALTH' });
      req.flush({ reportType: 'DB_HEALTH', status: 'PASS', summary: 'OK', detail: '', completedAt: '2026-01-01' });

      expect(component.diagResult.status).toBe('PASS');
    });

    it('runDiag should clear previous result', () => {
      component.diagResult = { reportType: 'FULL', status: 'PASS' };
      component.runDiag();

      expect(component.diagResult).toBeNull();

      httpCtrl.expectOne('/api/v1/admin/diagnostics').flush({ reportType: 'FULL', status: 'PASS', summary: 'OK' });
    });
  });

  describe('audit', () => {
    beforeEach(() => {
      fixture.detectChanges();
      flushInitialRequests();
    });

    it('loadAudit should GET audit logs', () => {
      component.loadAudit();
      const req = httpCtrl.expectOne('/api/v1/admin/audit');
      req.flush([
        { id: 1, usernameAttempt: 'alice', success: true, ipAddress: '127.0.0.1', createdAt: '2026-01-01' },
      ]);
      expect(component.auditLogs.length).toBe(1);
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

    it('should display Administrator title', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.page__title')?.textContent).toContain('Administrator');
    });

    it('should have 8 tab buttons', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const tabs = el.querySelectorAll('.tabs button');
      expect(tabs.length).toBe(8);
    });

    it('should show import section by default', () => {
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Canonical data import');
    });

    it('should show ranking section when tab is changed', () => {
      component.tab = 'ranking';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Search ranking');
    });

    it('should show cleaning rules section', () => {
      component.tab = 'rules';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Cleaning rule sets');
    });

    it('should show dictionaries section', () => {
      component.tab = 'dicts';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Field standard dictionaries');
    });

    it('should show users section', () => {
      component.tab = 'users';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('User management');
    });

    it('should show alerts section', () => {
      component.tab = 'alerts';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('System alerts');
    });

    it('should show diagnostics section', () => {
      component.tab = 'diag';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Diagnostics');
    });

    it('should show audit section', () => {
      component.tab = 'audit';
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('h2')?.textContent).toContain('Login audit log');
    });

    it('should render user rows when data present', () => {
      component.tab = 'users';
      component.users = [
        { id: 1, username: 'alice', enabled: true },
        { id: 2, username: 'bob', enabled: false },
      ];
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const rows = el.querySelectorAll('tbody tr');
      expect(rows.length).toBe(2);
    });

    it('should render alert rows with acknowledge button for unacknowledged', () => {
      component.tab = 'alerts';
      component.alerts = [{ id: 1, severity: 'WARNING', source: 'MANUAL', title: 'Test', acknowledged: false }];
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      const btn = el.querySelector('tbody .btn-sm');
      expect(btn?.textContent).toContain('Acknowledge');
    });

    it('should render diagnostics result when available', () => {
      component.tab = 'diag';
      component.diagResult = { reportType: 'FULL', status: 'PASS', summary: 'All good', detail: 'Details here', completedAt: '2026-01-01' };
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.diag-result h3')?.textContent).toContain('PASS');
    });
  });
});
