import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, LinkDto } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '../../shared/components/toast/toast.service';
import { VoiceDistorterService, VoiceFilterType } from '../../core/services/voice-distorter.service';
import confetti from 'canvas-confetti';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslatePipe],
  templateUrl: './public-profile.html'
})
export class PublicProfileComponent implements OnInit, OnDestroy {
  profile = signal<LinkDto | null>(null);
  messageContent = signal('');
  isSending = signal(false);
  isSent = signal(false);
  error = signal('');
  
  public translate = inject(TranslateService);
  private toastService = inject(ToastService);
  private voiceDistorterService = inject(VoiceDistorterService);
  slug = signal('');
  isLoggingIn = signal(false);
  isSuperWhispr = signal(false);

  // --- Voice Memo State (US-07) ---
  inputMode = signal<'TEXT' | 'AUDIO'>('TEXT');
  isRecording = signal(false);
  recordingSeconds = signal(0);
  rawAudioBlob = signal<Blob | null>(null);
  processedAudioUrl = signal<string | null>(null);
  processedAudioBase64 = signal<string | null>(null);
  recordedDuration = signal(0);
  selectedFilter = signal<VoiceFilterType>('ROBOT');
  isPlayingPreview = signal(false);
  isProcessingFilter = signal(false);

  // --- Visitor Tips State (US-09) ---
  selectedTipAmount = signal<number>(0);
  isTipSuccess = signal(false);
  isSuperWhisprSuccess = signal(false);
  tipSuccessAmount = signal<number | null>(null);

  private recordingInterval: any = null;
  private previewAudio: HTMLAudioElement | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const slugParam = params.get('slug') || '';
      this.slug.set(slugParam);
      if (slugParam) {
        this.loadProfile();
      }
    });

    this.route.queryParamMap.subscribe(params => {
      const payment = params.get('payment');
      const tip = params.get('tip');
      const superWhispr = params.get('super_whispr');
      const amount = params.get('amount');
      const mockTipId = params.get('mockTipId');
      const mockSuperWhisprId = params.get('mockSuperWhisprId');

      if (payment === 'success' && tip === 'true') {
        this.isSent.set(true);
        this.isTipSuccess.set(true);
        this.tipSuccessAmount.set(amount ? parseFloat(amount) : null);
        if (mockTipId) {
          this.apiService.simulateTip(mockTipId).subscribe();
        }
        confetti({
          particleCount: 200,
          spread: 100,
          origin: { y: 0.5 },
          colors: ['#10b981', '#fbbf24', '#8b5cf6', '#ec4899']
        });
      } else if (payment === 'success' && superWhispr === 'true') {
        this.isSent.set(true);
        this.isSuperWhisprSuccess.set(true);
        if (mockSuperWhisprId) {
          this.apiService.simulateSuperWhispr(mockSuperWhisprId).subscribe();
        }
        confetti({
          particleCount: 250,
          spread: 110,
          origin: { y: 0.5 },
          colors: ['#fbbf24', '#f59e0b', '#d97706', '#8b5cf6']
        });
      } else if (payment === 'cancelled') {
        this.toastService.info('Paiement annulé. Aucun message n\'a été envoyé.');
      }
    });
  }

  ngOnDestroy(): void {
    this.cleanupAudio();
  }

  loadProfile(): void {
    this.apiService.getLinkInfo(this.slug()).subscribe({
      next: (data: LinkDto) => {
        this.profile.set(data);
      },
      error: () => {
        this.error.set("Profil introuvable ou lien inactif.");
      }
    });
  }

  setInputMode(mode: 'TEXT' | 'AUDIO'): void {
    this.inputMode.set(mode);
    if (mode === 'TEXT') {
      this.cancelOrResetAudio();
    }
  }

  setTipAmount(amount: number): void {
    this.selectedTipAmount.set(amount);
  }

  async startAudioRecording(): Promise<void> {
    try {
      this.stopPreview();
      await this.voiceDistorterService.startRecording();
      this.isRecording.set(true);
      this.recordingSeconds.set(0);
      this.rawAudioBlob.set(null);
      this.processedAudioUrl.set(null);
      this.processedAudioBase64.set(null);

      this.recordingInterval = setInterval(() => {
        const next = this.recordingSeconds() + 1;
        this.recordingSeconds.set(next);
        if (next >= 10) {
          this.stopAudioRecording();
        }
      }, 1000);
    } catch (err: any) {
      this.toastService.error(err?.message || "Impossible d'accéder au micro.");
    }
  }

  async stopAudioRecording(): Promise<void> {
    if (this.recordingInterval) {
      clearInterval(this.recordingInterval);
      this.recordingInterval = null;
    }

    if (!this.isRecording()) return;
    this.isRecording.set(false);

    try {
      const result = await this.voiceDistorterService.stopRecording();
      this.rawAudioBlob.set(result.blob);
      this.recordedDuration.set(result.durationSeconds);
      await this.applyFilterToCurrentAudio();
    } catch (err: any) {
      this.toastService.error("Erreur lors de la capture audio.");
    }
  }

  async changeFilter(filter: VoiceFilterType): Promise<void> {
    this.selectedFilter.set(filter);
    if (this.rawAudioBlob()) {
      await this.applyFilterToCurrentAudio();
    }
  }

  private async applyFilterToCurrentAudio(): Promise<void> {
    const raw = this.rawAudioBlob();
    if (!raw) return;

    this.isProcessingFilter.set(true);
    this.stopPreview();

    try {
      const { processedBlob, base64Url } = await this.voiceDistorterService.applyFilter(
        raw,
        this.selectedFilter()
      );
      this.processedAudioBase64.set(base64Url);
      const url = URL.createObjectURL(processedBlob);
      this.processedAudioUrl.set(url);
    } catch (err) {
      console.error('Erreur filtrage audio:', err);
    } finally {
      this.isProcessingFilter.set(false);
    }
  }

  togglePreviewAudio(): void {
    const audioUrl = this.processedAudioUrl();
    if (!audioUrl) return;

    if (this.isPlayingPreview()) {
      this.stopPreview();
      return;
    }

    this.stopPreview();
    this.previewAudio = new Audio(audioUrl);
    this.previewAudio.onended = () => {
      this.isPlayingPreview.set(false);
    };
    this.previewAudio.onerror = () => {
      this.isPlayingPreview.set(false);
    };
    this.previewAudio.play().then(() => {
      this.isPlayingPreview.set(true);
    }).catch(err => {
      console.warn("Lecture audio bloquée:", err);
      this.isPlayingPreview.set(false);
    });
  }

  private stopPreview(): void {
    if (this.previewAudio) {
      this.previewAudio.pause();
      this.previewAudio.currentTime = 0;
      this.previewAudio = null;
    }
    this.isPlayingPreview.set(false);
  }

  cancelOrResetAudio(): void {
    this.stopPreview();
    if (this.recordingInterval) {
      clearInterval(this.recordingInterval);
      this.recordingInterval = null;
    }
    this.voiceDistorterService.cancelRecording();
    this.isRecording.set(false);
    this.recordingSeconds.set(0);
    this.rawAudioBlob.set(null);
    this.processedAudioUrl.set(null);
    this.processedAudioBase64.set(null);
    this.recordedDuration.set(0);
  }

  private cleanupAudio(): void {
    this.stopPreview();
    if (this.recordingInterval) {
      clearInterval(this.recordingInterval);
      this.recordingInterval = null;
    }
    this.voiceDistorterService.cancelRecording();
  }

  sendMessage(): void {
    if (!this.profile()) return;

    const isAudio = this.inputMode() === 'AUDIO';
    if (!isAudio && !this.messageContent().trim()) return;
    if (isAudio && !this.processedAudioBase64()) return;

    const messageType = this.isSuperWhispr() ? 'SUPER_WHISPR' : (isAudio ? 'AUDIO' : 'text');
    const content = isAudio
      ? `🎙️ Mémo vocal (${this.selectedFilter()})`
      : this.messageContent();

    const audioUrl = isAudio ? this.processedAudioBase64() || undefined : undefined;
    const audioDuration = isAudio ? this.recordedDuration() || 5 : undefined;
    const filter = isAudio ? this.selectedFilter() : undefined;

    // Si Super Whispr sélectionné sans pourboire -> Stripe Checkout Super Whispr (0,99 €)
    if (this.isSuperWhispr() && this.selectedTipAmount() === 0) {
      this.isSending.set(true);
      this.apiService.createSuperWhisprCheckout({
        slug: this.slug(),
        content: content,
        audioUrl: audioUrl,
        audioDurationSeconds: audioDuration,
        voiceFilter: filter
      }).subscribe({
        next: (res) => {
          this.isSending.set(false);
          if (res.checkoutUrl) {
            window.location.href = res.checkoutUrl;
          } else {
            this.isSent.set(true);
            this.isSuperWhisprSuccess.set(true);
            this.cancelOrResetAudio();
            confetti({
              particleCount: 200,
              spread: 100,
              origin: { y: 0.5 },
              colors: ['#fbbf24', '#f59e0b', '#d97706', '#8b5cf6']
            });
          }
        },
        error: (err) => {
          this.isSending.set(false);
          let errorMsg = "Erreur lors de l'initialisation du paiement Stripe Super Whispr.";
          if (err.status === 400 && err.error && typeof err.error === 'string') {
            errorMsg = err.error;
          }
          this.toastService.error(errorMsg);
        }
      });
      return;
    }

    // Si pourboire sélectionné -> Stripe Checkout
    if (this.selectedTipAmount() > 0) {
      this.isSending.set(true);
      this.apiService.createTipCheckout({
        slug: this.slug(),
        content: content,
        type: messageType,
        tipAmountEur: this.selectedTipAmount(),
        audioUrl: audioUrl,
        audioDurationSeconds: audioDuration,
        voiceFilter: filter
      }).subscribe({
        next: (res) => {
          this.isSending.set(false);
          if (res.checkoutUrl) {
            window.location.href = res.checkoutUrl;
          } else {
            this.isSent.set(true);
            this.isTipSuccess.set(true);
            this.tipSuccessAmount.set(this.selectedTipAmount());
            this.cancelOrResetAudio();
            confetti({
              particleCount: 150,
              spread: 80,
              origin: { y: 0.6 },
              colors: ['#10b981', '#fbbf24', '#8b5cf6']
            });
          }
        },
        error: (err) => {
          this.isSending.set(false);
          let errorMsg = "Erreur lors de l'initialisation du paiement Stripe.";
          if (err.status === 400 && err.error && typeof err.error === 'string') {
            errorMsg = err.error;
          }
          this.toastService.error(errorMsg);
        }
      });
      return;
    }

    this.isSending.set(true);
    this.apiService.sendMessage(
      this.slug(),
      content,
      messageType,
      audioUrl,
      audioDuration,
      filter
    ).subscribe({
      next: () => {
        this.isSending.set(false);
        this.isSent.set(true);
        this.isTipSuccess.set(false);
        this.messageContent.set('');
        this.cancelOrResetAudio();
        this.error.set('');
        
        // Explosion de confettis ! 🎉
        confetti({
          particleCount: 150,
          spread: 80,
          origin: { y: 0.6 },
          colors: ['#8b5cf6', '#ec4899', '#10b981', '#f59e0b', '#3b82f6']
        });
      },
      error: (err) => {
        this.isSending.set(false);
        let errorMsg = "Erreur lors de l'envoi du message.";
        if (err.status === 400 && err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        } else if (err.status === 429) {
          errorMsg = "Trop de messages envoyés. Veuillez patienter.";
        }
        this.error.set(errorMsg);
        this.toastService.error(errorMsg);
      }
    });
  }

  loginToDemo(): void {
    this.isLoggingIn.set(true);
    this.authService.login('demo@whispr.com', 'password123').subscribe({
      next: () => {
        this.isLoggingIn.set(false);
        this.router.navigate(['/inbox']);
      },
      error: () => {
        this.isLoggingIn.set(false);
        this.error.set("Erreur de connexion au compte démo.");
      }
    });
  }

  sendAnother(): void {
    this.isSent.set(false);
    this.isTipSuccess.set(false);
    this.isSuperWhisprSuccess.set(false);
    this.tipSuccessAmount.set(null);
    this.selectedTipAmount.set(0);
    this.isSuperWhispr.set(false);
    this.messageContent.set('');
    this.cancelOrResetAudio();
  }

}
