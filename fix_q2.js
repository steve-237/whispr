const fs = require('fs');
const file = 'frontend/src/app/features/inbox/inbox/inbox.component.html';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(/<button \*ngFor="let q of quickQuestions\(\)" \(click\)="profileDailyQuestion\.set\(q\)"/g, `<button *ngFor="let q of quickQuestions()" (click)="profileDailyQuestion.set((q | translate))"`);

c = c.replace(/\{\{\s*q\.length > 28 \? q\.substring\(0, 28\) \+ '\.\.\.' : q\s*\}\}/g, `{{ (q | translate).length > 28 ? (q | translate).substring(0, 28) + '...' : (q | translate) }}`);

fs.writeFileSync(file, c);
