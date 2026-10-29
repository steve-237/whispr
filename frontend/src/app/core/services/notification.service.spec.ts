import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let originalNotification: any;
  let originalWindow: any;

  beforeEach(() => {
    originalNotification = (global as any).Notification;
    originalWindow = (global as any).window;
  });

  afterEach(() => {
    (global as any).Notification = originalNotification;
    (global as any).window = originalWindow;
    vi.restoreAllMocks();
  });

  it('devrait détecter correctement si Notification n est pas supporté', () => {
    delete (global as any).Notification;
    (global as any).window = {};
    service = new NotificationService();

    expect(service.isSupported()).toBe(false);
    expect(service.hasPermission()).toBe(false);
    expect(service.permission()).toBe('unsupported');
  });

  it('devrait initialiser la permission existante si supporté', () => {
    const mockNotif = {
      permission: 'granted',
      requestPermission: vi.fn()
    };
    (global as any).Notification = mockNotif;
    (global as any).window = { Notification: mockNotif };

    service = new NotificationService();
    expect(service.isSupported()).toBe(true);
    expect(service.hasPermission()).toBe(true);
    expect(service.permission()).toBe('granted');
  });

  it('devrait demander la permission via requestPermission()', async () => {
    const mockNotif = {
      permission: 'default',
      requestPermission: vi.fn().mockResolvedValue('granted')
    };
    (global as any).Notification = mockNotif;
    (global as any).window = { Notification: mockNotif };

    service = new NotificationService();
    expect(service.permission()).toBe('default');

    const result = await service.requestPermission();
    expect(result).toBe('granted');
    expect(service.permission()).toBe('granted');
    expect(service.hasPermission()).toBe(true);
  });

  it('ne devrait pas afficher de notification si non autorisé', async () => {
    const mockNotif = {
      permission: 'denied',
      requestPermission: vi.fn()
    };
    (global as any).Notification = mockNotif;
    (global as any).window = { Notification: mockNotif };

    service = new NotificationService();
    const result = await service.showWhisperNotification({ force: true });
    expect(result).toBeNull();
  });

  it('devrait créer une Notification avec options et gérer le clic', async () => {
    let createdInstance: any = null;
    class MockNotification {
      static permission = 'granted';
      static requestPermission = vi.fn();
      public onclick: any = null;
      constructor(public title: string, public options: any) {
        createdInstance = this;
      }
    }

    (global as any).Notification = MockNotification;
    (global as any).window = { Notification: MockNotification, focus: vi.fn() };

    service = new NotificationService();

    const onClickMock = vi.fn();
    const notif = await service.showWhisperNotification({
      title: 'Secret !',
      body: 'Nouveau message reçu',
      force: true,
      onClick: onClickMock
    });

    expect(notif).toBe(createdInstance);
    expect(createdInstance.title).toBe('Secret !');
    expect(createdInstance.options.body).toBe('Nouveau message reçu');

    // Simuler le clic
    if (createdInstance.onclick) {
      createdInstance.onclick({ preventDefault: vi.fn() });
      expect(onClickMock).toHaveBeenCalled();
      expect((global as any).window.focus).toHaveBeenCalled();
    }
  });
});
