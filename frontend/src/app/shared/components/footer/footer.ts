import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer style="text-align: center; padding: 2rem; color: var(--color-text-muted); font-size: 0.875rem; margin-top: 4rem;">
      <p>&copy; 2026 Whispr Platform. Protected by AI moderation.</p>
    </footer>
  `
})
export class FooterComponent {}
