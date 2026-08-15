# CHANGELOG - Whispr App

## [1.0.0] - 2026-08-15
### 🚀 Cœur de l'application (Release Principale)
- **Frontend** : Application complète développée avec Angular 18 (Standalone Components, Signals).
- **Backend** : API robuste en Java 21 avec Spring Boot 3 et base de données PostgreSQL.
- **Progressive Web App (PWA)** : Application installable sur mobile et ordinateur de bureau avec manifeste et Service Worker configurés.
- **Internationalisation (i18n)** : Traduction automatique (Anglais/Français) avec détection du navigateur et bouton de sélection manuel.

### 🔒 Sécurité & Modération IA
- **Analyse IA intégrée** : Détection de toxicité en temps réel bloquant les messages haineux ou inappropriés.
- **Filtrage avancé** : Enregistrement de l'Adresse IP brute, empreinte de l'appareil (Device Fingerprint) et localisation (Pays/Ville) des expéditeurs de messages.

### 🛠️ Panel d'Administration Complet
- Interface d'administration sécurisée accessible via `/admin` (réservée au rôle `ADMIN`).
- **Vue Globale (Analytics)** : Graphiques interactifs (Chart.js) montrant les inscriptions sur 30 jours et la santé de la plateforme (Messages sains vs toxiques).
- **Gestion des Utilisateurs** :
  - Suspension immédiate de compte (Bannissement) avec l'état `BANNED`.
  - Bouton de réinitialisation de mot de passe générant un code aléatoire sécurisé.
  - Suppression définitive (supprime utilisateur + liens + messages).
  - Bouton de pistage pour visualiser spécifiquement tous les messages reçus par un utilisateur.
- **Gestion des Messages** : Suppression de messages isolés ou en masse.
- **Journaux d'Audit** : Historique ultra-détaillé masquant par défaut les adresses IP en clair, qui sont uniquement accessibles par le "Super Administrateur" (`admin@whispr.com`).
- **Actions en Masse (Bulk Actions)** : Système de cases à cocher pour supprimer simultanément 50 utilisateurs ou messages d'un clic.

### 📱 Interface Utilisateur
- Design moderne "Glassmorphism" avec palette de couleurs violette.
- Totalement optimisé "Mobile-First" (Header adaptatif, grilles fluides).
- Animations de chargement et de transitions.
- Profil de démonstration sécurisé pour essayer l'application.

### ☁️ Déploiement Cloud
- Configuration Vercel prête (`vercel.json`) pour l'hébergement Frontend.
- Dockerfile optimisé Alpine (Java 21) pour l'hébergement Backend sur Render.com.
- Base de données PostgreSQL & Redis prêtes à être connectées en production via variables d'environnement.
