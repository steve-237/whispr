import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, MessageDto } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-inbox',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './inbox.component.html'
})
export class InboxComponent implements OnInit {
  messages = signal<MessageDto[]>([]);
  isLoading = signal(true);
  error = signal('');
  pseudo = signal('');
  messageToDelete = signal<string | null>(null);
  isDeleting = signal(false);
  isCopied = signal(false);

  // Customization Signals
  showCustomization = signal(false);
  profileBio = signal('');
  profileDailyQuestion = signal('');
  profileThemeId = signal('neon');
  isSavingProfile = signal(false);
  profileSavedSuccess = signal(false);
  
  quickQuestions = signal<string[]>([
    'Posez-moi une question anonyme et sincère... 🤫',
    'Quel est votre avis honnête sur moi ? 💭',
    'Avoue-moi un secret en toute discrétion... 🔒',
    'Un défaut ou une qualité que vous me trouvez ? ✨',
    'Quelle est votre première impression de moi ? 👀'
  ]);

  constructor(private apiService: ApiService, private authService: AuthService) {
    this.pseudo.set(this.authService.currentUser() || '');
  }

  ngOnInit(): void {
    this.loadMessages();
    this.loadProfileInfo();
  }

  loadMessages(): void {
    this.apiService.getInbox().subscribe({
      next: (data) => {
        this.messages.set(data);
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
        alert('Erreur lors de la sauvegarde de la personnalisation.');
      }
    });
  }

  selectQuickQuestion(q: string): void {
    this.profileDailyQuestion.set(q);
  }

  getProfileLink(): string {
    return `${window.location.origin}/${this.pseudo()}`;
  }

  copyLink(): void {
    navigator.clipboard.writeText(this.getProfileLink()).then(() => {
      this.isCopied.set(true);
      setTimeout(() => this.isCopied.set(false), 2000);
    }).catch(() => {
      alert('Erreur lors de la copie du lien.');
    });
  }

  shareLink(): void {
    const link = this.getProfileLink();
    const text = 'Envoyez-moi un message anonyme et secret ! 🤫';
    
    if (navigator.share) {
      navigator.share({
        title: 'Mon Lien Secret',
        text: text,
        url: link
      }).catch(console.error);
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
      },
      error: () => {
        alert('Erreur lors de la suppression du message.');
        this.isDeleting.set(false);
        this.messageToDelete.set(null);
      }
    });
  }
}
