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
    expect(comp.isPlayingAudio).toBe(false);
  });

  it('devrait accepter un input msg', () => {
    const comp = new MessageCardComponent();
    const mockMsg: MessageDto = {
      id: 'test-1',
      content: 'Secret hello',
      createdAt: '2026-10-23T20:00:00Z',
      isRead: false,
      type: 'SUPER_WHISPR',
      audioUrl: 'data:audio/wav;base64,UklGRg==',
      audioDurationSeconds: 7,
      voiceFilter: 'ROBOT'
    };
    comp.msg = mockMsg;

    expect(comp.msg.id).toBe('test-1');
    expect(comp.msg.type).toBe('SUPER_WHISPR');
    expect(comp.msg.audioDurationSeconds).toBe(7);
    expect(comp.msg.voiceFilter).toBe('ROBOT');
  });

  it('devrait accepter un message avec pourboire visiteur', () => {
    const comp = new MessageCardComponent();
    const mockMsg: MessageDto = {
      id: 'tip-1',
      content: 'Bravo pour ton contenu !',
      createdAt: '2026-10-30T10:00:00Z',
      isRead: false,
      type: 'text',
      tipAmountEur: 5.00,
      isTipPaid: true
    };
    comp.msg = mockMsg;

    expect(comp.msg.tipAmountEur).toBe(5.00);
    expect(comp.msg.isTipPaid).toBe(true);
  });

  it('devrait gérer la pause audio', () => {
    const comp = new MessageCardComponent();
    comp.isPlayingAudio = true;
    comp.pauseAudio();
    expect(comp.isPlayingAudio).toBe(false);
  });
});
