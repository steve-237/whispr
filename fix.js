const fs = require('fs');
const file = 'frontend/src/app/shared/components/header/header.ts';
let c = fs.readFileSync(file, 'utf8');
c = c.replace(/\{\{.*?translate\.currentLang\(\) === .*?\}\}/, "{{ translate.currentLang() === 'fr' ? '🇫🇷' : '🇬🇧' }}");
fs.writeFileSync(file, c);
