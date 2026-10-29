const admin = require("firebase-admin");

if (!admin.apps.length) {
    admin.initializeApp();
}

const db = admin.firestore();

async function syncAllEligibleProducts() {
    console.log("Starting projection sync for all eligible products...");
    const now = Date.now();
    const prodSnap = await db.collection("products").get();

    let synced = 0;
    let skipped = 0;

    for (const doc of prodSnap.docs) {
        const data = doc.data();
        const productId = doc.id;

        let hasMerchant = Boolean(data.merchantId && typeof data.merchantId === "string" && data.merchantId.trim());
        let hasDarkStore = Boolean(data.darkStoreId && typeof data.darkStoreId === "string" && data.darkStoreId.trim());

        // Legacy XOR self-healing
        if (hasMerchant && hasDarkStore && (data.darkStoreId.trim() === (data.darkHubId || "").trim() || data.darkStoreId.trim().startsWith("GHO-") || data.darkStoreId.trim().startsWith("HUB-"))) {
            hasDarkStore = false;
        }

        const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);
        if (!validXOR) {
            console.log(`Skipping '${productId}' (${data.name}): Invalid XOR ownership.`);
            skipped++;
            continue;
        }

        let expectedHubId = "";
        if (hasMerchant) {
            const mSnap = await db.collection("merchants").doc(data.merchantId.trim()).get();
            if (!mSnap.exists) {
                console.log(`Skipping '${productId}' (${data.name}): Merchant record missing.`);
                skipped++;
                continue;
            }
            const mData = mSnap.data();
            const regHub = (mData.registeredHubId || "").trim();
            expectedHubId = (regHub && regHub !== "undefined") ? regHub : ((mData.darkHubId || "").trim() || (data.darkHubId || "").trim());
        } else if (hasDarkStore) {
            expectedHubId = (data.darkHubId || data.darkStoreId || "").trim();
        }

        const isVerified = data.isVerified === true;
        const isApproved = data.finalApproval === true || (data.isAdminOverride === true && Boolean(data.overrideReason));
        const isPublished = data.publishingStatus === "PUBLISHED";
        const isAvailable = data.isAvailable === true;

        if (!isVerified || !isApproved || !isPublished || !isAvailable) {
            console.log(`Skipping '${productId}' (${data.name}): Ineligible (verified=${isVerified}, approved=${isApproved}, published=${isPublished}, available=${isAvailable}).`);
            skipped++;
            continue;
        }

        const stock = data.stockQuantity !== undefined ? data.stockQuantity : (data.stockCount || 0);

        const publicData = {
            id: productId,
            name: data.name || "",
            brand: data.brand || "",
            description: data.description || "",
            shortDescription: data.shortDescription || "",
            price: data.price || 0,
            originalPrice: data.originalPrice || 0,
            imageUrl: data.imageUrl || "",
            images: data.images || [],
            category: data.category || "",
            subCategory: data.subCategory || "",
            stockQuantity: stock,
            isInStock: data.isInStock === true,
            isAvailable: data.isAvailable === true,
            isVerified: data.isVerified === true,
            isComingSoon: Boolean(data.isComingSoon),
            darkHubId: expectedHubId,
            merchantId: hasMerchant ? data.merchantId.trim() : null,
            darkStoreId: hasDarkStore ? data.darkStoreId.trim() : null,
            barcode: data.barcode || "",
            unit: data.unit || "1 unit",
            weight: data.weight || "",
            variants: data.variants || [],
            availableCoupons: data.availableCoupons || [],
            rating: data.rating || 0,
            ratingCount: data.ratingCount || 0,
            searchNames: data.searchNames || [],
            isFoodProduct: data.isFoodProduct || false,
            minOrderQuantity: data.minOrderQuantity || 1,
            freeDeliveryMinAmount: data.freeDeliveryMinAmount || 0,
            freeDeliveryMinQuantity: data.freeDeliveryMinQuantity || 0,
            sequenceNumber: data.sequenceNumber || 999,
            isPreorderEnabled: Boolean(data.isPreorderEnabled),
            currentPreorderCount: data.currentPreorderCount || 0,
            maxPreorderCapacity: data.maxPreorderCapacity || 0,
            preorderStart: data.preorderStart || null,
            preorderEnd: data.preorderEnd || null,
            flashSale: Boolean(data.flashSale),
            saleStart: data.saleStart || null,
            saleEnd: data.saleEnd || null,
            discountPercent: data.discountPercent || 0,
            projectionVersion: now,
            updatedAt: now
        };

        await db.collection("public_products").doc(productId).set(publicData);
        console.log(`✅ SYNCED to /public_products: '${productId}' (${data.name})`);
        synced++;
    }

    console.log(`\nSync finished! Synced: ${synced}, Skipped: ${skipped}`);
}

syncAllEligibleProducts().catch(err => {
    console.error("Error running syncAllEligibleProducts:", err);
    process.exit(1);
});
