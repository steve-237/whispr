const fs = require('fs');
const file = 'frontend/src/app/features/inbox/inbox/inbox.component.html';
let c = fs.readFileSync(file, 'utf8');

// Fix: replace the pipe inside the action expression with a method call
c = c.replace(/\(click\)="profileDailyQuestion\.set\(\(q \| translate\)\)"/g, `(click)="selectQuickQuestionTranslation(q)"`);

fs.writeFileSync(file, c);
