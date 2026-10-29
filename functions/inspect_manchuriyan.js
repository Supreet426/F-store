const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

async function inspectManchuriyan() {
    console.log("Searching for Manchuriyan in products collection...");
    const snapshot = await db.collection("products").get();
    const matches = snapshot.docs.filter(d => {
        const name = (d.data().name || "").toLowerCase();
        return name.includes("manchur") || name.includes("snack");
    });

    console.log(`Found ${matches.length} matching products.`);

    for (const doc of matches) {
        const data = doc.data();
        console.log(`\n========================================`);
        console.log(`Product ID: ${doc.id}`);
        console.log(`  name: "${data.name}"`);
        console.log(`  category: "${data.category}"`);
        console.log(`  merchantId: "${data.merchantId}" (type: ${typeof data.merchantId})`);
        console.log(`  darkStoreId: "${data.darkStoreId}" (type: ${typeof data.darkStoreId})`);
        console.log(`  darkHubId: "${data.darkHubId}" (type: ${typeof data.darkHubId})`);
        console.log(`  isVerified: ${data.isVerified}`);
        console.log(`  finalApproval: ${data.finalApproval}`);
        console.log(`  isAdminOverride: ${data.isAdminOverride}`);
        console.log(`  overrideReason: "${data.overrideReason}"`);
        console.log(`  publishingStatus: "${data.publishingStatus}"`);
        console.log(`  isAvailable: ${data.isAvailable}`);
        console.log(`  isInStock: ${data.isInStock}`);
        console.log(`  stockQuantity: ${data.stockQuantity}`);

        // Check XOR Ownership
        const hasMerchant = Boolean(data.merchantId && typeof data.merchantId === "string" && data.merchantId.trim());
        const hasDarkStore = Boolean(data.darkStoreId && typeof data.darkStoreId === "string" && data.darkStoreId.trim());
        const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);
        console.log(`  [CHECK] validXOR: ${validXOR} (hasMerchant: ${hasMerchant}, hasDarkStore: ${hasDarkStore})`);

        // Check merchant/darkstore record
        if (hasMerchant) {
            const mSnap = await db.collection("merchants").doc(data.merchantId.trim()).get();
            if (!mSnap.exists) {
                console.log(`  [CHECK] Merchant doc exists: FALSE`);
            } else {
                const mData = mSnap.data();
                console.log(`  [CHECK] Merchant doc: isApproved=${mData.isApproved}, isActive=${mData.isActive}, registeredHubId="${mData.registeredHubId}"`);
            }
        } else if (hasDarkStore) {
            const dsSnap = await db.collection("dark_stores").doc(data.darkStoreId.trim()).get();
            if (!dsSnap.exists) {
                console.log(`  [CHECK] DarkStore doc exists: FALSE`);
            } else {
                const dsData = dsSnap.data();
                console.log(`  [CHECK] DarkStore doc: darkHubId="${dsData.darkHubId}"`);
            }
        }

        // Check if present in public_products
        const pubSnap = await db.collection("public_products").doc(doc.id).get();
        console.log(`  [PUBLIC_PRODUCTS] Exists in public_products: ${pubSnap.exists}`);
        if (pubSnap.exists) {
            console.log(`  [PUBLIC_PRODUCTS] Data:`, pubSnap.data());
        }
    }
}

inspectManchuriyan().then(() => process.exit(0)).catch(err => {
    console.error(err);
    process.exit(1);
});
