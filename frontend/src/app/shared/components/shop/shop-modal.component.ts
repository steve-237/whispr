import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WalletService } from './wallet.service';
import { TranslatePipe } from '@ngx-translate/core';
import { ToastService } from '../toast/toast.service';
import confetti from 'canvas-confetti';

@Component({
  selector: 'app-shop-modal',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
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
            Boutique Whispr
          </h2>
          <p style="color: var(--color-text-muted); font-size: 0.9rem;">
            Débloquez des indices secrets ou devenez membre PRO !
          </p>

          <!-- Solde actuel & Total dépensé -->
          <div style="display: flex; gap: 0.75rem; justify-content: center; margin-top: 0.75rem; flex-wrap: wrap;">
            <div style="background: rgba(251, 191, 36, 0.1); border: 1px solid rgba(251, 191, 36, 0.3); border-radius: 20px; padding: 0.35rem 0.9rem; color: #fbbf24; font-weight: 700; font-size: 0.85rem; display: flex; align-items: center; gap: 0.35rem;">
              <span>🪙</span> {{ walletService.wallet()?.coins || 0 }} pièces
            </div>
            <div style="background: rgba(16, 185, 129, 0.1); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 20px; padding: 0.35rem 0.9rem; color: #10b981; font-weight: 700; font-size: 0.85rem;">
              💳 {{ walletService.wallet()?.totalSpentEur || 0 }} € dépensés
            </div>
          </div>
        </div>

        <!-- Section 📣 Espace Créateur & Influenceur (Partage de Revenus) -->
        <div style="background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(147, 51, 234, 0.15)); border: 1px solid rgba(59, 130, 246, 0.3); border-radius: 18px; padding: 1.15rem; margin-bottom: 1.5rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
            <div style="display: flex; align-items: center; gap: 0.4rem; font-weight: 800; font-size: 0.95rem; color: #60a5fa;">
              <span>📣</span>
              <span>Programme Créateurs & Influenceurs</span>
            </div>
            <span style="background: #2563eb; color: white; font-size: 0.65rem; font-weight: 800; padding: 0.15rem 0.5rem; border-radius: 10px;">
              30% COMMISSION
            </span>
          </div>
          <p style="font-size: 0.8rem; color: #cbd5e1; margin-bottom: 0.75rem; line-height: 1.4;">
            Partagez votre lien de profil sur Instagram, TikTok ou Snapchat. Chaque fois que vos abonnés débloquent des indices ou achètent des pièces sur vos messages, vous gagnez <strong>30% de commission</strong> !
          </p>
          <div style="background: rgba(0,0,0,0.3); border-radius: 12px; padding: 0.6rem 0.8rem; display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
            <div>
              <div style="font-size: 0.7rem; color: var(--color-text-muted);">Vos gains disponibles</div>
              <div style="font-size: 1.15rem; font-weight: 800; color: #10b981;">
                {{ walletService.wallet()?.earningsEur || '0.00' }} €
              </div>
            </div>
            <button (click)="copyAffiliateLink()" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; padding: 0.4rem 0.75rem; border-radius: 10px; font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: all 0.2s;">
              📋 Copier mon lien affilié
            </button>
          </div>
          <div style="font-size: 0.7rem; color: #94a3b8; text-align: center;">
            🎯 Retrait des gains possible dès 20,00 € (PayPal / Virement bancaire)
          </div>
        </div>

        <!-- Bannière Whispr PRO Club -->
        <div style="background: linear-gradient(135deg, rgba(236, 72, 153, 0.2), rgba(139, 92, 246, 0.2)); border: 1px solid rgba(236, 72, 153, 0.4); border-radius: 20px; padding: 1.25rem; margin-bottom: 1.5rem; position: relative; overflow: hidden;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
            <div>
              <div style="display: inline-block; background: linear-gradient(135deg, #ec4899, #8b5cf6); color: white; font-weight: 800; font-size: 0.7rem; padding: 0.2rem 0.6rem; border-radius: 12px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 0.3rem;">
                👑 Whispr PRO Club
              </div>
              <h3 style="font-size: 1.2rem; font-weight: 800; color: white; margin: 0;">Indices Illimités & Gratuit</h3>
            </div>
            <div style="text-align: right;">
              <span style="font-size: 1.4rem; font-weight: 800; color: #ec4899;">4,99 €</span>
              <span style="font-size: 0.8rem; color: var(--color-text-muted);">/mois</span>
            </div>
          </div>
          <ul style="margin: 0 0 1rem 0; padding-left: 1.2rem; font-size: 0.85rem; color: #cbd5e1; line-height: 1.5;">
            <li>🔓 Tous les indices de tous vos messages débloqués sans pièces</li>
            <li>✨ Thèmes VIP dorés & badges exclusifs</li>
            <li>🎁 50 pièces offertes chaque mois</li>
          </ul>
          
          <button *ngIf="!walletService.wallet()?.isPro" 
                  (click)="buy('PRO_MONTHLY', 'Abonnement Whispr PRO Club activé ! 👑')" 
                  [disabled]="walletService.isLoading()"
                  class="btn" style="width: 100%; background: linear-gradient(135deg, #ec4899, #8b5cf6); color: white; font-weight: 700; padding: 0.8rem; border-radius: 14px; border: none; cursor: pointer; box-shadow: 0 4px 15px rgba(236, 72, 153, 0.4);">
            {{ walletService.isLoading() ? 'Validation du paiement...' : 'Passer PRO (Simulation Stripe - 4,99 €)' }}
          </button>
          
          <div *ngIf="walletService.wallet()?.isPro" style="text-align: center; color: #10b981; font-weight: 700; font-size: 0.9rem; padding: 0.5rem; background: rgba(16, 185, 129, 0.15); border-radius: 10px;">
            ✓ Vous êtes déjà membre PRO ! (Actif jusqu'au {{ walletService.wallet()?.proExpiresAt | date:'shortDate' }})
          </div>
        </div>

        <!-- Section Packs de Pièces -->
        <h4 style="font-size: 1rem; font-weight: 700; color: white; margin-bottom: 0.8rem;">Recharger en Pièces Virtuelles</h4>
        <div style="display: flex; flex-direction: column; gap: 0.8rem;">
          
          <!-- Pack 1 -->
          <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 1rem; display: flex; justify-content: space-between; align-items: center; transition: all 0.2s;" onmouseover="this.style.borderColor='rgba(251,191,36,0.5)'" onmouseout="this.style.borderColor='rgba(255,255,255,0.08)'">
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span style="font-size: 1.8rem;">🪙</span>
              <div>
                <div style="font-weight: 700; color: white; font-size: 1rem;">Pack Découverte</div>
                <div style="font-size: 0.8rem; color: #fbbf24; font-weight: 600;">+50 pièces (3 indices)</div>
              </div>
            </div>
            <button (click)="buy('COINS_50', '+50 pièces ajoutées ! 🪙')" [disabled]="walletService.isLoading()" class="btn btn-primary" style="padding: 0.6rem 1rem; font-size: 0.9rem; font-weight: 700;">
              0,99 €
            </button>
          </div>

          <!-- Pack 2 (Populaire) -->
          <div style="background: rgba(139, 92, 246, 0.08); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: 16px; padding: 1rem; display: flex; justify-content: space-between; align-items: center; position: relative;">
            <div style="position: absolute; top: -8px; right: 20px; background: #8b5cf6; color: white; font-size: 0.65rem; font-weight: 800; padding: 0.15rem 0.6rem; border-radius: 20px; text-transform: uppercase;">
              ⭐ Populaire
            </div>
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span style="font-size: 1.8rem;">💰</span>
              <div>
                <div style="font-weight: 700; color: white; font-size: 1rem;">Pack Enquêteur</div>
                <div style="font-size: 0.8rem; color: #fbbf24; font-weight: 600;">+150 pièces (10 indices)</div>
              </div>
            </div>
            <button (click)="buy('COINS_150', '+150 pièces ajoutées ! 💰')" [disabled]="walletService.isLoading()" class="btn btn-primary" style="padding: 0.6rem 1rem; font-size: 0.9rem; font-weight: 700; background: linear-gradient(135deg, #8b5cf6, #ec4899);">
              2,49 €
            </button>
          </div>

          <!-- Pack 3 (VIP) -->
          <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 1rem; display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span style="font-size: 1.8rem;">🏆</span>
              <div>
                <div style="font-weight: 700; color: white; font-size: 1rem;">Pack Coffre-Fort</div>
                <div style="font-size: 0.8rem; color: #fbbf24; font-weight: 600;">+500 pièces (33 indices)</div>
              </div>
            </div>
            <button (click)="buy('COINS_500', '+500 pièces ajoutées ! 🏆')" [disabled]="walletService.isLoading()" class="btn btn-primary" style="padding: 0.6rem 1rem; font-size: 0.9rem; font-weight: 700;">
              5,99 €
            </button>
          </div>

        </div>

        <div style="text-align: center; margin-top: 1.5rem; font-size: 0.75rem; color: var(--color-text-muted);">
          🔒 Simulation de paiement sécurisé Stripe • Les soldes et dépenses sont comptabilisés en temps réel.
        </div>

      </div>
    </div>
  `
})
export class ShopModalComponent {
  walletService = inject(WalletService);
  toastService = inject(ToastService);

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
    this.walletService.buyPack(
      packId,
      () => {
        confetti({
          particleCount: 120,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#fbbf24', '#8b5cf6', '#ec4899', '#10b981']
        });
        this.toastService.success(successMsg);
      },
      (err) => {
        this.toastService.error("Erreur lors de la simulation d'achat");
      }
    );
  }
}
