import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="position: fixed; top: 20px; right: 20px; z-index: 99999; display: flex; flex-direction: column; gap: 10px;">
      <div *ngFor="let toast of toastService.toasts()" 
           class="animate-fade-in"
           [ngStyle]="{
             'background': toast.type === 'success' ? 'rgba(16, 185, 129, 0.9)' : (toast.type === 'error' ? 'rgba(239, 68, 68, 0.9)' : 'rgba(59, 130, 246, 0.9)'),
             'backdrop-filter': 'blur(12px)',
             'color': 'white',
             'padding': '14px 20px',
             'border-radius': '10px',
             'box-shadow': '0 10px 25px rgba(0,0,0,0.2)',
             'display': 'flex',
             'align-items': 'center',
             'gap': '12px',
             'font-weight': '600',
             'font-size': '0.95rem',
             'cursor': 'pointer',
             'border': '1px solid rgba(255,255,255,0.2)',
             'min-width': '280px',
             'transform': 'translateY(0)',
             'transition': 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
           }"
           (click)="toastService.remove(toast.id)">
        
        <span *ngIf="toast.type === 'success'" style="font-size: 1.2rem;">✅</span>
        <span *ngIf="toast.type === 'error'" style="font-size: 1.2rem;">⚠️</span>
        <span *ngIf="toast.type === 'info'" style="font-size: 1.2rem;">ℹ️</span>
        
        <span style="flex: 1; letter-spacing: 0.2px;">{{ toast.message }}</span>
      </div>
    </div>
  `
})
export class ToastComponent {
  toastService = inject(ToastService);
}
