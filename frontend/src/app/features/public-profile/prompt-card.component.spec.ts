import { describe, it, expect, vi } from 'vitest';
import { PromptCardComponent } from './prompt-card.component';
import { PromptDto } from '../../core/services/api.service';

describe('PromptCardComponent', () => {
  const mockPrompt: PromptDto = {
    id: 'p-1',
    question: 'Note mon style sur 10',
    type: 'RATING',
    isActive: true,
    scaleMax: 10,
    totalResponses: 4,
    averageRating: 8.5,
    ratingCount: 4,
    options: [],
    createdAt: new Date().toISOString()
  };

  it('devrait être initialisé correctement', () => {
    const comp = new PromptCardComponent();
    comp.prompt = mockPrompt;

    expect(comp.scaleArray().length).toBe(10);
    expect(comp.selectedRating()).toBeNull();
    expect(comp.selectedPollOptionId()).toBeNull();
  });

  it('devrait émettre ratingSubmitted avec note et commentaire', () => {
    const comp = new PromptCardComponent();
    comp.prompt = mockPrompt;
    const emitSpy = vi.spyOn(comp.ratingSubmitted, 'emit');

    comp.selectRating(9);
    comp.promptTextAnswer.set('Trop stylé !');
    comp.onRatingSubmit();

    expect(emitSpy).toHaveBeenCalledWith({
      rating: 9,
      comment: 'Trop stylé !'
    });
  });

  it('devrait émettre pollVoted avec l id de l option', () => {
    const comp = new PromptCardComponent();
    comp.prompt = { ...mockPrompt, type: 'POLL' };
    const emitSpy = vi.spyOn(comp.pollVoted, 'emit');

    comp.selectPollOption('opt-123');
    comp.onPollSubmit();

    expect(emitSpy).toHaveBeenCalledWith('opt-123');
  });
});
