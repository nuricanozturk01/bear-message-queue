import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  submit(): void {
    this.error.set(null);
    this.loading.set(true);
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const msg =
          err.error?.message ??
          (typeof err.error === 'string' ? err.error : null) ??
          'Sign in failed. Check your credentials.';
        this.error.set(msg);
      },
      complete: () => this.loading.set(false),
    });
  }
}
