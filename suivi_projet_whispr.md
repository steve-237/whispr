# 📝 Suivi du Projet : Whispr

Ce document sert de journal de bord et trace l'évolution de la construction du MVP de la plateforme Whispr. Il est mis à jour à chaque étape majeure accomplie.

## Phase 1 : Conception & Architecture (Sprint 0)
- [x] Analyse approfondie du Cahier des charges.
- [x] Définition de l'architecture technique (Angular, Spring Boot Java 21, PostgreSQL, Redis).
- [x] Création du plan de route.

## Phase 2 : Initialisation du Socle Technique
- [x] **Backend** : Génération du squelette Spring Boot avec les dépendances clés (Web, JPA, Security, Flyway, PostgreSQL).
- [x] **Frontend** : Génération du projet Angular (Standalone components, Routing).
- [x] **Vérification** : Compilation réussie des deux projets (Java 21).

## Phase 3 : Modèle de Données (Backend)
- [x] **Infrastructure** : Création du `docker-compose.yml` (Postgres, Redis).
- [x] **Configuration** : `application.yml` pour Spring Boot, activation de Flyway.
- [x] **Base de Données** : Script de migration `V1__Initial_Schema.sql` structurant les tables.
- [x] **Entités JPA** : `User`, `Profile`, `Link`, `Message`, `AuditLog`.
- [x] **Couche d'accès aux données (Repositories)** : Création des interfaces Spring Data JPA.
- [x] **Couche de Logique Métier (Services)** : Création de `UserService`, `LinkService`, `MessageService`.

## Phase 4 : API REST & Sécurité (Terminé pour le MVP)
- `[x]` Création des Controllers REST.
- `[x]` Configuration de Spring Security (PermitAll temporaire pour accès Swagger et endpoints publics).
- `[x]` Configuration de Swagger UI (OpenAPI via springdoc).

## Phase 5 : Développement Frontend (En cours)
- [x] Implémentation du Design System (Couleurs, Typographie, Animations).
- [x] Création des layouts principaux.
- [x] Intégration de l'API Backend.

---
*Dernière mise à jour : Initialisation et Couche Service Terminées.*
