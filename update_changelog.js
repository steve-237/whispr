const fs = require('fs');
const file = 'CHANGELOG.md';
let content = fs.readFileSync(file, 'utf8');

const newRelease = `## [1.1.0] - 2026-10-08
### 🚀 Améliorations et Correctifs
- **WebSockets** : Correction de l'abonnement en temps réel. Les messages arrivent maintenant instantanément sans recharger la page.
- **Messages** : L'état "Lu" (isRead) est maintenant correctement sauvegardé dans la base de données et les messages lus ne s'affichent plus comme nouveaux après un rechargement.
- **UI/UX Mobile** : Amélioration générale du design sur mobile, notamment pour la navigation, la mise en page des messages, et l'affichage des textes.
- **Traductions** : Traduction dynamique des questions rapides de profil et mise à jour des traductions manquantes.
- **Backend CI/CD** : Mise à jour de la configuration de déploiement Render et correction des tests qui bloquaient le pipeline.
- **Anti-Spam** : Ajustement de la limite de messages (de 5 à 50) pour faciliter les tests utilisateurs.

`;

content = content.replace('## [1.0.0]', newRelease + '## [1.0.0]');
fs.writeFileSync(file, content);
