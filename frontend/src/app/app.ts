import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from './core/services/api.service';
import { TranslateService, TranslatePipe, translate } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, TranslatePipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('frontend');
  public isBackendReady = signal<boolean>(false);
  public isWakingUp = signal<boolean>(false);

  // Signaux de traduction
  wakeUpText = translate('APP.WAKE_UP');
  wakeUpMsg1 = translate('APP.WAKE_UP_MSG1');
  wakeUpMsg2 = translate('APP.WAKE_UP_MSG2');

  constructor(private apiService: ApiService, private translate: TranslateService) {
    // Configuration de la langue par défaut
    this.translate.addLangs(['fr', 'en']);
    
    // Détecter la langue du navigateur
    const browserLang = navigator.language || navigator.languages?.[0] || 'fr';
    const langToUse = browserLang.toLowerCase().includes('en') ? 'en' : 'fr';
    
    // Si la langue est sauvegardée dans le localStorage, on l'utilise, sinon on prend celle du navigateur
    const savedLang = localStorage.getItem('whispr_lang');
    const finalLang = savedLang || langToUse;
    this.translate.setFallbackLang(finalLang);
    this.translate.use(finalLang);
  }

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
