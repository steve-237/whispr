import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

export interface AuthResponse {
  token: string;
  pseudo: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';
  
  public isAuthenticated = signal<boolean>(this.hasToken());
  public currentUser = signal<string | null>(this.getPseudo());
  public userRole = signal<string | null>(this.getRole());

  constructor(private http: HttpClient, private router: Router) {}

  register(email: string, pseudo: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, { email, pseudo, password }).pipe(
      tap(response => this.handleAuthentication(response))
    );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(response => this.handleAuthentication(response))
    );
  }

  logout(): void {
    localStorage.removeItem('whispr_token');
    localStorage.removeItem('whispr_pseudo');
    localStorage.removeItem('whispr_role');
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
    this.userRole.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('whispr_token');
  }

  isAdmin(): boolean {
    return this.userRole() === 'ADMIN';
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('whispr_token');
  }

  private getPseudo(): string | null {
    return localStorage.getItem('whispr_pseudo');
  }

  private getRole(): string | null {
    return localStorage.getItem('whispr_role');
  }

  private handleAuthentication(response: AuthResponse): void {
    localStorage.setItem('whispr_token', response.token);
    localStorage.setItem('whispr_pseudo', response.pseudo);
    if (response.role) {
      localStorage.setItem('whispr_role', response.role);
    }
    this.isAuthenticated.set(true);
    this.currentUser.set(response.pseudo);
    this.userRole.set(response.role || 'USER');
  }
}
