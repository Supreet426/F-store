const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

// Copy of syncPublicProjectionInternal logic to debug
async function debugSync(productId) {
    const prodRef = db.collection("products").doc(productId);
    const pubRef = db.collection("public_products").doc(productId);

    const now = Date.now();

    const prodSnap = await prodRef.get();
    const pubSnap = await pubRef.get();

    if (!prodSnap.exists) return console.log("Product does not exist.");

    const data = prodSnap.data();

    const hasMerchant = Boolean(data.merchantId && typeof data.merchantId === "string" && data.merchantId.trim());
    const hasDarkStore = Boolean(data.darkStoreId && typeof data.darkStoreId === "string" && data.darkStoreId.trim());
    const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

    if (!validXOR) {
        return console.log("Failed validXOR:", { hasMerchant, hasDarkStore });
    }

    let expectedHubId = "";
    if (hasMerchant) {
        const mSnap = await db.collection("merchants").doc(data.merchantId.trim()).get();
        if (!mSnap.exists) return console.log("Failed merchant profile exists");
        expectedHubId = (mSnap.data().registeredHubId || "").trim();
    } else if (hasDarkStore) {
        const dsSnap = await db.collection("dark_stores").doc(data.darkStoreId.trim()).get();
        if (!dsSnap.exists) return console.log("Failed dsSnap exists");
        expectedHubId = (dsSnap.data().darkHubId || "").trim();
    }

    const validHub = Boolean(data.darkHubId && typeof data.darkHubId === "string" && data.darkHubId.trim() === expectedHubId && expectedHubId !== "");
    if (!validHub) {
        return console.log("Failed validHub:", { darkHubId: data.darkHubId, expectedHubId });
    }

    const isLegacy = data.publishingStatus === undefined;
    const isVerified = data.isVerified === true || data.verified === true || isLegacy;
    const isApproved = data.finalApproval === true || (data.isAdminOverride === true && Boolean(data.overrideReason)) || isLegacy;
    const isPublished = data.publishingStatus === "PUBLISHED" || isLegacy;
    const isAvailable = data.isAvailable === true || data.available === true || isLegacy;

    console.log("Checks:", { isVerified, isApproved, isPublished, isAvailable });
}

debugSync("prod_1792756410547").then(() => process.exit(0)).catch(err => {
    console.error(err);
    process.exit(1);
});
