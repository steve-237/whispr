import { describe, it, expect, vi, beforeEach } from 'vitest';
import { of } from 'rxjs';
import { AuthService, AuthResponse } from './auth.service';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

describe('AuthService', () => {
  let service: AuthService;
  let httpClientMock: any;
  let routerMock: any;
  let storage: Record<string, string> = {};

  beforeEach(() => {
    storage = {};
    const localStorageMock = {
      getItem: vi.fn((key: string) => storage[key] || null),
      setItem: vi.fn((key: string, value: string) => {
        storage[key] = value;
      }),
      removeItem: vi.fn((key: string) => {
        delete storage[key];
      }),
      clear: vi.fn(() => {
        storage = {};
      })
    };
    vi.stubGlobal('localStorage', localStorageMock);

    httpClientMock = {
      post: vi.fn()
    };
    routerMock = {
      navigate: vi.fn()
    };

    service = new AuthService(
      httpClientMock as unknown as HttpClient,
      routerMock as unknown as Router
    );
  });

  it('devrait être initialisé non connecté si pas de token', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.isAdmin()).toBe(false);
  });

  it('devrait gérer la connexion avec succès', () => {
    const mockResponse: AuthResponse = {
      token: 'jwt-token-123',
      pseudo: 'alice',
      role: 'USER'
    };
    httpClientMock.post.mockReturnValue(of(mockResponse));

    service.login('alice@example.com', 'password123').subscribe();

    expect(httpClientMock.post).toHaveBeenCalled();
    expect(localStorage.setItem).toHaveBeenCalledWith('whispr_token', 'jwt-token-123');
    expect(localStorage.setItem).toHaveBeenCalledWith('whispr_pseudo', 'alice');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()).toBe('alice');
    expect(service.isAdmin()).toBe(false);
  });

  it('devrait identifier un compte administrateur', () => {
    const adminResponse: AuthResponse = {
      token: 'admin-jwt',
      pseudo: 'adminUser',
      role: 'ADMIN'
    };
    httpClientMock.post.mockReturnValue(of(adminResponse));

    service.login('admin@whispr.com', 'adminPass').subscribe();

    expect(service.isAdmin()).toBe(true);
  });

  it('devrait vider les signaux et rediriger lors de la déconnexion', () => {
    storage['whispr_token'] = 'token';
    storage['whispr_pseudo'] = 'user';
    storage['whispr_role'] = 'USER';

    service.logout();

    expect(localStorage.removeItem).toHaveBeenCalledWith('whispr_token');
    expect(localStorage.removeItem).toHaveBeenCalledWith('whispr_pseudo');
    expect(localStorage.removeItem).toHaveBeenCalledWith('whispr_role');
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });
});
