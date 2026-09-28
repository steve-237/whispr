import { Component, EventEmitter, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './onboarding.html'
})
export class OnboardingComponent {
  @Output() completed = new EventEmitter<void>();
  public translate = inject(TranslateService);

  currentStep = signal(1);

  nextStep() {
    if (this.currentStep() < 3) {
      this.currentStep.set(this.currentStep() + 1);
    } else {
      this.complete();
    }
  }

  complete() {
    this.completed.emit();
  }
}
