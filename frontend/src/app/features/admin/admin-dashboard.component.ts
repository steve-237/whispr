import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-container animate-fade-in" style="max-width: 1000px; margin: 2rem auto; padding: 0 1rem;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; flex-wrap: wrap; gap: 1rem;">
        <h1 style="margin-bottom: 0; display: flex; align-items: center; gap: 0.5rem; color: #ef4444;">
          <svg style="width: 32px; height: 32px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
          Panneau d'Administration
        </h1>
        <button class="btn btn-glass" (click)="loadData()" [disabled]="isLoading()" style="font-size: 0.9rem; padding: 0.5rem 1rem;">
          🔄 {{ isLoading() ? 'Chargement...' : 'Rafraîchir' }}
        </button>
      </div>

      <div *ngIf="isLoading()" style="text-align: center; color: var(--color-text-muted); padding: 3rem;">
        Chargement des données administrateur...
      </div>

      <ng-container *ngIf="!isLoading()">
        
        <!-- Stats Cards -->
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; margin-bottom: 3rem;">
          <div class="glass-panel card-hover" style="padding: 1.5rem; text-align: center;">
            <div style="font-size: 3rem; font-weight: bold; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
              {{ stats().totalUsers || 0 }}
            </div>
            <div style="color: var(--color-text-muted); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 1px;">Utilisateurs</div>
          </div>
          <div class="glass-panel card-hover" style="padding: 1.5rem; text-align: center;">
            <div style="font-size: 3rem; font-weight: bold; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
              {{ stats().totalProfiles || 0 }}
            </div>
            <div style="color: var(--color-text-muted); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 1px;">Profils (Liens)</div>
          </div>
          <div class="glass-panel card-hover" style="padding: 1.5rem; text-align: center;">
            <div style="font-size: 3rem; font-weight: bold; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
              {{ stats().totalMessages || 0 }}
            </div>
            <div style="color: var(--color-text-muted); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 1px;">Messages Envoyés</div>
          </div>
        </div>

        <!-- Users Table -->
        <div class="glass-panel" style="padding: 2rem;">
          <h2 style="margin-bottom: 1.5rem; font-size: 1.25rem;">Utilisateurs Inscrits</h2>
          
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; text-align: left;">
              <thead>
                <tr style="border-bottom: 1px solid rgba(255,255,255,0.1);">
                  <th style="padding: 1rem; color: var(--color-text-muted);">Pseudo</th>
                  <th style="padding: 1rem; color: var(--color-text-muted);">Email</th>
                  <th style="padding: 1rem; color: var(--color-text-muted);">Rôle</th>
                  <th style="padding: 1rem; color: var(--color-text-muted);">Inscription</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let user of users()" class="row-hover" style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                  <td style="padding: 1rem; font-weight: 600;">{{ user.pseudo }}</td>
                  <td style="padding: 1rem; color: var(--color-text-muted);">{{ user.email }}</td>
                  <td style="padding: 1rem;">
                    <span [style.color]="user.role === 'ADMIN' ? '#ef4444' : '#10b981'" style="background: rgba(255,255,255,0.05); padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.8rem;">
                      {{ user.role }}
                    </span>
                  </td>
                  <td style="padding: 1rem; color: var(--color-text-muted); font-size: 0.9rem;">
                    {{ user.createdAt | date:'short' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div *ngIf="users().length === 0" style="text-align: center; color: var(--color-text-muted); padding: 2rem;">
            Aucun utilisateur trouvé.
          </div>
        </div>

        <!-- Audit Logs Table -->
        <div class="glass-panel" style="padding: 2rem; margin-top: 3rem;">
          <h2 style="margin-bottom: 1.5rem; font-size: 1.25rem;">Journaux d'Audit (Sécurité)</h2>
          
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; text-align: left;">
              <thead>
                <tr style="border-bottom: 1px solid rgba(255,255,255,0.1);">
                  <th style="padding: 1rem; color: var(--color-text-muted);">Cible</th>
                  <th style="padding: 1rem; color: var(--color-text-muted);">IP (Hachée)</th>
                  <th style="padding: 1rem; color: var(--color-text-muted);">Localisation</th>
                  <th style="padding: 1rem; color: var(--color-text-muted);">Date</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let log of auditLogs()" class="row-hover" style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                  <td style="padding: 1rem; font-weight: 600;">@{{ log.targetUser }}</td>
                  <td style="padding: 1rem; font-family: monospace; color: var(--color-text-muted); font-size: 0.8rem;">
                    {{ log.hashedIp }}
                  </td>
                  <td style="padding: 1rem;">
                    <span style="background: rgba(255,255,255,0.05); padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.8rem;">
                      📍 {{ log.country || 'Inconnue' }}
                    </span>
                  </td>
                  <td style="padding: 1rem; color: var(--color-text-muted); font-size: 0.9rem;">
                    {{ log.createdAt | date:'short' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div *ngIf="auditLogs().length === 0" style="text-align: center; color: var(--color-text-muted); padding: 2rem;">
            Aucun journal d'audit trouvé.
          </div>
        </div>

      </ng-container>
    </div>
  `
})
export class AdminDashboardComponent implements OnInit {
  stats = signal<any>({});
  users = signal<any[]>([]);
  auditLogs = signal<any[]>([]);
  isLoading = signal(true);

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.apiService.getAdminStats().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.apiService.getAdminUsers().subscribe({
          next: (u) => {
            this.users.set(u);
            this.apiService.getAdminAuditLogs().subscribe({
              next: (logs) => {
                this.auditLogs.set(logs);
                this.isLoading.set(false);
              },
              error: () => this.isLoading.set(false)
            });
          },
          error: () => this.isLoading.set(false)
        });
      },
      error: () => this.isLoading.set(false)
    });
  }
}
