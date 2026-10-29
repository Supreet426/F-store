const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

async function inspectBreakfastProducts() {
    console.log("Inspecting Breakfast category products...");
    const snapshot = await db.collection("products").get();
    const breakfastProds = snapshot.docs.filter(d => {
        const cat = (d.data().category || "").toLowerCase();
        return cat.includes("breakfast") || cat.includes("morning");
    });

    console.log(`Found ${breakfastProds.length} Breakfast/Morning products.`);

    for (const doc of breakfastProds) {
        const data = doc.data();
        console.log(`\nProduct ID: ${doc.id} - ${data.name}`);
        console.log(`  category: "${data.category}"`);
        console.log(`  merchantId: "${data.merchantId}"`);
        console.log(`  darkStoreId: "${data.darkStoreId}"`);
        console.log(`  darkHubId: "${data.darkHubId}"`);
        console.log(`  isVerified: ${data.isVerified}, finalApproval: ${data.finalApproval}, publishingStatus: "${data.publishingStatus}"`);
        console.log(`  isAvailable: ${data.isAvailable}, isInStock: ${data.isInStock}, isComingSoon: ${data.isComingSoon}`);

        // Check merchant profile if merchantId set
        if (data.merchantId) {
            const mSnap = await db.collection("merchants").doc(data.merchantId.trim()).get();
            if (!mSnap.exists) {
                console.log(`  ❌ MERCHANT PROFILE MISSING for ID "${data.merchantId}"!`);
            } else {
                const mData = mSnap.data();
                console.log(`  Merchant profile exists: isApproved=${mData.isApproved}, isActive=${mData.isActive}, registeredHubId="${mData.registeredHubId}"`);
            }
        }

        // Check if exists in public_products
        const pubSnap = await db.collection("public_products").doc(doc.id).get();
        console.log(`  In public_products: ${pubSnap.exists}`);
    }
}

inspectBreakfastProducts().then(() => process.exit(0)).catch(err => {
    console.error(err);
    process.exit(1);
});
