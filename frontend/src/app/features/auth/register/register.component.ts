import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  email = '';
  pseudo = '';
  password = '';
  isLoading = signal(false);
  error = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(event?: Event): void {
    if (event) event.preventDefault();
    if (!this.email || !this.pseudo || !this.password) {
      this.error.set('Veuillez remplir tous les champs.');
      return;
    }
    this.isLoading.set(true);
    this.error.set('');

    this.authService.register(this.email, this.pseudo, this.password).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/inbox']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.message || 'Erreur lors de l\'inscription');
      }
    });
  }
}
