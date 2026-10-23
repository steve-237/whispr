import { describe, it, expect } from 'vitest';
import { MessageCardComponent } from './message-card.component';
import { MessageDto } from '../../../core/services/api.service';

describe('MessageCardComponent', () => {
  it('devrait être instancié avec les valeurs par défaut', () => {
    const comp = new MessageCardComponent();
    expect(comp.isOpenedClues).toBe(false);
    expect(comp.clues).toEqual([]);
    expect(comp.isUnlockingClue).toBe(false);
    expect(comp.replyText).toBe('');
    expect(comp.isCapturing).toBe(false);
    expect(comp.isPro).toBe(false);
  });

  it('devrait accepter un input msg', () => {
    const comp = new MessageCardComponent();
    const mockMsg: MessageDto = {
      id: 'test-1',
      content: 'Secret hello',
      createdAt: '2026-10-23T20:00:00Z',
      isRead: false,
      type: 'SUPER_WHISPR'
    };
    comp.msg = mockMsg;

    expect(comp.msg.id).toBe('test-1');
    expect(comp.msg.type).toBe('SUPER_WHISPR');
  });
});
