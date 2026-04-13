import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  error: string | null = null;
  loading = false;

  submit(): void {
    this.error = null;
    this.loading = true;
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: (me) => {
        this.loading = false;
        if (me.roles.includes('ADMIN')) {
          void this.router.navigate(['/admin']);
        } else if (me.roles.includes('DISPATCHER')) {
          void this.router.navigate(['/dispatcher']);
        } else {
          void this.router.navigate(['/passenger']);
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Invalid username or password.';
      },
    });
  }
}
