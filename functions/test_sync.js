const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

async function testSync() {
    const productId = "prod_1792756410547";
    const prodRef = db.collection("products").doc(productId);
    const pubRef = db.collection("public_products").doc(productId);

    const prodSnap = await prodRef.get();
    const data = prodSnap.data();
    console.log("Data:", data);

    const hasMerchant = Boolean(data.merchantId && typeof data.merchantId === "string" && data.merchantId.trim());
    const hasDarkStore = Boolean(data.darkStoreId && typeof data.darkStoreId === "string" && data.darkStoreId.trim());
    console.log("hasMerchant:", hasMerchant, "hasDarkStore:", hasDarkStore);

    if (hasDarkStore) {
        const dsSnap = await db.collection("dark_stores").doc(data.darkStoreId.trim()).get();
        console.log("dsSnap exists:", dsSnap.exists);
        if (dsSnap.exists) {
            console.log("dsSnap data:", dsSnap.data());
        }
    }
}

testSync().then(() => process.exit(0)).catch(err => {
    console.error(err);
    process.exit(1);
});
