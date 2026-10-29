const admin = require('firebase-admin');

admin.initializeApp();

const db = admin.firestore();

async function inspect() {
    const snapshot = await db.collection('products').limit(5).get();
    console.log(`Found ${snapshot.size} products.`);
    snapshot.forEach(doc => {
        console.log(`--- Product ID: ${doc.id} ---`);
        const data = doc.data();
        for (const [key, val] of Object.entries(data)) {
            console.log(`  ${key}: ${typeof val} = ${JSON.stringify(val)}`);
        }
    });
}

inspect().catch(console.error);