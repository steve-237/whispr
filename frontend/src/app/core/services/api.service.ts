import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LinkDto {
  id: string;
  slug: string;
  isActive: boolean;
  profileBio: string;
  profileAvatarUrl: string;
  profileThemeId: string;
  profileDailyQuestion: string;
}

export interface WalletDto {
  coins: number;
  totalSpentEur: number;
  isPro: boolean;
  proExpiresAt?: string;
  earningsEur?: number;
  referralCount?: number;
  affiliateCode?: string;
}

export interface ClueResponseDto {
  clueType: string;
  clueValue: string;
  isUnlocked: boolean;
}

export interface MessageSendRequest {
  content: string;
  type: string;
}

export interface MessageDto {
  id: string;
  content: string;
  type: string;
  status: string;
  createdAt: string;
  country?: string;
  deviceHint?: string;
  isRead?: boolean;
  aiCategory?: string;
}

export interface ProfileUpdateRequest {
  bio?: string;
  dailyQuestion?: string;
  themeId?: string;
  isActive?: boolean;
}

export interface StatsDto {
  totalMessages: number;
  positiveCount: number;
  neutralCount: number;
  negativeCount: number;
  aiSummary: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  checkHealth(): Observable<any> {
    return this.http.get(`${this.API_URL}/health`);
  }


  getLinkInfo(slug: string): Observable<LinkDto> {
    return this.http.get<LinkDto>(`${this.API_URL}/links/${slug}`);
  }

  sendMessage(slug: string, content: string, type: string = 'text'): Observable<void> {
    const request: MessageSendRequest = { content, type };
    return this.http.post<void>(`${this.API_URL}/messages/send/${slug}`, request);
  }

  // --- Monétisation & Simulation Stripe ---
  getWallet(): Observable<WalletDto> {
    return this.http.get<WalletDto>(`${this.API_URL}/monetization/wallet`);
  }

  simulateCheckout(packId: string): Observable<WalletDto> {
    return this.http.post<WalletDto>(`${this.API_URL}/monetization/checkout-simulate`, { packId });
  }

  getCluesForMessage(messageId: string): Observable<ClueResponseDto[]> {
    return this.http.get<ClueResponseDto[]>(`${this.API_URL}/monetization/clues/${messageId}`);
  }

  unlockClue(messageId: string, clueType: string): Observable<ClueResponseDto> {
    return this.http.post<ClueResponseDto>(`${this.API_URL}/monetization/unlock-clue`, { messageId, clueType });
  }

  getInbox(): Observable<MessageDto[]> {
    return this.http.get<MessageDto[]>(`${this.API_URL}/messages/inbox`);
  }

  deleteMessage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/messages/${id}`);
  }

  updateMyProfile(request: ProfileUpdateRequest): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/profiles/my`, request);
  }

  getMyStats(): Observable<StatsDto> {
    return this.http.get<StatsDto>(`${this.API_URL}/stats/my`);
  }

  getAdminStats(): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/admin/stats`);
  }

  getAdminUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/admin/users`);
  }

  deleteAdminUser(pseudo: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/admin/users/${pseudo}`);
  }

  getAdminMessages(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/admin/messages`);
  }

  deleteAdminMessage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/admin/messages/${id}`);
  }

  getAdminAuditLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/admin/audit-logs`);
  }

  banAdminUser(pseudo: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/admin/users/${pseudo}/ban`, {});
  }

  unbanAdminUser(pseudo: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/admin/users/${pseudo}/unban`, {});
  }

  resetAdminUserPassword(pseudo: string): Observable<{newPassword: string}> {
    return this.http.post<{newPassword: string}>(`${this.API_URL}/admin/users/${pseudo}/reset-password`, {});
  }

  deleteAdminUsersBulk(pseudonyms: string[]): Observable<void> {
    return this.http.request<void>('delete', `${this.API_URL}/admin/users/bulk`, { body: pseudonyms });
  }

  deleteAdminMessagesBulk(ids: string[]): Observable<void> {
    return this.http.request<void>('delete', `${this.API_URL}/admin/messages/bulk`, { body: ids });
  }

  markAsRead(messageId: string): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/messages/${messageId}/read`, {});
  }
}




