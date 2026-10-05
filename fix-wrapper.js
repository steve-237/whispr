const fs = require('fs');
const path = 'frontend/src/app/features/inbox/inbox/inbox.component.html';
let content = fs.readFileSync(path, 'utf8');

// The file currently has:
// <!-- Bloc Paramètres --><div *ngIf="showSettings()" class="animate-fade-in" style="margin-bottom: 2rem;">
//   <!-- Lien Secret -->
content = content.replace('<!-- Bloc Paramètres --><div *ngIf="showSettings()" class="animate-fade-in" style="margin-bottom: 2rem;">\n  <!-- Lien Secret -->', '<!-- Lien Secret -->');

// And it also has TWO `</div>` before `<!-- Liste des Messages -->` because I added one in my last script.
// Let's replace any multiple `</div>` right before `<!-- Liste des Messages -->` with a single `</div>`.
content = content.replace(/<\/div>\s*<\/div>\s*<!-- Liste des Messages -->/, '</div>\n\n  <!-- Liste des Messages -->');

fs.writeFileSync(path, content, 'utf8');
console.log('Fixed extra wrapper');
