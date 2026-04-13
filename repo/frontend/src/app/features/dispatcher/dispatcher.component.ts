import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-dispatcher',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dispatcher.component.html',
  styleUrl: './dispatcher.component.scss',
})
export class DispatcherComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  tab: 'workflows' | 'tasks' = 'workflows';

  workflows: any[] = [];
  tasks: any[] = [];

  wfDefinitionId: number | null = null;
  wfTitle = '';

  taskFilter: 'PENDING' | 'ALL' = 'PENDING';

  ngOnInit(): void {
    this.loadWorkflows();
    this.loadTasks();
  }

  loadWorkflows(): void {
    this.http.get<any[]>('/api/v1/workflows').subscribe({
      next: (r) => (this.workflows = r),
    });
  }

  createWorkflow(): void {
    if (!this.wfDefinitionId || !this.wfTitle) return;
    this.http
      .post('/api/v1/workflows', {
        definitionId: this.wfDefinitionId,
        title: this.wfTitle,
      })
      .subscribe({
        next: () => {
          this.loadWorkflows();
          this.loadTasks();
          this.wfDefinitionId = null;
          this.wfTitle = '';
        },
      });
  }

  loadTasks(): void {
    const params = this.taskFilter === 'PENDING' ? '?status=PENDING' : '';
    this.http.get<any[]>(`/api/v1/tasks${params}`).subscribe({
      next: (r) => (this.tasks = r),
    });
  }

  decideTask(taskId: number, action: 'approve' | 'reject' | 'return', comment: string = ''): void {
    this.http
      .post(`/api/v1/tasks/${taskId}/${action}`, { note: comment || `${action} by dispatcher` })
      .subscribe({
        next: () => {
          this.loadTasks();
          this.loadWorkflows();
        },
      });
  }

  logout(): void {
    this.auth.logout();
  }
}
