import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MessageDto, ClueResponseDto } from '../../../core/services/api.service';

@Component({
  selector: 'app-message-card',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, TranslatePipe],
  template: `
    <div class="animate-fade-in" 
         [ngStyle]="{
           'background': msg.type === 'SUPER_WHISPR' ? 'linear-gradient(135deg, rgba(251, 191, 36, 0.12), rgba(245, 158, 11, 0.05))' : (msg.isRead ? 'rgba(255,255,255,0.025)' : 'rgba(139, 92, 246, 0.06)')
         }"
         style="border: 1px solid; padding: 1.25rem; border-radius: 18px; position: relative; transition: all 0.2s; cursor: pointer;"
         [style.border-color]="msg.type === 'SUPER_WHISPR' ? 'rgba(251, 191, 36, 0.6)' : (!msg.isRead ? 'rgba(139, 92, 246, 0.3)' : 'rgba(255,255,255,0.06)')"
         [style.box-shadow]="msg.type === 'SUPER_WHISPR' ? '0 0 20px rgba(251, 191, 36, 0.15)' : 'none'"
         (click)="markRead.emit(msg)">
      
      <!-- Badge SUPER WHISPR -->
      <div *ngIf="msg.type === 'SUPER_WHISPR'" style="position: absolute; top: -10px; right: 50px; background: linear-gradient(135deg, #fbbf24, #d97706); color: #000; padding: 0.25rem 0.8rem; border-radius: 20px; font-size: 0.7rem; font-weight: 900; letter-spacing: 0.5px; text-transform: uppercase; box-shadow: 0 4px 12px rgba(251, 191, 36, 0.4); display: flex; align-items: center; gap: 0.3rem;">
        ⭐ SUPER WHISPR
      </div>

      <!-- Badge Nouveau -->
      <div *ngIf="!msg.isRead" style="position: absolute; top: -8px; left: 16px; background: var(--color-primary); color: white; padding: 0.2rem 0.7rem; border-radius: 20px; font-size: 0.65rem; font-weight: 800; letter-spacing: 0.5px; text-transform: uppercase; box-shadow: 0 3px 8px rgba(139, 92, 246, 0.4);">
        {{ 'INBOX.MSG_BADGE' | translate }}
      </div>

      <!-- Supprimer -->
      <button (click)="delete.emit(msg.id); $event.stopPropagation()" style="position: absolute; top: 0.75rem; right: 0.75rem; background: rgba(0,0,0,0.15); border: none; color: var(--color-text-muted); padding: 0.35rem; border-radius: 50%; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s;" title="Supprimer">
        <svg style="width: 14px; height: 14px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
      </button>

      <!-- Contenu -->
      <p style="font-size: 1.05rem; line-height: 1.6; margin: 0 2rem 1rem 0; color: white; word-break: break-word;" [style.font-weight]="msg.isRead ? '400' : '600'">
        "{{ msg.content }}"
      </p>

      <!-- Méta -->
      <div style="display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; font-size: 0.75rem; color: var(--color-text-muted); margin-bottom: 0.8rem;">
        <span style="display: flex; align-items: center; gap: 0.3rem; background: rgba(0,0,0,0.2); padding: 0.25rem 0.6rem; border-radius: 10px;">
          <svg style="width: 12px; height: 12px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {{ msg.createdAt | date:'short' }}
        </span>
        <span *ngIf="msg.aiCategory" [ngStyle]="{
          'color': msg.aiCategory === 'POSITIVE' ? '#10b981' : (msg.aiCategory === 'NEGATIVE' ? '#ef4444' : '#94a3b8'),
          'background': msg.aiCategory === 'POSITIVE' ? 'rgba(16, 185, 129, 0.15)' : (msg.aiCategory === 'NEGATIVE' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(148, 163, 184, 0.15)')
        }" style="padding: 0.25rem 0.6rem; border-radius: 10px; font-weight: 700; display: flex; align-items: center; gap: 0.3rem; letter-spacing: 0.5px;">
          {{ msg.aiCategory === 'POSITIVE' ? '😊' : (msg.aiCategory === 'NEGATIVE' ? '😠' : '😐') }} {{ (msg.aiCategory === 'POSITIVE' ? 'INBOX.SENTIMENT_POSITIVE' : (msg.aiCategory === 'NEGATIVE' ? 'INBOX.SENTIMENT_NEGATIVE' : 'INBOX.SENTIMENT_NEUTRAL')) | translate }}
        </span>

        <span *ngIf="msg.country" style="display: flex; align-items: center; gap: 0.3rem; background: rgba(0,0,0,0.2); padding: 0.25rem 0.6rem; border-radius: 10px;">
          📍 {{ msg.country }}
        </span>
      </div>

      <!-- Section Indices Secrets -->
      <div style="background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.06); border-radius: 14px; padding: 0.75rem; margin-bottom: 0.8rem;">
        <div (click)="toggleClues.emit(msg.id); $event.stopPropagation()" style="display: flex; justify-content: space-between; align-items: center; cursor: pointer;">
          <div style="display: flex; align-items: center; gap: 0.4rem; font-weight: 700; font-size: 0.85rem; color: #fbbf24;">
            <span>🕵️</span>
            <span>Démasquer l'expéditeur (Indices Secrets)</span>
          </div>
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <span *ngIf="isPro" style="background: linear-gradient(135deg, #ec4899, #8b5cf6); color: white; font-size: 0.65rem; font-weight: 800; padding: 0.15rem 0.5rem; border-radius: 10px;">PRO GRATUIT</span>
            <span style="color: var(--color-text-muted); font-size: 0.8rem;">{{ isOpenedClues ? '▲' : '▼' }}</span>
          </div>
        </div>

        <!-- Liste des indices dépliée -->
        <div *ngIf="isOpenedClues" class="animate-fade-in" style="margin-top: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; border-top: 1px solid rgba(255,255,255,0.06); padding-top: 0.75rem;">
          <div *ngFor="let clue of clues" style="display: flex; justify-content: space-between; align-items: center; background: rgba(255,255,255,0.02); padding: 0.5rem 0.75rem; border-radius: 10px; font-size: 0.8rem;">
            <div style="display: flex; align-items: center; gap: 0.5rem;">
              <span>{{ clue.clueType === 'LOCATION' ? '📍 Ville exacte' : (clue.clueType === 'DEVICE' ? '📱 Modèle téléphone' : '📡 Opérateur / 5G') }}</span>
            </div>
            <div>
              <span *ngIf="clue.isUnlocked" style="color: #10b981; font-weight: 700; background: rgba(16,185,129,0.15); padding: 0.2rem 0.6rem; border-radius: 8px;">
                {{ clue.clueValue }}
              </span>
              <button *ngIf="!clue.isUnlocked" (click)="unlock.emit({ msgId: msg.id, clueType: clue.clueType }); $event.stopPropagation()" [disabled]="isUnlockingClue" style="background: rgba(251, 191, 36, 0.15); border: 1px solid rgba(251, 191, 36, 0.4); color: #fbbf24; font-weight: 700; padding: 0.25rem 0.6rem; border-radius: 8px; font-size: 0.75rem; cursor: pointer; display: flex; align-items: center; gap: 0.3rem;">
                <span>🔒 Débloquer</span>
                <span>({{ isPro ? '0 🪙' : '15 🪙' }})</span>
              </button>
            </div>
          </div>

          <div *ngIf="clues.length === 0" style="text-align: center; color: var(--color-text-muted); font-size: 0.8rem; padding: 0.5rem;">
            Chargement des indices...
          </div>
        </div>
      </div>

      <!-- Zone Réponse pour Statut / Story -->
      <div style="margin-top: 0.6rem; margin-bottom: 0.6rem; background: rgba(0,0,0,0.2); border-radius: 12px; padding: 0.6rem; border: 1px solid rgba(255,255,255,0.06);">
        <div style="font-size: 0.75rem; color: var(--color-text-muted); margin-bottom: 0.4rem; font-weight: 600; display: flex; align-items: center; gap: 0.3rem;">
          <span>✍️</span>
          <span>{{ 'INBOX.REPLY_LABEL' | translate }}</span>
        </div>
        <textarea 
          [ngModel]="replyText"
          (ngModelChange)="replyChange.emit($event)"
          (click)="$event.stopPropagation()"
          [placeholder]="'INBOX.REPLY_PLACEHOLDER' | translate"
          rows="2"
          style="width: 100%; box-sizing: border-box; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 0.5rem 0.7rem; color: white; font-size: 0.85rem; outline: none; resize: vertical; font-family: inherit; margin-bottom: 0.5rem;"></textarea>
        
        <button class="btn btn-primary" (click)="capture.emit(msg); $event.stopPropagation()" [disabled]="isCapturing" style="width: 100%; padding: 0.65rem; font-size: 0.85rem; font-weight: 700; border-radius: 10px; display: flex; align-items: center; justify-content: center; gap: 0.5rem; cursor: pointer;">
          <svg style="width: 16px; height: 16px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"></path></svg>
          {{ isCapturing ? ('INBOX.BTN_STORY_LOADING' | translate) : ('INBOX.BTN_STORY' | translate) }}
        </button>
      </div>
    </div>
  `
})
export class MessageCardComponent {
  @Input({ required: true }) msg!: MessageDto;
  @Input() isOpenedClues = false;
  @Input() clues: ClueResponseDto[] = [];
  @Input() isUnlockingClue = false;
  @Input() replyText = '';
  @Input() isCapturing = false;
  @Input() isPro = false;

  @Output() markRead = new EventEmitter<MessageDto>();
  @Output() delete = new EventEmitter<string>();
  @Output() toggleClues = new EventEmitter<string>();
  @Output() unlock = new EventEmitter<{ msgId: string; clueType: string }>();
  @Output() replyChange = new EventEmitter<string>();
  @Output() capture = new EventEmitter<MessageDto>();
}
