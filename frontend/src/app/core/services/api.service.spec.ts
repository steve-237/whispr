import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService, MessageDto, LinkDto } from './api.service';
import { environment } from '../../../environments/environment';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;
  const API_URL = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // S'assure qu'aucune requête HTTP inattendue n'est en cours
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  describe('sendMessage', () => {
    it('devrait envoyer un message texte à un slug donné', () => {
      const slug = 'test-slug';
      const content = 'Un message très secret';

      service.sendMessage(slug, content).subscribe();

      const req = httpMock.expectOne(`${API_URL}/messages/send/${slug}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ content, type: 'text' });

      req.flush(null); // Simule une réponse vide (void) avec succès
    });
  });

  describe('getInbox', () => {
    it('devrait récupérer la liste des messages', () => {
      const mockMessages: MessageDto[] = [
        { id: '1', content: 'Message 1', type: 'text', status: 'UNREAD', createdAt: '2023-10-10' },
        { id: '2', content: 'Message 2', type: 'text', status: 'READ', createdAt: '2023-10-11' }
      ];

      service.getInbox().subscribe((messages) => {
        expect(messages.length).toBe(2);
        expect(messages).toEqual(mockMessages);
      });

      const req = httpMock.expectOne(`${API_URL}/messages/inbox`);
      expect(req.request.method).toBe('GET');

      req.flush(mockMessages);
    });
  });

  describe('getLinkInfo', () => {
    it('devrait récupérer les infos publiques du profil', () => {
      const mockLink: LinkDto = {
        id: '123',
        slug: 'test-slug',
        isActive: true,
        profileBio: 'Ma bio',
        profileAvatarUrl: '',
        profileThemeId: 'neon',
        profileDailyQuestion: 'Posez une question'
      };

      service.getLinkInfo('test-slug').subscribe((link) => {
        expect(link.slug).toBe('test-slug');
        expect(link.profileBio).toBe('Ma bio');
      });

      const req = httpMock.expectOne(`${API_URL}/links/test-slug`);
      expect(req.request.method).toBe('GET');

      req.flush(mockLink);
    });
  });

});
