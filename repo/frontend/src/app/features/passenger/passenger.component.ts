import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-passenger',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './passenger.component.html',
  styleUrl: './passenger.component.scss',
})
export class PassengerComponent {
  readonly auth = inject(AuthService);

  logout(): void {
    this.auth.logout();
  }
}
