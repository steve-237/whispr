import { describe, it, expect, vi, beforeEach } from 'vitest';
import { of } from 'rxjs';
import { ApiService, MessageDto, LinkDto, VisitorAnalyticsDto, WalletDto } from './api.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

describe('ApiService', () => {
  let service: ApiService;
  let httpClientMock: any;
  const API_URL = environment.apiUrl;

  beforeEach(() => {
    httpClientMock = {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn()
    };
    service = new ApiService(httpClientMock as unknown as HttpClient);
  });

  it('devrait être initialisé correctement', () => {
    expect(service).toBeTruthy();
  });

  describe('sendMessage', () => {
    it('devrait envoyer une requête POST avec le slug et le contenu', () => {
      const slug = 'test-slug';
      const content = 'Un message secret';
      httpClientMock.post.mockReturnValue(of({}));

      service.sendMessage(slug, content).subscribe();

      expect(httpClientMock.post).toHaveBeenCalledWith(
        `${API_URL}/messages/send/${slug}`,
        { content, type: 'text' }
      );
    });

    it('devrait envoyer le type personnalisé si spécifié', () => {
      const slug = 'test-slug';
      httpClientMock.post.mockReturnValue(of({}));

      service.sendMessage(slug, 'Hello', 'SUPER_WHISPR').subscribe();

      expect(httpClientMock.post).toHaveBeenCalledWith(
        `${API_URL}/messages/send/${slug}`,
        { content: 'Hello', type: 'SUPER_WHISPR' }
      );
    });
  });

  describe('getInbox', () => {
    it('devrait récupérer la boîte de réception', () => {
      const mockMessages: MessageDto[] = [
        { id: '1', content: 'Message 1', type: 'text', status: 'UNREAD', createdAt: '2026-10-23' },
        { id: '2', content: 'Message 2', type: 'text', status: 'READ', createdAt: '2026-10-23' }
      ];
      httpClientMock.get.mockReturnValue(of(mockMessages));

      let result: MessageDto[] = [];
      service.getInbox().subscribe(data => (result = data));

      expect(httpClientMock.get).toHaveBeenCalledWith(`${API_URL}/messages/inbox`);
      expect(result).toEqual(mockMessages);
    });
  });

  describe('getLinkInfo', () => {
    it('devrait récupérer les informations publiques du lien', () => {
      const mockLink: LinkDto = {
        id: '123',
        slug: 'mon-lien',
        isActive: true,
        profileBio: 'Ma bio',
        profileAvatarUrl: '',
        profileThemeId: 'neon',
        profileDailyQuestion: 'Posez-moi une question'
      };
      httpClientMock.get.mockReturnValue(of(mockLink));

      let result: LinkDto | undefined;
      service.getLinkInfo('mon-lien').subscribe(data => (result = data));

      expect(httpClientMock.get).toHaveBeenCalledWith(`${API_URL}/links/mon-lien`);
      expect(result?.slug).toBe('mon-lien');
    });
  });

  describe('deleteMessage & markAsRead', () => {
    it('devrait appeler DELETE pour supprimer un message', () => {
      httpClientMock.delete.mockReturnValue(of(null));
      service.deleteMessage('msg-123').subscribe();
      expect(httpClientMock.delete).toHaveBeenCalledWith(`${API_URL}/messages/msg-123`);
    });

    it('devrait appeler PUT pour marquer un message comme lu', () => {
      httpClientMock.put.mockReturnValue(of(null));
      service.markAsRead('msg-123').subscribe();
      expect(httpClientMock.put).toHaveBeenCalledWith(`${API_URL}/messages/msg-123/read`, {});
    });
  });

  describe('Analytics & Monétisation', () => {
    it('devrait récupérer les analytics visiteurs', () => {
      const mockAnalytics: VisitorAnalyticsDto = {
        totalViews: 42,
        totalMessages: 10,
        sources: { 'Instagram': 5 },
        topCities: { 'Paris': 4 },
        topCountries: { 'France': 10 }
      };
      httpClientMock.get.mockReturnValue(of(mockAnalytics));

      let res: VisitorAnalyticsDto | undefined;
      service.getVisitorAnalytics().subscribe(data => (res = data));

      expect(httpClientMock.get).toHaveBeenCalledWith(`${API_URL}/monetization/visitor-analytics`);
      expect(res?.totalViews).toBe(42);
    });

    it('devrait récupérer les informations du portefeuille', () => {
      const mockWallet: WalletDto = {
        coins: 100,
        totalSpentEur: 4.99,
        isPro: true
      };
      httpClientMock.get.mockReturnValue(of(mockWallet));

      let wallet: WalletDto | undefined;
      service.getWallet().subscribe(data => (wallet = data));

      expect(httpClientMock.get).toHaveBeenCalledWith(`${API_URL}/monetization/wallet`);
      expect(wallet?.isPro).toBe(true);
    });
  });
});
