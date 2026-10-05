const fs = require('fs');
const path = 'frontend/src/app/features/inbox/inbox/inbox.component.html';
let content = fs.readFileSync(path, 'utf8');

// 1. Fix the wrapper logic
const blocParamStart = `<!-- Bloc Paramètres -->\n  <div *ngIf="showSettings()" class="animate-fade-in" style="margin-bottom: 2rem;">\n  <!-- Lien Secret -->`;
content = content.replace(blocParamStart, `<!-- Lien Secret -->`);

const listStart = `  </div>\n\n  <!-- Liste des Messages -->`;
content = content.replace(listStart, `  <!-- Liste des Messages -->`);

const customStart = `  <!-- Personnalisation de Profil -->`;
const customStartNew = `<!-- Bloc Paramètres -->\n  <div *ngIf="showSettings()" class="animate-fade-in" style="margin-bottom: 2rem;">\n  <!-- Personnalisation de Profil -->`;
content = content.replace(customStart, customStartNew);

const listStart2 = `  <!-- Liste des Messages -->`;
const listStart2New = `  </div>\n\n  <!-- Liste des Messages -->`;
content = content.replace(listStart2, listStart2New);

// 2. Fix Double Encoded Characters
content = content.replace(/ðŸ“ /g, '📍');
content = content.replace(/\?\?\?\?\?\?/g, '👀');
content = content.replace(/Ã‰crire une rÃ©ponse/g, 'Écrire une réponse');
content = content.replace(/Ma rÃ©ponse/g, 'Ma réponse');
content = content.replace(/ðŸ¤«/g, '🤫');
content = content.replace(/ðŸ”—/g, '🔗');

fs.writeFileSync(path, content, 'utf8');
console.log('HTML fixed successfully');
