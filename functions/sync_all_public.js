const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

async function syncAllProducts() {
    console.log("Fetching all products...");
    const snapshot = await db.collection("products").get();
    console.log(`Found ${snapshot.docs.length} products.`);

    let updatedCount = 0;
    const batchSize = 10;
    let batch = db.batch();
    let count = 0;

    for (const doc of snapshot.docs) {
        batch.update(doc.ref, { updatedAt: Date.now() });
        count++;
        updatedCount++;
        if (count >= batchSize) {
            await batch.commit();
            console.log(`Committed batch of ${count}`);
            batch = db.batch();
            count = 0;
        }
    }
    if (count > 0) {
        await batch.commit();
        console.log(`Committed final batch of ${count}`);
    }

    console.log(`Successfully touched ${updatedCount} products to trigger onProductWritten sync!`);
}

syncAllProducts().then(() => process.exit(0)).catch(err => {
    console.error(err);
    process.exit(1);
});
