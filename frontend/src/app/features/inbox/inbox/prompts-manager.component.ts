import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PromptDto, CreatePromptRequest } from '../../../core/services/api.service';

@Component({
  selector: 'app-prompts-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div *ngIf="isOpen" class="modal-overlay animate-fade-in" style="position: fixed; inset: 0; background: rgba(0,0,0,0.85); backdrop-filter: blur(12px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem;">
      <div class="glass-panel" style="width: 100%; max-width: 600px; max-height: 90vh; overflow-y: auto; padding: 1.75rem; border-radius: 24px; position: relative; border: 1px solid rgba(139, 92, 246, 0.3);">
        
        <!-- Fermer -->
        <button (click)="close.emit()" style="position: absolute; top: 1.25rem; right: 1.25rem; background: rgba(255,255,255,0.08); border: none; color: white; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; cursor: pointer;">
          ✕
        </button>

        <div style="display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.5rem;">
          <span style="font-size: 1.5rem;">🎯</span>
          <h2 style="margin: 0; font-size: 1.35rem; font-weight: 800; color: white;">Whispr Prompts & Sondages</h2>
        </div>
        <p style="color: var(--color-text-muted); font-size: 0.85rem; margin: 0 0 1.5rem 0;">
          Posez une question spécifique pour stimuler vos abonnés et collecter des avis honnêtes.
        </p>

        <!-- Suggestions rapides -->
        <div style="margin-bottom: 1.5rem;">
          <label style="display: block; font-size: 0.75rem; color: var(--color-text-muted); text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px; margin-bottom: 0.5rem;">
            💡 Suggestions populaires :
          </label>
          <div style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
            <button (click)="selectPreset('Note mon style / outfit sur 10', 'RATING', 10)" style="font-size: 0.78rem; padding: 0.35rem 0.7rem; border-radius: 20px; background: rgba(234, 179, 8, 0.15); border: 1px solid rgba(234, 179, 8, 0.3); color: #fde047; cursor: pointer;">
              ⭐ Note mon style sur 10
            </button>
            <button (click)="selectPreset('Quel est mon pire défaut sans filtre ?', 'TEXT')" style="font-size: 0.78rem; padding: 0.35rem 0.7rem; border-radius: 20px; background: rgba(16, 185, 129, 0.15); border: 1px solid rgba(16, 185, 129, 0.3); color: #6ee7b7; cursor: pointer;">
              🎯 Mon pire défaut ?
            </button>
            <button (click)="selectPreset('Dois-je faire une FAQ en direct ?', 'POLL', undefined, ['Oui carrément !', 'Pas intéressé', 'Seulement si c\\'est drôle'])" style="font-size: 0.78rem; padding: 0.35rem 0.7rem; border-radius: 20px; background: rgba(139, 92, 246, 0.15); border: 1px solid rgba(139, 92, 246, 0.3); color: #c4b5fd; cursor: pointer;">
              📊 Faire une FAQ ?
            </button>
            <button (click)="selectPreset('Que penses-tu de moi en secret ?', 'TEXT')" style="font-size: 0.78rem; padding: 0.35rem 0.7rem; border-radius: 20px; background: rgba(236, 72, 153, 0.15); border: 1px solid rgba(236, 72, 153, 0.3); color: #f472b6; cursor: pointer;">
              🤫 Que penses-tu de moi ?
            </button>
          </div>
        </div>

        <!-- Formulaire de Création -->
        <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 18px; padding: 1.25rem; margin-bottom: 1.5rem;">
          <h4 style="margin: 0 0 1rem 0; color: white; font-size: 1rem; font-weight: 700;">Nouveau Sondage</h4>

          <!-- Format Selector -->
          <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem; margin-bottom: 1rem;">
            <button type="button" (click)="promptType.set('POLL')" [style.background]="promptType() === 'POLL' ? 'var(--color-primary)' : 'rgba(255,255,255,0.04)'" [style.color]="promptType() === 'POLL' ? 'white' : 'var(--color-text-muted)'" style="padding: 0.6rem 0.3rem; border-radius: 10px; border: 1px solid rgba(255,255,255,0.08); font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: all 0.2s;">
              📊 QCM
            </button>
            <button type="button" (click)="promptType.set('RATING')" [style.background]="promptType() === 'RATING' ? 'linear-gradient(135deg, #fbbf24, #f59e0b)' : 'rgba(255,255,255,0.04)'" [style.color]="promptType() === 'RATING' ? 'black' : 'var(--color-text-muted)'" style="padding: 0.6rem 0.3rem; border-radius: 10px; border: 1px solid rgba(255,255,255,0.08); font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: all 0.2s;">
              ⭐ Note /10
            </button>
            <button type="button" (click)="promptType.set('TEXT')" [style.background]="promptType() === 'TEXT' ? 'var(--color-accent)' : 'rgba(255,255,255,0.04)'" [style.color]="promptType() === 'TEXT' ? 'white' : 'var(--color-text-muted)'" style="padding: 0.6rem 0.3rem; border-radius: 10px; border: 1px solid rgba(255,255,255,0.08); font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: all 0.2s;">
              ✍️ Libre
            </button>
          </div>

          <!-- Question Input -->
          <div style="margin-bottom: 1rem;">
            <label style="display: block; font-size: 0.75rem; color: var(--color-text-muted); text-transform: uppercase; font-weight: 600; margin-bottom: 0.4rem;">
              Question ou Intitulé :
            </label>
            <input type="text" [ngModel]="question()" (ngModelChange)="question.set($event)" placeholder="Ex: Quelle note donnez-vous à mon profil ?" style="width: 100%; padding: 0.75rem 1rem; border-radius: 10px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); color: white; outline: none; font-size: 0.9rem; box-sizing: border-box;">
          </div>

          <!-- Options pour QCM -->
          <div *ngIf="promptType() === 'POLL'" style="margin-bottom: 1rem;">
            <label style="display: block; font-size: 0.75rem; color: var(--color-text-muted); text-transform: uppercase; font-weight: 600; margin-bottom: 0.4rem;">
              Options de réponse :
            </label>
            <div style="display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 0.5rem;">
              <div *ngFor="let opt of options(); let i = index" style="display: flex; gap: 0.4rem; align-items: center;">
                <input type="text" [ngModel]="opt" (ngModelChange)="updateOption(i, $event)" [placeholder]="'Option ' + (i + 1)" style="flex: 1; padding: 0.6rem 0.85rem; border-radius: 8px; background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.08); color: white; outline: none; font-size: 0.85rem;">
                <button *ngIf="options().length > 2" (click)="removeOption(i)" type="button" style="background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239, 68, 68, 0.3); color: #f87171; border-radius: 8px; width: 34px; height: 34px; cursor: pointer; display: flex; align-items: center; justify-content: center;">
                  ✕
                </button>
              </div>
            </div>
            <button *ngIf="options().length < 6" (click)="addOption()" type="button" style="font-size: 0.8rem; background: rgba(255,255,255,0.05); border: 1px dashed rgba(255,255,255,0.2); color: white; padding: 0.4rem 0.8rem; border-radius: 8px; cursor: pointer;">
              + Ajouter une option
            </button>
          </div>

          <button (click)="onSubmit()" [disabled]="isSaving || !question().trim()" class="btn btn-primary" style="width: 100%; padding: 0.85rem; font-weight: 800; font-size: 0.95rem;">
            {{ isSaving ? 'Création en cours...' : 'Activer sur mon profil 🎯' }}
          </button>
        </div>

        <!-- Historique des sondages créés -->
        <div>
          <h4 style="margin: 0 0 0.75rem 0; color: white; font-size: 0.95rem; font-weight: 700;">Vos sondages enregistrés</h4>
          <div *ngIf="promptsList.length === 0" style="text-align: center; color: var(--color-text-muted); font-size: 0.85rem; padding: 1.5rem;">
            Aucun sondage créé pour l'instant.
          </div>
          <div style="display: flex; flex-direction: column; gap: 0.75rem;">
            <div *ngFor="let p of promptsList" style="background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.06); border-radius: 14px; padding: 1rem; display: flex; justify-content: space-between; align-items: center; gap: 0.75rem;">
              <div style="text-align: left; flex: 1;">
                <div style="display: flex; align-items: center; gap: 0.4rem; margin-bottom: 0.25rem;">
                  <span [style.background]="p.isActive ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255,255,255,0.08)'" [style.color]="p.isActive ? '#10b981' : 'var(--color-text-muted)'" style="font-size: 0.7rem; font-weight: 700; padding: 0.1rem 0.4rem; border-radius: 6px;">
                    {{ p.isActive ? 'ACTIF' : 'INACTIF' }}
                  </span>
                  <span style="font-size: 0.75rem; color: var(--color-text-muted);">
                    {{ p.totalResponses }} réponse{{ p.totalResponses > 1 ? 's' : '' }}
                  </span>
                  <span *ngIf="p.type === 'RATING' && p.averageRating" style="font-size: 0.75rem; color: #fde047;">
                    • ⭐ {{ p.averageRating }}/{{ p.scaleMax || 10 }}
                  </span>
                </div>
                <p style="margin: 0; color: white; font-weight: 600; font-size: 0.9rem;">{{ p.question }}</p>
              </div>
              <div style="display: flex; gap: 0.4rem; align-items: center;">
                <button (click)="toggleStatus.emit({ id: p.id, active: p.isActive })" [style.background]="p.isActive ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)'" [style.color]="p.isActive ? '#f87171' : '#34d399'" style="padding: 0.4rem 0.7rem; border-radius: 8px; border: none; font-size: 0.78rem; font-weight: 700; cursor: pointer;">
                  {{ p.isActive ? 'Désactiver' : 'Activer' }}
                </button>
                <button (click)="delete.emit(p.id)" style="background: rgba(255,255,255,0.05); color: #fca5a5; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; width: 32px; height: 32px; cursor: pointer; display: flex; align-items: center; justify-content: center;" title="Supprimer">
                  ✕
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  `
})
export class PromptsManagerComponent {
  @Input() isOpen = false;
  @Input() promptsList: PromptDto[] = [];
  @Input() isSaving = false;

  @Output() close = new EventEmitter<void>();
  @Output() create = new EventEmitter<CreatePromptRequest>();
  @Output() toggleStatus = new EventEmitter<{ id: string, active: boolean }>();
  @Output() delete = new EventEmitter<string>();

  question = signal('');
  promptType = signal<'TEXT' | 'POLL' | 'RATING'>('POLL');
  options = signal<string[]>(['Oui', 'Non']);
  scaleMax = signal<number>(10);

  selectPreset(q: string, type: 'TEXT' | 'POLL' | 'RATING', scale?: number, opts?: string[]): void {
    this.question.set(q);
    this.promptType.set(type);
    if (opts && opts.length >= 2) {
      this.options.set([...opts]);
    }
    if (scale) {
      this.scaleMax.set(scale);
    }
  }

  addOption(): void {
    if (this.options().length < 6) {
      this.options.update(opts => [...opts, '']);
    }
  }

  removeOption(index: number): void {
    if (this.options().length > 2) {
      this.options.update(opts => opts.filter((_, i) => i !== index));
    }
  }

  updateOption(index: number, val: string): void {
    this.options.update(opts => {
      const copy = [...opts];
      copy[index] = val;
      return copy;
    });
  }

  onSubmit(): void {
    const q = this.question().trim();
    if (!q) return;

    let validOpts: string[] | undefined = undefined;
    if (this.promptType() === 'POLL') {
      validOpts = this.options().map(o => o.trim()).filter(o => o.length > 0);
      if (validOpts.length < 2) return;
    }

    this.create.emit({
      question: q,
      type: this.promptType(),
      options: validOpts,
      scaleMax: this.promptType() === 'RATING' ? this.scaleMax() : undefined
    });

    this.question.set('');
    this.options.set(['Oui', 'Non']);
  }
}
