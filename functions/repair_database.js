const admin = require("firebase-admin");

if (!admin.apps.length) {
    admin.initializeApp();
}

const db = admin.firestore();

const isApplyMode = process.argv.includes("--apply");
console.log(`====================================================`);
console.log(`FLASHO DATABASE REPAIR UTILITY`);
console.log(`MODE: ${isApplyMode ? "APPLY (WRITES ENABLED)" : "DRY-RUN (NO WRITES MADE)"}`);
console.log(`====================================================\n`);

async function runRepair() {
    const report = {
        inspectedProducts: 0,
        repairedProducts: [],
        repairedMerchants: [],
        projectionAudit: []
    };

    // 1. Inspect Merchant 1EGtepHheWXplNxcaknVxChKxfp1
    console.log("--- 1. AUDITING MERCHANT '1EGtepHheWXplNxcaknVxChKxfp1' ---");
    const mRef = db.collection("merchants").doc("1EGtepHheWXplNxcaknVxChKxfp1");
    const mSnap = await mRef.get();

    if (mSnap.exists) {
        const mData = mSnap.data();
        console.log(`Merchant 1EGtepHheWXplNxcaknVxChKxfp1 found: tradeName='${mData.tradeName}', registeredHubId='${mData.registeredHubId}'`);
        if (!mData.registeredHubId || mData.registeredHubId === "undefined") {
            console.log(`ACTION NEEDED: Set registeredHubId = 'GHO-01' for Merchant 1EGtepHheWXplNxcaknVxChKxfp1.`);
            if (isApplyMode) {
                await mRef.update({ registeredHubId: "GHO-01", updatedAt: Date.now() });
                console.log(`APPLIED: Updated Merchant 1EGtepHheWXplNxcaknVxChKxfp1 registeredHubId to 'GHO-01'.`);
            }
            report.repairedMerchants.push("1EGtepHheWXplNxcaknVxChKxfp1 -> registeredHubId: GHO-01");
        } else {
            console.log(`Merchant registeredHubId is already valid: '${mData.registeredHubId}'. No fix needed.`);
        }
    } else {
        console.log("Merchant '1EGtepHheWXplNxcaknVxChKxfp1' not found in Firestore.");
    }
    console.log("");

    // 2. Inspect Manchuriyan prod_1792674780526
    console.log("--- 2. AUDITING MANCHURIYAN 'prod_1792674780526' ---");
    const manchRef = db.collection("products").doc("prod_1792674780526");
    const manchSnap = await manchRef.get();

    if (manchSnap.exists) {
        const pData = manchSnap.data();
        console.log(`Manchuriyan Data: name='${pData.name}', merchantId='${pData.merchantId}', darkStoreId='${pData.darkStoreId}', darkHubId='${pData.darkHubId}', finalApproval=${pData.finalApproval}, isVerified=${pData.isVerified}, publishingStatus='${pData.publishingStatus}'`);

        const hasMerchant = Boolean(pData.merchantId && typeof pData.merchantId === "string" && pData.merchantId.trim());
        const hasDarkStore = Boolean(pData.darkStoreId && typeof pData.darkStoreId === "string" && pData.darkStoreId.trim());

        if (hasMerchant || pData.darkStoreId) {
            console.log(`ACTION NEEDED: Preserving Merchant Product ownership for Manchuriyan. Setting merchantId='1EGtepHheWXplNxcaknVxChKxfp1', clearing darkStoreId, setting darkHubId='GHO-01'.`);
            const updates = {
                merchantId: "1EGtepHheWXplNxcaknVxChKxfp1",
                darkStoreId: null,
                darkHubId: "GHO-01",
                updatedAt: Date.now()
            };
            if (isApplyMode) {
                await manchRef.update(updates);
                console.log(`APPLIED: Manchuriyan prod_1792674780526 restored as Merchant product (merchantId='1EGtepHheWXplNxcaknVxChKxfp1', darkStoreId=null, darkHubId='GHO-01').`);
            }
            report.repairedProducts.push("prod_1792674780526 (Manchuriyan) -> merchantId: 1EGtepHheWXplNxcaknVxChKxfp1, darkStoreId: null");
        } else {
            console.log("Manchuriyan XOR ownership is valid.");
        }
    } else {
        console.log("Manchuriyan 'prod_1792674780526' not found in Firestore.");
    }
    console.log("");

    // 3. Inspect Draft Products ("Tata Salt", "Fresh Garlic", "fresh Onion")
    console.log("--- 3. AUDITING DRAFT PRODUCTS ---");
    const draftNames = ["Tata Salt", "Fresh Garlic", "fresh Onion"];
    const allProdSnap = await db.collection("products").get();

    for (const doc of allProdSnap.docs) {
        const pData = doc.data();
        if (draftNames.some(d => (pData.name || "").toLowerCase().includes(d.toLowerCase()))) {
            console.log(`Draft Audit: ID='${doc.id}', Name='${pData.name}', finalApproval=${pData.finalApproval}, isVerified=${pData.isVerified}, publishingStatus='${pData.publishingStatus}'`);
            console.log(`POLICY: Unapproved drafts MUST NOT be forcefully published without explicit admin approval action.`);
        }
    }
    console.log("");

    // 4. Audit Public Projection Eligibility across all products
    console.log("--- 4. AUDITING PUBLIC PROJECTION ELIGIBILITY & SYNC ---");
    const pubSnap = await db.collection("public_products").get();
    const publicMap = new Map();
    pubSnap.docs.forEach(d => publicMap.set(d.id, d.data()));

    let totalProducts = allProdSnap.size;
    let eligibleCount = 0;
    let projectedCount = publicMap.size;

    console.log(`Total Source Products in /products: ${totalProducts}`);
    console.log(`Total Projected Products in /public_products: ${projectedCount}`);

    for (const doc of allProdSnap.docs) {
        const pData = doc.data();
        const pId = doc.id;

        const hasMerchant = Boolean(pData.merchantId && typeof pData.merchantId === "string" && pData.merchantId.trim());
        const hasDarkStore = Boolean(pData.darkStoreId && typeof pData.darkStoreId === "string" && pData.darkStoreId.trim());
        const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

        const isVerified = pData.isVerified === true;
        const isApproved = pData.finalApproval === true || (pData.isAdminOverride === true && Boolean(pData.overrideReason));
        const isPublished = pData.publishingStatus === "PUBLISHED";
        const isAvailable = pData.isAvailable === true;

        const isEligible = validXOR && isVerified && isApproved && isPublished && isAvailable;

        if (isEligible) {
            eligibleCount++;
            const inPublic = publicMap.has(pId);
            if (!inPublic) {
                console.log(`MISMATCH: Eligible product '${pId}' ('${pData.name}') is MISSING from /public_products.`);
                report.projectionAudit.push(`Missing from /public_products: ${pId} (${pData.name})`);
            }
        }
    }

    console.log(`\nAudit Summary:`);
    console.log(`- Eligible Products for Public Projection: ${eligibleCount}`);
    console.log(`- Currently Projected in /public_products: ${projectedCount}`);
    console.log(`- Repaired Merchants: ${report.repairedMerchants.length}`);
    console.log(`- Repaired Products: ${report.repairedProducts.length}`);

    if (!isApplyMode) {
        console.log(`\n[DRY-RUN COMPLETE] To apply fixes, rerun with: node functions/repair_database.js --apply`);
    } else {
        console.log(`\n[APPLY COMPLETE] Database updates committed successfully.`);
    }
}

runRepair().catch(err => {
    console.error("Fatal Error running database repair utility:", err);
    process.exit(1);
});
