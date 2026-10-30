import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Injector, runInInjectionContext } from '@angular/core';
import { InboxComponent } from './inbox.component';
import { ApiService, AdvertisementDto } from '../../../core/services/api.service';
import { WalletService } from '../../../shared/components/shop/wallet.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';

describe('InboxComponent Ad & Notification UX', () => {
  let component: InboxComponent;
  let mockApiService: any;
  let mockWalletService: any;
  let mockNotificationService: any;
  let mockRouter: any;
  let mockToastService: any;
  let mockTranslateService: any;

  beforeEach(() => {
    mockApiService = {
      recordAdClick: vi.fn().mockReturnValue(of({})),
      recordAdView: vi.fn().mockReturnValue(of({})),
      getAds: vi.fn().mockReturnValue(of([])),
      getMessages: vi.fn().mockReturnValue(of([])),
      getProfile: vi.fn().mockReturnValue(of({ slug: 'testuser' })),
      getStats: vi.fn().mockReturnValue(of({ positive: 0, neutral: 0, negative: 0, total: 0 })),
      getVisitorAnalytics: vi.fn().mockReturnValue(of({ totalVisits: 0 })),
      getBlockedSenders: vi.fn().mockReturnValue(of([]))
    };

    mockWalletService = {
      loadWallet: vi.fn(),
      openShop: vi.fn(),
      wallet: vi.fn().mockReturnValue({ coins: 100, isPro: false })
    };

    mockNotificationService = {
      isSupported: vi.fn().mockReturnValue(true),
      hasPermission: vi.fn().mockReturnValue(true),
      permission: vi.fn().mockReturnValue('granted'),
      requestPermission: vi.fn().mockResolvedValue('granted'),
      showWhisperNotification: vi.fn().mockResolvedValue(null)
    };

    mockRouter = {
      navigateByUrl: vi.fn()
    };

    mockToastService = {
      success: vi.fn(),
      error: vi.fn()
    };

    mockTranslateService = {
      instant: (k: string) => k,
      currentLang: () => 'fr'
    };

    const injector = Injector.create({
      providers: [
        { provide: WalletService, useValue: mockWalletService },
        { provide: Router, useValue: mockRouter }
      ]
    });

    component = runInInjectionContext(injector, () => {
      return new InboxComponent(
        mockApiService,
        { currentUser: () => 'testuser', isAuthenticated: () => true } as any,
        mockTranslateService as any,
        mockToastService,
        { setTitle: vi.fn() } as any,
        { connect: () => of(), disconnect: vi.fn() } as any,
        mockNotificationService
      );
    });
  });

  it('devrait ouvrir la boutique si la publicité est PRO ou pointe vers /inbox', () => {
    const proAd: AdvertisementDto = {
      id: 1,
      title: 'Passez à Whispr PRO Club 👑',
      description: 'Indices illimités',
      ctaText: 'Découvrir l\'offre',
      targetUrl: '/inbox',
      badgeText: 'Sponsorisé'
    };

    component.onAdClick(proAd);

    expect(mockApiService.recordAdClick).toHaveBeenCalledWith(1);
    expect(mockWalletService.openShop).toHaveBeenCalled();
  });

  it('devrait ouvrir un nouvel onglet si la publicité pointe vers un lien externe', () => {
    const mockOpen = vi.fn();
    (globalThis as any).window = { open: mockOpen };
    const externalAd: AdvertisementDto = {
      id: 2,
      title: 'Partenaire Externe',
      description: 'Super produit',
      ctaText: 'En savoir plus',
      targetUrl: 'https://example.com/partner',
      badgeText: 'Annonce'
    };

    component.onAdClick(externalAd);

    expect(mockApiService.recordAdClick).toHaveBeenCalledWith(2);
    expect(mockOpen).toHaveBeenCalledWith('https://example.com/partner', '_blank', 'noopener,noreferrer');
  });

  it('devrait basculer l affichage de la modal explicative des notifications', () => {
    expect(component.showNotifModal()).toBe(false);
    component.toggleNotifModal();
    expect(component.showNotifModal()).toBe(true);
    component.toggleNotifModal();
    expect(component.showNotifModal()).toBe(false);
  });

  it('devrait tester la notification sonore si permission accordée', () => {
    component.testPushNotification();
    expect(mockNotificationService.showWhisperNotification).toHaveBeenCalledWith({
      title: 'INBOX.PUSH_TITLE',
      body: 'INBOX.PUSH_BODY',
      force: true
    });
    expect(mockToastService.success).toHaveBeenCalled();
  });
});
