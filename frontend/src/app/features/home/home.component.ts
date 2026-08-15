import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container animate-fade-in" style="text-align: center; margin-top: 2rem; max-width: 800px; padding-bottom: 3rem;">
      
      <div style="display: inline-block; padding: 0.5rem 1rem; background: rgba(255, 255, 255, 0.05); border: 1px solid var(--color-border); border-radius: 20px; font-size: 0.9rem; color: var(--color-text-muted); margin-bottom: 2rem;">
        🚀 Lancement de la version Beta de Whispr
      </div>

      <h1 style="font-size: clamp(1.75rem, 8vw, 4rem); line-height: 1.1; margin-bottom: 1.5rem; background: linear-gradient(135deg, #ffffff, #a3a3a3); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
        Recevez des messages <br/> <span style="background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">totalement anonymes</span>
      </h1>
      
      <p style="font-size: clamp(0.95rem, 3vw, 1.25rem); color: var(--color-text-muted); margin-bottom: 2rem; line-height: 1.6; max-width: 600px; margin-left: auto; margin-right: auto; padding: 0 0.5rem;">
        Whispr est la plateforme ultime pour découvrir ce que vos amis pensent vraiment de vous. 
        Partagez votre lien unique sur vos réseaux sociaux et lisez leurs réponses secrètes dans votre boîte de réception privée.
      </p>

      <div style="display: flex; gap: 1rem; justify-content: center; margin-bottom: 3rem; flex-wrap: wrap; padding: 0 1rem;">
        <a routerLink="/register" class="btn btn-primary" style="font-size: 1.1rem; padding: 1rem 2rem; text-decoration: none;">
          Créer mon lien gratuit
        </a>
        <a routerLink="/demo" class="btn btn-glass" style="font-size: 1.1rem; padding: 1rem 2rem; text-decoration: none; display: flex; align-items: center; gap: 0.5rem;">
          <svg style="width: 20px; height: 20px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          Essayer la Démo
        </a>
      </div>

      <h2 style="font-size: clamp(1.5rem, 5vw, 2.5rem); margin: 3rem 0 2rem 0;">Comment ça marche ?</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 2rem; text-align: left; margin-bottom: 4rem;">
        <div class="glass-panel" style="padding: 2rem; position: relative;">
          <div style="position: absolute; top: -15px; left: -15px; width: 40px; height: 40px; background: var(--color-primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.2rem;">1</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem;">Créez votre lien</h3>
          <p style="color: var(--color-text-muted); line-height: 1.5;">Inscrivez-vous en quelques secondes et obtenez votre lien personnel du type whispr.com/pseudo.</p>
        </div>
        <div class="glass-panel" style="padding: 2rem; position: relative;">
          <div style="position: absolute; top: -15px; left: -15px; width: 40px; height: 40px; background: var(--color-primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.2rem;">2</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem;">Partagez-le</h3>
          <p style="color: var(--color-text-muted); line-height: 1.5;">Ajoutez votre lien dans votre bio Instagram, TikTok, Snapchat ou Twitter pour que vos abonnés le voient.</p>
        </div>
        <div class="glass-panel" style="padding: 2rem; position: relative;">
          <div style="position: absolute; top: -15px; left: -15px; width: 40px; height: 40px; background: var(--color-primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 1.2rem;">3</div>
          <h3 style="font-size: 1.25rem; margin-bottom: 1rem;">Lisez en secret</h3>
          <p style="color: var(--color-text-muted); line-height: 1.5;">Recevez des messages totalement anonymes. Notre IA filtre automatiquement le contenu haineux.</p>
        </div>
      </div>

      <h2 style="font-size: clamp(1.5rem, 5vw, 2.5rem); margin: 3rem 0 2rem 0;">Foire Aux Questions</h2>
      <div style="text-align: left; max-width: 700px; margin: 0 auto 5rem auto; display: flex; flex-direction: column; gap: 1rem;">
        <div class="glass-panel" style="padding: 1.5rem;">
          <h4 style="font-size: 1.1rem; margin-bottom: 0.5rem;">L'application est-elle vraiment anonyme ?</h4>
          <p style="color: var(--color-text-muted);">Oui. Contrairement à d'autres applications qui gardent les adresses IP en clair, Whispr utilise des algorithmes de hachage cryptographique pour pseudonymiser toutes les données de connexion.</p>
        </div>
        <div class="glass-panel" style="padding: 1.5rem;">
          <h4 style="font-size: 1.1rem; margin-bottom: 0.5rem;">Comment fonctionne la protection par IA ?</h4>
          <p style="color: var(--color-text-muted);">Tous les messages reçus sont analysés en temps réel par notre intelligence artificielle avant même d'arriver dans votre boîte de réception. Les messages toxiques sont automatiquement filtrés ou signalés.</p>
        </div>
      </div>

    </div>
  `
})
export class HomeComponent {}
