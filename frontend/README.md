# Whispr - Frontend (Angular)

Ce dossier contient l'interface utilisateur web de Whispr, développée avec le framework **Angular**.

## ⚙️ Prérequis

- **Node.js** (LTS recommandé, ex: v20 ou supérieur).
- **npm** (inclus avec Node.js).

## 🚀 Lancement Rapide

1. Installez les dépendances du projet :
   ```bash
   npm install
   ```
2. Lancez le serveur de développement local :
   ```bash
   npm start
   # ou via Angular CLI
   npx ng serve
   ```
3. L'application sera accessible sur : [http://localhost:4200/](http://localhost:4200/). L'application se rechargera automatiquement si vous modifiez un fichier source.

## 📂 Structure des dossiers (Feature-First)

Le projet suit une architecture modulaire ("Feature-First") facilitant la scalabilité :
- `src/app/core/` : Services singuliers (Singletons), intercepteurs HTTP, configuration, et gardes de sécurité (Guards).
- `src/app/shared/` : Composants réutilisables, directives, pipes, et modèles.
- `src/app/features/` : Les modules métier spécifiques (ex: `auth`, `inbox`, `profile`, `moderation`).
- `src/styles/` : Les feuilles de styles globales, variables CSS/SCSS, et la définition du Design System (animations, typographie, Glassmorphism).

## 🛠️ Build de Production

Pour compiler le projet pour la production :
```bash
npm run build
```
Les artefacts générés seront stockés dans le répertoire `dist/`. Ils sont optimisés et prêts à être déployés sur un hébergement statique ou un CDN.
