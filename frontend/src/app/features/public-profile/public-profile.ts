import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, LinkDto } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

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
      },
      error: (err) => {
        this.isSending.set(false);
        if (err.status === 400 && err.error && typeof err.error === 'string') {
          this.error.set(err.error);
        } else if (err.status === 429) {
          this.error.set("Vous envoyez trop de messages. Veuillez patienter.");
        } else {
          this.error.set("Erreur lors de l'envoi du message.");
        }
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
