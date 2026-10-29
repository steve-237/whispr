import { describe, it, expect, vi, beforeEach } from 'vitest';
import { of, throwError } from 'rxjs';
import { WalletService } from './wallet.service';
import { WalletDto } from '../../../core/services/api.service';

describe('WalletService', () => {
  let service: WalletService;
  let apiServiceMock: any;

  beforeEach(() => {
    apiServiceMock = {
      getWallet: vi.fn().mockReturnValue(of({ coins: 0, totalSpentEur: 0, isPro: false })),
      createCheckoutSession: vi.fn().mockReturnValue(of({ sessionId: 'cs_123', checkoutUrl: 'https://checkout.stripe.com/test' })),
      simulateCheckout: vi.fn(),
      getPayoutHistory: vi.fn().mockReturnValue(of([]))
    };

    service = new WalletService(apiServiceMock);
  });

  it('devrait être initialisé avec des valeurs par défaut', () => {
    expect(service.wallet()).toBeNull();
    expect(service.showShopModal()).toBe(false);
    expect(service.isLoading()).toBe(false);
  });

  it('devrait ouvrir et fermer la boutique', () => {
    service.openShop();
    expect(service.showShopModal()).toBe(true);

    service.closeShop();
    expect(service.showShopModal()).toBe(false);
  });

  it('devrait charger les données du portefeuille', () => {
    const mockWallet: WalletDto = {
      coins: 50,
      totalSpentEur: 2.49,
      isPro: false
    };
    apiServiceMock.getWallet.mockReturnValue(of(mockWallet));

    service.loadWallet();

    expect(apiServiceMock.getWallet).toHaveBeenCalled();
    expect(service.wallet()).toEqual(mockWallet);
  });

  it('devrait initialiser une session Stripe Checkout lors de buyPack', () => {
    const onSuccess = vi.fn();

    service.buyPack('COINS_50', 'PROMO10', onSuccess);

    expect(apiServiceMock.createCheckoutSession).toHaveBeenCalledWith('COINS_50', 'PROMO10');
    expect(service.isLoading()).toBe(false);
    expect(onSuccess).toHaveBeenCalled();
  });

  it('devrait utiliser simulateCheckout en fallback lors d une erreur Stripe', () => {
    const updatedWallet: WalletDto = {
      coins: 100,
      totalSpentEur: 5.48,
      isPro: true
    };
    apiServiceMock.createCheckoutSession.mockReturnValue(throwError(() => new Error('Stripe unavailable')));
    apiServiceMock.simulateCheckout.mockReturnValue(of(updatedWallet));
    const onSuccess = vi.fn();

    service.buyPack('COINS_50', undefined, onSuccess);

    expect(apiServiceMock.simulateCheckout).toHaveBeenCalledWith('COINS_50');
    expect(service.wallet()).toEqual(updatedWallet);
    expect(service.isLoading()).toBe(false);
    expect(onSuccess).toHaveBeenCalled();
  });

  it('devrait gérer l\'échec complet d\'un achat', () => {
    apiServiceMock.createCheckoutSession.mockReturnValue(throwError(() => new Error('Stripe unavailable')));
    apiServiceMock.simulateCheckout.mockReturnValue(throwError(() => new Error('Erreur réseau')));
    const onError = vi.fn();

    service.buyPack('COINS_50', undefined, undefined, onError);

    expect(service.isLoading()).toBe(false);
    expect(onError).toHaveBeenCalled();
  });
});
