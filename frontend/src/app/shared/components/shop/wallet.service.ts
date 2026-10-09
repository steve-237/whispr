import { Injectable, signal, inject } from '@angular/core';
import { ApiService, WalletDto } from '../../../core/services/api.service';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private apiService = inject(ApiService);

  wallet = signal<WalletDto | null>(null);
  showShopModal = signal(false);
  isLoading = signal(false);

  loadWallet(): void {
    this.apiService.getWallet().subscribe({
      next: (data: WalletDto) => this.wallet.set(data),
      error: (err: any) => console.error('Erreur chargement wallet', err)
    });
  }

  openShop(): void {
    this.showShopModal.set(true);
  }

  closeShop(): void {
    this.showShopModal.set(false);
  }

  buyPack(packId: string, onSuccess?: () => void, onError?: (err: any) => void): void {
    this.isLoading.set(true);
    this.apiService.simulateCheckout(packId).subscribe({
      next: (updatedWallet: WalletDto) => {
        this.isLoading.set(false);
        this.wallet.set(updatedWallet);
        if (onSuccess) onSuccess();
      },
      error: (err: any) => {
        this.isLoading.set(false);
        console.error('Erreur checkout', err);
        if (onError) onError(err);
      }
    });
  }
}
