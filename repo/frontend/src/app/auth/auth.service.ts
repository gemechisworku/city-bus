import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, of, switchMap, tap, throwError } from 'rxjs';
import type { LoginResponse, MeResponse } from './auth.models';

const TOKEN_KEY = 'citybus.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly userSubject = new BehaviorSubject<MeResponse | null>(null);
  readonly user$ = this.userSubject.asObservable();

  constructor() {
    if (this.isLoggedIn()) {
      this.refreshMe().subscribe({ error: () => this.clearLocalSession() });
    }
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(username: string, password: string): Observable<MeResponse> {
    return this.http.post<LoginResponse>('/api/v1/auth/login', { username, password }).pipe(
      tap((res) => localStorage.setItem(TOKEN_KEY, res.accessToken)),
      switchMap(() => this.http.get<MeResponse>('/api/v1/auth/me')),
      tap((u) => this.userSubject.next(u))
    );
  }

  refreshMe(): Observable<MeResponse> {
    return this.http.get<MeResponse>('/api/v1/auth/me').pipe(
      tap((u) => this.userSubject.next(u)),
      catchError((err) => {
        this.clearLocalSession();
        return throwError(() => err);
      })
    );
  }

  ensureUser(): Observable<MeResponse> {
    const current = this.userSubject.value;
    if (current) {
      return of(current);
    }
    return this.refreshMe();
  }

  logout(): void {
    const token = this.getToken();
    this.clearLocalSession();
    if (token) {
      this.http.post('/api/v1/auth/logout', {}).subscribe();
    }
    void this.router.navigate(['/login']);
  }

  hasAnyRole(allowed: string[]): boolean {
    const u = this.userSubject.value;
    if (!u) {
      return false;
    }
    return allowed.some((r) => u.roles.includes(r));
  }

  private clearLocalSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.userSubject.next(null);
  }
}
