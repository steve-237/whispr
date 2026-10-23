import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WebSocketService } from './websocket.service';
import { MessageDto } from './api.service';

// Mock @stomp/stompjs Client
vi.mock('@stomp/stompjs', () => {
  class MockClient {
    active = false;
    activate = vi.fn();
    deactivate = vi.fn();
    subscribe = vi.fn();
    onConnect?: () => void;
    onStompError?: (frame: any) => void;
    constructor(public config: any) {}
  }
  return {
    Client: MockClient
  };
});

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    vi.clearAllMocks();
    service = new WebSocketService();
  });

  it('devrait être instancié correctement', () => {
    expect(service).toBeDefined();
  });

  it('devrait activer le client STOMP lors de connect()', () => {
    const observable = service.connect('test@example.com');
    expect(observable).toBeDefined();

    const stompClient = (service as any).stompClient;
    expect(stompClient).toBeDefined();
    expect(stompClient.activate).toHaveBeenCalled();
  });

  it('devrait émettre un message lorsqu un message STOMP arrive', () => {
    const receivedMessages: MessageDto[] = [];
    service.connect('test@example.com').subscribe(msg => {
      receivedMessages.push(msg);
    });

    const stompClient = (service as any).stompClient;
    expect(stompClient.onConnect).toBeDefined();

    // Simuler subscription callback
    let subscriptionCallback: any;
    stompClient.subscribe.mockImplementation((topic: string, cb: any) => {
      subscriptionCallback = cb;
    });

    stompClient.onConnect();
    expect(stompClient.subscribe).toHaveBeenCalledWith('/topic/user/test@example.com/messages', expect.any(Function));

    // Envoyer un message simulé
    const mockMsg: MessageDto = {
      id: 'msg-123',
      content: 'Coucou secret',
      createdAt: '2026-10-23T20:00:00Z',
      isRead: false
    };

    subscriptionCallback({ body: JSON.stringify(mockMsg) });

    expect(receivedMessages.length).toBe(1);
    expect(receivedMessages[0].id).toBe('msg-123');
    expect(receivedMessages[0].content).toBe('Coucou secret');
  });

  it('devrait déconnecter et désactiver le client STOMP', () => {
    service.connect('test@example.com');
    const stompClient = (service as any).stompClient;

    service.disconnect();
    expect(stompClient.deactivate).toHaveBeenCalled();
    expect((service as any).stompClient).toBeNull();
  });
});
