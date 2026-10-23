import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = new ToastService();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('devrait ajouter un toast de succès', () => {
    service.success('Opération réussie');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].message).toBe('Opération réussie');
    expect(toasts[0].type).toBe('success');
  });

  it('devrait ajouter un toast d\'erreur', () => {
    service.error('Une erreur est survenue');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('error');
  });

  it('devrait supprimer un toast manuellement', () => {
    service.info('Info message');
    const id = service.toasts()[0].id;
    service.remove(id);
    expect(service.toasts().length).toBe(0);
  });

  it('devrait supprimer automatiquement un toast après 3.5 secondes', () => {
    service.success('Disparition programmée');
    expect(service.toasts().length).toBe(1);

    vi.advanceTimersByTime(3600);
    expect(service.toasts().length).toBe(0);
  });
});
