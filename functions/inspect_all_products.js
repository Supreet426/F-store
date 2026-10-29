const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp({
        projectId: 'fryumm-5c865'
    });
}

const db = admin.firestore();

async function inspectAll() {
    const snapshot = await db.collection("products").get();
    console.log(`Total products in /products: ${snapshot.size}`);

    let missingInPublic = 0;
    const issuesSummary = {};

    for (const doc of snapshot.docs) {
        const data = doc.data();
        const pubSnap = await db.collection("public_products").doc(doc.id).get();

        const hasMerchant = Boolean(data.merchantId && typeof data.merchantId === "string" && data.merchantId.trim());
        const hasDarkStore = Boolean(data.darkStoreId && typeof data.darkStoreId === "string" && data.darkStoreId.trim());
        const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

        let hubReason = "";
        let expectedHubId = "";
        if (hasMerchant) {
            const mSnap = await db.collection("merchants").doc(data.merchantId.trim()).get();
            if (!mSnap.exists) {
                hubReason = "Merchant profile missing";
            } else {
                const mData = mSnap.data();
                if (mData.isApproved !== true || mData.isActive !== true) {
                    hubReason = "Merchant unapproved/inactive";
                }
                expectedHubId = (mData.registeredHubId || "").trim();
            }
        } else if (hasDarkStore) {
            const dsSnap = await db.collection("dark_stores").doc(data.darkStoreId.trim()).get();
            if (!dsSnap.exists) {
                hubReason = "Dark store record missing";
            } else {
                expectedHubId = (dsSnap.data().darkHubId || "").trim();
            }
        }

        const validHub = Boolean(data.darkHubId && typeof data.darkHubId === "string" && data.darkHubId.trim() === expectedHubId && expectedHubId !== "");

        const isVerified = data.isVerified === true;
        const isApproved = data.finalApproval === true || (data.isAdminOverride === true && Boolean(data.overrideReason));
        const isPublished = data.publishingStatus === "PUBLISHED";
        const isAvailable = data.isAvailable === true;

        if (!pubSnap.exists) {
            missingInPublic++;
            const reasons = [];
            if (!validXOR) reasons.push(`XOR_VIOLATION (hasMerchant:${hasMerchant}, hasDarkStore:${hasDarkStore})`);
            if (hubReason) reasons.push(`HUB_FAIL (${hubReason})`);
            else if (!validHub) reasons.push(`HUB_MISMATCH (prodDarkHubId:'${data.darkHubId}', expected:'${expectedHubId}')`);
            if (!isVerified) reasons.push("NOT_VERIFIED");
            if (!isApproved) reasons.push("NOT_APPROVED");
            if (!isPublished) reasons.push(`PUB_STATUS:'${data.publishingStatus}'`);
            if (!isAvailable) reasons.push("NOT_AVAILABLE");

            const reasonStr = reasons.join(", ");
            issuesSummary[reasonStr] = (issuesSummary[reasonStr] || 0) + 1;
            console.log(`❌ Missing in public_products: ID=${doc.id} | Name="${data.name}" | Reason: [${reasonStr}]`);
        }
    }

    console.log(`\n================ SUMMARY ================`);
    console.log(`Total Products: ${snapshot.size}`);
    console.log(`Missing in public_products: ${missingInPublic}`);
    console.log(`Issues Breakdown:`, issuesSummary);
}

inspectAll().then(() => process.exit(0)).catch(err => {
    console.error(err);
    process.exit(1);
});
