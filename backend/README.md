# Whispr - Backend API (Spring Boot)

Ce dossier contient le code source de l'API REST de Whispr, développée en Java 21 avec Spring Boot.

## ⚙️ Prérequis

- **Java 21** installé (`JAVA_HOME` correctement configuré).
- **Maven** (bien que le Wrapper Maven `mvnw` soit inclus et recommandé).
- **Docker** pour lancer l'infrastructure de base de données.

## 🚀 Lancement Rapide

1. Assurez-vous que l'infrastructure Docker est lancée (via `docker-compose up -d` à la racine du projet).
2. Depuis ce dossier `backend/`, exécutez l'application via le wrapper Maven :
   ```bash
   # Sur Linux / macOS
   ./mvnw spring-boot:run
   
   # Sur Windows
   .\mvnw.cmd spring-boot:run
   ```

## 🗄️ Base de Données et Migrations

Le projet utilise **PostgreSQL**. La structure de la base de données est gérée automatiquement par **Flyway**.
- Au lancement de l'application, Spring Boot va demander à Flyway d'exécuter les scripts SQL se trouvant dans `src/main/resources/db/migration/`.
- La migration `V1__Initial_Schema.sql` crée toutes les tables de base (`users`, `profiles`, `links`, `messages`, `audit_logs`).

## 📚 Documentation API (Swagger UI)

L'API est documentée selon la spécification OpenAPI 3.x.
Une fois le serveur démarré, vous pouvez explorer et tester l'API de manière interactive via **Swagger UI**.
- Accédez à : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Pour la spécification JSON brute : [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 🧪 Exécution des Tests

Pour exécuter les tests unitaires et d'intégration :
```bash
.\mvnw.cmd clean test
```
