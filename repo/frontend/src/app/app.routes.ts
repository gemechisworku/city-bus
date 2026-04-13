import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'passenger',
    loadComponent: () =>
      import('./features/passenger/passenger.component').then((m) => m.PassengerComponent),
    canActivate: [authGuard, roleGuard(['ADMIN', 'PASSENGER'])],
  },
  {
    path: 'dispatcher',
    loadComponent: () =>
      import('./features/dispatcher/dispatcher.component').then((m) => m.DispatcherComponent),
    canActivate: [authGuard, roleGuard(['ADMIN', 'DISPATCHER'])],
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin.component').then((m) => m.AdminComponent),
    canActivate: [authGuard, roleGuard(['ADMIN'])],
  },
  { path: '**', redirectTo: 'login' },
];
