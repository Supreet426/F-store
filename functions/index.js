const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

// Master Admin Whitelist
const MASTER_ADMIN_EMAILS = ["ranjanamishra1117@gmail.com", "supreet.mishr@gmail.com"];

function isMasterAdminEmail(email) {
    return email && MASTER_ADMIN_EMAILS.includes(email.toLowerCase().trim());
}

// Trusted User / Account Verification Helper - STRICT CANONICAL ONLY (FAIL CLOSED)
async function verifyActiveCaller(db, callerUid, callerEmail) {
    const cleanEmail = (callerEmail || "").toLowerCase().trim();
    if (isMasterAdminEmail(cleanEmail)) {
        return { uid: callerUid, role: "ADMIN", isMasterAdmin: true };
    }

    // Check users collection
    const userSnap = await db.collection("users").doc(callerUid).get();
    if (userSnap.exists) {
        const u = userSnap.data();
        if (u.isActive !== true) {
            throw new functions.https.HttpsError("permission-denied", "User account is disabled or inactive.");
        }
        return { uid: callerUid, role: (u.role || u.userRole || "CUSTOMER").toUpperCase(), isMasterAdmin: false };
    }

    // Check merchants collection - ENFORCE ONBOARDING & ACCESS GATE (STRICT CANONICAL ONLY)
    const merchantSnap = await db.collection("merchants").doc(callerUid).get();
    if (merchantSnap.exists) {
        const m = merchantSnap.data();
        if (m.isApproved !== true || m.isActive !== true) {
            throw new functions.https.HttpsError("permission-denied", "Merchant account is unapproved, disabled, or inactive.");
        }
        return { uid: callerUid, role: "MERCHANT", registeredHubId: m.registeredHubId || "", isMasterAdmin: false };
    }

    // Check staff collection
    const staffSnap = await db.collection("staff").doc(callerUid).get();
    if (staffSnap.exists) {
        const s = staffSnap.data();
        if (s.isActive !== true) {
            throw new functions.https.HttpsError("permission-denied", "Staff account is disabled or inactive.");
        }
        return { uid: callerUid, role: "STAFF", staffDepartment: (s.staffDepartment || "").toUpperCase(), assignedHubId: s.assignedHubId || "", isMasterAdmin: false };
    }

    throw new functions.https.HttpsError("permission-denied", "Caller identity not found or inactive.");
}

exports.createAppUser = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated."
        );
    }

    const { email, password, name, phone, role, staffDepartment } = data;

    if (!email || !password || !name) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Email, password, and name are required."
        );
    }

    try {
        const userRecord = await admin.auth().createUser({
            email: email,
            password: password,
            displayName: name,
            phoneNumber: phone && phone.startsWith("+") ? phone : undefined
        });

        const targetCollection = role === "ADMIN" ? "users" : (role === "RIDER" ? "riders" : (role === "OTHER" ? "staff" : (role === "CUSTOMER" ? "customers" : "merchants")));

        if (role === "ADMIN") {
            if (!isMasterAdminEmail(email)) {
                throw new functions.https.HttpsError("permission-denied", "Security Violation: Only whitelisted master admins can be assigned ADMIN role.");
            }
            await admin.auth().setCustomUserClaims(userRecord.uid, { admin: true });
        }

        await admin.firestore().collection(targetCollection).doc(userRecord.uid).set({
            uid: userRecord.uid,
            name: name,
            email: email,
            phoneNumber: phone || "",
            role: role || "CUSTOMER",
            staffDepartment: role === "OTHER" ? (staffDepartment || "") : "",
            isActive: true,
            createdAt: Date.now()
        });

        return {
            success: true,
            uid: userRecord.uid,
            message: "User account created successfully."
        };
    } catch (error) {
        console.error("Error creating user in Cloud Function:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

exports.updateUserRole = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated."
        );
    }

    const callerUid = context.auth.uid;
    const { targetUid, newRole } = data || {};

    if (!targetUid || typeof targetUid !== "string" || !targetUid.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "Target user ID (targetUid) is required.");
    }

    if (!newRole || typeof newRole !== "string" || !newRole.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "New role (newRole) is required.");
    }

    const cleanRole = newRole.trim().toUpperCase();
    const ALLOWED_ROLES = ["CUSTOMER", "ADMIN", "MERCHANT", "RIDER", "OTHER"];

    if (!ALLOWED_ROLES.includes(cleanRole)) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            `Invalid role '${cleanRole}'. Allowed roles are: ${ALLOWED_ROLES.join(", ")}.`
        );
    }

    const db = admin.firestore();
    const callerDoc = await db.collection("users").doc(callerUid).get();
    const callerData = callerDoc.exists ? callerDoc.data() : {};
    const callerRole = (callerData.role || callerData.userRole || "").toUpperCase();
    const callerEmail = (callerData.email || context.auth.token.email || "").toLowerCase().trim();

    const isCallerAdmin = callerRole === "ADMIN" || context.auth.token.admin === true || isMasterAdminEmail(callerEmail);

    if (!isCallerAdmin) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Security Violation: Only administrators can modify user roles."
        );
    }

    const targetDocRef = db.collection("users").doc(targetUid);
    const targetDoc = await targetDocRef.get();

    if (!targetDoc.exists) {
        throw new functions.https.HttpsError(
            "not-found",
            `User document for UID '${targetUid}' not found.`
        );
    }

    const targetData = targetDoc.data();
    const currentTargetRole = (targetData.role || targetData.userRole || "").toUpperCase();

    try {
        if (currentTargetRole === "MERCHANT" && cleanRole !== "MERCHANT") {
            const financialAuditData = {
                uid: targetUid,
                email: targetData.email || "",
                legalName: targetData.legalName || "",
                businessType: targetData.businessType || "",
                gstin: targetData.gstin || "",
                fssaiNumber: targetData.fssaiNumber || "",
                fssaiExpiryDate: targetData.fssaiExpiryDate || "",
                panNumber: targetData.panNumber || "",
                bankName: targetData.bankName || "",
                bankAccountName: targetData.bankAccountName || "",
                bankAccountNumber: targetData.bankAccountNumber || "",
                bankIfscCode: targetData.bankIfscCode || "",
                upiId: targetData.upiId || "",
                storeAddress: targetData.storeAddress || "",
                city: targetData.city || "",
                state: targetData.state || "",
                pincode: targetData.pincode || "",
                archivedAt: Date.now(),
                archivedBy: callerUid,
                previousRole: currentTargetRole,
                newRole: cleanRole
            };

            await db.collection("merchant_financial_archives").doc(targetUid).set(financialAuditData, { merge: true });
        }

        if (cleanRole === "ADMIN") {
            await admin.auth().setCustomUserClaims(targetUid, { admin: true, role: "ADMIN" });
        } else {
            await admin.auth().setCustomUserClaims(targetUid, { admin: false, role: cleanRole });
        }

        const updateData = {
            role: cleanRole,
            userRole: cleanRole,
            updatedAt: Date.now()
        };

        if (currentTargetRole === "MERCHANT" && cleanRole !== "MERCHANT") {
            updateData.isApproved = false;
            updateData.isActive = false;
            updateData.blockReason = `Role changed from MERCHANT to ${cleanRole}. Active selling privileges revoked. Financial records archived.`;
        }

        await targetDocRef.set(updateData, { merge: true });

        return {
            success: true,
            uid: targetUid,
            previousRole: currentTargetRole,
            newRole: cleanRole,
            message: `User role updated successfully to ${cleanRole}. Merchant financial records securely archived.`
        };
    } catch (error) {
        console.error("Error in updateUserRole Cloud Function:", error);
        if (error instanceof functions.https.HttpsError) throw error;
        throw new functions.https.HttpsError("internal", error.message || "Failed to update user role.");
    }
});

exports.onOrderCreated = functions.firestore
    .document("orders/{orderId}")
    .onCreate(async (snapshot, context) => {
        const orderData = snapshot.data();
        const orderId = context.params.orderId;

        if (!orderData.deliveryOtp) {
            const generatedOtp = Math.floor(1000 + Math.random() * 9000).toString();
            try {
                await snapshot.ref.update({
                    deliveryOtp: generatedOtp,
                    otpGeneratedAt: Date.now()
                });
            } catch (err) {
                console.error("Error setting delivery OTP:", err);
            }
        }

        try {
            const message = {
                topic: "all_riders",
                notification: {
                    title: "⚡ New Flash-O Order Available!",
                    body: `New order #${orderId.slice(0, 6)} placed. Tap to review details & accept.`
                },
                data: {
                    type: "NEW_ORDER",
                    orderId: orderId,
                    amount: String(orderData.totalAmount || orderData.grandTotal || 0)
                },
                android: {
                    priority: "high",
                    notification: { sound: "default" }
                }
            };
            await admin.messaging().send(message);
        } catch (err) {
            console.error("Error broadcasting new order to riders topic:", err);
        }
    });

exports.onFcmBroadcastCreated = functions.firestore
    .document("fcm_broadcasts/{broadcastId}")
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        const broadcastId = context.params.broadcastId;

        if (data.status === "sent") return null;

        try {
            const targetTopic = data.topic || "all_customers";
            const message = {
                topic: targetTopic,
                notification: {
                    title: data.title || "Flash-O Alert",
                    body: data.body || ""
                },
                data: {
                    type: "ADMIN_BROADCAST",
                    broadcastId: broadcastId,
                    click_action: "FLUTTER_NOTIFICATION_CLICK"
                },
                android: {
                    priority: "high",
                    notification: {
                        sound: "default",
                        channelId: "general_notifications"
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            contentAvailable: true,
                            sound: "default"
                        }
                    }
                }
            };

            if (data.imageUrl) {
                message.notification.imageUrl = data.imageUrl;
                message.android = { notification: { imageUrl: data.imageUrl } };
            }

            await admin.messaging().send(message);

            return snapshot.ref.update({
                status: "sent",
                sentAt: admin.firestore.FieldValue.serverTimestamp()
            });
        } catch (error) {
            console.error("Error sending FCM broadcast:", error);
            return snapshot.ref.update({ status: "failed", error: error.message });
        }
    });

exports.onOrderStatusUpdated = functions.firestore
    .document("orders/{orderId}")
    .onUpdate(async (change, context) => {
        const beforeData = change.before.data();
        const afterData = change.after.data();
        const orderId = context.params.orderId;

        if (beforeData.orderStatus !== "DELIVERED" && afterData.orderStatus === "DELIVERED") {
            const items = afterData.items || [];
            if (items.length > 0) {
                const db = admin.firestore();
                try {
                    await db.runTransaction(async (transaction) => {
                        for (const item of items) {
                            const productId = item.productId || item.id;
                            if (!productId) continue;

                            const productRef = db.collection("products").doc(productId);
                            const prodDoc = await transaction.get(productRef);

                            if (prodDoc.exists) {
                                const currentStock = prodDoc.data().stockCount !== undefined
                                    ? prodDoc.data().stockCount
                                    : (prodDoc.data().stock !== undefined ? prodDoc.data().stock : 10);
                                const qty = parseInt(item.quantity) || 1;
                                const newStock = Math.max(0, currentStock - qty);

                                transaction.update(productRef, {
                                    stockCount: newStock,
                                    stockQuantity: newStock,
                                    stock: newStock,
                                    inStock: newStock > 0,
                                    isInStock: newStock > 0,
                                    isAvailable: newStock > 0,
                                    updatedAt: Date.now()
                                });
                            }
                        }
                    });
                } catch (err) {
                    console.error("Error in stock deduction transaction:", err);
                }
            }
        }

        if (beforeData.orderStatus !== afterData.orderStatus && (afterData.customerUid || afterData.userId)) {
            const customerUid = afterData.customerUid || afterData.userId;
            try {
                const userDoc = await admin.firestore().collection("customers").doc(customerUid).get();
                const fcmToken = userDoc.exists ? userDoc.data().fcmToken : null;

                if (fcmToken) {
                    await admin.messaging().send({
                        token: fcmToken,
                        notification: {
                            title: `Order Update: #${orderId.slice(0, 6)}`,
                            body: `Your order status has changed to ${afterData.orderStatus}.`
                        },
                        data: {
                            type: "ORDER_STATUS_UPDATE",
                            orderId: orderId,
                            status: afterData.orderStatus
                        }
                    });
                }
            } catch (err) {
                console.error("Error sending order status push to customer:", err);
            }
        }
    });

exports.onNotificationCreated = functions.firestore
    .document("notifications/{notificationId}")
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        const notificationId = context.params.notificationId;

        if (data.status !== "pending") return null;

        try {
            let message;

            if (data.type === "BROADCAST") {
                let targetTopic = "all_customers";
                if (data.target === "riders") targetTopic = "riders";
                else if (data.target === "merchants") targetTopic = "merchants";
                else if (data.target === "active_customers") targetTopic = "active_customers";
                else if (data.target === "inactive_customers") targetTopic = "inactive_customers";

                message = {
                    topic: targetTopic,
                    notification: {
                        title: data.title,
                        body: data.body,
                    },
                    data: {
                        type: "PROMOTION",
                        actionUrl: data.actionUrl || "",
                        imageUrl: data.imageUrl || ""
                    },
                    android: {
                        priority: "high",
                        notification: {
                            imageUrl: data.imageUrl || "",
                            sound: "default",
                            channelId: "general_notifications"
                        }
                    },
                    apns: {
                        payload: {
                            aps: {
                                contentAvailable: true,
                                sound: "default"
                            }
                        },
                        fcm_options: {
                            image: data.imageUrl || ""
                        }
                    }
                };
            } else if (data.type === "ORDER_CALL") {
                const targetUid = data.targetUid;
                const userDoc = await admin.firestore().collection("users").doc(targetUid).get();
                let userData = userDoc.data();
                if (!userData) {
                    const merchantDoc = await admin.firestore().collection("merchants").doc(targetUid).get();
                    userData = merchantDoc.data();
                }
                if (!userData) {
                    const riderDoc = await admin.firestore().collection("riders").doc(targetUid).get();
                    userData = riderDoc.data();
                }

                if (!userData || !userData.fcmToken) {
                    return snapshot.ref.update({ status: "failed", error: "No token found" });
                }

                message = {
                    token: userData.fcmToken,
                    data: {
                        type: "ORDER_CALL",
                        orderId: data.data.orderId,
                        customerName: data.data.customerName,
                        amount: data.data.amount,
                        priority: "high"
                    },
                    android: { priority: "high" }
                };
            } else {
                const targetUid = data.userId;
                const userDoc = await admin.firestore().collection("users").doc(targetUid).get();
                let userData = userDoc.data();

                if (!userData) {
                    const merchantDoc = await admin.firestore().collection("merchants").doc(targetUid).get();
                    userData = merchantDoc.data();
                }

                if (!userData || !userData.fcmToken) {
                    return snapshot.ref.update({ status: "failed", error: "No token found" });
                }

                message = {
                    token: userData.fcmToken,
                    notification: {
                        title: data.title,
                        body: data.body,
                    },
                    data: {
                        actionUrl: data.actionUrl || ""
                    },
                    android: {
                        priority: "high",
                        notification: {
                            sound: "default",
                            channelId: "general_notifications"
                        }
                    }
                };
            }

            await admin.messaging().send(message);

            return snapshot.ref.update({
                status: "sent",
                sentAt: admin.firestore.FieldValue.serverTimestamp()
            });

        } catch (error) {
            console.error("Error sending notification:", error);
            return snapshot.ref.update({ status: "failed", error: error.message });
        }
    });

/**
 * CASHFREE PAYMENT GATEWAY INTEGRATION (FROZEN - UNTOUCHED)
 */

exports.createCashfreeOrder = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const { orderId, amount, customerId, customerPhone, customerEmail, customerName } = data;

    const APP_ID = "YOUR_CASHFREE_APP_ID";
    const SECRET_KEY = "YOUR_CASHFREE_SECRET_KEY";
    const IS_SANDBOX = true;

    const baseUrl = IS_SANDBOX ? "https://sandbox.cashfree.com/pg/orders" : "https://api.cashfree.com/pg/orders";

    try {
        const response = await fetch(baseUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "x-client-id": APP_ID,
                "x-client-secret": SECRET_KEY,
                "x-api-version": "2023-08-01"
            },
            body: JSON.stringify({
                order_id: orderId,
                order_amount: amount,
                order_currency: "INR",
                customer_details: {
                    customer_id: customerId,
                    customer_phone: customerPhone,
                    customer_email: customerEmail || "customer@flasho.com",
                    customer_name: customerName || "Customer"
                },
                order_meta: {
                    notify_url: `https://${process.env.GCLOUD_PROJECT}.cloudfunctions.net/cashfreeWebhook`
                }
            })
        });

        const result = await response.json();

        if (!response.ok) {
            console.error("Cashfree Order Creation Failed:", result);
            throw new functions.https.HttpsError("internal", result.message || "Failed to create Cashfree order");
        }

        return {
            payment_session_id: result.payment_session_id,
            order_id: result.order_id,
            order_status: result.order_status
        };
    } catch (error) {
        console.error("Cashfree Error:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

exports.cashfreeWebhook = functions.https.onRequest(async (req, res) => {
    const payload = req.body;
    const orderData = payload.data?.order;
    const paymentData = payload.data?.payment;

    if (!orderData || !paymentData) {
        return res.status(400).send("Invalid payload");
    }

    const orderId = orderData.order_id;
    const paymentStatus = paymentData.payment_status;

    try {
        const db = admin.firestore();
        const orderRef = db.collection("orders").doc(orderId);
        const orderSnap = await orderRef.get();

        if (orderSnap.exists) {
            const updates = {
                paymentStatus: paymentStatus === "SUCCESS" ? "SUCCESS" : "FAILED",
                cashfreePaymentId: paymentData.cf_payment_id,
                updatedAt: Date.now()
            };

            await orderRef.update(updates);
        }

        res.status(200).send("Webhook Processed");
    } catch (error) {
        console.error("Webhook Error:", error);
        res.status(500).send("Internal Server Error");
    }
});

/**
 * CANONICAL PRODUCT SYSTEM CLOUD FUNCTIONS
 */

function normalizeTimestamp(val) {
    if (!val) return null;
    if (typeof val === "number" && !isNaN(val)) return val;
    if (typeof val === "object" && typeof val.toMillis === "function") return val.toMillis();
    if (typeof val === "object" && typeof val._seconds === "number") return val._seconds * 1000;
    if (typeof val === "string") {
        const parsed = Date.parse(val);
        if (!isNaN(parsed)) return parsed;
    }
    return null;
}

function validatePreorderContract(data, now) {
    if (data.isPreorderEnabled !== true) {
        return { valid: false, reason: "Pre-order disabled." };
    }

    const startMs = normalizeTimestamp(data.preorderStart);
    const endMs = normalizeTimestamp(data.preorderEnd);

    if (startMs === null || endMs === null) {
        return { valid: false, reason: "Pre-order contract missing required start or end bounds." };
    }

    if (startMs >= endMs) {
        return { valid: false, reason: "Invalid pre-order contract: preorderStart must be strictly less than preorderEnd." };
    }

    if (now < startMs) {
        return { valid: false, reason: "Pre-order window is not active yet." };
    }

    if (now > endMs) {
        return { valid: false, reason: "Pre-order window has expired." };
    }

    return { valid: true, startMs, endMs };
}

// Internal helper for public projection sync
async function syncPublicProjectionInternal(productId) {
    const db = admin.firestore();
    const prodRef = db.collection("products").doc(productId);
    const pubRef = db.collection("public_products").doc(productId);

    const now = Date.now();

    return await db.runTransaction(async (transaction) => {
        const prodSnap = await transaction.get(prodRef);
        const pubSnap = await transaction.get(pubRef);

        const existingPub = pubSnap.exists ? pubSnap.data() : null;

        // Monotonicity check: Prevent older async triggers from overwriting newer updates
        if (existingPub && existingPub.projectionVersion && existingPub.projectionVersion > now) {
            return { synced: false, reason: "Stale projection trigger ignored." };
        }

        if (!prodSnap.exists) {
            if (pubSnap.exists) transaction.delete(pubRef);
            return { synced: false, reason: "Product does not exist." };
        }

        const data = prodSnap.data();

        // 1. Strict XOR Canonical Ownership Validation
        const hasMerchant = Boolean(data.merchantId && typeof data.merchantId === "string" && data.merchantId.trim());
        const hasDarkStore = Boolean(data.darkStoreId && typeof data.darkStoreId === "string" && data.darkStoreId.trim());
        const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

        if (!validXOR) {
            if (pubSnap.exists) transaction.delete(pubRef);
            return { synced: false, reason: "Ineligible: Canonical XOR ownership violation." };
        }

        // Validate trusted hub against authoritative profile record
        let expectedHubId = "";
        if (hasMerchant) {
            const mSnap = await transaction.get(db.collection("merchants").doc(data.merchantId.trim()));
            if (!mSnap.exists) {
                if (pubSnap.exists) transaction.delete(pubRef);
                return { synced: false, reason: "Ineligible: Merchant profile record missing." };
            }
            const mData = mSnap.data();
            if (mData.isApproved !== true || mData.isActive !== true) {
                if (pubSnap.exists) transaction.delete(pubRef);
                return { synced: false, reason: "Ineligible: Merchant account is unapproved or inactive." };
            }
            expectedHubId = (mData.registeredHubId || "").trim();
        } else if (hasDarkStore) {
            const dsSnap = await transaction.get(db.collection("dark_stores").doc(data.darkStoreId.trim()));
            if (!dsSnap.exists) {
                if (pubSnap.exists) transaction.delete(pubRef);
                return { synced: false, reason: "Ineligible: Dark store record missing." };
            }
            expectedHubId = (dsSnap.data().darkHubId || "").trim();
        }

        const validHub = Boolean(data.darkHubId && typeof data.darkHubId === "string" && data.darkHubId.trim() === expectedHubId && expectedHubId !== "");
        if (!validHub) {
            if (pubSnap.exists) transaction.delete(pubRef);
            return { synced: false, reason: "Ineligible: Product darkHubId does not match trusted profile/store hub." };
        }

        // 2. Business verification check
        const isVerified = data.isVerified === true;

        // 3. Approval check: finalApproval == true OR (isAdminOverride == true AND overrideReason set)
        const isApproved = data.finalApproval === true || (data.isAdminOverride === true && Boolean(data.overrideReason));

        // 4. Publishing & availability
        const isPublished = data.publishingStatus === "PUBLISHED";
        const isAvailable = data.isAvailable === true;

        // 5. Stock / Preorder availability check
        const stock = data.stockQuantity !== undefined ? data.stockQuantity : (data.stockCount || 0);
        const standardInStock = (data.isInStock !== false && data.inStock !== false) && stock > 0;

        const preorderContract = validatePreorderContract(data, now);
        const preorderAvailable = preorderContract.valid && (data.maxPreorderCapacity === 0 || (data.currentPreorderCount || 0) < data.maxPreorderCapacity);

        const inStock = standardInStock || preorderAvailable;

        // 6. Vacation / Schedule check
        let onVacation = false;
        if (data.vacationStart && data.vacationEnd) {
            const vStart = normalizeTimestamp(data.vacationStart);
            const vEnd = normalizeTimestamp(data.vacationEnd);
            if (vStart && vEnd && now >= vStart && now <= vEnd) {
                onVacation = true;
            }
        }

        const isEligible = isVerified && isApproved && isPublished && isAvailable && inStock && !onVacation;

        if (!isEligible) {
            if (pubSnap.exists) transaction.delete(pubRef);
            return { synced: false, reason: "Ineligible for public catalog projection." };
        }

        // Allowlisted public fields ONLY - Excludes merchant rates, profit margins, staff feedback
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
            isInStock: standardInStock,
            isAvailable: true,
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
            isPreorderEnabled: data.isPreorderEnabled || false,
            currentPreorderCount: data.currentPreorderCount || 0,
            maxPreorderCapacity: data.maxPreorderCapacity || 0,
            preorderStart: normalizeTimestamp(data.preorderStart),
            preorderEnd: normalizeTimestamp(data.preorderEnd),
            projectionVersion: now,
            updatedAt: now
        };

        // Full document overwrite ensures no stale or un-allowlisted fields persist
        transaction.set(pubRef, publicData);
        return { synced: true, projectionVersion: now };
    });
}

// 1. createProductDraft
exports.createProductDraft = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
    const db = admin.firestore();

    const callerProfile = await verifyActiveCaller(db, callerUid, callerEmail);

    const { merchantId, darkStoreId, productData } = data || {};

    const hasMerchant = Boolean(merchantId && typeof merchantId === "string" && merchantId.trim());
    const hasDarkStore = Boolean(darkStoreId && typeof darkStoreId === "string" && darkStoreId.trim());

    if ((hasMerchant && hasDarkStore) || (!hasMerchant && !hasDarkStore)) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "XOR Ownership Violation: Product must specify EXACTLY ONE of 'merchantId' or 'darkStoreId'."
        );
    }

    let derivedDarkHubId = "";

    if (hasMerchant) {
        if (!callerProfile.isMasterAdmin && callerUid !== merchantId) {
            throw new functions.https.HttpsError("permission-denied", "You can only create product drafts for your own authenticated merchant ID.");
        }
        const merchantDoc = await db.collection("merchants").doc(merchantId).get();
        if (!merchantDoc.exists) {
            throw new functions.https.HttpsError("not-found", `Merchant record '${merchantId}' not found.`);
        }
        const mData = merchantDoc.data();
        if (mData.isApproved !== true || mData.isActive !== true) {
            throw new functions.https.HttpsError("permission-denied", "Merchant account is unapproved, disabled, or inactive.");
        }
        if (!mData.registeredHubId || typeof mData.registeredHubId !== "string" || !mData.registeredHubId.trim()) {
            throw new functions.https.HttpsError("failed-precondition", `Merchant '${merchantId}' has no registeredHubId configured.`);
        }
        derivedDarkHubId = mData.registeredHubId.trim();
    } else if (hasDarkStore) {
        const darkStoreDoc = await db.collection("dark_stores").doc(darkStoreId).get();
        if (!darkStoreDoc.exists) {
            throw new functions.https.HttpsError("not-found", `Dark Store record '${darkStoreId}' not found.`);
        }
        const dsData = darkStoreDoc.data();
        if (!dsData.darkHubId || typeof dsData.darkHubId !== "string" || !dsData.darkHubId.trim()) {
            throw new functions.https.HttpsError("failed-precondition", `Dark store '${darkStoreId}' has no darkHubId configured.`);
        }
        derivedDarkHubId = dsData.darkHubId.trim();
    }

    const newProdRef = db.collection("products").doc();
    const newProdId = newProdRef.id;

    const cleanData = { ...(productData || {}) };
    delete cleanData.id;
    delete cleanData.merchantId;
    delete cleanData.darkStoreId;
    delete cleanData.darkHubId;
    delete cleanData.sellerId;
    delete cleanData.storeId;
    delete cleanData.hubId;
    delete cleanData.ownerId;
    delete cleanData.revBilling;
    delete cleanData.revLegal;
    delete cleanData.revQuality;
    delete cleanData.revInventory;
    delete cleanData.approvedRevBilling;
    delete cleanData.approvedRevLegal;
    delete cleanData.approvedRevQuality;
    delete cleanData.approvedRevInventory;
    delete cleanData.finalApproval;
    delete cleanData.isAdminOverride;
    delete cleanData.overrideReason;
    delete cleanData.overrideBy;
    delete cleanData.overrideAt;
    delete cleanData.isVerified;
    delete cleanData.publishingStatus;
    delete cleanData.projectionVersion;

    const draftDoc = {
        ...cleanData,
        id: newProdId,
        merchantId: hasMerchant ? merchantId : null,
        darkStoreId: hasDarkStore ? darkStoreId : null,
        darkHubId: derivedDarkHubId,
        revBilling: 1,
        revLegal: 1,
        revQuality: 1,
        revInventory: 1,
        approvedRevBilling: 0,
        approvedRevLegal: 0,
        approvedRevQuality: 0,
        approvedRevInventory: 0,
        finalApproval: false,
        publishingStatus: "DRAFT",
        isVerified: false,
        isAvailable: false,
        isAdminOverride: false,
        currentPreorderCount: 0,
        createdBy: callerUid,
        createdAt: Date.now(),
        updatedAt: Date.now()
    };

    await newProdRef.set(draftDoc);

    return {
        success: true,
        productId: newProdId,
        darkHubId: derivedDarkHubId,
        message: "Product draft created successfully."
    };
});

// 2. updateProductDraft
exports.updateProductDraft = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
    const db = admin.firestore();

    const callerProfile = await verifyActiveCaller(db, callerUid, callerEmail);

    const { productId, updateData } = data || {};

    if (!productId) {
        throw new functions.https.HttpsError("invalid-argument", "productId is required.");
    }

    const prodRef = db.collection("products").doc(productId);
    const prodSnap = await prodRef.get();

    if (!prodSnap.exists) {
        throw new functions.https.HttpsError("not-found", `Product '${productId}' not found.`);
    }

    const existing = prodSnap.data();

    // Canonical ownership check: Ordinary ADMIN does NOT bypass merchant ownership checks
    const hasMerchant = Boolean(existing.merchantId && typeof existing.merchantId === "string" && existing.merchantId.trim());
    const hasDarkStore = Boolean(existing.darkStoreId && typeof existing.darkStoreId === "string" && existing.darkStoreId.trim());
    const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

    if (!validXOR) {
        throw new functions.https.HttpsError("failed-precondition", "Product has invalid or malformed canonical ownership.");
    }

    if (!callerProfile.isMasterAdmin) {
        if (hasMerchant) {
            if (existing.merchantId !== callerUid) {
                throw new functions.https.HttpsError("permission-denied", "You do not own this merchant product.");
            }
        } else if (hasDarkStore) {
            if (callerProfile.role === "STAFF" && callerProfile.assignedHubId && existing.darkHubId && callerProfile.assignedHubId !== existing.darkHubId) {
                throw new functions.https.HttpsError("permission-denied", "Staff member hub assignment does not match product darkHubId.");
            }
        }
    }

    const updates = { ...(updateData || {}) };

    // Strip client attempts to pass server-owned authority & workflow state fields
    delete updates.id;
    delete updates.merchantId;
    delete updates.darkStoreId;
    delete updates.darkHubId;
    delete updates.sellerId;
    delete updates.storeId;
    delete updates.hubId;
    delete updates.ownerId;
    delete updates.revBilling;
    delete updates.revLegal;
    delete updates.revQuality;
    delete updates.revInventory;
    delete updates.approvedRevBilling;
    delete updates.approvedRevLegal;
    delete updates.approvedRevQuality;
    delete updates.approvedRevInventory;
    delete updates.finalApproval;
    delete updates.isAdminOverride;
    delete updates.overrideReason;
    delete updates.overrideBy;
    delete updates.overrideAt;
    delete updates.isVerified;
    delete updates.publishingStatus;
    delete updates.projectionVersion;

    let revBilling = existing.revBilling || 1;
    let revLegal = existing.revLegal || 1;
    let revQuality = existing.revQuality || 1;
    let revInventory = existing.revInventory || 1;

    let billingChanged = false;
    let legalChanged = false;
    let qualityChanged = false;
    let inventoryChanged = false;

    // Billing sensitive fields
    if (updates.basePrice !== undefined || updates.price !== undefined || updates.taxRate !== undefined ||
        updates.cgstRate !== undefined || updates.sgstRate !== undefined || updates.igstRate !== undefined ||
        updates.merchantRate !== undefined || updates.discountValue !== undefined || updates.handlingFee !== undefined ||
        updates.deliveryCharge !== undefined || updates.freeDeliveryMinAmount !== undefined ||
        updates.freeDeliveryMinQuantity !== undefined || updates.minOrderQuantity !== undefined) {
        revBilling++;
        billingChanged = true;
    }

    // Legal sensitive fields
    if (updates.hsnCode !== undefined || updates.legalDisclaimer !== undefined || updates.fssaiNumber !== undefined ||
        updates.isFoodProduct !== undefined || updates.expiryDate !== undefined) {
        revLegal++;
        legalChanged = true;
    }

    // Quality sensitive fields
    if (updates.name !== undefined || updates.brand !== undefined || updates.description !== undefined ||
        updates.shortDescription !== undefined || updates.imageUrl !== undefined || updates.images !== undefined ||
        updates.category !== undefined || updates.subCategory !== undefined || updates.variants !== undefined ||
        updates.specifications !== undefined || updates.nutritionalInfo !== undefined) {
        revQuality++;
        qualityChanged = true;
    }

    // Inventory sensitive fields
    if (updates.stockQuantity !== undefined || updates.minStockLevel !== undefined || updates.aisle !== undefined ||
        updates.rack !== undefined || updates.barcode !== undefined || updates.unit !== undefined ||
        updates.weight !== undefined || updates.unitType !== undefined || updates.measurementUnit !== undefined) {
        revInventory++;
        inventoryChanged = true;
    }

    updates.revBilling = revBilling;
    updates.revLegal = revLegal;
    updates.revQuality = revQuality;
    updates.revInventory = revInventory;
    updates.updatedAt = Date.now();

    // If any department revision changed, finalApproval is invalidated!
    const anyChanged = billingChanged || legalChanged || qualityChanged || inventoryChanged;
    if (anyChanged && !callerProfile.isMasterAdmin) {
        updates.finalApproval = false;
    }

    await prodRef.update(updates);

    // Sync public projection if published
    await syncPublicProjectionInternal(productId);

    return {
        success: true,
        productId,
        finalApprovalInvalidated: anyChanged && !callerProfile.isMasterAdmin,
        message: "Product draft updated successfully."
    };
});

// 3. reviewProductDepartment
exports.reviewProductDepartment = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
    const db = admin.firestore();

    const callerProfile = await verifyActiveCaller(db, callerUid, callerEmail);

    const { productId, department, approved } = data || {};

    if (!productId || !department) {
        throw new functions.https.HttpsError("invalid-argument", "productId and department are required.");
    }

    const validDepartments = ["BILLING", "LEGAL", "QUALITY", "INVENTORY"];
    const depUpper = department.toUpperCase();

    if (!validDepartments.includes(depUpper)) {
        throw new functions.https.HttpsError("invalid-argument", `Invalid department '${department}'. Allowed: ${validDepartments.join(", ")}`);
    }

    // Check staff department authorization
    if (!callerProfile.isMasterAdmin) {
        if (callerProfile.role !== "STAFF" || callerProfile.staffDepartment !== depUpper) {
            throw new functions.https.HttpsError("permission-denied", `Caller is not authorized staff for department '${depUpper}'.`);
        }
    }

    const prodRef = db.collection("products").doc(productId);
    const prodSnap = await prodRef.get();

    if (!prodSnap.exists) {
        throw new functions.https.HttpsError("not-found", `Product '${productId}' not found.`);
    }

    const p = prodSnap.data();

    // Canonical ownership check
    const hasMerchant = Boolean(p.merchantId && typeof p.merchantId === "string" && p.merchantId.trim());
    const hasDarkStore = Boolean(p.darkStoreId && typeof p.darkStoreId === "string" && p.darkStoreId.trim());
    const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

    if (!validXOR) {
        throw new functions.https.HttpsError("failed-precondition", "Product has invalid or malformed canonical ownership.");
    }

    if (!callerProfile.isMasterAdmin && callerProfile.role === "STAFF" && callerProfile.assignedHubId) {
        if (p.darkHubId !== callerProfile.assignedHubId) {
            throw new functions.https.HttpsError("permission-denied", "Staff member hub assignment does not match product darkHubId.");
        }
    }

    const updates = { updatedAt: Date.now() };

    if (depUpper === "BILLING") {
        updates.approvedRevBilling = approved ? (p.revBilling || 1) : 0;
    } else if (depUpper === "LEGAL") {
        updates.approvedRevLegal = approved ? (p.revLegal || 1) : 0;
    } else if (depUpper === "QUALITY") {
        updates.approvedRevQuality = approved ? (p.revQuality || 1) : 0;
    } else if (depUpper === "INVENTORY") {
        updates.approvedRevInventory = approved ? (p.revInventory || 1) : 0;
    }

    const finalBilling = updates.approvedRevBilling !== undefined ? updates.approvedRevBilling : (p.approvedRevBilling || 0);
    const finalLegal = updates.approvedRevLegal !== undefined ? updates.approvedRevLegal : (p.approvedRevLegal || 0);
    const finalQuality = updates.approvedRevQuality !== undefined ? updates.approvedRevQuality : (p.approvedRevQuality || 0);
    const finalInventory = updates.approvedRevInventory !== undefined ? updates.approvedRevInventory : (p.approvedRevInventory || 0);

    const allApproved = finalBilling === (p.revBilling || 1) &&
                        finalLegal === (p.revLegal || 1) &&
                        finalQuality === (p.revQuality || 1) &&
                        finalInventory === (p.revInventory || 1);

    updates.finalApproval = allApproved;

    await prodRef.update(updates);

    if (p.publishingStatus === "PUBLISHED") {
        await syncPublicProjectionInternal(productId);
    }

    return {
        success: true,
        productId,
        department: depUpper,
        allDepartmentsApproved: allApproved,
        finalApproval: allApproved
    };
});

// 4. grantFinalApproval
exports.grantFinalApproval = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
    const db = admin.firestore();

    const callerProfile = await verifyActiveCaller(db, callerUid, callerEmail);

    if (!callerProfile.isMasterAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Only Whitelisted Master Admins can grant final approval.");
    }

    const { productId, commissionRate, taxRate } = data || {};

    if (!productId) {
        throw new functions.https.HttpsError("invalid-argument", "productId is required.");
    }

    const prodRef = db.collection("products").doc(productId);
    const prodSnap = await prodRef.get();

    if (!prodSnap.exists) {
        throw new functions.https.HttpsError("not-found", `Product '${productId}' not found.`);
    }

    const p = prodSnap.data();

    // Canonical ownership check
    const hasMerchant = Boolean(p.merchantId && typeof p.merchantId === "string" && p.merchantId.trim());
    const hasDarkStore = Boolean(p.darkStoreId && typeof p.darkStoreId === "string" && p.darkStoreId.trim());
    const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

    if (!validXOR) {
        throw new functions.https.HttpsError("failed-precondition", "Product has invalid or malformed canonical ownership.");
    }

    // Verify all 4 department revisions match
    const billingOk = (p.approvedRevBilling || 0) === (p.revBilling || 1);
    const legalOk = (p.approvedRevLegal || 0) === (p.revLegal || 1);
    const qualityOk = (p.approvedRevQuality || 0) === (p.revQuality || 1);
    const inventoryOk = (p.approvedRevInventory || 0) === (p.revInventory || 1);

    if (!billingOk || !legalOk || !qualityOk || !inventoryOk) {
        throw new functions.https.HttpsError(
            "failed-precondition",
            "Cannot grant final approval until all 4 departments (BILLING, LEGAL, QUALITY, INVENTORY) have approved current revisions."
        );
    }

    const updates = {
        finalApproval: true,
        updatedAt: Date.now()
    };

    if (commissionRate !== undefined) updates.commissionRate = commissionRate;
    if (taxRate !== undefined) {
        updates.taxRate = taxRate;
        updates.cgstRate = taxRate / 2.0;
        updates.sgstRate = taxRate / 2.0;
    }

    await prodRef.update(updates);

    if (p.publishingStatus === "PUBLISHED") {
        await syncPublicProjectionInternal(productId);
    }

    return {
        success: true,
        productId,
        finalApproval: true,
        message: "Final approval granted successfully."
    };
});

// 5. grantAdminOverride
exports.grantAdminOverride = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();

    if (!isMasterAdminEmail(callerEmail)) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Security Violation: Only whitelisted Master Admins (ranjanamishra1117@gmail.com, supreet.mishr@gmail.com) can grant Admin Override."
        );
    }

    const { productId, overrideReason } = data || {};

    if (!productId || !overrideReason || typeof overrideReason !== "string" || !overrideReason.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "productId and explicit overrideReason are required.");
    }

    const db = admin.firestore();
    const prodRef = db.collection("products").doc(productId);
    const prodSnap = await prodRef.get();

    if (!prodSnap.exists) {
        throw new functions.https.HttpsError("not-found", `Product '${productId}' not found.`);
    }

    // STRICT WORKFLOW SEPARATION: Admin override ONLY records explicit audit metadata.
    // It does NOT fabricate department approvals, set isVerified = true, set isAvailable = true, or publish automatically!
    const updates = {
        isAdminOverride: true,
        overrideReason: overrideReason.trim(),
        overrideBy: callerUid,
        overrideAt: Date.now(),
        updatedAt: Date.now()
    };

    await prodRef.update(updates);

    return {
        success: true,
        productId,
        isAdminOverride: true,
        message: "Master Admin override metadata recorded successfully."
    };
});

// 6. publishProduct
exports.publishProduct = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
    const db = admin.firestore();

    const callerProfile = await verifyActiveCaller(db, callerUid, callerEmail);

    const { productId } = data || {};

    if (!productId) {
        throw new functions.https.HttpsError("invalid-argument", "productId is required.");
    }

    const prodRef = db.collection("products").doc(productId);
    const prodSnap = await prodRef.get();

    if (!prodSnap.exists) {
        throw new functions.https.HttpsError("not-found", `Product '${productId}' not found.`);
    }

    const p = prodSnap.data();

    // Canonical ownership check
    const hasMerchant = Boolean(p.merchantId && typeof p.merchantId === "string" && p.merchantId.trim());
    const hasDarkStore = Boolean(p.darkStoreId && typeof p.darkStoreId === "string" && p.darkStoreId.trim());
    const validXOR = (hasMerchant && !hasDarkStore) || (!hasMerchant && hasDarkStore);

    if (!validXOR) {
        throw new functions.https.HttpsError("failed-precondition", "Product has invalid or malformed canonical ownership.");
    }

    if (!callerProfile.isMasterAdmin) {
        if (hasMerchant) {
            if (p.merchantId !== callerUid) {
                throw new functions.https.HttpsError("permission-denied", "You do not own this product.");
            }
        } else if (hasDarkStore) {
            if (callerProfile.role === "STAFF" && callerProfile.assignedHubId && p.darkHubId && callerProfile.assignedHubId !== p.darkHubId) {
                throw new functions.https.HttpsError("permission-denied", "Staff member hub assignment does not match product darkHubId.");
            }
        }
    }

    if (p.publishingStatus === "ARCHIVED") {
        throw new functions.https.HttpsError("failed-precondition", "Cannot publish an ARCHIVED product.");
    }

    if (!p.isVerified) {
        throw new functions.https.HttpsError("failed-precondition", "Product must be verified by business before publishing.");
    }

    const isApproved = p.finalApproval === true || (p.isAdminOverride === true && Boolean(p.overrideReason));
    if (!isApproved) {
        throw new functions.https.HttpsError("failed-precondition", "Product requires final approval or a valid Master Admin override before publishing.");
    }

    await prodRef.update({
        publishingStatus: "PUBLISHED",
        isAvailable: true,
        updatedAt: Date.now()
    });

    const syncRes = await syncPublicProjectionInternal(productId);

    return {
        success: true,
        productId,
        publishingStatus: "PUBLISHED",
        projectionResult: syncRes,
        message: "Product published and synchronized with public catalog."
    };
});

// 7. syncPublicProjection (Protected Callable)
exports.syncPublicProjection = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const callerEmail = (context.auth.token.email || "").toLowerCase().trim();
    const db = admin.firestore();

    const callerProfile = await verifyActiveCaller(db, callerUid, callerEmail);

    const { productId } = data || {};
    if (!productId || typeof productId !== "string" || !productId.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "productId is required.");
    }

    const cleanProdId = productId.trim();
    const prodSnap = await db.collection("products").doc(cleanProdId).get();

    if (!prodSnap.exists) {
        throw new functions.https.HttpsError("not-found", `Product '${cleanProdId}' not found.`);
    }

    const p = prodSnap.data();

    // Authorization: Owner Merchant, Authorized Hub Staff, or Whitelisted Master Admin
    if (!callerProfile.isMasterAdmin) {
        if (p.merchantId) {
            if (p.merchantId !== callerUid) {
                throw new functions.https.HttpsError("permission-denied", "You can only synchronize projection for your own products.");
            }
        } else if (p.darkStoreId) {
            if (callerProfile.role !== "STAFF" || (callerProfile.assignedHubId && p.darkHubId && callerProfile.assignedHubId !== p.darkHubId)) {
                throw new functions.https.HttpsError("permission-denied", "Unauthorized to synchronize company store product projection.");
            }
        } else {
            throw new functions.https.HttpsError("failed-precondition", "Product has invalid or malformed ownership.");
        }
    }

    const res = await syncPublicProjectionInternal(cleanProdId);
    return { success: true, productId: cleanProdId, result: res };
});

// 8. reservePreorderItem (Idempotent & Bounded)
exports.reservePreorderItem = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const { productId, quantity, idempotencyKey } = data || {};

    const qty = parseInt(quantity) || 1;
    if (qty <= 0 || qty > 10) {
        throw new functions.https.HttpsError("invalid-argument", "Quantity must be between 1 and 10.");
    }

    if (!productId) {
        throw new functions.https.HttpsError("invalid-argument", "productId is required.");
    }

    if (!idempotencyKey || typeof idempotencyKey !== "string" || !idempotencyKey.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "Valid idempotencyKey is required.");
    }

    const cleanKey = idempotencyKey.trim();
    const resId = `res_${callerUid}_${cleanKey}`;

    const db = admin.firestore();
    const prodRef = db.collection("products").doc(productId);
    const resRef = db.collection("preorder_reservations").doc(resId);

    const now = Date.now();

    try {
        const result = await db.runTransaction(async (transaction) => {
            const resSnap = await transaction.get(resRef);
            if (resSnap.exists) {
                const existingRes = resSnap.data();
                if (existingRes.customerId !== callerUid || existingRes.productId !== productId) {
                    throw new Error("Idempotency key collision with different reservation scope.");
                }
                return {
                    productId,
                    reservationId: resId,
                    quantity: existingRes.quantity,
                    isExisting: true,
                    message: "Pre-order reservation already processed (idempotent response)."
                };
            }

            const snap = await transaction.get(prodRef);
            if (!snap.exists) {
                throw new Error("Product not found.");
            }

            const p = snap.data();

            // Strict canonical preorder window contract check
            const windowCheck = validatePreorderContract(p, now);
            if (!windowCheck.valid) {
                throw new Error(windowCheck.reason);
            }

            const current = p.currentPreorderCount || 0;
            const maxCap = p.maxPreorderCapacity || 0;

            if (maxCap > 0 && (current + qty) > maxCap) {
                throw new Error(`Pre-order capacity exceeded. Available capacity: ${Math.max(0, maxCap - current)}.`);
            }

            const newCount = current + qty;
            transaction.update(prodRef, {
                currentPreorderCount: newCount,
                updatedAt: now
            });

            transaction.set(resRef, {
                reservationId: resId,
                idempotencyKey: cleanKey,
                customerId: callerUid,
                productId,
                quantity: qty,
                status: "RESERVED",
                createdAt: now
            });

            return {
                productId,
                reservationId: resId,
                quantity: qty,
                isExisting: false,
                newPreorderCount: newCount,
                maxCapacity: maxCap
            };
        });

        return {
            success: true,
            ...result,
            message: "Pre-order item reserved successfully."
        };
    } catch (err) {
        console.error("Error in reservePreorderItem:", err);
        throw new functions.https.HttpsError("failed-precondition", err.message || "Failed to reserve preorder item.");
    }
});

// 9. cancelPreorderReservation (Idempotent)
exports.cancelPreorderReservation = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }

    const callerUid = context.auth.uid;
    const { reservationId } = data || {};

    if (!reservationId || typeof reservationId !== "string" || !reservationId.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "reservationId is required.");
    }

    const db = admin.firestore();
    const resRef = db.collection("preorder_reservations").doc(reservationId.trim());

    try {
        const result = await db.runTransaction(async (transaction) => {
            const resSnap = await transaction.get(resRef);
            if (!resSnap.exists) {
                throw new Error("Reservation record not found.");
            }

            const resData = resSnap.data();
            if (resData.customerId !== callerUid) {
                throw new Error("You can only cancel your own preorder reservation.");
            }

            if (resData.status === "CANCELLED") {
                return { reservationId, isAlreadyCancelled: true };
            }

            const prodRef = db.collection("products").doc(resData.productId);
            const prodSnap = await transaction.get(prodRef);

            if (prodSnap.exists) {
                const currentCount = prodSnap.data().currentPreorderCount || 0;
                const newCount = Math.max(0, currentCount - resData.quantity);
                transaction.update(prodRef, {
                    currentPreorderCount: newCount,
                    updatedAt: Date.now()
                });
            }

            transaction.update(resRef, {
                status: "CANCELLED",
                cancelledAt: Date.now()
            });

            return { reservationId, isAlreadyCancelled: false };
        });

        return {
            success: true,
            ...result,
            message: "Preorder reservation cancelled and capacity released."
        };
    } catch (err) {
        console.error("Error in cancelPreorderReservation:", err);
        throw new functions.https.HttpsError("failed-precondition", err.message || "Failed to cancel preorder reservation.");
    }
});
