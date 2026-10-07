const fs = require('fs');

const frPath = 'frontend/public/assets/i18n/fr.json';
const frData = JSON.parse(fs.readFileSync(frPath, 'utf8'));
frData.INBOX.Q1 = "Posez-moi une question anonyme et sincère... 🤫";
frData.INBOX.Q2 = "Quel est votre avis honnête sur moi ? 👀";
frData.INBOX.Q3 = "Avoue-moi un secret en toute discrétion... 🤫";
frData.INBOX.Q4 = "Un défaut ou une qualité que vous me trouvez ? 🤔";
frData.INBOX.Q5 = "Quelle est votre première impression de moi ? 🧐";
fs.writeFileSync(frPath, JSON.stringify(frData, null, 2));

const enPath = 'frontend/public/assets/i18n/en.json';
const enData = JSON.parse(fs.readFileSync(enPath, 'utf8'));
enData.INBOX.Q1 = "Ask me an anonymous and sincere question... 🤫";
enData.INBOX.Q2 = "What is your honest opinion about me? 👀";
enData.INBOX.Q3 = "Confess a secret to me discreetly... 🤫";
enData.INBOX.Q4 = "A flaw or a quality you find in me? 🤔";
enData.INBOX.Q5 = "What was your first impression of me? 🧐";
fs.writeFileSync(enPath, JSON.stringify(enData, null, 2));
