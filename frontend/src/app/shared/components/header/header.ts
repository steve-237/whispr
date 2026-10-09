import { Component, inject } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';

import { WalletService } from '../shop/wallet.service';

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
        <nav style="display: flex; gap: 0.6rem; align-items: center;">
          
          <!-- Badge Pièces & Boutique (si connecté) -->
          <ng-container *ngIf="authService.isAuthenticated()">
            <button (click)="walletService.openShop()" style="background: rgba(251, 191, 36, 0.15); border: 1px solid rgba(251, 191, 36, 0.4); color: #fbbf24; padding: 0.4rem 0.75rem; border-radius: 20px; font-weight: 800; font-size: 0.85rem; display: flex; align-items: center; gap: 0.35rem; cursor: pointer; transition: all 0.2s;" onmouseover="this.style.transform='scale(1.05)'" onmouseout="this.style.transform='scale(1)'">
              <span>🪙</span>
              <span>{{ walletService.wallet()?.coins || 0 }}</span>
              <span *ngIf="walletService.wallet()?.isPro" style="background: linear-gradient(135deg, #ec4899, #8b5cf6); color: white; font-size: 0.65rem; padding: 0.1rem 0.4rem; border-radius: 8px; margin-left: 0.2rem;">PRO</span>
              <span style="background: rgba(251, 191, 36, 0.3); border-radius: 50%; width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; font-size: 0.75rem; margin-left: 0.1rem;">+</span>
            </button>
          </ng-container>

          <button (click)="switchLang()" style="padding: 0.4rem 0.8rem; font-size: 0.95rem; font-weight: 600; color: white; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); cursor: pointer; border-radius: 20px; display: flex; align-items: center; justify-content: center; gap: 0.4rem; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.2);" onmouseover="this.style.background='rgba(255,255,255,0.15)'" onmouseout="this.style.background='rgba(255,255,255,0.1)'">
            {{ translate.currentLang() === 'fr' ? '🇫🇷' : '🇬🇧' }}
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
  walletService = inject(WalletService);
  translate = inject(TranslateService);
  router = inject(Router);

  constructor() {
    if (this.authService.isAuthenticated()) {
      this.walletService.loadWallet();
    }
  }

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
