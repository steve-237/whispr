import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-inbox',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './inbox.component.html'
})
export class InboxComponent implements OnInit {
  messages = signal<any[]>([]);
  isLoading = signal(true);
  error = signal('');
  pseudo = signal('');
  messageToDelete = signal<string | null>(null);
  isDeleting = signal(false);
  isCopied = signal(false);

  constructor(private apiService: ApiService, private authService: AuthService) {
    this.pseudo.set(this.authService.currentUser() || '');
  }

  ngOnInit(): void {
    this.loadMessages();
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
      // Fallback to WhatsApp
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
