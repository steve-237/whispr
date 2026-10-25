---
title: '[BUG] Échec de communication avec le backend en production (Login & Registration)'
labels: ['bug', 'production', 'auth', 'database']
assignees: 'steve-237'
status: 'in_progress'
created_at: '2026-10-24'
---

# Ticket #BUG-001 : Échec de communication avec le backend en production (Login & Register)

## 📌 Description du problème
Sur l'environnement de production ([https://whispr-theta-smoky.vercel.app](https://whispr-theta-smoky.vercel.app)), les utilisateurs ne parviennent pas à se connecter ni à créer de compte.
Bien que l'endpoint de vérification de santé (`/api/health`) réponde avec un statut `200 UP`, toute tentative d'authentification (`POST /api/auth/login`) ou d'enregistrement (`POST /api/auth/register`) reste bloquée en attente jusqu'à expiration du délai réseau (Timeout / 504 Gateway Timeout).

---

## 🔍 Analyse & Causes Racines

1. **Format de l'URI PostgreSQL sur Render (`DATABASE_URL`)** :
   - Render injecte une variable `DATABASE_URL` standard du type `postgresql://user:password@host:port/database` ou `postgres://...`.
   - Le driver JDBC officiel PostgreSQL (`org.postgresql.Driver`) n'accepte pas ce format sans le schéma `jdbc:postgresql://` et sans parsing séparé des identifiants `user:password`.
   - Sans adaptateur, l'acquisition de connexion PostgreSQL échoue ou suspend les requêtes.

2. **Exigence SSL obligatoire dans le cloud (`sslmode=require`)** :
   - Les bases PostgreSQL hébergées sur Render imposent le chiffrement SSL. Sans le paramètre `sslmode=require`, la négociation réseau reste bloquée.

3. **Absence de résilience du pool HikariCP** :
   - Par défaut, le timeout de connexion Hikari est de 30 secondes sans politique de déconnexion rapide, provoquant le gel complet des threads HTTP de Spring Boot.

4. **Taille minimale de la clé JWT** :
   - L'algorithme HMAC-SHA256 (JJWT) requiert une clé minimale de 256 bits (32 octets). Si la variable `JWT_SECRET` en production est plus courte, une exception `WeakKeyException` est levée lors de la signature des tokens d'authentification.

5. **Gestion du statut réseau et feedback utilisateur sur le frontend** :
   - Le frontend traduisait systématiquement les erreurs réseau ou de timeout (code 0 ou 504 lors d'un cold-start Render) en "Identifiants incorrects", masquant la cause réelle à l'utilisateur.

---

## 🛠️ Plan de Résolution appliqué

- [x] **Implémentation de `DataSourceConfig.java`** : Conversion dynamique et automatique des URLs cloud `postgres://` et `postgresql://` vers `jdbc:postgresql://` avec extraction des identifiants et ajout du paramètre `sslmode=require`.
- [x] **Paramétrage résilient du pool HikariCP** : Configuration d'un timeout strict de connexion à 15s (`connection-timeout: 15000`) et validation à 5s.
- [x] **Sécurisation de la clé JWT (`JwtUtil.java`)** : Dérivation automatique avec padding SHA-256 pour garantir 256 bits d'entropie quelle que soit la longueur de la variable d'environnement.
- [x] **Assouplissement et fiabilisation CORS (`SecurityConfig.java`)** : Support étendu des sous-domaines Vercel (`allowedOriginPatterns: https://*.vercel.app`) et nettoyage des slashes terminaux.
- [x] **Amélioration UX / Feedback frontend (`login.component.ts`, `register.component.ts`)** : Message explicite informant l'utilisateur en cas de mise en veille / réveil du serveur cloud.
- [x] **Vérification de la base PostgreSQL sur Render** : Vérifier que l'instance de base de données Render gratuite n'a pas dépassé son cycle de validité de 30 jours.
