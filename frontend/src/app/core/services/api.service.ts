import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LinkDto {
  id: string;
  slug: string;
  isActive: boolean;
  profileBio: string;
  profileAvatarUrl: string;
  profileThemeId: string;
  profileDailyQuestion: string;
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
}

export interface ProfileUpdateRequest {
  bio?: string;
  dailyQuestion?: string;
  themeId?: string;
  isActive?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly API_URL = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getLinkInfo(slug: string): Observable<LinkDto> {
    return this.http.get<LinkDto>(`${this.API_URL}/links/${slug}`);
  }

  sendMessage(slug: string, content: string): Observable<void> {
    const request: MessageSendRequest = { content, type: 'text' };
    return this.http.post<void>(`${this.API_URL}/messages/send/${slug}`, request);
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

  getAdminStats(): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/admin/stats`);
  }

  getAdminUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/admin/users`);
  }

  getAdminAuditLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/admin/audit-logs`);
  }
}
