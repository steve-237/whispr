import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from './core/services/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('frontend');
  public isBackendReady = signal<boolean>(false);
  public isWakingUp = signal<boolean>(false);

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.checkBackendStatus();
  }

  checkBackendStatus() {
    // Si le serveur répond vite (moins d'1 seconde), on ne montre pas l'écran de réveil
    const timeoutId = setTimeout(() => {
      if (!this.isBackendReady()) {
        this.isWakingUp.set(true);
      }
    }, 1000);

    const check = () => {
      this.apiService.checkHealth().subscribe({
        next: () => {
          clearTimeout(timeoutId);
          this.isBackendReady.set(true);
          this.isWakingUp.set(false);
        },
        error: () => {
          // Si erreur, on reteste dans 3 secondes
          setTimeout(check, 3000);
        }
      });
    };

    check();
  }
}
