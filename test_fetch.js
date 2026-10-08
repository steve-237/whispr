const fs = require('fs');

async function test() {
    const res = await fetch('https://whispr-z6zx.onrender.com/api/health');
    console.log(await res.text());
}
test();
