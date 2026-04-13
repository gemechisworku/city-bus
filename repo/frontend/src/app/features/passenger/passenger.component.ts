import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-passenger',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './passenger.component.html',
  styleUrl: './passenger.component.scss',
})
export class PassengerComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  tab: 'search' | 'reservations' | 'checkins' | 'messages' | 'prefs' | 'dnd' = 'search';

  searchQuery = '';
  searchResults: any[] = [];
  searchError = '';
  selectedRoute: any = null;
  selectedRouteStopId: number | null = null;

  reservations: any[] = [];
  resScheduleId: number | null = null;
  resStopId: number | null = null;

  checkins: any[] = [];
  checkinStopId: number | null = null;

  messages: any[] = [];
  msgSubject = '';
  msgBody = '';

  prefs: any = { enabled: true, minutesBefore: 10, channel: 'IN_APP' };

  dndWindows: any[] = [];
  dndDay: number = 0;
  dndStart = '22:00';
  dndEnd = '07:00';

  ngOnInit(): void {
    this.loadReservations();
    this.loadCheckins();
    this.loadMessages();
    this.loadPrefs();
    this.loadDndWindows();
  }

  search(): void {
    if (this.searchQuery.length < 2) return;
    this.searchError = '';
    this.selectedRoute = null;
    this.selectedRouteStopId = null;
    this.http.get<any[]>(`/api/v1/search/results?q=${encodeURIComponent(this.searchQuery)}&limit=10`).subscribe({
      next: (r) => (this.searchResults = r),
      error: (e) => (this.searchError = e.error?.message || 'Search failed'),
    });
  }

  loadRouteSchedules(routeId: number): void {
    this.http.get<any>(`/api/v1/routes/${routeId}`).subscribe({
      next: (r) => { this.selectedRoute = r; this.selectedRouteStopId = null; },
      error: () => (this.searchError = 'Failed to load route details'),
    });
  }

  reserveFromSchedule(scheduleId: number): void {
    if (!this.selectedRouteStopId) return;
    this.http.post('/api/v1/passenger/reservations', { scheduleId, stopId: this.selectedRouteStopId }).subscribe({
      next: () => {
        this.loadReservations();
        this.loadMessages();
        this.selectedRoute = null;
        this.selectedRouteStopId = null;
        this.tab = 'reservations';
      },
      error: (e) => (this.searchError = e.error?.message || 'Reservation failed'),
    });
  }

  useStopForCheckin(stopId: number): void {
    this.checkinStopId = stopId;
    this.tab = 'checkins';
  }

  loadReservations(): void {
    this.http.get<any[]>('/api/v1/passenger/reservations').subscribe({
      next: (r) => (this.reservations = r),
    });
  }

  createReservation(): void {
    if (!this.resScheduleId || !this.resStopId) return;
    this.http.post('/api/v1/passenger/reservations', { scheduleId: this.resScheduleId, stopId: this.resStopId }).subscribe({
      next: () => { this.loadReservations(); this.loadMessages(); this.resScheduleId = null; this.resStopId = null; },
    });
  }

  cancelReservation(id: number): void {
    this.http.put(`/api/v1/passenger/reservations/${id}`, { status: 'CANCELLED' }).subscribe({
      next: () => { this.loadReservations(); this.loadMessages(); },
    });
  }

  confirmReservation(id: number): void {
    this.http.put(`/api/v1/passenger/reservations/${id}`, { status: 'CONFIRMED' }).subscribe({
      next: () => this.loadReservations(),
    });
  }

  loadCheckins(): void {
    this.http.get<any[]>('/api/v1/passenger/checkins').subscribe({
      next: (r) => (this.checkins = r),
    });
  }

  createCheckin(): void {
    if (!this.checkinStopId) return;
    this.http.post('/api/v1/passenger/checkins', { stopId: this.checkinStopId }).subscribe({
      next: () => { this.loadCheckins(); this.checkinStopId = null; },
    });
  }

  loadMessages(): void {
    this.http.get<any[]>('/api/v1/messages').subscribe({
      next: (r) => (this.messages = r),
    });
  }

  markRead(id: number): void {
    this.http.post(`/api/v1/messages/${id}/read`, {}).subscribe({
      next: () => this.loadMessages(),
    });
  }

  sendMessage(): void {
    if (!this.msgSubject || !this.msgBody) return;
    this.http.post('/api/v1/messages', { subject: this.msgSubject, body: this.msgBody }).subscribe({
      next: () => { this.loadMessages(); this.msgSubject = ''; this.msgBody = ''; },
    });
  }

  loadPrefs(): void {
    this.http.get<any>('/api/v1/passenger/reminder-preferences').subscribe({
      next: (r) => (this.prefs = r),
    });
  }

  savePrefs(): void {
    this.http.put('/api/v1/passenger/reminder-preferences', this.prefs).subscribe({
      next: (r) => (this.prefs = r),
    });
  }

  loadDndWindows(): void {
    this.http.get<any[]>('/api/v1/passenger/dnd-windows').subscribe({
      next: (r) => (this.dndWindows = r),
    });
  }

  createDndWindow(): void {
    this.http.post('/api/v1/passenger/dnd-windows', {
      dayOfWeek: this.dndDay,
      startTime: this.dndStart,
      endTime: this.dndEnd,
    }).subscribe({
      next: () => this.loadDndWindows(),
    });
  }

  deleteDndWindow(id: number): void {
    this.http.delete(`/api/v1/passenger/dnd-windows/${id}`).subscribe({
      next: () => this.loadDndWindows(),
    });
  }

  readonly dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  logout(): void {
    this.auth.logout();
  }
}
