import { Injectable, signal } from '@angular/core';

export interface WhisperNotificationOptions {
  title?: string;
  body?: string;
  icon?: string;
  badge?: string;
  messageId?: string | number;
  onClick?: () => void;
  force?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  public readonly permission = signal<NotificationPermission | 'unsupported'>('default');

  constructor() {
    this.initPermissionState();
  }

  private getNotificationApi(): any {
    if (typeof window !== 'undefined' && (window as any).Notification) {
      return (window as any).Notification;
    }
    if (typeof Notification !== 'undefined') {
      return Notification;
    }
    return null;
  }

  /**
   * Initialise l'état actuel des permissions du navigateur.
   */
  private initPermissionState(): void {
    const api = this.getNotificationApi();
    if (api && api.permission) {
      this.permission.set(api.permission);
    } else {
      this.permission.set('unsupported');
    }
  }

  /**
   * Vérifie si l'API Notification est supportée.
   */
  isSupported(): boolean {
    return this.getNotificationApi() !== null;
  }

  /**
   * Vérifie si la permission de notification a été accordée.
   */
  hasPermission(): boolean {
    return this.permission() === 'granted';
  }

  /**
   * Demande la permission à l'utilisateur d'envoyer des notifications.
   */
  async requestPermission(): Promise<NotificationPermission | 'unsupported'> {
    const api = this.getNotificationApi();
    if (!api || !api.requestPermission) {
      this.permission.set('unsupported');
      return 'unsupported';
    }

    try {
      const result = await api.requestPermission();
      this.permission.set(result);
      return result;
    } catch (e) {
      console.error('Erreur lors de la demande de notification', e);
      return this.permission();
    }
  }

  /**
   * Affiche une notification lorsque l'application est en arrière-plan ou minimisée.
   */
  async showWhisperNotification(options: WhisperNotificationOptions = {}): Promise<any | null> {
    if (!this.isSupported() || !this.hasPermission()) {
      return null;
    }

    // Afficher seulement si la page est masquée/en arrière-plan (sauf si forcé)
    const isBackground = typeof document !== 'undefined' && (document.visibilityState === 'hidden' || !document.hasFocus());
    if (!isBackground && !options.force) {
      return null;
    }

    const title = options.title || 'Nouveau message secret ! 🤫';
    const notificationOptions: NotificationOptions = {
      body: options.body || 'Quelqu\'un vous a envoyé un chuchotement anonyme.',
      icon: options.icon || '/favicon.ico',
      badge: options.badge || '/favicon.ico',
      tag: options.messageId ? `whispr-msg-${options.messageId}` : 'whispr-new-message',
      data: {
        messageId: options.messageId,
        url: '/inbox'
      }
    };

    // 1. Tenter via le Service Worker si actif
    if (typeof navigator !== 'undefined' && 'serviceWorker' in navigator && navigator.serviceWorker.ready) {
      try {
        const registration = await navigator.serviceWorker.ready;
        if (registration && typeof registration.showNotification === 'function') {
          await registration.showNotification(title, notificationOptions);
          return null;
        }
      } catch (err) {
        // Fallback vers l'API Notification standard
      }
    }

    // 2. Fallback via Notification native du navigateur
    const NotificationApi = this.getNotificationApi();
    if (!NotificationApi) {
      return null;
    }

    try {
      const notification = new NotificationApi(title, notificationOptions);
      notification.onclick = (event: any) => {
        if (event && event.preventDefault) {
          event.preventDefault();
        }
        if (typeof window !== 'undefined' && window.focus) {
          window.focus();
        }
        if (options.onClick) {
          options.onClick();
        }
      };
      return notification;
    } catch (e) {
      console.warn('Impossible d\'afficher la notification native', e);
      return null;
    }
  }
}
