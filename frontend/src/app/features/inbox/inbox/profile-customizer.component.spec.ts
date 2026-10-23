import { describe, it, expect, vi } from 'vitest';
import { ProfileCustomizerComponent } from './profile-customizer.component';

describe('ProfileCustomizerComponent', () => {
  it('devrait être instancié avec les valeurs par défaut', () => {
    const comp = new ProfileCustomizerComponent();
    expect(comp.themeId).toBe('neon');
    expect(comp.dailyQuestion).toBe('');
    expect(comp.bio).toBe('');
    expect(comp.isSaving).toBe(false);
    expect(comp.isSavedSuccess).toBe(false);
    expect(comp.isPro).toBe(false);
    expect(comp.themes.length).toBe(5);
  });

  it('devrait émettre themeChange lors de la sélection d un thème', () => {
    const comp = new ProfileCustomizerComponent();
    const emitSpy = vi.spyOn(comp.themeChange, 'emit');

    const selectedTheme = comp.themes[1]; // sunset
    comp.onThemeSelected(selectedTheme);

    expect(emitSpy).toHaveBeenCalledWith(selectedTheme);
  });
});
