import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  template: `
    <div class="container animate-fade-in" style="text-align: center; margin-top: 2rem; max-width: 800px; padding-bottom: 3rem;">
      
      <div style="display: inline-block; padding: 0.5rem 1rem; background: rgba(255, 255, 255, 0.05); border: 1px solid var(--color-border); border-radius: 20px; font-size: 0.9rem; color: var(--color-text-muted); margin-bottom: 2rem;">
        {{ 'HOME.BETA' | translate }}
      </div>

      <h1 style="font-size: clamp(1.75rem, 8vw, 4rem); line-height: 1.1; margin-bottom: 1.5rem; background: linear-gradient(135deg, #ffffff, #a3a3a3); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
        {{ 'HOME.TITLE_1' | translate }} <br/> <span style="background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">{{ 'HOME.TITLE_2' | translate }}</span>
      </h1>
      
      <p style="font-size: clamp(0.95rem, 3vw, 1.25rem); color: var(--color-text-muted); margin-bottom: 2rem; line-height: 1.6; max-width: 600px; margin-left: auto; margin-right: auto; padding: 0 0.5rem;">
        {{ 'HOME.SUBTITLE' | translate }}
      </p>

      <div style="display: flex; gap: 1rem; justify-content: center; margin-bottom: 3rem; flex-wrap: wrap; padding: 0 1rem;">
        <a routerLink="/register" class="btn btn-primary" style="font-size: 1.1rem; padding: 1rem 2rem; text-decoration: none;">
          {{ 'HOME.BTN_CREATE' | translate }}
        </a>
        <a routerLink="/demo" class="btn btn-glass" style="font-size: 1.1rem; padding: 1rem 2rem; text-decoration: none; display: flex; align-items: center; gap: 0.5rem;">
          <svg style="width: 20px; height: 20px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {{ 'HOME.BTN_DEMO' | translate }}
        </a>
      </div>

      <!-- Features Section -->
      <h2 style="font-size: clamp(1.5rem, 5vw, 2.5rem); margin: 3rem 0 2rem 0;">{{ 'HOME.FEATURES_TITLE' | translate }}</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; text-align: center; margin-bottom: 4rem;">
        <div class="glass-panel card-hover" style="padding: 2rem;">
          <div style="font-size: 2.5rem; margin-bottom: 1rem;">🛡️</div>
          <h3 style="font-size: 1.15rem; margin-bottom: 0.8rem;">{{ 'HOME.FEATURE1_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); font-size: 0.95rem; line-height: 1.5;">{{ 'HOME.FEATURE1_DESC' | translate }}</p>
        </div>
        <div class="glass-panel card-hover" style="padding: 2rem;">
          <div style="font-size: 2.5rem; margin-bottom: 1rem;">🎨</div>
          <h3 style="font-size: 1.15rem; margin-bottom: 0.8rem;">{{ 'HOME.FEATURE2_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); font-size: 0.95rem; line-height: 1.5;">{{ 'HOME.FEATURE2_DESC' | translate }}</p>
        </div>
        <div class="glass-panel card-hover" style="padding: 2rem;">
          <div style="font-size: 2.5rem; margin-bottom: 1rem;">📸</div>
          <h3 style="font-size: 1.15rem; margin-bottom: 0.8rem;">{{ 'HOME.FEATURE3_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); font-size: 0.95rem; line-height: 1.5;">{{ 'HOME.FEATURE3_DESC' | translate }}</p>
        </div>
      </div>

      <h2 style="font-size: clamp(1.5rem, 5vw, 2.5rem); margin: 3rem 0 2rem 0;">{{ 'HOME.HOW_IT_WORKS' | translate }}</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 2rem; text-align: left; margin-bottom: 4rem;">
        <div class="glass-panel" style="padding: 2rem; position: relative;">
          <div style="position: absolute; top: -15px; left: -15px; width: 40px; height: 40px; background: var(--color-primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.2rem;">1</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem;">{{ 'HOME.STEP1_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); line-height: 1.5;">{{ 'HOME.STEP1_DESC' | translate }}</p>
        </div>
        <div class="glass-panel" style="padding: 2rem; position: relative;">
          <div style="position: absolute; top: -15px; left: -15px; width: 40px; height: 40px; background: var(--color-primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.2rem;">2</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem;">{{ 'HOME.STEP2_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); line-height: 1.5;">{{ 'HOME.STEP2_DESC' | translate }}</p>
        </div>
        <div class="glass-panel" style="padding: 2rem; position: relative;">
          <div style="position: absolute; top: -15px; left: -15px; width: 40px; height: 40px; background: var(--color-primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.2rem;">3</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem;">{{ 'HOME.STEP3_TITLE' | translate }}</h3>
          <p style="color: var(--color-text-muted); line-height: 1.5;">{{ 'HOME.STEP3_DESC' | translate }}</p>
        </div>
      </div>

      <h2 style="font-size: clamp(1.5rem, 5vw, 2.5rem); margin: 3rem 0 2rem 0;">{{ 'HOME.FAQ_TITLE' | translate }}</h2>
      <div style="text-align: left; max-width: 700px; margin: 0 auto 5rem auto; display: flex; flex-direction: column; gap: 1rem;">
        <div class="glass-panel" style="padding: 1.5rem;">
          <h4 style="font-size: 1.1rem; margin-bottom: 0.5rem;">{{ 'HOME.FAQ1_Q' | translate }}</h4>
          <p style="color: var(--color-text-muted);">{{ 'HOME.FAQ1_A' | translate }}</p>
        </div>
        <div class="glass-panel" style="padding: 1.5rem;">
          <h4 style="font-size: 1.1rem; margin-bottom: 0.5rem;">{{ 'HOME.FAQ2_Q' | translate }}</h4>
          <p style="color: var(--color-text-muted);">{{ 'HOME.FAQ2_A' | translate }}</p>
        </div>
      </div>

      <!-- Stats Section -->
      <div style="display: flex; justify-content: center; gap: 3rem; flex-wrap: wrap; margin: 5rem 0; padding: 2rem; background: rgba(0,0,0,0.2); border-radius: 20px; border: 1px solid rgba(255,255,255,0.05);">
        <div style="text-align: center;">
          <div style="font-size: 2.5rem; font-weight: 800; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">{{ 'HOME.STATS_USERS' | translate }}</div>
          <div style="color: var(--color-text-muted); font-size: 0.9rem; margin-top: 0.5rem; text-transform: uppercase; letter-spacing: 1px;">{{ 'HOME.STATS_USERS_DESC' | translate }}</div>
        </div>
        <div style="text-align: center;">
          <div style="font-size: 2.5rem; font-weight: 800; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">{{ 'HOME.STATS_MSGS' | translate }}</div>
          <div style="color: var(--color-text-muted); font-size: 0.9rem; margin-top: 0.5rem; text-transform: uppercase; letter-spacing: 1px;">{{ 'HOME.STATS_MSGS_DESC' | translate }}</div>
        </div>
        <div style="text-align: center;">
          <div style="font-size: 2.5rem; font-weight: 800; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">{{ 'HOME.STATS_SAFE' | translate }}</div>
          <div style="color: var(--color-text-muted); font-size: 0.9rem; margin-top: 0.5rem; text-transform: uppercase; letter-spacing: 1px;">{{ 'HOME.STATS_SAFE_DESC' | translate }}</div>
        </div>
      </div>

      <!-- AI Protection Highlight -->
      <div class="glass-panel" style="margin: 5rem 0; padding: 3rem 2rem; text-align: center; border-color: rgba(var(--color-primary-rgb), 0.3); background: linear-gradient(180deg, rgba(var(--color-primary-rgb), 0.1) 0%, rgba(0,0,0,0) 100%);">
        <div style="font-size: 3rem; margin-bottom: 1rem;">🤖🛡️</div>
        <h2 style="font-size: 2rem; margin-bottom: 1.5rem;">{{ 'HOME.AI_SAFE_TITLE' | translate }}</h2>
        <p style="color: var(--color-text-muted); line-height: 1.6; max-width: 600px; margin: 0 auto;">{{ 'HOME.AI_SAFE_DESC' | translate }}</p>
      </div>

      <!-- Testimonials -->
      <h2 style="font-size: clamp(1.5rem, 5vw, 2.5rem); margin: 5rem 0 2rem 0;">{{ 'HOME.TESTIMONIALS_TITLE' | translate }}</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; text-align: left; margin-bottom: 5rem;">
        <div class="glass-panel" style="padding: 1.5rem; position: relative;">
          <div style="color: var(--color-primary); font-size: 2rem; position: absolute; top: 1rem; right: 1.5rem; opacity: 0.3;">"</div>
          <p style="font-style: italic; color: var(--color-text-muted); margin-bottom: 1.5rem; line-height: 1.5;">{{ 'HOME.TESTIMONIAL1_TEXT' | translate }}</p>
          <div style="font-weight: 600; font-size: 0.9rem;">{{ 'HOME.TESTIMONIAL1_AUTHOR' | translate }}</div>
        </div>
        <div class="glass-panel" style="padding: 1.5rem; position: relative;">
          <div style="color: var(--color-primary); font-size: 2rem; position: absolute; top: 1rem; right: 1.5rem; opacity: 0.3;">"</div>
          <p style="font-style: italic; color: var(--color-text-muted); margin-bottom: 1.5rem; line-height: 1.5;">{{ 'HOME.TESTIMONIAL2_TEXT' | translate }}</p>
          <div style="font-weight: 600; font-size: 0.9rem;">{{ 'HOME.TESTIMONIAL2_AUTHOR' | translate }}</div>
        </div>
        <div class="glass-panel" style="padding: 1.5rem; position: relative;">
          <div style="color: var(--color-primary); font-size: 2rem; position: absolute; top: 1rem; right: 1.5rem; opacity: 0.3;">"</div>
          <p style="font-style: italic; color: var(--color-text-muted); margin-bottom: 1.5rem; line-height: 1.5;">{{ 'HOME.TESTIMONIAL3_TEXT' | translate }}</p>
          <div style="font-weight: 600; font-size: 0.9rem;">{{ 'HOME.TESTIMONIAL3_AUTHOR' | translate }}</div>
        </div>
      </div>

      <!-- Bottom CTA -->
      <div style="margin: 6rem 0 3rem 0; padding-top: 4rem; border-top: 1px solid rgba(255,255,255,0.05);">
        <h2 style="font-size: 2rem; margin-bottom: 1rem;">{{ 'HOME.BOTTOM_CTA_TITLE' | translate }}</h2>
        <p style="color: var(--color-text-muted); margin-bottom: 2rem;">{{ 'HOME.BOTTOM_CTA_DESC' | translate }}</p>
        <a routerLink="/register" class="btn btn-primary" style="font-size: 1.1rem; padding: 1rem 2rem; text-decoration: none;">
          {{ 'HOME.BTN_CREATE' | translate }}
        </a>
      </div>

    </div>
  `
})
export class HomeComponent {}
