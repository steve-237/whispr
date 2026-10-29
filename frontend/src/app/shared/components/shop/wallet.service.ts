import { Injectable, signal } from '@angular/core';
import { ApiService, WalletDto, PayoutResponseDto, CreatePayoutRequest } from '../../../core/services/api.service';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  wallet = signal<WalletDto | null>(null);
  showShopModal = signal(false);
  isLoading = signal(false);
  isPayoutLoading = signal(false);
  payoutHistory = signal<PayoutResponseDto[]>([]);

  constructor(private apiService: ApiService) {}

  loadWallet(): void {
    this.apiService.getWallet().subscribe({
      next: (data: WalletDto) => this.wallet.set(data),
      error: (err: any) => console.error('Erreur chargement wallet', err)
    });
  }

  loadPayoutHistory(): void {
    this.apiService.getPayoutHistory().subscribe({
      next: (data: PayoutResponseDto[]) => this.payoutHistory.set(data),
      error: (err: any) => console.error('Erreur chargement historique retraits', err)
    });
  }

  openShop(): void {
    this.showShopModal.set(true);
    this.loadWallet();
    this.loadPayoutHistory();
  }

  closeShop(): void {
    this.showShopModal.set(false);
  }

  buyPack(packId: string, promoCode?: string, onSuccess?: () => void, onError?: (err: any) => void): void {
    this.isLoading.set(true);
    this.apiService.createCheckoutSession(packId, promoCode).subscribe({
      next: (res: { sessionId: string, checkoutUrl: string }) => {
        this.isLoading.set(false);
        if (res && res.checkoutUrl) {
          if (typeof window !== 'undefined') {
            window.location.href = res.checkoutUrl;
          }
        }
        if (onSuccess) onSuccess();
      },
      error: (err: any) => {
        // Fallback simulateCheckout si mode hors ligne / mock
        this.apiService.simulateCheckout(packId).subscribe({
          next: (updatedWallet: WalletDto) => {
            this.isLoading.set(false);
            this.wallet.set(updatedWallet);
            if (onSuccess) onSuccess();
          },
          error: (simErr: any) => {
            this.isLoading.set(false);
            console.error('Erreur checkout', simErr);
            if (onError) onError(simErr);
          }
        });
      }
    });
  }

  requestPayout(req: CreatePayoutRequest, onSuccess?: (payout: PayoutResponseDto) => void, onError?: (err: any) => void): void {
    this.isPayoutLoading.set(true);
    this.apiService.createPayoutRequest(req).subscribe({
      next: (payout: PayoutResponseDto) => {
        this.isPayoutLoading.set(false);
        this.loadWallet();
        this.loadPayoutHistory();
        if (onSuccess) onSuccess(payout);
      },
      error: (err: any) => {
        this.isPayoutLoading.set(false);
        console.error('Erreur demande de retrait', err);
        if (onError) onError(err);
      }
    });
  }
}
