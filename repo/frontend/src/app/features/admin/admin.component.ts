import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  tab: 'import' | 'ranking' | 'rules' | 'dicts' | 'notifs' | 'users' | 'alerts' | 'diag' | 'audit' = 'import';

  // --- Import ---
  importFile: File | null = null;
  importStatus = '';

  // --- Ranking ---
  ranking: any = {};

  // --- Cleaning rules ---
  rules: any[] = [];
  ruleName = '';
  ruleFieldTarget = '';
  rulePattern = '';
  ruleReplacement = '';

  // --- Dictionaries ---
  dicts: any[] = [];
  dictFieldName = '';
  dictCanonicalValue = '';
  dictAliases = '';

  // --- Notification templates (message copy) ---
  notifTemplates: any[] = [];
  notifCode = '';
  notifSubject = '';
  notifBody = '';
  notifChannel = 'IN_APP';

  // --- Users ---
  users: any[] = [];

  // --- Alerts ---
  alerts: any[] = [];
  alertSeverity = 'WARNING';
  alertSource = 'MANUAL';
  alertTitle = '';
  alertDetail = '';

  // --- Diagnostics ---
  diagResult: any = null;
  diagType = 'FULL';

  // --- Audit ---
  auditLogs: any[] = [];

  ngOnInit(): void {
    this.loadRanking();
    this.loadRules();
    this.loadDicts();
    this.loadNotificationTemplates();
    this.loadUsers();
    this.loadAlerts();
    this.loadAudit();
  }

  // ---- Import ----
  onFileSelected(event: Event): void {
    const el = event.target as HTMLInputElement;
    this.importFile = el.files?.[0] ?? null;
  }

  triggerImport(): void {
    if (!this.importFile) return;
    const reader = new FileReader();
    reader.onload = () => {
      const json = JSON.parse(reader.result as string);
      this.importStatus = 'Importing...';
      this.http.post<any>('/api/v1/admin/imports/run', json).subscribe({
        next: (r) => (this.importStatus = `Import complete — job #${r.jobId}, status: ${r.status}`),
        error: (e) => (this.importStatus = `Error: ${e.error?.message || e.message}`),
      });
    };
    reader.readAsText(this.importFile);
  }

  // ---- Ranking ----
  loadRanking(): void {
    this.http.get<any>('/api/v1/admin/ranking-config').subscribe({ next: (r) => (this.ranking = r) });
  }

  saveRanking(): void {
    this.http.put('/api/v1/admin/ranking-config', this.ranking).subscribe({
      next: (r) => (this.ranking = r),
    });
  }

  // ---- Cleaning rules ----
  loadRules(): void {
    this.http.get<any[]>('/api/v1/admin/cleaning-rules').subscribe({ next: (r) => (this.rules = r) });
  }

  createRule(): void {
    if (!this.ruleName) return;
    this.http
      .post('/api/v1/admin/cleaning-rules', {
        name: this.ruleName,
        fieldTarget: this.ruleFieldTarget || null,
        pattern: this.rulePattern,
        replacement: this.ruleReplacement,
        enabled: true,
      })
      .subscribe({ next: () => { this.loadRules(); this.ruleName = ''; this.ruleFieldTarget = ''; this.rulePattern = ''; this.ruleReplacement = ''; } });
  }

  deleteRule(id: number): void {
    this.http.delete(`/api/v1/admin/cleaning-rules/${id}`).subscribe({ next: () => this.loadRules() });
  }

  // ---- Dictionaries ----
  loadDicts(): void {
    this.http.get<any[]>('/api/v1/admin/dictionaries').subscribe({ next: (r) => (this.dicts = r) });
  }

  createDict(): void {
    if (!this.dictFieldName || !this.dictCanonicalValue) return;
    const aliases = this.dictAliases.trim() ? this.dictAliases.split(',').map((s: string) => s.trim()).join(', ') : null;
    this.http
      .post('/api/v1/admin/dictionaries', { fieldName: this.dictFieldName, canonicalValue: this.dictCanonicalValue, aliases, enabled: true })
      .subscribe({ next: () => { this.loadDicts(); this.dictFieldName = ''; this.dictCanonicalValue = ''; this.dictAliases = ''; } });
  }

  deleteDict(id: number): void {
    this.http.delete(`/api/v1/admin/dictionaries/${id}`).subscribe({ next: () => this.loadDicts() });
  }

  // ---- Notification templates ----
  loadNotificationTemplates(): void {
    this.http.get<any[]>('/api/v1/admin/notification-templates').subscribe({ next: (r) => (this.notifTemplates = r) });
  }

  createNotificationTemplate(): void {
    if (!this.notifCode || !this.notifSubject || !this.notifBody) return;
    this.http
      .post('/api/v1/admin/notification-templates', {
        code: this.notifCode,
        subject: this.notifSubject,
        bodyTemplate: this.notifBody,
        channel: this.notifChannel || 'IN_APP',
        enabled: true,
      })
      .subscribe({
        next: () => {
          this.loadNotificationTemplates();
          this.notifCode = '';
          this.notifSubject = '';
          this.notifBody = '';
          this.notifChannel = 'IN_APP';
        },
      });
  }

  deleteNotificationTemplate(id: number): void {
    this.http.delete(`/api/v1/admin/notification-templates/${id}`).subscribe({ next: () => this.loadNotificationTemplates() });
  }

  // ---- Users ----
  loadUsers(): void {
    this.http.get<any[]>('/api/v1/admin/users').subscribe({ next: (r) => (this.users = r) });
  }

  toggleUser(userId: number, enabled: boolean): void {
    this.http.put(`/api/v1/admin/users/${userId}`, { enabled }).subscribe({ next: () => this.loadUsers() });
  }

  // ---- Alerts ----
  loadAlerts(): void {
    this.http.get<any[]>('/api/v1/admin/alerts').subscribe({ next: (r) => (this.alerts = r) });
  }

  createAlert(): void {
    if (!this.alertTitle) return;
    this.http
      .post('/api/v1/admin/alerts', { severity: this.alertSeverity, source: this.alertSource, title: this.alertTitle, detail: this.alertDetail || null })
      .subscribe({ next: () => { this.loadAlerts(); this.alertTitle = ''; this.alertDetail = ''; } });
  }

  ackAlert(id: number): void {
    this.http.post(`/api/v1/admin/alerts/${id}/acknowledge`, {}).subscribe({ next: () => this.loadAlerts() });
  }

  // ---- Diagnostics ----
  runDiag(): void {
    this.diagResult = null;
    this.http.post<any>('/api/v1/admin/diagnostics', { reportType: this.diagType }).subscribe({
      next: (r) => (this.diagResult = r),
    });
  }

  // ---- Audit ----
  loadAudit(): void {
    this.http.get<any[]>('/api/v1/admin/audit').subscribe({ next: (r) => (this.auditLogs = r) });
  }

  logout(): void {
    this.auth.logout();
  }
}
