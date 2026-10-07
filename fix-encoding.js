const fs = require('fs');
const path = require('path');

function walk(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(function(file) {
        file = dir + '/' + file;
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            results = results.concat(walk(file));
        } else {
            if (file.endsWith('.ts') || file.endsWith('.html') || file.endsWith('.css')) {
                results.push(file);
            }
        }
    });
    return results;
}

const files = walk('frontend/src');

for (let f of files) {
    let original = fs.readFileSync(f, 'utf8');
    let content = original;
    
    content = content.replace(/ðŸ—‘ï¸ /g, '🗑️');
    content = content.replace(/ðŸ”‘/g, '🔑');
    content = content.replace(/ðŸ”®/g, '🔮');
    content = content.replace(/ðŸŒ¿/g, '🌿');

    if (content !== original) {
        fs.writeFileSync(f, content, 'utf8');
        console.log('Fixed more in', f);
    }
}
