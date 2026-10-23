import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-profile-customizer',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  template: `
    <div class="animate-fade-in" style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); padding: 1.5rem; border-radius: 20px; margin-bottom: 2rem;">
      <h3 style="margin: 0 0 1.5rem 0; font-size: 1.15rem; font-weight: 700; color: white;">{{ 'INBOX.CUSTOM_TITLE' | translate }}</h3>
      
      <div style="display: flex; flex-direction: column; gap: 1.5rem;">
        <!-- Thème -->
        <div>
          <label style="display: block; font-weight: 600; margin-bottom: 0.6rem; font-size: 0.8rem; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 1px;">{{ 'INBOX.CUSTOM_THEME' | translate }}</label>
          <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.6rem;">
            <button *ngFor="let theme of themes"
              (click)="onThemeSelected(theme)"
              [style.border-color]="themeId === theme.id ? theme.color : 'rgba(255,255,255,0.08)'"
              [style.background]="themeId === theme.id ? theme.color + '25' : 'rgba(255,255,255,0.02)'"
              [style.border-width]="themeId === theme.id ? '2px' : '1px'"
              style="padding: 0.8rem 0.5rem; border-radius: 14px; font-weight: 700; font-size: 0.85rem; border-style: solid; cursor: pointer; color: white; transition: all 0.2s; text-align: center; display: flex; align-items: center; justify-content: center; gap: 0.4rem;">
              <span>{{ theme.key.startsWith('👑') ? theme.key : (theme.key | translate) }}</span>
              <span *ngIf="theme.proOnly && !isPro" style="background: rgba(234, 179, 8, 0.2); color: #fbbf24; font-size: 0.65rem; padding: 0.1rem 0.35rem; border-radius: 6px;">PRO</span>
            </button>
          </div>
        </div>
        
        <!-- Question du jour -->
        <div>
          <label style="display: block; font-weight: 600; margin-bottom: 0.6rem; font-size: 0.8rem; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 1px;">{{ 'INBOX.CUSTOM_QUESTION' | translate }}</label>
          <div style="display: flex; gap: 0.4rem; flex-wrap: wrap; margin-bottom: 0.6rem;">
            <button *ngFor="let q of quickQuestions" (click)="quickQuestionSelected.emit(q)" style="font-size: 0.75rem; padding: 0.35rem 0.7rem; border-radius: 20px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: var(--color-text-muted); cursor: pointer; transition: all 0.2s; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px;">
              {{ (q | translate).length > 28 ? (q | translate).substring(0, 28) + '...' : (q | translate) }}
            </button>
          </div>
          <input type="text" [ngModel]="dailyQuestion" (ngModelChange)="dailyQuestionChange.emit($event)" [placeholder]="'INBOX.CUSTOM_QUESTION_PLACEHOLDER' | translate" style="width: 100%; padding: 0.85rem 1rem; border-radius: 14px; background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.08); color: white; outline: none; font-size: 0.95rem; box-sizing: border-box;">
        </div>

        <!-- Bio -->
        <div>
          <label style="display: block; font-weight: 600; margin-bottom: 0.6rem; font-size: 0.8rem; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 1px;">{{ 'INBOX.CUSTOM_BIO' | translate }}</label>
          <input type="text" [ngModel]="bio" (ngModelChange)="bioChange.emit($event)" [placeholder]="'INBOX.CUSTOM_BIO_PLACEHOLDER' | translate" style="width: 100%; padding: 0.85rem 1rem; border-radius: 14px; background: rgba(0,0,0,0.25); border: 1px solid rgba(255,255,255,0.08); color: white; outline: none; font-size: 0.95rem; box-sizing: border-box;">
        </div>
        
        <!-- Sauvegarder -->
        <button (click)="save.emit()" [disabled]="isSaving" [style.background]="isSavedSuccess ? '#10b981' : 'var(--color-primary)'" style="width: 100%; padding: 1.1rem; font-weight: 800; border-radius: 14px; font-size: 1.05rem; color: white; border: none; cursor: pointer; transition: all 0.3s; box-shadow: 0 8px 20px rgba(139, 92, 246, 0.4); text-transform: uppercase; letter-spacing: 0.5px;" [style.opacity]="isSaving ? '0.7' : '1'">
          {{ isSaving ? ('INBOX.BTN_SAVING' | translate) : (isSavedSuccess ? ('INBOX.BTN_SAVED' | translate) : ('INBOX.BTN_SAVE' | translate)) }}
        </button>
      </div>
    </div>
  `
})
export class ProfileCustomizerComponent {
  @Input() themeId = 'neon';
  @Input() dailyQuestion = '';
  @Input() bio = '';
  @Input() quickQuestions: string[] = [];
  @Input() isSaving = false;
  @Input() isSavedSuccess = false;
  @Input() isPro = false;

  @Output() themeChange = new EventEmitter<any>();
  @Output() dailyQuestionChange = new EventEmitter<string>();
  @Output() bioChange = new EventEmitter<string>();
  @Output() quickQuestionSelected = new EventEmitter<string>();
  @Output() save = new EventEmitter<void>();

  themes = [
    { id: 'neon', color: '#8b5cf6', key: 'INBOX.THEME_NEON', proOnly: false },
    { id: 'sunset', color: '#f97316', key: 'INBOX.THEME_SUNSET', proOnly: false },
    { id: 'emerald', color: '#10b981', key: 'INBOX.THEME_EMERALD', proOnly: false },
    { id: 'dark', color: '#94a3b8', key: 'INBOX.THEME_DARK', proOnly: false },
    { id: 'gold', color: '#eab308', key: '👑 Gold VIP (PRO)', proOnly: true }
  ];

  onThemeSelected(theme: any): void {
    this.themeChange.emit(theme);
  }
}
