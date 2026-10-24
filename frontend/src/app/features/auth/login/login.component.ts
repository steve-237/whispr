import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslatePipe],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  isLoading = signal(false);
  error = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(event?: Event): void {
    if (event) event.preventDefault();
    const cleanEmail = this.email ? this.email.trim() : '';
    if (!cleanEmail || !this.password) return;
    
    this.isLoading.set(true);
    this.error.set('');

    this.authService.login(cleanEmail, this.password).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/inbox']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set('LOGIN.ERROR_CREDENTIALS');
      }
    });
  }
}
