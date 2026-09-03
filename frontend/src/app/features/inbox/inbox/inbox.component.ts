import { Component, OnInit, signal, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, MessageDto, StatsDto } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import html2canvas from 'html2canvas';
import { Client } from '@stomp/stompjs';
import { environment } from '../../../../environments/environment';
import Chart from 'chart.js/auto';

import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-inbox',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, TranslatePipe],
  templateUrl: './inbox.component.html'
})
export class InboxComponent implements OnInit, OnDestroy, AfterViewInit {
  messages = signal<MessageDto[]>([]);
  isLoading = signal(true);
  error = signal('');
  pseudo = signal('');
  messageToDelete = signal<string | null>(null);
  isDeleting = signal(false);
  isCopied = signal(false);

  // Customization
  showCustomization = signal(false);
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
    'Posez-moi une question anonyme et sincère... 🤫',
    'Quel est votre avis honnête sur moi ? 💭',
    'Avoue-moi un secret en toute discrétion... 🔒',
    'Un défaut ou une qualité que vous me trouvez ? ✨',
    'Quelle est votre première impression de moi ? 👀'
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
    this.loadMessages();
    this.loadProfileInfo();
    this.loadStats();
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
    const token = localStorage.getItem('token');
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
      this.titleService.setTitle($() Whispr - Anonymous Messages);
    } else {
      this.titleService.setTitle('Whispr - Anonymous Messages');
    }
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

  selectQuickQuestion(q: string): void {
    this.profileDailyQuestion.set(q);
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

