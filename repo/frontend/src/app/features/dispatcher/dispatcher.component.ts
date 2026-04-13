import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-dispatcher',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dispatcher.component.html',
  styleUrl: './dispatcher.component.scss',
})
export class DispatcherComponent {
  readonly auth = inject(AuthService);

  logout(): void {
    this.auth.logout();
  }
}
