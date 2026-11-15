import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PromptDto } from '../../core/services/api.service';

@Component({
  selector: 'app-prompt-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div *ngIf="prompt" class="glass-panel animate-fade-in" style="padding: 1.5rem; margin-bottom: 1.5rem; text-align: left; background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(236, 72, 153, 0.08)); border: 1px solid rgba(139, 92, 246, 0.35); border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.25);">
      
      <!-- Badge Type Prompt -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
        <span *ngIf="prompt.type === 'POLL'" style="display: inline-flex; align-items: center; gap: 0.35rem; background: rgba(139, 92, 246, 0.25); color: #c4b5fd; padding: 0.25rem 0.65rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;">
          📊 Sondage Anonyme
        </span>
        <span *ngIf="prompt.type === 'RATING'" style="display: inline-flex; align-items: center; gap: 0.35rem; background: rgba(234, 179, 8, 0.2); color: #fde047; padding: 0.25rem 0.65rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;">
          ⭐ Évaluation Anonyme
        </span>
        <span *ngIf="prompt.type === 'TEXT'" style="display: inline-flex; align-items: center; gap: 0.35rem; background: rgba(16, 185, 129, 0.2); color: #6ee7b7; padding: 0.25rem 0.65rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;">
          🎯 Question Ouverte
        </span>
        <span style="font-size: 0.75rem; color: var(--color-text-muted);">
          {{ prompt.totalResponses }} réponse{{ prompt.totalResponses > 1 ? 's' : '' }}
        </span>
      </div>

      <!-- Question -->
      <h3 style="margin: 0 0 1.25rem 0; font-size: 1.15rem; font-weight: 700; color: white; line-height: 1.4;">
        {{ prompt.question }}
      </h3>

      <!-- 1. CAS TYPE : POLL (QCM) -->
      <div *ngIf="prompt.type === 'POLL'">
        <div style="display: flex; flex-direction: column; gap: 0.6rem; margin-bottom: 1.25rem;">
          <div *ngFor="let opt of prompt.options" 
            (click)="selectPollOption(opt.id)"
            [style.border-color]="selectedPollOptionId() === opt.id ? 'var(--color-primary)' : 'rgba(255,255,255,0.1)'"
            [style.background]="selectedPollOptionId() === opt.id ? 'rgba(139, 92, 246, 0.2)' : 'rgba(255,255,255,0.03)'"
            style="position: relative; overflow: hidden; padding: 0.85rem 1rem; border-radius: 12px; border: 1px solid; cursor: pointer; transition: all 0.2s; display: flex; justify-content: space-between; align-items: center;">
            
            <!-- Barre de progression après vote -->
            <div *ngIf="isSubmitted" 
              [style.width.%]="opt.percentage"
              style="position: absolute; top: 0; left: 0; bottom: 0; background: rgba(139, 92, 246, 0.25); z-index: 1; transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);">
            </div>

            <span style="position: relative; z-index: 2; font-weight: 600; font-size: 0.95rem; color: white;">
              {{ opt.optionText }}
            </span>
            <span *ngIf="isSubmitted" style="position: relative; z-index: 2; font-weight: 700; font-size: 0.85rem; color: #c4b5fd;">
              {{ opt.percentage }}% ({{ opt.voteCount }})
            </span>
          </div>
        </div>

        <button *ngIf="!isSubmitted" 
          (click)="onPollSubmit()" 
          [disabled]="isSubmitting || !selectedPollOptionId()"
          class="btn btn-primary" 
          style="width: 100%; padding: 0.85rem; font-weight: 700; font-size: 0.95rem; display: flex; align-items: center; justify-content: center; gap: 0.5rem;">
          <span>🗳️</span>
          <span>{{ isSubmitting ? 'Enregistrement...' : 'Voter anonymement' }}</span>
        </button>
      </div>

      <!-- 2. CAS TYPE : RATING (Note sur 10) -->
      <div *ngIf="prompt.type === 'RATING'">
        <!-- Moyenne communautaire -->
        <div *ngIf="prompt.averageRating !== null && prompt.averageRating !== undefined" style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1rem; padding: 0.6rem 0.85rem; background: rgba(234, 179, 8, 0.1); border-radius: 10px; border: 1px solid rgba(234, 179, 8, 0.2);">
          <span style="font-size: 1.25rem;">⭐</span>
          <span style="font-size: 0.9rem; color: #fef08a; font-weight: 600;">
            Note moyenne : <strong>{{ prompt.averageRating }} / {{ prompt.scaleMax || 10 }}</strong> (sur {{ prompt.ratingCount }} avis)
          </span>
        </div>

        <div *ngIf="!isSubmitted">
          <label style="display: block; font-size: 0.8rem; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600; margin-bottom: 0.5rem;">
            Choisissez une note :
          </label>
          <div style="display: grid; grid-template-columns: repeat(5, 1fr); gap: 0.4rem; margin-bottom: 0.75rem;">
            <button *ngFor="let score of scaleArray()"
              type="button"
              (click)="selectRating(score)"
              [style.background]="selectedRating() === score ? 'linear-gradient(135deg, #fbbf24, #f59e0b)' : 'rgba(255,255,255,0.05)'"
              [style.color]="selectedRating() === score ? '#000' : '#fff'"
              [style.borderColor]="selectedRating() === score ? '#fbbf24' : 'rgba(255,255,255,0.1)'"
              style="padding: 0.6rem 0.2rem; border-radius: 10px; font-weight: 800; font-size: 0.9rem; border: 1px solid; cursor: pointer; transition: all 0.2s;">
              {{ score }}
            </button>
          </div>

          <textarea 
            [ngModel]="promptTextAnswer()" 
            (ngModelChange)="promptTextAnswer.set($event)"
            rows="2" 
            placeholder="Un petit mot confidentiel pour expliquer votre note (facultatif)..."
            style="width: 100%; padding: 0.75rem 1rem; border-radius: 10px; background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.08); color: white; outline: none; font-size: 0.9rem; box-sizing: border-box; resize: none; margin-bottom: 0.75rem;">
          </textarea>

          <button 
            (click)="onRatingSubmit()" 
            [disabled]="isSubmitting || selectedRating() === null"
            class="btn btn-primary" 
            style="width: 100%; padding: 0.85rem; font-weight: 700; font-size: 0.95rem; background: linear-gradient(135deg, #fbbf24, #d97706); color: black;">
            <span>⭐</span>
            <span>{{ isSubmitting ? 'Envoi...' : 'Envoyer ma note anonyme' }}</span>
          </button>
        </div>
      </div>

      <!-- 3. CAS TYPE : TEXT (Question Ouverte Dédiée) -->
      <div *ngIf="prompt.type === 'TEXT' && !isSubmitted">
        <textarea 
          [ngModel]="promptTextAnswer()" 
          (ngModelChange)="promptTextAnswer.set($event)"
          rows="3" 
          placeholder="Écrivez votre réponse en toute franchise..."
          style="width: 100%; padding: 0.75rem 1rem; border-radius: 10px; background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.08); color: white; outline: none; font-size: 0.9rem; box-sizing: border-box; resize: none; margin-bottom: 0.75rem;">
        </textarea>
        <button 
          (click)="onTextSubmit()" 
          [disabled]="isSubmitting || !promptTextAnswer().trim()"
          class="btn btn-primary" 
          style="width: 100%; padding: 0.85rem; font-weight: 700; font-size: 0.95rem;">
          <span>🎯</span>
          <span>{{ isSubmitting ? 'Envoi...' : 'Répondre à cette question' }}</span>
        </button>
      </div>

      <!-- Feedback de succès après vote ou réponse -->
      <div *ngIf="isSubmitted" style="margin-top: 1rem; padding: 0.75rem; background: rgba(16, 185, 129, 0.15); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 10px; text-align: center; color: #6ee7b7; font-size: 0.9rem; font-weight: 600;">
        ✓ {{ feedbackMessage || 'Merci pour votre participation anonyme !' }}
      </div>

    </div>
  `
})
export class PromptCardComponent {
  @Input() prompt!: PromptDto;
  @Input() isSubmitting = false;
  @Input() isSubmitted = false;
  @Input() feedbackMessage = '';

  @Output() pollVoted = new EventEmitter<string>();
  @Output() ratingSubmitted = new EventEmitter<{ rating: number, comment?: string }>();
  @Output() textSubmitted = new EventEmitter<string>();

  selectedPollOptionId = signal<string | null>(null);
  selectedRating = signal<number | null>(null);
  promptTextAnswer = signal<string>('');

  scaleArray(): number[] {
    const max = this.prompt?.scaleMax || 10;
    return Array.from({ length: max }, (_, i) => i + 1);
  }

  selectPollOption(id: string): void {
    if (this.isSubmitted) return;
    this.selectedPollOptionId.set(id);
  }

  onPollSubmit(): void {
    const id = this.selectedPollOptionId();
    if (id) {
      this.pollVoted.emit(id);
    }
  }

  selectRating(score: number): void {
    if (this.isSubmitted) return;
    this.selectedRating.set(score);
  }

  onRatingSubmit(): void {
    const r = this.selectedRating();
    if (r !== null) {
      this.ratingSubmitted.emit({
        rating: r,
        comment: this.promptTextAnswer().trim() || undefined
      });
    }
  }

  onTextSubmit(): void {
    const text = this.promptTextAnswer().trim();
    if (text) {
      this.textSubmitted.emit(text);
    }
  }
}
