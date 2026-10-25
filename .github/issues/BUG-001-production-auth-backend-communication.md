---
title: '[BUG] Échec de communication avec le backend en production (Login & Registration)'
labels: ['bug', 'production', 'auth', 'database']
assignees: 'steve-237'
status: 'in_progress'
created_at: '2026-10-24'
updated_at: '2026-10-25'
---

# Ticket #BUG-001 : Échec de communication avec le backend en production (Login & Register)

## 📌 Description du problème
Sur l'environnement de production ([https://whispr-theta-smoky.vercel.app](https://whispr-theta-smoky.vercel.app)), les utilisateurs rencontraient une erreur `Could not open JPA EntityManager for transaction` ou un blocage lors des tentatives de connexion ou d'inscription.

---

## 🔍 Analyse & Causes Racines

1. **Format de l'URI PostgreSQL sur Render (`DATABASE_URL`)** :
   - Render injecte une variable `DATABASE_URL` standard du type `postgresql://user:password@host:port/database`.
   - Le driver JDBC officiel PostgreSQL (`org.postgresql.Driver`) n'accepte pas ce format sans le schéma `jdbc:postgresql://` et sans parsing séparé des identifiants `user:password`.

2. **Politique SSL sur le réseau interne Render vs Cloud public (`sslmode=prefer`)** :
   - Sur le réseau privé interne de Render (`dpg-xxxx-a:5432`), PostgreSQL ne supporte pas le handshake SSL. Forcer `sslmode=require` provoquait l'échec immédiat de la connexion HikariCP (`Could not open JPA EntityManager for transaction`).
   - L'adoption de `sslmode=prefer` négocie automatiquement le chiffrement SSL quand il est disponible (externe, Neon, Supabase) et bascule gracieusement en non-SSL sur le réseau interne de Render.

3. **Absence de diagnostic direct sur l'état de la base** :
   - L'endpoint `/api/health` renvoyait statiquement `status: UP` sans tester la connexion JDBC, masquant les erreurs de pool Hikari.

4. **Gestion des exceptions transactionnelles Spring** :
   - `CannotCreateTransactionException` n'était pas interceptée spécifiquement et remontait comme une erreur 500 brute au frontend.

---

## 🛠️ Plan de Résolution appliqué

- [x] **Implémentation de `DataSourceConfig.java` résiliente** : Conversion dynamique des URLs cloud `postgres://` et `postgresql://` vers `jdbc:postgresql://` avec `sslmode=prefer`.
- [x] **Diagnostic JDBC sur `/api/health` (`HealthController.java`)** : Sonde en temps réel testant la connectivité de la base de données et affichant le statut `CONNECTED` ou le détail de l'erreur `DISCONNECTED`.
- [x] **Gestionnaire d'erreurs transactionnelles (`GlobalExceptionHandler.java`)** : Capture de `CannotCreateTransactionException` renvoyant un statut HTTP 503 explicite.
- [x] **Sécurisation de la clé JWT (`JwtUtil.java`)** : Dérivation automatique avec padding SHA-256 pour garantir 256 bits d'entropie.
- [x] **Assouplissement et fiabilisation CORS (`SecurityConfig.java`)** : Support étendu des sous-domaines Vercel (`allowedOriginPatterns: https://*.vercel.app`).
