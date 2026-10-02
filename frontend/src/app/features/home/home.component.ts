import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  template: `
    <!-- Arrière-plan animé -->
    <div class="bg-grid"></div>

    <div class="container animate-fade-in" style="text-align: center; margin-top: 2rem; max-width: 1000px; padding-bottom: 3rem; position: relative;">
      
      <!-- Hero Section avec effet 3D Mockup -->
      <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 70vh; margin-bottom: 2rem;">
        
        <div style="display: inline-block; padding: 0.5rem 1rem; background: rgba(255, 255, 255, 0.05); border: 1px solid var(--color-border); border-radius: 20px; font-size: 0.9rem; color: var(--color-primary); margin-bottom: 2rem; font-weight: 600; box-shadow: 0 0 15px rgba(139, 92, 246, 0.2);">
          ✨ {{ 'HOME.BETA' | translate }}
        </div>

        <h1 style="font-size: clamp(2.5rem, 8vw, 5.5rem); line-height: 1.1; margin-bottom: 1.5rem; font-weight: 900; letter-spacing: -2px;">
          <span style="color: #fff; text-shadow: 0 0 30px rgba(255,255,255,0.2);">{{ 'HOME.TITLE_1' | translate }}</span> <br/> 
          <span style="background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; text-shadow: 0 10px 40px rgba(236, 72, 153, 0.3);">{{ 'HOME.TITLE_2' | translate }}</span>
        </h1>
        
        <p style="font-size: clamp(1rem, 3vw, 1.25rem); color: var(--color-text-muted); margin-bottom: 3rem; line-height: 1.6; max-width: 600px; margin-left: auto; margin-right: auto; padding: 0 0.5rem;">
          {{ 'HOME.SUBTITLE' | translate }}
        </p>

        <div style="display: flex; gap: 1rem; justify-content: center; margin-bottom: 4rem; flex-wrap: wrap; padding: 0 1rem; position: relative; z-index: 10;">
          <a routerLink="/register" class="btn btn-primary" style="font-size: 1.2rem; padding: 1.2rem 2.5rem; text-decoration: none; border-radius: 100px;">
            {{ 'HOME.BTN_CREATE' | translate }} 🚀
          </a>
          <a routerLink="/demo" class="btn btn-glass" style="font-size: 1.2rem; padding: 1.2rem 2.5rem; text-decoration: none; border-radius: 100px; display: flex; align-items: center; gap: 0.5rem;">
            <svg style="width: 20px; height: 20px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            {{ 'HOME.BTN_DEMO' | translate }}
          </a>
        </div>

        <!-- Mockup Flottant 3D -->
        <div style="width: 100%; max-width: 400px; margin: 0 auto; animation: float 6s ease-in-out infinite; perspective: 1000px;">
          <div class="glass-panel" style="padding: 2rem; border-radius: 20px; text-align: left; background: rgba(30, 41, 59, 0.8); border: 1px solid rgba(139, 92, 246, 0.3); box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 40px rgba(139, 92, 246, 0.2); transform: rotateX(10deg) rotateY(-5deg);">
            <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem;">
              <div style="width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); display: flex; align-items: center; justify-content: center; font-size: 1.2rem;">🤫</div>
              <div>
                <div style="font-weight: 700; color: #fff;">Anonyme</div>
                <div style="font-size: 0.8rem; color: var(--color-text-muted);">Il y a 2 minutes</div>
              </div>
            </div>
            <p style="color: #fff; font-size: 1.1rem; line-height: 1.5; margin-bottom: 1rem;">
              "J'adore vraiment ce que tu fais, continue comme ça ! 🔥"
            </p>
            <div style="font-size: 0.8rem; color: #10b981; display: inline-block; padding: 0.3rem 0.8rem; background: rgba(16, 185, 129, 0.1); border-radius: 10px;">
              IA : Positif (100%)
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Marquee Banner -->
    <div class="marquee-container" style="margin-bottom: 4rem;">
      <div class="marquee-content">
        <!-- Répéter deux fois pour l'effet infini -->
        <ng-container *ngFor="let i of [1, 2]">
          <span class="marquee-item">🔒 Anonymat Garanti</span>
          <span class="marquee-item">•</span>
          <span class="marquee-item">🚀 +10 000 Messages Envoyés</span>
          <span class="marquee-item">•</span>
          <span class="marquee-item">🤖 Filtre Anti-Harcèlement IA</span>
          <span class="marquee-item">•</span>
          <span class="marquee-item">📸 Partage Facile en Story</span>
          <span class="marquee-item">•</span>
        </ng-container>
      </div>
    </div>

    <div class="container" style="max-width: 1000px; padding-bottom: 5rem;">
      <!-- Features Section -->
      <h2 style="font-size: clamp(2rem, 5vw, 3rem); margin: 3rem 0 3rem 0; text-align: center; font-weight: 800;">{{ 'HOME.FEATURES_TITLE' | translate }}</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 2rem; text-align: center; margin-bottom: 6rem;">
        <div class="glass-panel glow-card" style="padding: 2.5rem 2rem;">
          <div style="font-size: 3rem; margin-bottom: 1.5rem;">🛡️</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem; color: #fff;">{{ 'HOME.FEATURE1_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); font-size: 1rem; line-height: 1.6;">{{ 'HOME.FEATURE1_DESC' | translate }}</p>
        </div>
        <div class="glass-panel glow-card" style="padding: 2.5rem 2rem;">
          <div style="font-size: 3rem; margin-bottom: 1.5rem;">🎨</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem; color: #fff;">{{ 'HOME.FEATURE2_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); font-size: 1rem; line-height: 1.6;">{{ 'HOME.FEATURE2_DESC' | translate }}</p>
        </div>
        <div class="glass-panel glow-card" style="padding: 2.5rem 2rem;">
          <div style="font-size: 3rem; margin-bottom: 1.5rem;">📸</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem; color: #fff;">{{ 'HOME.FEATURE3_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); font-size: 1rem; line-height: 1.6;">{{ 'HOME.FEATURE3_DESC' | translate }}</p>
        </div>
      </div>

      <!-- How it Works -->
      <h2 style="font-size: clamp(2rem, 5vw, 3rem); margin: 3rem 0 3rem 0; text-align: center; font-weight: 800;">{{ 'HOME.HOW_IT_WORKS' | translate }}</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 3rem; text-align: left; margin-bottom: 6rem;">
        <div style="position: relative;">
          <div style="font-size: 4rem; font-weight: 900; color: rgba(255,255,255,0.05); position: absolute; top: -30px; left: -10px; z-index: -1;">01</div>
          <h3 style="font-size: 1.3rem; margin-bottom: 1rem; color: var(--color-primary);">{{ 'HOME.STEP1_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); line-height: 1.6;">{{ 'HOME.STEP1_DESC' | translate }}</p>
        </div>
        <div style="position: relative;">
          <div style="font-size: 4rem; font-weight: 900; color: rgba(255,255,255,0.05); position: absolute; top: -30px; left: -10px; z-index: -1;">02</div>
          <h3 style="font-size: 1.3rem; margin-bottom: 1rem; color: var(--color-primary);">{{ 'HOME.STEP2_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); line-height: 1.6;">{{ 'HOME.STEP2_DESC' | translate }}</p>
        </div>
        <div style="position: relative;">
          <div style="font-size: 4rem; font-weight: 900; color: rgba(255,255,255,0.05); position: absolute; top: -30px; left: -10px; z-index: -1;">03</div>
          <h3 style="font-size: 1.3rem; margin-bottom: 1rem; color: var(--color-primary);">{{ 'HOME.STEP3_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); line-height: 1.6;">{{ 'HOME.STEP3_DESC' | translate }}</p>
        </div>
      </div>

      <!-- FAQ Section -->
      <h2 style="font-size: clamp(2rem, 5vw, 3rem); margin: 3rem 0 3rem 0; text-align: center; font-weight: 800;">{{ 'HOME.FAQ_TITLE' | translate }}</h2>
      <div style="text-align: left; max-width: 800px; margin: 0 auto 5rem auto; display: flex; flex-direction: column; gap: 1.5rem;">
        <div class="glass-panel" style="padding: 2rem;">
          <h4 style="font-size: 1.2rem; margin-bottom: 0.8rem; color: #fff;">{{ 'HOME.FAQ1_Q' | translate }}</h4>
          <p style="color: var(--color-text-muted); line-height: 1.6;">{{ 'HOME.FAQ1_A' | translate }}</p>
        </div>
        <div class="glass-panel" style="padding: 2rem;">
          <h4 style="font-size: 1.2rem; margin-bottom: 0.8rem; color: #fff;">{{ 'HOME.FAQ2_Q' | translate }}</h4>
          <p style="color: var(--color-text-muted); line-height: 1.6;">{{ 'HOME.FAQ2_A' | translate }}</p>
        </div>
      </div>

      <!-- Bottom CTA -->
      <div style="margin: 6rem 0 3rem 0; padding: 4rem 2rem; text-align: center; background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(236, 72, 153, 0.1)); border-radius: 30px; border: 1px solid rgba(255,255,255,0.1);">
        <h2 style="font-size: 2.5rem; margin-bottom: 1rem; font-weight: 800;">{{ 'HOME.BOTTOM_CTA_TITLE' | translate }}</h2>
        <p style="color: var(--color-text-muted); margin-bottom: 3rem; font-size: 1.1rem; max-width: 500px; margin-left: auto; margin-right: auto;">{{ 'HOME.BOTTOM_CTA_DESC' | translate }}</p>
        <a routerLink="/register" class="btn btn-primary" style="font-size: 1.2rem; padding: 1.2rem 3rem; text-decoration: none; border-radius: 100px; box-shadow: 0 10px 30px rgba(139, 92, 246, 0.4);">
          {{ 'HOME.BTN_CREATE' | translate }} 🚀
        </a>
      </div>

    </div>
  `
})
export class HomeComponent {}
