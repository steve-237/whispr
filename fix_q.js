const fs = require('fs');
const file = 'frontend/src/app/features/inbox/inbox/inbox.component.ts';
let c = fs.readFileSync(file, 'utf8');
c = c.replace(/quickQuestions = signal<string\[\]>\(\[[\s\S]*?\]\);/, `quickQuestions = signal<string[]>([
    'INBOX.Q1',
    'INBOX.Q2',
    'INBOX.Q3',
    'INBOX.Q4',
    'INBOX.Q5'
  ]);`);
fs.writeFileSync(file, c);
