import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, CommonModule, TranslatePipe],
  template: `
    <header class="glass-panel" style="margin: 0.5rem; padding: 0.75rem 1rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.75rem; max-width: 1200px; margin-left: auto; margin-right: auto;">
      <a routerLink="/" class="logo" style="text-decoration: none; font-family: var(--font-family-heading); font-size: 1.5rem; font-weight: 800; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
        Whispr.
      </a>
      <nav style="display: flex; gap: 0.5rem; align-items: center; flex-wrap: wrap;">
        <button (click)="switchLang()" class="btn btn-glass" style="padding: 0.25rem 0.5rem; font-size: 1.2rem; background: transparent; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center;">
          {{ translate.currentLang() === 'fr' ? '🇫🇷' : '🇬🇧' }}
        </button>
        <ng-container *ngIf="!authService.isAuthenticated()">
          <a routerLink="/demo" class="btn btn-glass" style="text-decoration: none;">{{ 'HEADER.DEMO' | translate }}</a>
          <a routerLink="/login" class="btn btn-glass" style="text-decoration: none;">{{ 'HEADER.LOGIN' | translate }}</a>
        </ng-container>

        <ng-container *ngIf="authService.isAuthenticated()">
          <a *ngIf="authService.isAdmin()" routerLink="/admin" class="btn btn-glass" style="text-decoration: none; color: #ef4444; border-color: rgba(239, 68, 68, 0.3);">
            <span style="display: flex; align-items: center; gap: 0.5rem;">
              <svg style="width: 16px; height: 16px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"></path></svg>
              {{ 'HEADER.ADMIN' | translate }}
            </span>
          </a>
          <a routerLink="/inbox" class="btn btn-primary" style="text-decoration: none;">{{ 'HEADER.WORKSPACE' | translate }}</a>
        </ng-container>
      </nav>
    </header>
  `
})
export class HeaderComponent {
  authService = inject(AuthService);
  translate = inject(TranslateService);

  switchLang() {
    const currentLang = this.translate.currentLang() || this.translate.fallbackLang() || 'fr';
    const newLang = currentLang === 'fr' ? 'en' : 'fr';
    this.translate.use(newLang);
    localStorage.setItem('whispr_lang', newLang);
  }
}
