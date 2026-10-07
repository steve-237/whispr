import { Component, inject } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, CommonModule, TranslatePipe],
  template: `
    <div style="padding: 0 1rem; position: sticky; top: 1rem; z-index: 50;">
      <header style="background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.08); padding: 0.8rem 1.5rem; display: flex; justify-content: space-between; align-items: center; border-radius: 100px; max-width: 1000px; margin: 0 auto; box-shadow: 0 10px 40px rgba(0,0,0,0.3);">
        <a routerLink="/" style="text-decoration: none; font-family: var(--font-family-heading); font-size: 1.4rem; font-weight: 800; background: linear-gradient(135deg, #fff, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: -0.5px;">
          Whispr<span style="color: var(--color-primary); -webkit-text-fill-color: var(--color-primary);">.</span>
        </a>
        <nav style="display: flex; gap: 0.8rem; align-items: center;">
          <button (click)="switchLang()" style="padding: 0.4rem 0.8rem; font-size: 0.95rem; font-weight: 600; color: white; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); cursor: pointer; border-radius: 20px; display: flex; align-items: center; justify-content: center; gap: 0.4rem; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.2);" onmouseover="this.style.background='rgba(255,255,255,0.15)'" onmouseout="this.style.background='rgba(255,255,255,0.1)'">
            {{ translate.currentLang() === 'fr' ? 'FR 🇫🇷' : 'EN 🇬🇧' }}
          </button>
          
          <ng-container *ngIf="!authService.isAuthenticated()">
            <a routerLink="/login" style="text-decoration: none; color: white; font-size: 0.9rem; font-weight: 600; padding: 0.5rem 1rem; background: rgba(255,255,255,0.1); border-radius: 20px; transition: background 0.2s;">{{ 'HEADER.LOGIN' | translate }}</a>
          </ng-container>

          <ng-container *ngIf="authService.isAuthenticated()">
            <a *ngIf="authService.isAdmin()" routerLink="/admin" style="text-decoration: none; color: #ef4444; font-size: 0.9rem; font-weight: 600; padding: 0.5rem 1rem; background: rgba(239, 68, 68, 0.1); border-radius: 20px;">
              Admin
            </a>
            <a *ngIf="!isInboxRoute()" routerLink="/inbox" style="text-decoration: none; color: white; font-size: 0.9rem; font-weight: 600; padding: 0.5rem 1rem; background: var(--color-primary); border-radius: 20px; box-shadow: 0 4px 15px rgba(139, 92, 246, 0.3);">
              {{ 'HEADER.WORKSPACE' | translate }}
            </a>
          </ng-container>
        </nav>
      </header>
    </div>
  `
})
export class HeaderComponent {
  authService = inject(AuthService);
  translate = inject(TranslateService);
  router = inject(Router);

  switchLang() {
    const currentLang = this.translate.currentLang() || this.translate.fallbackLang() || 'fr';
    const newLang = currentLang === 'fr' ? 'en' : 'fr';
    this.translate.use(newLang);
    localStorage.setItem('whispr_lang', newLang);
  }

  isInboxRoute(): boolean {
    return this.router.url === '/inbox';
  }
}
