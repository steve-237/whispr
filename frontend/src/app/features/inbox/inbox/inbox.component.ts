import { Component, OnInit, signal, OnDestroy, AfterViewInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, MessageDto, StatsDto } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import html2canvas from 'html2canvas';
import { Client } from '@stomp/stompjs';
import { environment } from '../../../../environments/environment';
import Chart from 'chart.js/auto';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OnboardingComponent } from '../onboarding/onboarding.component';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Title } from '@angular/platform-browser';
import { WalletService } from '../../../shared/components/shop/wallet.service';
import { ClueResponseDto, AdvertisementDto, VisitorAnalyticsDto } from '../../../core/services/api.service';

@Component({
  selector: 'app-inbox',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, TranslatePipe, OnboardingComponent],
  templateUrl: './inbox.component.html'
})
export class InboxComponent implements OnInit, OnDestroy, AfterViewInit {
  walletService = inject(WalletService);
  showOnboarding = signal(false);
  messages = signal<MessageDto[]>([]);
  isLoading = signal(true);
  error = signal('');
  pseudo = signal('');
  messageToDelete = signal<string | null>(null);
  isDeleting = signal(false);
  isCopied = signal(false);

  // Vraies Analytics Visiteurs & Provenance
  visitorAnalytics = signal<VisitorAnalyticsDto | null>(null);
  topSourceLabel = signal<string>('Direct (100%)');
  topCityLabel = signal<string>('Paris (100%)');

  // Publicités pour les non-PRO
  ads = signal<AdvertisementDto[]>([]);
  currentAd = signal<AdvertisementDto | null>(null);

  // Indices secrets par message
  openedCluesMessageId = signal<string | null>(null);
  messageClues = signal<{ [msgId: string]: ClueResponseDto[] }>({});
  isUnlockingClue = signal(false);

  // Customization
  showCustomization = signal(false);
  showSettings = signal(false);
  profileBio = signal('');
  profileDailyQuestion = signal('');
  profileThemeId = signal('neon');
  isSavingProfile = signal(false);
  profileSavedSuccess = signal(false);
  
  // Dashboard IA Stats
  showStats = signal(false);
  stats = signal<StatsDto | null>(null);
  chart: any = null;

  // Story
  messageToCapture = signal<MessageDto | null>(null);
  isCapturing = signal(false);

  highlightedMessageId = signal<string | null>(null);
  revealedHints = signal<{ [key: string]: boolean }>({});

  revealHint(msgId: string): void {
    this.revealedHints.update(h => ({ ...h, [msgId]: true }));
  }

  // Réponses pour les Stories
  replyTexts = signal<{ [key: string]: string }>({});

  quickQuestions = signal<string[]>([
    'INBOX.Q1',
    'INBOX.Q2',
    'INBOX.Q3',
    'INBOX.Q4',
    'INBOX.Q5'
  ]);

  private stompClient: Client | null = null;

  constructor(
    private apiService: ApiService, 
    private authService: AuthService,
    public translate: TranslateService,
    private toastService: ToastService,
    private titleService: Title
  ) {
    this.pseudo.set(this.authService.currentUser() || '');
  }

  ngOnInit(): void {
    if (!localStorage.getItem('whispr_onboarding_v1')) {
      this.showOnboarding.set(true);
    }
    this.walletService.loadWallet();
    this.loadMessages();
    this.loadProfileInfo();
    this.loadStats();
    this.loadVisitorAnalytics();
    this.loadAds();
    this.initWebSocket();
  }

  ngAfterViewInit(): void {
    // Chart is initialized in toggleStats when view becomes visible
  }

  ngOnDestroy(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
    if (this.chart) {
      this.chart.destroy();
    }
  }

  initWebSocket(): void {
    // Assuming backend runs on 8081
    const email = this.authService.currentUser() || this.pseudo(); 
    // Wait, the JWT token holds the email, but since I don't have an email in authService out of the box in frontend,
    // let's just listen to `/topic/user/${email}/messages`.
    // We can extract email from token if needed, or we just rely on pseudo if they are same.
    // In our JWT, email is the subject. We can grab it from localStorage token.
    const token = localStorage.getItem('whispr_token');
    let emailFromToken = this.pseudo();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        emailFromToken = payload.sub; // subject is email
      } catch(e) {}
    }

    this.stompClient = new Client({
      brokerURL: environment.wsUrl.replace('http', 'ws'),
      debug: function (str) {
        console.log('[STOMP] ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connecté au WebSocket', frame);
      this.stompClient?.subscribe(`/topic/user/${emailFromToken}/messages`, (message) => {
        if (message.body) {
          const newMessage: MessageDto = JSON.parse(message.body);
          
          // Ajouter dynamiquement en haut de la liste
          this.messages.update(msgs => [newMessage, ...msgs]);
          
          // Mettre en surbrillance pendant 3 secondes
          this.highlightedMessageId.set(newMessage.id);
          setTimeout(() => {
            if (this.highlightedMessageId() === newMessage.id) {
              this.highlightedMessageId.set(null);
            }
          }, 3000);
          
          // Jouer un son (optionnel, on utilise un bip natif court)
          try {
             const audio = new Audio('https://www.soundjay.com/buttons/sounds/button-14.mp3');
             audio.volume = 0.5;
             audio.play().catch(e => console.log('Audio non lu automatiquement', e));
          } catch(e) {}

          // Notification Push du navigateur
          if ('Notification' in window && Notification.permission === 'granted') {
            try {
              new Notification('Nouveau message secret sur Whispr ! 🤫', {
                body: newMessage.content.length > 50 ? newMessage.content.substring(0, 50) + '...' : newMessage.content,
                icon: '/favicon.ico'
              });
            } catch(e) {}
          } else if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
          }

          // Rafraîchir les stats
          this.loadStats();
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Erreur Broker STOMP: ' + frame.headers['message']);
    };

    this.stompClient.activate();
  }

  updateTitleBadge(): void {
    const unreadCount = this.messages().filter(m => m.status === 'UNREAD').length;
    if (unreadCount > 0) {
      this.titleService.setTitle(`(${unreadCount}) Whispr - Anonymous Messages`);
    } else {
      this.titleService.setTitle('Whispr - Anonymous Messages');
    }
  }

  closeOnboarding(): void {
    localStorage.setItem('whispr_onboarding_v1', 'done');
    this.showOnboarding.set(false);
  }

  loadMessages(): void {
    this.apiService.getInbox().subscribe({
      next: (data) => {
        this.messages.set(data);
        this.updateTitleBadge();
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les messages.');
        this.isLoading.set(false);
      }
    });
  }

  loadProfileInfo(): void {
    if (!this.pseudo()) return;
    this.apiService.getLinkInfo(this.pseudo()).subscribe({
      next: (data) => {
        if (data.profileBio) this.profileBio.set(data.profileBio);
        if (data.profileDailyQuestion) this.profileDailyQuestion.set(data.profileDailyQuestion);
        if (data.profileThemeId) this.profileThemeId.set(data.profileThemeId);
      },
      error: (err) => console.error('Erreur chargement profil', err)
    });
  }

  loadStats(): void {
    this.apiService.getMyStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        if (this.showStats() && this.chart) {
          this.updateChartData(data);
        }
      },
      error: (err) => console.error('Erreur chargement stats', err)
    });
  }

  loadVisitorAnalytics(): void {
    this.apiService.getVisitorAnalytics().subscribe({
      next: (analytics) => {
        this.visitorAnalytics.set(analytics);
        
        // Calculer Top Source avec pourcentage réel
        if (analytics.sources && Object.keys(analytics.sources).length > 0) {
          const sorted = Object.entries(analytics.sources).sort((a, b) => b[1] - a[1]);
          const top = sorted[0];
          const total = Object.values(analytics.sources).reduce((acc, v) => acc + v, 0);
          const pct = Math.round((top[1] / (total || 1)) * 100);
          this.topSourceLabel.set(`${top[0]} (${pct}%)`);
        }

        // Calculer Top Ville avec pourcentage réel
        if (analytics.topCities && Object.keys(analytics.topCities).length > 0) {
          const sorted = Object.entries(analytics.topCities).sort((a, b) => b[1] - a[1]);
          const top = sorted[0];
          const total = Object.values(analytics.topCities).reduce((acc, v) => acc + v, 0);
          const pct = Math.round((top[1] / (total || 1)) * 100);
          this.topCityLabel.set(`${top[0]} (${pct}%)`);
        }
      },
      error: (err) => console.error('Erreur chargement visitor analytics', err)
    });
  }

  loadAds(): void {
    this.apiService.getActiveAdvertisements().subscribe({
      next: (adsList) => {
        this.ads.set(adsList);
        if (adsList.length > 0) {
          // Prendre une pub active au hasard ou la première
          const randomAd = adsList[Math.floor(Math.random() * adsList.length)];
          this.currentAd.set(randomAd);
          // Enregistrer une vue
          this.apiService.recordAdView(randomAd.id).subscribe();
        }
      },
      error: (err) => console.error('Erreur chargement publicités', err)
    });
  }

  onAdClick(ad: AdvertisementDto): void {
    this.apiService.recordAdClick(ad.id).subscribe();
    window.open(ad.targetUrl, '_blank');
  }

  toggleStats(): void {
    this.showStats.set(!this.showStats());
    
    // Initialiser le graphique si on ouvre le panneau
    if (this.showStats()) {
      setTimeout(() => {
        this.renderChart();
      }, 100); // laisser le DOM s'afficher
    }
  }

  renderChart(): void {
    const canvas = document.getElementById('sentimentChart') as HTMLCanvasElement;
    const statsData = this.stats();
    if (!canvas || !statsData) return;

    if (this.chart) {
      this.chart.destroy();
    }

    this.chart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['Positif', 'Neutre', 'Négatif'],
        datasets: [{
          data: [statsData.positiveCount, statsData.neutralCount, statsData.negativeCount],
          backgroundColor: [
            '#10B981', // Emerald (Positif)
            '#6B7280', // Gray (Neutre)
            '#EF4444'  // Red (Négatif)
          ],
          hoverOffset: 4,
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#E5E7EB' }
          }
        },
        cutout: '75%'
      }
    });
  }

  updateChartData(statsData: StatsDto): void {
    if (!this.chart) return;
    this.chart.data.datasets[0].data = [statsData.positiveCount, statsData.neutralCount, statsData.negativeCount];
    this.chart.update();
  }

  saveProfileCustomization(): void {
    this.isSavingProfile.set(true);
    this.profileSavedSuccess.set(false);
    this.apiService.updateMyProfile({
      bio: this.profileBio(),
      dailyQuestion: this.profileDailyQuestion(),
      themeId: this.profileThemeId()
    }).subscribe({
      next: () => {
        this.isSavingProfile.set(false);
        this.profileSavedSuccess.set(true);
        setTimeout(() => this.profileSavedSuccess.set(false), 3500);
      },
      error: () => {
        this.isSavingProfile.set(false);
        this.toastService.error('Erreur lors de la sauvegarde de la personnalisation.');
      }
    });
  }

  selectTheme(theme: { id: string, proOnly: boolean }): void {
    if (theme.proOnly && !this.walletService.wallet()?.isPro) {
      this.toastService.error('Ce thème doré est réservé aux membres Whispr PRO Club 👑');
      this.walletService.openShop();
      return;
    }
    this.profileThemeId.set(theme.id);
  }

  selectQuickQuestion(q: string): void {
    this.profileDailyQuestion.set(q);
  }

  selectQuickQuestionTranslation(q: string): void {
    const translated = this.translate.instant(q);
    this.profileDailyQuestion.set(translated);
  }

  getProfileLink(): string {
    const base = environment.frontendUrl || window.location.origin;
    return `${base}/${this.pseudo()}`;
  }

  copyLink(): void {
    navigator.clipboard.writeText(this.getProfileLink()).then(() => {
      this.isCopied.set(true);
      setTimeout(() => this.isCopied.set(false), 2000);
    }).catch(() => this.toastService.error('Erreur lors de la copie du lien.'));
  }

  shareLink(): void {
    const link = this.getProfileLink();
    const title = this.translate.instant('INBOX.SHARE_TITLE');
    const text = this.translate.instant('INBOX.SHARE_TEXT');
    if (navigator.share) {
      navigator.share({ title: title, text: text, url: link }).catch(console.error);
    } else {
      window.open(`https://wa.me/?text=${encodeURIComponent(text + ' ' + link)}`, '_blank');
    }
  }

  logout(): void {
    this.authService.logout();
  }

  openDeleteModal(id: string): void {
    this.messageToDelete.set(id);
  }

  cancelDelete(): void {
    this.messageToDelete.set(null);
  }

  confirmDelete(): void {
    const id = this.messageToDelete();
    if (!id) return;

    this.isDeleting.set(true);
    this.apiService.deleteMessage(id).subscribe({
      next: () => {
        const currentMessages = this.messages();
        this.messages.set(currentMessages.filter(m => m.id !== id));
        this.isDeleting.set(false);
        this.messageToDelete.set(null);
        this.loadStats(); // update chart on delete
      },
      error: () => {
        this.toastService.error('Erreur lors de la suppression du message.');
        this.isDeleting.set(false);
        this.messageToDelete.set(null);
      }
    });
  }

  markAsRead(msg: MessageDto): void {
    if (msg.isRead) return;
    
    // Optimistic UI
    const currentMsgs = this.messages();
    const index = currentMsgs.findIndex(m => m.id === msg.id);
    if (index !== -1) {
      const updatedMsgs = [...currentMsgs];
      updatedMsgs[index] = { ...msg, isRead: true };
      this.messages.set(updatedMsgs);
      this.updateTitleBadge();
    }

    this.apiService.markAsRead(msg.id).subscribe({
      next: () => {},
      error: (err) => console.error('Erreur markAsRead', err)
    });
  }

  toggleClues(msgId: string): void {
    if (this.openedCluesMessageId() === msgId) {
      this.openedCluesMessageId.set(null);
      return;
    }
    this.openedCluesMessageId.set(msgId);
    if (!this.messageClues()[msgId]) {
      this.apiService.getCluesForMessage(msgId).subscribe({
        next: (clues) => {
          this.messageClues.update(m => ({ ...m, [msgId]: clues }));
        },
        error: (err) => console.error('Erreur chargement indices', err)
      });
    }
  }

  unlockClue(msgId: string, clueType: string): void {
    const isPro = this.walletService.wallet()?.isPro;
    const coins = this.walletService.wallet()?.coins || 0;
    if (!isPro && coins < 15) {
      this.toastService.error('Solde insuffisant (15 pièces requises). Rechargez vos pièces dans la boutique !');
      this.walletService.openShop();
      return;
    }

    this.isUnlockingClue.set(true);
    this.apiService.unlockClue(msgId, clueType).subscribe({
      next: (unlocked) => {
        this.isUnlockingClue.set(false);
        // Mettre à jour l'indice dans la liste
        this.messageClues.update(m => {
          const list = (m[msgId] || []).map(c => c.clueType === clueType ? unlocked : c);
          return { ...m, [msgId]: list };
        });
        // Recharger le wallet pour mettre à jour le solde
        this.walletService.loadWallet();
        this.toastService.success('Indice débloqué avec succès ! 🕵️');
      },
      error: (err) => {
        this.isUnlockingClue.set(false);
        this.toastService.error("Erreur lors du déblocage de l'indice.");
      }
    });
  }

  updateReplyText(msgId: string, text: string): void {
    const current = this.replyTexts();
    this.replyTexts.set({ ...current, [msgId]: text });
  }

  getReplyText(msgId: string): string {
    return this.replyTexts()[msgId] || '';
  }

  async captureStory(msg: MessageDto): Promise<void> {
    this.messageToCapture.set(msg);
    this.isCapturing.set(true);
    
    setTimeout(async () => {
      try {
        const element = document.getElementById('story-sticker-capture');
        if (!element) throw new Error('Element introuvable');
        
        const canvas = await html2canvas(element, { backgroundColor: null, scale: 2 });
        
        canvas.toBlob(async (blob) => {
          if (!blob) throw new Error('Blob généré vide');
          
          const file = new File([blob], 'whispr-story.png', { type: 'image/png' });
          if (navigator.canShare && navigator.canShare({ files: [file] })) {
            try {
              await navigator.share({
                title: this.translate.instant('INBOX.SHARE_TITLE'),
                text: this.translate.instant('INBOX.SHARE_TEXT') + this.getProfileLink(),
                files: [file]
              });
            } catch (err) {
              this.downloadImage(canvas.toDataURL('image/png'));
            }
          } else {
            this.downloadImage(canvas.toDataURL('image/png'));
          }
          
          this.messageToCapture.set(null);
          this.isCapturing.set(false);
        }, 'image/png');
        
      } catch (err) {
        console.error('Erreur de capture', err);
        alert('Erreur lors de la génération de la Story.');
        this.messageToCapture.set(null);
        this.isCapturing.set(false);
      }
    }, 150);
  }

  private downloadImage(dataUrl: string): void {
    const link = document.createElement('a');
    link.download = 'whispr-story.png';
    link.href = dataUrl;
    link.click();
  }
}





