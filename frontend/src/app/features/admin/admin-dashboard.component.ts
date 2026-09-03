import { Component, OnInit, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../shared/components/toast/toast.service';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="admin-container animate-fade-in" style="max-width: 1200px; margin: 2rem auto; padding: 0 1rem;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; flex-wrap: wrap; gap: 1rem;">
        <h1 style="margin-bottom: 0; display: flex; align-items: center; gap: 0.5rem; color: #ef4444;">
          <svg style="width: 32px; height: 32px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
          Panneau d'Administration
        </h1>
        <button class="btn btn-glass" (click)="loadData()" [disabled]="isLoading()" style="font-size: 0.9rem; padding: 0.5rem 1rem;">
          🔄 {{ isLoading() ? 'Chargement...' : 'Rafraîchir' }}
        </button>
      </div>

      <!-- Navigation Tabs -->
      <div style="display: flex; gap: 1rem; margin-bottom: 2rem; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 1rem; overflow-x: auto;">
        <button (click)="activeTab.set('overview')" [style.color]="activeTab() === 'overview' ? '#ef4444' : 'var(--color-text)'" [style.border-bottom]="activeTab() === 'overview' ? '2px solid #ef4444' : 'none'" style="background: transparent; border: none; font-size: 1.1rem; font-weight: 600; cursor: pointer; padding: 0.5rem 1rem;">
          📊 Vue Globale
        </button>
        <button (click)="activeTab.set('users')" [style.color]="activeTab() === 'users' ? '#ef4444' : 'var(--color-text)'" [style.border-bottom]="activeTab() === 'users' ? '2px solid #ef4444' : 'none'" style="background: transparent; border: none; font-size: 1.1rem; font-weight: 600; cursor: pointer; padding: 0.5rem 1rem;">
          👥 Utilisateurs
        </button>
        <button (click)="activeTab.set('messages')" [style.color]="activeTab() === 'messages' ? '#ef4444' : 'var(--color-text)'" [style.border-bottom]="activeTab() === 'messages' ? '2px solid #ef4444' : 'none'" style="background: transparent; border: none; font-size: 1.1rem; font-weight: 600; cursor: pointer; padding: 0.5rem 1rem;">
          💬 Messages
        </button>
      </div>

      <div *ngIf="isLoading()" style="text-align: center; color: var(--color-text-muted); padding: 3rem;">
        Chargement des données administrateur...
      </div>

      <ng-container *ngIf="!isLoading()">
        
        <!-- ONGLET : VUE GLOBALE -->
        <div [hidden]="activeTab() !== 'overview'" class="animate-fade-in">
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

          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; margin-bottom: 3rem;">
            <div class="glass-panel" style="padding: 1.5rem;">
              <h3 style="margin-top: 0; color: var(--color-text-muted);">Inscriptions (30 derniers jours)</h3>
              <canvas #usersChart></canvas>
            </div>
            <div class="glass-panel" style="padding: 1.5rem;">
              <h3 style="margin-top: 0; color: var(--color-text-muted);">Santé de la Plateforme</h3>
              <canvas #messagesChart></canvas>
            </div>
          </div>

          <div class="glass-panel" style="padding: 2rem;">
            <h2 style="margin-bottom: 1.5rem; font-size: 1.25rem;">Journaux d'Audit (Sécurité avancée)</h2>
            <div style="overflow-x: auto;">
              <table style="width: 100%; border-collapse: collapse; text-align: left;">
                <thead>
                  <tr style="border-bottom: 1px solid rgba(255,255,255,0.1);">
                    <th style="padding: 1rem; color: var(--color-text-muted);">Cible</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Identifiant Réseau (IP)</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Localisation & Appareil</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Date</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let log of auditLogs()" class="row-hover" style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <td style="padding: 1rem; font-weight: 600;">@{{ log.targetUser }}</td>
                    <td style="padding: 1rem; font-family: monospace; color: var(--color-text-muted); font-size: 0.8rem;">
                      <div style="color: #ef4444; font-weight: bold; font-size: 0.95rem;">{{ log.rawIp || 'IP Inconnue' }}</div>
                      <div style="font-size: 0.7rem; color: rgba(255,255,255,0.3); word-break: break-all;" title="Hashed IP">{{ log.hashedIp }}</div>
                    </td>
                    <td style="padding: 1rem;">
                      <span style="background: rgba(255,255,255,0.05); padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.8rem;">
                        📍 {{ log.country || 'Inconnue' }}
                      </span>
                      <div style="margin-top: 0.5rem; font-size: 0.75rem; color: var(--color-text-muted); max-width: 250px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" [title]="log.userAgent">
                        🖥️ {{ log.userAgent || 'Appareil Inconnu' }}
                      </div>
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
        </div>

        <!-- ONGLET : UTILISATEURS -->
        <div [hidden]="activeTab() !== 'users'" class="animate-fade-in">
          <div class="glass-panel" style="padding: 2rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
              <h2 style="margin: 0; font-size: 1.25rem;">Gestion des Utilisateurs</h2>
              <button *ngIf="selectedUsers().length > 0" (click)="deleteSelectedUsers()" class="btn" style="background: rgba(239, 68, 68, 0.2); border: 1px solid #ef4444; color: #ef4444;">
                🗑️ Supprimer la sélection ({{ selectedUsers().length }})
              </button>
            </div>
            <div style="overflow-x: auto;">
              <table style="width: 100%; border-collapse: collapse; text-align: left;">
                <thead>
                  <tr style="border-bottom: 1px solid rgba(255,255,255,0.1);">
                    <th style="padding: 1rem; width: 40px;">
                      <input type="checkbox" (change)="toggleAllUsers($event)" [checked]="users().length > 0 && selectedUsers().length === users().length">
                    </th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Pseudo</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Email</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Rôle</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Inscription</th>
                    <th style="padding: 1rem; color: var(--color-text-muted); text-align: right;">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let user of users()" class="row-hover" style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <td style="padding: 1rem;">
                      <input type="checkbox" [checked]="selectedUsers().includes(user.pseudo)" (change)="toggleUser(user.pseudo)">
                    </td>
                    <td style="padding: 1rem; font-weight: 600;">@{{ user.pseudo }}</td>
                    <td style="padding: 1rem; color: var(--color-text-muted);">{{ user.email }}</td>
                    <td style="padding: 1rem;">
                      <span *ngIf="user.role === 'ADMIN'" style="color: #ef4444; background: rgba(255,255,255,0.05); padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.8rem;">ADMIN</span>
                      <span *ngIf="user.role === 'USER'" style="color: #10b981; background: rgba(255,255,255,0.05); padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.8rem;">USER</span>
                      <span *ngIf="user.role === 'BANNED'" style="color: #f59e0b; background: rgba(245,158,11,0.1); padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.8rem; font-weight: bold;">BANNED</span>
                    </td>
                    <td style="padding: 1rem; color: var(--color-text-muted); font-size: 0.9rem;">
                      {{ user.createdAt | date:'short' }}
                    </td>
                    <td style="padding: 1rem; text-align: right; display: flex; gap: 0.5rem; justify-content: flex-end; flex-wrap: wrap;">
                      <button (click)="viewUserMessages(user.pseudo)" class="btn" style="background: rgba(59, 130, 246, 0.2); border: 1px solid rgba(59, 130, 246, 0.4); color: #3b82f6; padding: 0.4rem 0.8rem; font-size: 0.85rem;" title="Filtrer les messages">
                        🔍 Messages
                      </button>
                      <button *ngIf="user.role !== 'ADMIN'" (click)="resetPassword(user.pseudo)" class="btn" style="background: rgba(139, 92, 246, 0.2); border: 1px solid rgba(139, 92, 246, 0.4); color: #8b5cf6; padding: 0.4rem 0.8rem; font-size: 0.85rem;" title="Générer un nouveau mot de passe">
                        🔑 Reset
                      </button>
                      <button *ngIf="user.role !== 'ADMIN' && user.role !== 'BANNED'" (click)="banUser(user.pseudo)" class="btn" style="background: rgba(245, 158, 11, 0.2); border: 1px solid rgba(245, 158, 11, 0.4); color: #f59e0b; padding: 0.4rem 0.8rem; font-size: 0.85rem;">
                        🚫 Bannir
                      </button>
                      <button *ngIf="user.role === 'BANNED'" (click)="unbanUser(user.pseudo)" class="btn" style="background: rgba(16, 185, 129, 0.2); border: 1px solid rgba(16, 185, 129, 0.4); color: #10b981; padding: 0.4rem 0.8rem; font-size: 0.85rem;">
                        ✅ Débannir
                      </button>
                      <button *ngIf="user.role !== 'ADMIN'" (click)="deleteUser(user.pseudo)" class="btn" style="background: transparent; border: 1px solid rgba(239, 68, 68, 0.4); color: #ef4444; padding: 0.4rem 0.8rem; font-size: 0.85rem;" title="Supprimer définitivement le compte">
                        🗑️
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div *ngIf="users().length === 0" style="text-align: center; color: var(--color-text-muted); padding: 2rem;">
              Aucun utilisateur trouvé.
            </div>
          </div>
        </div>

        <!-- ONGLET : MESSAGES -->
        <div [hidden]="activeTab() !== 'messages'" class="animate-fade-in">
          <div class="glass-panel" style="padding: 2rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
              <h2 style="margin: 0; font-size: 1.25rem;">Modération des Messages</h2>
              <div style="display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;">
                <button *ngIf="selectedMessages().length > 0" (click)="deleteSelectedMessages()" class="btn" style="background: rgba(239, 68, 68, 0.2); border: 1px solid #ef4444; color: #ef4444;">
                  🗑️ Supprimer la sélection ({{ selectedMessages().length }})
                </button>
                <input type="text" [(ngModel)]="searchPseudo" placeholder="🔍 Filtrer par pseudo cible..." style="padding: 0.5rem 1rem; border-radius: var(--radius-md); background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: white; outline: none; min-width: 250px;">
              </div>
            </div>
            
            <div style="overflow-x: auto;">
              <table style="width: 100%; border-collapse: collapse; text-align: left;">
                <thead>
                  <tr style="border-bottom: 1px solid rgba(255,255,255,0.1);">
                    <th style="padding: 1rem; width: 40px;">
                      <input type="checkbox" (change)="toggleAllMessages($event)" [checked]="filteredMessages().length > 0 && selectedMessages().length === filteredMessages().length">
                    </th>
                    <th style="padding: 1rem; color: var(--color-text-muted); width: 40%;">Contenu</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Destinataire</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Toxique ?</th>
                    <th style="padding: 1rem; color: var(--color-text-muted);">Date</th>
                    <th style="padding: 1rem; color: var(--color-text-muted); text-align: right;">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let msg of filteredMessages()" class="row-hover" style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                    <td style="padding: 1rem;">
                      <input type="checkbox" [checked]="selectedMessages().includes(msg.id)" (change)="toggleMessage(msg.id)">
                    </td>
                    <td style="padding: 1rem; font-style: italic; color: white;">"{{ msg.content }}"</td>
                    <td style="padding: 1rem; font-weight: 600;">@{{ msg.targetUser }}</td>
                    <td style="padding: 1rem;">
                      <span *ngIf="msg.isToxic" style="color: #ef4444; background: rgba(239, 68, 68, 0.1); padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.8rem; font-weight: bold;">
                        ⚠️ OUI
                      </span>
                      <span *ngIf="!msg.isToxic" style="color: #10b981; font-size: 0.8rem;">
                        NON
                      </span>
                    </td>
                    <td style="padding: 1rem; color: var(--color-text-muted); font-size: 0.9rem;">
                      {{ msg.createdAt | date:'short' }}
                    </td>
                    <td style="padding: 1rem; text-align: right;">
                      <button (click)="deleteMessage(msg.id)" class="btn" style="background: rgba(239, 68, 68, 0.2); border: 1px solid rgba(239, 68, 68, 0.4); color: #ef4444; padding: 0.4rem 0.8rem; font-size: 0.85rem;">
                        Supprimer
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div *ngIf="filteredMessages().length === 0" style="text-align: center; color: var(--color-text-muted); padding: 2rem;">
              Aucun message trouvé pour ce filtre.
            </div>
          </div>
        </div>

      </ng-container>
    </div>
  `
})
export class AdminDashboardComponent implements OnInit {
  activeTab = signal<'overview' | 'users' | 'messages'>('overview');
  searchPseudo = signal<string>('');
  
  stats = signal<any>({});
  users = signal<any[]>([]);
  messages = signal<any[]>([]);
  auditLogs = signal<any[]>([]);
  isLoading = signal(true);

  // Bulk selections
  selectedUsers = signal<string[]>([]);
  selectedMessages = signal<string[]>([]);

  @ViewChild('usersChart') usersChartRef!: ElementRef;
  @ViewChild('messagesChart') messagesChartRef!: ElementRef;
  
  private chartInstance1: Chart | null = null;
  private chartInstance2: Chart | null = null;

  filteredMessages = computed(() => {
    const term = this.searchPseudo().toLowerCase().trim();
    if (!term) return this.messages();
    return this.messages().filter(m => m.targetUser.toLowerCase().includes(term));
  });

  constructor(private apiService: ApiService, private toastService: ToastService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.apiService.getAdminStats().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.loadUsersAndMessages();
      },
      error: () => this.isLoading.set(false)
    });
  }

  private loadUsersAndMessages(): void {
    this.apiService.getAdminUsers().subscribe({
      next: (u) => {
        this.users.set(u);
        this.apiService.getAdminMessages().subscribe({
          next: (m) => {
            this.messages.set(m);
            this.apiService.getAdminAuditLogs().subscribe({
              next: (logs) => {
                this.auditLogs.set(logs);
                this.isLoading.set(false);
                this.selectedUsers.set([]);
                this.selectedMessages.set([]);
                setTimeout(() => this.renderCharts(), 100);
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

  viewUserMessages(pseudo: string): void {
    this.searchPseudo.set(pseudo);
    this.activeTab.set('messages');
  }

  banUser(pseudo: string): void {
    if (confirm(`Voulez-vous suspendre temporairement le compte de @${pseudo} ? (Il ne pourra plus se connecter)`)) {
      this.apiService.banAdminUser(pseudo).subscribe({
        next: () => this.loadData(),
        error: () => this.toastService.error('Erreur lors du bannissement.')
      });
    }
  }

  unbanUser(pseudo: string): void {
    if (confirm(`Voulez-vous restaurer l'accès de @${pseudo} ?`)) {
      this.apiService.unbanAdminUser(pseudo).subscribe({
        next: () => this.loadData(),
        error: () => alert('Erreur lors du débannissement.')
      });
    }
  }

  resetPassword(pseudo: string): void {
    if (confirm(`Voulez-vous générer un NOUVEAU mot de passe pour @${pseudo} ?`)) {
      this.apiService.resetAdminUserPassword(pseudo).subscribe({
        next: (res) => {
          alert(`Nouveau mot de passe généré pour @${pseudo} :\n\n${res.newPassword}\n\nVeuillez le transmettre à l'utilisateur.`);
          this.loadData();
        },
        error: () => alert('Erreur lors de la réinitialisation du mot de passe.')
      });
    }
  }

  deleteUser(pseudo: string): void {
    if (confirm(`ATTENTION: Êtes-vous sûr de vouloir SUPPRIMER DÉFINITIVEMENT @${pseudo} ainsi que toutes ses données ?`)) {
      this.apiService.deleteAdminUser(pseudo).subscribe({
        next: () => {
          alert('Utilisateur supprimé avec succès.');
          this.loadData();
        },
        error: () => this.toastService.error('Erreur de suppression.')
      });
    }
  }

  deleteMessage(id: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer définitivement ce message ?')) {
      this.apiService.deleteAdminMessage(id).subscribe({
        next: () => this.loadData(),
        error: () => this.toastService.error('Erreur de suppression.')
      });
    }
  }

  // --- Bulk Actions ---
  toggleAllUsers(event: any) {
    if (event.target.checked) {
      this.selectedUsers.set(this.users().map(u => u.pseudo));
    } else {
      this.selectedUsers.set([]);
    }
  }

  toggleUser(pseudo: string) {
    const current = this.selectedUsers();
    if (current.includes(pseudo)) {
      this.selectedUsers.set(current.filter(p => p !== pseudo));
    } else {
      this.selectedUsers.set([...current, pseudo]);
    }
  }

  deleteSelectedUsers() {
    const toDelete = this.selectedUsers();
    if (confirm(`Êtes-vous sûr de vouloir supprimer définitivement ${toDelete.length} utilisateur(s) ?`)) {
      this.apiService.deleteAdminUsersBulk(toDelete).subscribe({
        next: () => {
          alert('Utilisateurs supprimés.');
          this.loadData();
        },
        error: () => this.toastService.error('Erreur lors de la suppression.')
      });
    }
  }

  toggleAllMessages(event: any) {
    if (event.target.checked) {
      this.selectedMessages.set(this.filteredMessages().map(m => m.id));
    } else {
      this.selectedMessages.set([]);
    }
  }

  toggleMessage(id: string) {
    const current = this.selectedMessages();
    if (current.includes(id)) {
      this.selectedMessages.set(current.filter(i => i !== id));
    } else {
      this.selectedMessages.set([...current, id]);
    }
  }

  deleteSelectedMessages() {
    const toDelete = this.selectedMessages();
    if (confirm(`Êtes-vous sûr de vouloir supprimer définitivement ${toDelete.length} message(s) ?`)) {
      this.apiService.deleteAdminMessagesBulk(toDelete).subscribe({
        next: () => {
          alert('Messages supprimés.');
          this.loadData();
        },
        error: () => this.toastService.error('Erreur lors de la suppression.')
      });
    }
  }

  // --- Charts Logic ---
  private renderCharts() {
    if (!this.usersChartRef || !this.messagesChartRef) return;

    if (this.chartInstance1) this.chartInstance1.destroy();
    if (this.chartInstance2) this.chartInstance2.destroy();

    // Chart 1: Users registered in the last 30 days
    const usersData = this.users();
    const datesMap = new Map<string, number>();
    
    // Initialize last 30 days with 0
    for (let i = 29; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateString = d.toISOString().split('T')[0];
      datesMap.set(dateString, 0);
    }

    usersData.forEach(u => {
      if (u.createdAt) {
        const dateString = u.createdAt.split('T')[0];
        if (datesMap.has(dateString)) {
          datesMap.set(dateString, datesMap.get(dateString)! + 1);
        }
      }
    });

    this.chartInstance1 = new Chart(this.usersChartRef.nativeElement, {
      type: 'line',
      data: {
        labels: Array.from(datesMap.keys()).map(d => d.slice(5)), // MM-DD
        datasets: [{
          label: 'Nouveaux utilisateurs',
          data: Array.from(datesMap.values()),
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.2)',
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: { 
          y: { beginAtZero: true, ticks: { precision: 0, color: 'rgba(255,255,255,0.5)' } },
          x: { ticks: { color: 'rgba(255,255,255,0.5)' } }
        }
      }
    });

    // Chart 2: Toxicity distribution
    const msgs = this.messages();
    const toxicCount = msgs.filter(m => m.isToxic).length;
    const safeCount = msgs.length - toxicCount;

    this.chartInstance2 = new Chart(this.messagesChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Sains', 'Toxiques'],
        datasets: [{
          data: [safeCount, toxicCount],
          backgroundColor: ['#10b981', '#ef4444'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'bottom', labels: { color: 'white' } }
        }
      }
    });
  }
}

