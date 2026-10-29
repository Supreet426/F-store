const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

async function strictCleanIsVerified() {
    console.log("Strictly standardizing 'isVerified' field across all product documents...");

    // 1. Clean 'products' collection
    const prodSnap = await db.collection("products").get();
    console.log(`Processing ${prodSnap.docs.length} documents in 'products'...`);

    let prodBatch = db.batch();
    let prodCount = 0;

    for (const doc of prodSnap.docs) {
        const data = doc.data();
        const verifiedVal = data.isVerified !== undefined ? Boolean(data.isVerified) : (data.verified !== undefined ? Boolean(data.verified) : true);

        const updateData = {
            isVerified: verifiedVal,
            verified: admin.firestore.FieldValue.delete()
        };

        prodBatch.update(doc.ref, updateData);
        prodCount++;
    }

    if (prodCount > 0) {
        await prodBatch.commit();
        console.log(`Successfully standardized 'isVerified' in ${prodCount} documents in 'products'.`);
    }

    // 2. Clean 'public_products' collection
    const pubSnap = await db.collection("public_products").get();
    console.log(`Processing ${pubSnap.docs.length} documents in 'public_products'...`);

    let pubBatch = db.batch();
    let pubCount = 0;

    for (const doc of pubSnap.docs) {
        const data = doc.data();
        const verifiedVal = data.isVerified !== undefined ? Boolean(data.isVerified) : (data.verified !== undefined ? Boolean(data.verified) : true);

        const updateData = {
            isVerified: verifiedVal,
            verified: admin.firestore.FieldValue.delete()
        };

        pubBatch.update(doc.ref, updateData);
        pubCount++;
    }

    if (pubCount > 0) {
        await pubBatch.commit();
        console.log(`Successfully standardized 'isVerified' in ${pubCount} documents in 'public_products'.`);
    }
}

strictCleanIsVerified().then(() => process.exit(0)).catch(err => {
    console.error("Migration error:", err);
    process.exit(1);
});
