import { Component, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WalletService } from './wallet.service';
import { ToastService } from '../toast/toast.service';
import { ApiService } from '../../../core/services/api.service';
import confetti from 'canvas-confetti';

@Component({
  selector: 'app-shop-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  template: `
    <div *ngIf="walletService.showShopModal()" style="position: fixed; inset: 0; background: rgba(0,0,0,0.85); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 1rem;">
      <div class="glass-panel animate-fade-in custom-scrollbar" style="max-width: 550px; width: 100%; max-height: 88vh; overflow-y: auto; background: #0f172a; border: 1px solid rgba(255,255,255,0.15); border-radius: 24px; padding: 2rem 1.75rem; position: relative; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.7); scrollbar-width: thin; scrollbar-color: rgba(255,255,255,0.15) transparent;">
        
        <!-- Bouton Fermer -->
        <button (click)="walletService.closeShop()" style="position: absolute; top: 1.25rem; right: 1.25rem; background: rgba(255,255,255,0.1); border: none; color: white; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 1.2rem; transition: background 0.2s;">
          ✕
        </button>

        <!-- Titre & Solde -->
        <div style="text-align: center; margin-bottom: 1.75rem;">
          <div style="font-size: 2.2rem; margin-bottom: 0.25rem;">🪙</div>
          <h2 style="font-size: 1.7rem; font-weight: 800; margin-bottom: 0.25rem; background: linear-gradient(135deg, #fff, #fbbf24); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
            {{ 'SHOP.TITLE' | translate }}
          </h2>
          <p style="color: var(--color-text-muted); font-size: 0.9rem;">
            {{ 'SHOP.SUBTITLE' | translate }}
          </p>

          <!-- Solde actuel & Total dépensé -->
          <div style="display: flex; gap: 0.75rem; justify-content: center; margin-top: 0.75rem; flex-wrap: wrap;">
            <div style="background: rgba(251, 191, 36, 0.1); border: 1px solid rgba(251, 191, 36, 0.3); border-radius: 20px; padding: 0.35rem 0.9rem; color: #fbbf24; font-weight: 700; font-size: 0.85rem; display: flex; align-items: center; gap: 0.35rem;">
              <span>🪙</span> {{ walletService.wallet()?.coins || 0 }} {{ 'SHOP.COINS' | translate }}
            </div>
            <div style="background: rgba(16, 185, 129, 0.1); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 20px; padding: 0.35rem 0.9rem; color: #10b981; font-weight: 700; font-size: 0.85rem;">
              💳 {{ walletService.wallet()?.totalSpentEur || 0 }} € {{ 'SHOP.SPENT' | translate }}
            </div>
          </div>
        </div>

        <!-- Section 📣 Espace Créateur & Influenceur (Partage de Revenus & Retraits US-05) -->
        <div style="background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(147, 51, 234, 0.15)); border: 1px solid rgba(59, 130, 246, 0.3); border-radius: 18px; padding: 1.15rem; margin-bottom: 1.5rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
            <div style="display: flex; align-items: center; gap: 0.4rem; font-weight: 800; font-size: 0.95rem; color: #60a5fa;">
              <span>📣</span>
              <span>{{ 'SHOP.CREATOR_PROGRAM' | translate }}</span>
            </div>
            <span style="background: #2563eb; color: white; font-size: 0.65rem; font-weight: 800; padding: 0.15rem 0.5rem; border-radius: 10px;">
              {{ 'SHOP.CREATOR_COMMISSION' | translate }}
            </span>
          </div>
          <p style="font-size: 0.8rem; color: #cbd5e1; margin-bottom: 0.75rem; line-height: 1.4;">
            {{ 'SHOP.CREATOR_DESC' | translate }}
          </p>

          <!-- Solde gains & Actions -->
          <div style="background: rgba(0,0,0,0.3); border-radius: 12px; padding: 0.75rem 0.9rem; margin-bottom: 0.75rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
              <div>
                <div style="font-size: 0.7rem; color: var(--color-text-muted);">{{ 'SHOP.AVAILABLE_EARNINGS' | translate }}</div>
                <div style="font-size: 1.25rem; font-weight: 800; color: #10b981;">
                  {{ walletService.wallet()?.earningsEur || '0.00' }} €
                </div>
              </div>
              <div style="display: flex; gap: 0.5rem;">
                <button (click)="copyAffiliateLink()" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; padding: 0.4rem 0.75rem; border-radius: 10px; font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: all 0.2s;">
                  {{ 'SHOP.COPY_LINK' | translate }}
                </button>
                <button *ngIf="canWithdraw() && !showPayoutForm()" 
                        (click)="openPayoutForm()" 
                        style="background: linear-gradient(135deg, #10b981, #059669); border: none; color: white; padding: 0.4rem 0.85rem; border-radius: 10px; font-size: 0.75rem; font-weight: 800; cursor: pointer; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);">
                  {{ 'SHOP.WITHDRAW_EARNINGS' | translate }}
                </button>
              </div>
            </div>

            <!-- Progression si solde < 20€ -->
            <div *ngIf="!canWithdraw()" style="font-size: 0.75rem; color: #94a3b8; text-align: center; padding-top: 0.25rem; border-top: 1px dashed rgba(255,255,255,0.1);">
              {{ 'SHOP.WITHDRAW_MIN_NOTE_1' | translate }}{{ (20 - (walletService.wallet()?.earningsEur || 0)).toFixed(2) }}{{ 'SHOP.WITHDRAW_MIN_NOTE_2' | translate }}
            </div>
          </div>

          <!-- Formulaire de Demande de Retrait (US-05) -->
          <div *ngIf="showPayoutForm()" style="background: rgba(15, 23, 42, 0.9); border: 1px solid rgba(16, 185, 129, 0.4); border-radius: 14px; padding: 1rem; margin-bottom: 0.75rem;">
            <div style="font-weight: 800; font-size: 0.9rem; color: #10b981; margin-bottom: 0.75rem; display: flex; justify-content: space-between; align-items: center;">
              <span>{{ 'SHOP.PAYOUT_TITLE' | translate }}</span>
              <button (click)="showPayoutForm.set(false)" style="background: transparent; border: none; color: #94a3b8; cursor: pointer; font-size: 1rem;">✕</button>
            </div>

            <!-- Choix Mode de Paiement -->
            <div style="display: flex; gap: 0.5rem; margin-bottom: 0.8rem;">
              <button type="button" 
                      (click)="payoutMethod = 'PAYPAL'" 
                      [style.background]="payoutMethod === 'PAYPAL' ? '#2563eb' : 'rgba(255,255,255,0.08)'"
                      style="flex: 1; border: none; color: white; padding: 0.5rem; border-radius: 8px; font-size: 0.8rem; font-weight: 700; cursor: pointer;">
                {{ 'SHOP.METHOD_PAYPAL' | translate }}
              </button>
              <button type="button" 
                      (click)="payoutMethod = 'IBAN'" 
                      [style.background]="payoutMethod === 'IBAN' ? '#2563eb' : 'rgba(255,255,255,0.08)'"
                      style="flex: 1; border: none; color: white; padding: 0.5rem; border-radius: 8px; font-size: 0.8rem; font-weight: 700; cursor: pointer;">
                {{ 'SHOP.METHOD_IBAN' | translate }}
              </button>
            </div>

            <!-- Coordonnées selon méthode -->
            <div style="margin-bottom: 0.8rem;">
              <label style="display: block; font-size: 0.75rem; color: #cbd5e1; margin-bottom: 0.3rem;">
                {{ payoutMethod === 'PAYPAL' ? ('SHOP.PAYPAL_EMAIL_LABEL' | translate) : ('SHOP.IBAN_LABEL' | translate) }}
              </label>
              <input type="text" 
                     [(ngModel)]="payoutDetails" 
                     [placeholder]="payoutMethod === 'PAYPAL' ? 'exemple@paypal.com' : 'FR76 1234 5678 9012 3456 7890 123 - Jean Dupont'" 
                     style="width: 100%; box-sizing: border-box; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; padding: 0.5rem 0.7rem; color: white; font-size: 0.85rem;" />
            </div>

            <!-- Montant à retirer -->
            <div style="margin-bottom: 1rem;">
              <div style="display: flex; justify-content: space-between; font-size: 0.75rem; color: #cbd5e1; margin-bottom: 0.3rem;">
                <span>{{ 'SHOP.AMOUNT_TO_WITHDRAW' | translate }}</span>
                <span>{{ 'SHOP.MAX' | translate }} {{ walletService.wallet()?.earningsEur || 0 }} €</span>
              </div>
              <input type="number" 
                     step="0.01" 
                     min="20" 
                     [max]="walletService.wallet()?.earningsEur || 20" 
                     [(ngModel)]="payoutAmount" 
                     style="width: 100%; box-sizing: border-box; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; padding: 0.5rem 0.7rem; color: white; font-size: 0.85rem;" />
            </div>

            <!-- Bouton confirmation -->
            <div style="display: flex; gap: 0.5rem;">
              <button (click)="submitPayout()" 
                      [disabled]="walletService.isPayoutLoading() || !payoutDetails || payoutAmount < 20" 
                      style="flex: 2; background: linear-gradient(135deg, #10b981, #059669); border: none; color: white; font-weight: 700; padding: 0.6rem; border-radius: 10px; cursor: pointer; font-size: 0.85rem;">
                {{ walletService.isPayoutLoading() ? ('SHOP.PROCESSING' | translate) : ('SHOP.CONFIRM_PAYOUT' | translate) }}
              </button>
              <button (click)="showPayoutForm.set(false)" 
                      style="flex: 1; background: rgba(255,255,255,0.1); border: none; color: #cbd5e1; font-weight: 600; padding: 0.6rem; border-radius: 10px; cursor: pointer; font-size: 0.85rem;">
                Annuler
              </button>
            </div>
          </div>

          <!-- Lien Historique Retraits -->
          <div *ngIf="walletService.payoutHistory().length > 0" style="margin-top: 0.5rem;">
            <button (click)="togglePayoutHistory()" style="background: transparent; border: none; color: #60a5fa; font-size: 0.75rem; cursor: pointer; padding: 0; text-decoration: underline;">
              {{ showPayoutHistory() ? ('SHOP.HIDE_PAYOUTS' | translate) : (('SHOP.SHOW_PAYOUTS_1' | translate) + walletService.payoutHistory().length + ('SHOP.SHOW_PAYOUTS_2' | translate)) }}
            </button>

            <!-- Liste historique -->
            <div *ngIf="showPayoutHistory()" style="margin-top: 0.5rem; display: flex; flex-direction: column; gap: 0.4rem; max-height: 140px; overflow-y: auto;">
              <div *ngFor="let p of walletService.payoutHistory()" style="background: rgba(0,0,0,0.3); border-radius: 8px; padding: 0.45rem 0.65rem; display: flex; justify-content: space-between; align-items: center; font-size: 0.75rem;">
                <div>
                  <div style="font-weight: 700; color: white;">
                    {{ p.amountEur | number:'1.2-2' }} € via {{ p.method }}
                  </div>
                  <div style="font-size: 0.65rem; color: #94a3b8;">
                    {{ p.createdAt | date:'short' }}
                  </div>
                </div>
                <div style="text-align: right;">
                  <span [style.background]="getStatusBg(p.status)" [style.color]="getStatusColor(p.status)" style="padding: 0.15rem 0.45rem; border-radius: 6px; font-weight: 800; font-size: 0.65rem;">
                    {{ getStatusLabel(p.status) }}
                  </span>
                  <div *ngIf="p.adminNote" style="font-size: 0.65rem; color: #fbbf24; margin-top: 0.15rem;">
                    {{ p.adminNote }}
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Bannière Whispr PRO Club -->
        <div style="background: linear-gradient(135deg, rgba(236, 72, 153, 0.2), rgba(139, 92, 246, 0.2)); border: 1px solid rgba(236, 72, 153, 0.4); border-radius: 20px; padding: 1.25rem; margin-bottom: 1.5rem; position: relative; overflow: hidden;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
            <div>
              <div style="display: inline-block; background: linear-gradient(135deg, #ec4899, #8b5cf6); color: white; font-weight: 800; font-size: 0.7rem; padding: 0.2rem 0.6rem; border-radius: 12px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 0.3rem;">
                {{ 'SHOP.PRO_CLUB_TITLE' | translate }}
              </div>
              <h3 style="font-size: 1.2rem; font-weight: 800; color: white; margin: 0;">{{ 'SHOP.PRO_UNLIMITED_CLUES' | translate }}</h3>
            </div>
            <div style="text-align: right;">
              <span style="font-size: 1.4rem; font-weight: 800; color: #ec4899;">4,99 €</span>
              <span style="font-size: 0.8rem; color: var(--color-text-muted);">{{ 'SHOP.PER_MONTH' | translate }}</span>
            </div>
          </div>
          <ul style="margin: 0 0 1rem 0; padding-left: 1.2rem; font-size: 0.85rem; color: #cbd5e1; line-height: 1.5;">
            <li>{{ 'SHOP.PRO_PERK_1' | translate }}</li>
            <li>{{ 'SHOP.PRO_PERK_2' | translate }}</li>
            <li>{{ 'SHOP.PRO_PERK_3' | translate }}</li>
          </ul>
          
          <button *ngIf="!walletService.wallet()?.isPro" 
                  (click)="buy('PRO_MONTHLY', translate.instant('SHOP.PRO_SUB_ACTIVATED') || 'Abonnement Whispr PRO Club activé ! 👑')" 
                  [disabled]="walletService.isLoading()"
                  class="btn" style="width: 100%; background: linear-gradient(135deg, #ec4899, #8b5cf6); color: white; font-weight: 700; padding: 0.8rem; border-radius: 14px; border: none; cursor: pointer; box-shadow: 0 4px 15px rgba(236, 72, 153, 0.4);">
            {{ walletService.isLoading() ? ('SHOP.VALIDATING_PAYMENT' | translate) : ('SHOP.GO_PRO_BTN' | translate) }}
          </button>
          
          <div *ngIf="walletService.wallet()?.isPro" style="background: rgba(16, 185, 129, 0.12); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 14px; padding: 0.85rem 1rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
              <span style="color: #10b981; font-weight: 800; font-size: 0.9rem; display: flex; align-items: center; gap: 0.35rem;">
                <span>👑</span>
                <span>{{ 'SHOP.PRO_ACTIVE' | translate }}</span>
              </span>
              <span style="font-size: 0.75rem; color: #cbd5e1; background: rgba(255,255,255,0.06); padding: 0.15rem 0.5rem; border-radius: 8px;">
                {{ 'SHOP.PRO_VALID_UNTIL' | translate }} {{ walletService.wallet()?.proExpiresAt | date:'shortDate' }}
              </span>
            </div>
            <div style="display: flex; justify-content: flex-end; margin-top: 0.6rem; padding-top: 0.5rem; border-top: 1px dashed rgba(255,255,255,0.1);">
              <button 
                type="button"
                (click)="cancelSubscription()" 
                [disabled]="isCancellingSubscription()"
                style="background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239, 68, 68, 0.35); color: #ef4444; font-weight: 700; font-size: 0.75rem; padding: 0.35rem 0.8rem; border-radius: 8px; cursor: pointer; transition: all 0.2s;">
                {{ isCancellingSubscription() ? ('SHOP.PRO_CANCELLING' | translate) : ('SHOP.PRO_CANCEL' | translate) }}
              </button>
            </div>
          </div>
        </div>

                <!-- Code Promo Créateur -->
        <div style="background: rgba(255,255,255,0.03); border: 1px dashed rgba(255,255,255,0.15); border-radius: 14px; padding: 0.75rem 1rem; margin-bottom: 1.25rem; display: flex; align-items: center; gap: 0.75rem;">
          <span style="font-size: 1.2rem;">🎟️</span>
          <input type="text" [ngModel]="promoCodeInput()" (ngModelChange)="promoCodeInput.set($event)" [placeholder]="'SHOP.PROMO_PLACEHOLDER' | translate" style="flex: 1; background: transparent; border: none; color: white; font-size: 0.85rem; outline: none; font-weight: 600;" />
          <span *ngIf="promoCodeInput().trim()" style="font-size: 0.75rem; color: #10b981; font-weight: 700;">{{ 'SHOP.PROMO_APPLIED' | translate }}</span>
        </div>

        <!-- Section Packs de Pièces -->
        <h4 style="font-size: 1rem; font-weight: 700; color: white; margin-bottom: 0.8rem;">{{ 'SHOP.PACKS_TITLE' | translate }}</h4>
        <div style="display: flex; flex-direction: column; gap: 0.8rem;">
          
          <!-- Pack 1 -->
          <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 1rem; display: flex; justify-content: space-between; align-items: center; transition: all 0.2s;" onmouseover="this.style.borderColor='rgba(251,191,36,0.5)'" onmouseout="this.style.borderColor='rgba(255,255,255,0.08)'">
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span style="font-size: 1.8rem;">🪙</span>
              <div>
                <div style="font-weight: 700; color: white; font-size: 1rem;">{{ 'SHOP.PACK_1_TITLE' | translate }}</div>
                <div style="font-size: 0.8rem; color: #fbbf24; font-weight: 600;">{{ 'SHOP.PACK_1_DESC' | translate }}</div>
              </div>
            </div>
            <button (click)="buy('COINS_50', translate.instant('SHOP.PACK_1_SUCCESS') || '+50 pièces ajoutées ! 🪙')" [disabled]="walletService.isLoading()" class="btn btn-primary" style="padding: 0.6rem 1rem; font-size: 0.9rem; font-weight: 700;">
              0,99 €
            </button>
          </div>

          <!-- Pack 2 (Populaire) -->
          <div style="background: rgba(139, 92, 246, 0.08); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: 16px; padding: 1rem; display: flex; justify-content: space-between; align-items: center; position: relative;">
            <div style="position: absolute; top: -8px; right: 20px; background: #8b5cf6; color: white; font-size: 0.65rem; font-weight: 800; padding: 0.15rem 0.6rem; border-radius: 20px; text-transform: uppercase;">
              {{ 'SHOP.POPULAR' | translate }}
            </div>
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span style="font-size: 1.8rem;">💰</span>
              <div>
                <div style="font-weight: 700; color: white; font-size: 1rem;">{{ 'SHOP.PACK_2_TITLE' | translate }}</div>
                <div style="font-size: 0.8rem; color: #fbbf24; font-weight: 600;">{{ 'SHOP.PACK_2_DESC' | translate }}</div>
              </div>
            </div>
            <button (click)="buy('COINS_150', translate.instant('SHOP.PACK_2_SUCCESS') || '+150 pièces ajoutées ! 💰')" [disabled]="walletService.isLoading()" class="btn btn-primary" style="padding: 0.6rem 1rem; font-size: 0.9rem; font-weight: 700; background: linear-gradient(135deg, #8b5cf6, #ec4899);">
              2,49 €
            </button>
          </div>

          <!-- Pack 3 (VIP) -->
          <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 1rem; display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span style="font-size: 1.8rem;">🏆</span>
              <div>
                <div style="font-weight: 700; color: white; font-size: 1rem;">{{ 'SHOP.PACK_3_TITLE' | translate }}</div>
                <div style="font-size: 0.8rem; color: #fbbf24; font-weight: 600;">{{ 'SHOP.PACK_3_DESC' | translate }}</div>
              </div>
            </div>
            <button (click)="buy('COINS_500', translate.instant('SHOP.PACK_3_SUCCESS') || '+500 pièces ajoutées ! 🏆')" [disabled]="walletService.isLoading()" class="btn btn-primary" style="padding: 0.6rem 1rem; font-size: 0.9rem; font-weight: 700;">
              5,99 €
            </button>
          </div>

        </div>

        <div style="text-align: center; margin-top: 1.5rem; font-size: 0.75rem; color: var(--color-text-muted);">
          {{ 'SHOP.SECURE_STRIPE_NOTE' | translate }}
        </div>

      </div>
    </div>
  `
})
export class ShopModalComponent {
  walletService = inject(WalletService);
  toastService = inject(ToastService);
  translate = inject(TranslateService);
  apiService = inject(ApiService);
  isCancellingSubscription = signal(false);
  promoCodeInput = signal<string>("");

  showPayoutForm = signal(false);
  showPayoutHistory = signal(false);

  payoutMethod: 'PAYPAL' | 'IBAN' = 'PAYPAL';
  payoutDetails = '';
  payoutAmount = 20.00;

  canWithdraw(): boolean {
    const earnings = this.walletService.wallet()?.earningsEur || 0;
    return earnings >= 20.00;
  }

  openPayoutForm(): void {
    const earnings = this.walletService.wallet()?.earningsEur || 20.00;
    this.payoutAmount = Number(earnings);
    this.showPayoutForm.set(true);
  }

  togglePayoutHistory(): void {
    this.showPayoutHistory.update(v => !v);
  }

  submitPayout(): void {
    if (!this.payoutDetails || !this.payoutDetails.trim()) {
      this.toastService.error('Veuillez renseigner vos coordonnées de paiement.');
      return;
    }
    if (this.payoutAmount < 20.00) {
      this.toastService.error('Le montant minimum de retrait est de 20,00 €.');
      return;
    }

    this.walletService.requestPayout(
      {
        amountEur: this.payoutAmount,
        method: this.payoutMethod,
        payoutDetails: this.payoutDetails.trim()
      },
      (payout) => {
        this.showPayoutForm.set(false);
        this.payoutDetails = '';
        confetti({
          particleCount: 80,
          spread: 60,
          origin: { y: 0.7 }
        });
        this.toastService.success('Votre demande de retrait de ' + payout.amountEur + ' € a été envoyée ! 🎉');
      },
      (err) => {
        const msg = err?.error?.message || 'Erreur lors de la demande de retrait.';
        this.toastService.error(msg);
      }
    );
  }

  copyAffiliateLink(): void {
    const code = this.walletService.wallet()?.affiliateCode || '';
    const base = window.location.origin;
    const url = `${base}/${code}`;
    navigator.clipboard.writeText(url).then(() => {
      this.toastService.success('Lien créateur copié dans le presse-papier ! 📋');
    }).catch(() => {
      this.toastService.error('Erreur lors de la copie du lien.');
    });
  }

  buy(packId: string, successMsg: string): void {
    const promo = this.promoCodeInput().trim();
    this.walletService.buyPack(
      packId,
      promo ? promo : undefined,
      () => {
        confetti({
          particleCount: 120,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#fbbf24', '#8b5cf6', '#ec4899', '#10b981']
        });
        this.toastService.success(successMsg);
      },
      (err: any) => {
        this.toastService.error("Erreur lors de l'initialisation du paiement Stripe");
      }
    );
  }

  getStatusBg(status: string): string {
    switch (status) {
      case 'PAID': return 'rgba(16, 185, 129, 0.2)';
      case 'APPROVED': return 'rgba(59, 130, 246, 0.2)';
      case 'REJECTED': return 'rgba(239, 68, 68, 0.2)';
      default: return 'rgba(251, 191, 36, 0.2)';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PAID': return '#10b981';
      case 'APPROVED': return '#60a5fa';
      case 'REJECTED': return '#ef4444';
      default: return '#fbbf24';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PAID': return 'Payé ✅';
      case 'APPROVED': return 'Approuvé ⏳';
      case 'REJECTED': return 'Rejeté ❌';
      default: return 'En attente 🟡';
    }
  }

  cancelSubscription(): void {
    if (!confirm(this.translate.instant('SHOP.PRO_CANCEL_CONFIRM') || 'Êtes-vous sûr de vouloir résilier votre abonnement Whispr PRO Club ?')) return;
    this.isCancellingSubscription.set(true);
    this.apiService.cancelSubscription().subscribe({
      next: (updatedWallet: any) => {
        this.isCancellingSubscription.set(false);
        this.walletService.wallet.set(updatedWallet);
        this.toastService.info(this.translate.instant('SHOP.PRO_CANCEL_SUCCESS') || 'Votre abonnement a été résilié avec succès.');
      },
      error: (err: any) => {
        this.isCancellingSubscription.set(false);
        this.toastService.error(err?.error?.message || this.translate.instant('SHOP.PRO_CANCEL_ERROR') || "Erreur lors de la résiliation de l'abonnement.");
      }
    });
  }

}
