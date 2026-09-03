import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, LinkDto } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '../../shared/components/toast/toast.service';
import confetti from 'canvas-confetti';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslatePipe],
  templateUrl: './public-profile.html'
})
export class PublicProfileComponent implements OnInit {
  profile = signal<LinkDto | null>(null);
  messageContent = signal('');
  isSending = signal(false);
  isSent = signal(false);
  error = signal('');
  
  public translate = inject(TranslateService);
  private toastService = inject(ToastService);
  slug = signal('');
  isLoggingIn = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const slugParam = params.get('slug') || '';
      this.slug.set(slugParam);
      if (slugParam) {
        this.loadProfile();
      }
    });
  }

  loadProfile(): void {
    this.apiService.getLinkInfo(this.slug()).subscribe({
      next: (data: LinkDto) => {
        this.profile.set(data);
      },
      error: () => {
        this.error.set("Profil introuvable ou lien inactif.");
      }
    });
  }

  sendMessage(): void {
    if (!this.messageContent().trim() || !this.profile()) return;
    
    this.isSending.set(true);
    this.apiService.sendMessage(this.slug(), this.messageContent()).subscribe({
      next: () => {
        this.isSending.set(false);
        this.isSent.set(true);
        this.messageContent.set('');
        this.error.set('');
        
        // Explosion de confettis ! 🎉
        confetti({
          particleCount: 150,
          spread: 80,
          origin: { y: 0.6 },
          colors: ['#8b5cf6', '#ec4899', '#10b981', '#f59e0b', '#3b82f6']
        });
      },
      error: (err) => {
        this.isSending.set(false);
        let errorMsg = "Erreur lors de l'envoi du message.";
        if (err.status === 400 && err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        } else if (err.status === 429) {
          errorMsg = "Trop de messages envoyés. Veuillez patienter.";
        }
        this.error.set(errorMsg);
        this.toastService.error(errorMsg);
      }
    });
  }

  loginToDemo(): void {
    this.isLoggingIn.set(true);
    // Identifiants par défaut pour le compte de démo
    this.authService.login('demo@whispr.com', 'password123').subscribe({
      next: () => {
        this.isLoggingIn.set(false);
        this.router.navigate(['/inbox']);
      },
      error: () => {
        this.isLoggingIn.set(false);
        this.error.set("Erreur de connexion au compte démo.");
      }
    });
  }
}
