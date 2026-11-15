import { describe, it, expect, vi } from 'vitest';
import { PromptsManagerComponent } from './prompts-manager.component';

describe('PromptsManagerComponent', () => {
  it('devrait être instancié avec les valeurs par défaut', () => {
    const comp = new PromptsManagerComponent();
    expect(comp.isOpen).toBe(false);
    expect(comp.promptsList).toEqual([]);
    expect(comp.isSaving).toBe(false);
    expect(comp.question()).toBe('');
    expect(comp.promptType()).toBe('POLL');
    expect(comp.options()).toEqual(['Oui', 'Non']);
    expect(comp.scaleMax()).toBe(10);
  });

  it('devrait configurer un preset correctement', () => {
    const comp = new PromptsManagerComponent();
    comp.selectPreset('Note mon style sur 10', 'RATING', 10);

    expect(comp.question()).toBe('Note mon style sur 10');
    expect(comp.promptType()).toBe('RATING');
    expect(comp.scaleMax()).toBe(10);
  });

  it('devrait ajouter et retirer des options de sondage', () => {
    const comp = new PromptsManagerComponent();
    comp.addOption();
    expect(comp.options().length).toBe(3);

    comp.removeOption(2);
    expect(comp.options().length).toBe(2);

    // Ne doit pas descendre en dessous de 2 options
    comp.removeOption(0);
    expect(comp.options().length).toBe(2);
  });

  it('devrait émettre l événement create lors de la soumission d un sondage valide', () => {
    const comp = new PromptsManagerComponent();
    const emitSpy = vi.spyOn(comp.create, 'emit');

    comp.question.set('Quelle note me donnez-vous ?');
    comp.promptType.set('RATING');
    comp.scaleMax.set(10);

    comp.onSubmit();

    expect(emitSpy).toHaveBeenCalledWith({
      question: 'Quelle note me donnez-vous ?',
      type: 'RATING',
      options: undefined,
      scaleMax: 10
    });
  });
});
