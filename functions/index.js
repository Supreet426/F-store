const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.createAppUser = functions.https.onCall(async (data, context) => {
    // Verify caller authentication
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
        // 1. Create Auth Account using Firebase Admin SDK
        const userRecord = await admin.auth().createUser({
            email: email,
            password: password,
            displayName: name,
            phoneNumber: phone && phone.startsWith("+") ? phone : undefined
        });

        const targetCollection = role === "ADMIN" ? "users" : (role === "RIDER" ? "riders" : (role === "OTHER" ? "staff" : (role === "CUSTOMER" ? "customers" : "sellers")));

        const MASTER_ADMIN_EMAILS = ["ranjanamishra1117@gmail.com", "supreet.mishr@gmail.com"];

        // 2. Set Admin Claim if role is ADMIN
        if (role === "ADMIN") {
            if (!MASTER_ADMIN_EMAILS.includes(email.toLowerCase().trim())) {
                throw new functions.https.HttpsError("permission-denied", "Security Violation: Only whitelisted master admins can be assigned ADMIN role.");
            }
            await admin.auth().setCustomUserClaims(userRecord.uid, { admin: true });
        }

        // 3. Save Document in Firestore
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

// Automatically generate 4-Digit Delivery OTP when new order is created
exports.onOrderCreated = functions.firestore
    .document("orders/{orderId}")
    .onCreate(async (snapshot, context) => {
        const orderData = snapshot.data();
        const orderId = context.params.orderId;

        // Generate 4-digit OTP if not already set
        if (!orderData.deliveryOtp) {
            const generatedOtp = Math.floor(1000 + Math.random() * 9000).toString();
            try {
                await snapshot.ref.update({
                    deliveryOtp: generatedOtp,
                    otpGeneratedAt: Date.now()
                });
                console.log(`Generated Delivery OTP [${generatedOtp}] for Order #${orderId}`);
            } catch (err) {
                console.error("Error setting delivery OTP:", err);
            }
        }

        // Notify active riders about new pending order via FCM topic
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
                    notification: {
                        sound: "default"
                    }
                }
            };
            await admin.messaging().send(message);
        } catch (err) {
            console.error("Error broadcasting new order to riders topic:", err);
        }
    });

// Handle FCM Topic Broadcasts triggered from Web Admin app-control
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
                    broadcastId: broadcastId
                }
            };

            if (data.imageUrl) {
                message.notification.imageUrl = data.imageUrl;
                message.android = {
                    notification: {
                        imageUrl: data.imageUrl
                    }
                };
            }

            await admin.messaging().send(message);
            console.log(`FCM Broadcast ${broadcastId} sent to topic '${targetTopic}'`);

            return snapshot.ref.update({
                status: "sent",
                sentAt: admin.firestore.FieldValue.serverTimestamp()
            });
        } catch (error) {
            console.error("Error sending FCM broadcast:", error);
            return snapshot.ref.update({ status: "failed", error: error.message });
        }
    });

// Handle stock reduction & order status change notifications on order update
exports.onOrderStatusUpdated = functions.firestore
    .document("orders/{orderId}")
    .onUpdate(async (change, context) => {
        const beforeData = change.before.data();
        const afterData = change.after.data();
        const orderId = context.params.orderId;

        // Trigger stock deduction when status transitions to DELIVERED
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
                                    stock: newStock,
                                    inStock: newStock > 0,
                                    isAvailable: newStock > 0,
                                    updatedAt: Date.now()
                                });
                            }
                        }
                    });
                    console.log(`Deducted stock for items in delivered order #${orderId}`);
                } catch (err) {
                    console.error("Error in stock deduction transaction:", err);
                }
            }
        }

        // Notify customer when order status changes
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
                // Send to all customers via topic
                message = {
                    topic: "all_customers",
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
                        notification: {
                            imageUrl: data.imageUrl || ""
                        }
                    }
                };
            } else if (data.type === "ORDER_CALL") {
                // Send to specific seller/rider
                const targetUid = data.targetUid;
                const userDoc = await admin.firestore().collection("users").doc(targetUid).get();
                // If not in users, check sellers or riders
                let userData = userDoc.data();
                if (!userData) {
                    const sellerDoc = await admin.firestore().collection("sellers").doc(targetUid).get();
                    userData = sellerDoc.data();
                }
                if (!userData) {
                    const riderDoc = await admin.firestore().collection("riders").doc(targetUid).get();
                    userData = riderDoc.data();
                }

                if (!userData || !userData.fcmToken) {
                    console.log("No token found for user: ", targetUid);
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
                    android: {
                        priority: "high"
                    }
                };
            } else {
                // Individual Notification
                const targetUid = data.userId;
                const userDoc = await admin.firestore().collection("users").doc(targetUid).get();
                let userData = userDoc.data();

                if (!userData) {
                    const sellerDoc = await admin.firestore().collection("sellers").doc(targetUid).get();
                    userData = sellerDoc.data();
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
                    }
                };
            }

            // Send via FCM
            await admin.messaging().send(message);
            console.log("Notification sent successfully for: ", notificationId);

            return snapshot.ref.update({
                status: "sent",
                sentAt: admin.firestore.FieldValue.serverTimestamp()
            });

        } catch (error) {
            console.error("Error sending notification:", error);
            return snapshot.ref.update({ status: "failed", error: error.message });
        }
    });
