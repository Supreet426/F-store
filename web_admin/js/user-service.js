import { db, auth, firebaseConfig } from './firebase-config.js';
import { initializeApp, deleteApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import {
    collection,
    doc,
    onSnapshot,
    getDocs,
    setDoc,
    deleteDoc,
    updateDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import {
    getAuth,
    sendPasswordResetEmail,
    createUserWithEmailAndPassword,
    signOut
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";

const COLLECTIONS = ["users", "admins", "customers", "sellers", "riders", "staff", "customer_leads", "profiles", "accounts"];

const MASTER_ADMIN_EMAILS = [
    "ranjanamishra1117@gmail.com",
    "supreet.mishr@gmail.com"
];

function isMasterAdminEmail(emailStr) {
    if (!emailStr) return false;
    const lower = String(emailStr).toLowerCase().trim();
    return MASTER_ADMIN_EMAILS.includes(lower);
}

let collectionsCache = {};
let unifiedUsersList = [];
let activeUnsubscribers = [];
let dataChangeCallbacks = [];

/**
 * Register a listener callback to receive unified user list updates
 */
export function onUsersUpdated(callback) {
    if (typeof callback === 'function') {
        dataChangeCallbacks.push(callback);
        try {
            callback(unifiedUsersList);
        } catch (err) {
            console.error("Error executing initial user callback:", err);
        }
    }
}

function notifySubscribers() {
    dataChangeCallbacks.forEach(cb => {
        try {
            cb(unifiedUsersList);
        } catch (err) {
            console.error("Error in user data callback:", err);
        }
    });

    updateUserDomKPIs(unifiedUsersList);
}

function updateUserDomKPIs(users) {
    const totalCount = users.length;
    const kpiTotal = document.getElementById("kpi-total-users");
    const kpiOverviewTotal = document.getElementById("kpi-users");

    if (kpiTotal) kpiTotal.textContent = totalCount;
    if (kpiOverviewTotal) kpiOverviewTotal.textContent = totalCount;

    const customersCount = users.filter(u => u.role === "CUSTOMER").length;
    const merchantsCount = users.filter(u => u.role === "MERCHANT" || u.role === "STORE_OWNER").length;
    const ridersCount = users.filter(u => u.role === "RIDER").length;
    const adminsCount = users.filter(u => u.role === "ADMIN").length;
    const staffCount = users.filter(u => u.role === "OTHER").length;

    const elCust = document.getElementById("kpi-customers");
    const elMerch = document.getElementById("kpi-merchants");
    const elRiders = document.getElementById("kpi-riders");
    const elAdmins = document.getElementById("kpi-admins");
    const elStaff = document.getElementById("kpi-staff");

    if (elCust) elCust.textContent = customersCount;
    if (elMerch) elMerch.textContent = merchantsCount;
    if (elRiders) elRiders.textContent = ridersCount;
    if (elAdmins) elAdmins.textContent = adminsCount;
    if (elStaff) elStaff.textContent = staffCount;
}

/**
 * Perform one-time direct getDocs fetch across all Firestore user collections
 */
export async function fetchAllUsersFromFirestore() {
    for (const colName of COLLECTIONS) {
        try {
            const snap = await getDocs(collection(db, colName));
            if (!snap.empty) {
                parseCollectionDocs(colName, snap.docs);
            }
        } catch (err) {
            console.warn(`[UserService] getDocs notice for ${colName}:`, err.message || err);
        }
    }

    try {
        const orderSnap = await getDocs(collection(db, "orders"));
        if (!orderSnap.empty) {
            parseOrdersDocs(orderSnap.docs);
        }
    } catch (err) {
        console.warn("[UserService] getDocs notice for orders:", err.message || err);
    }

    if (auth.currentUser) {
        const currentUser = auth.currentUser;
        const currentEmail = String(currentUser.email || "").toLowerCase().trim();
        if (isMasterAdminEmail(currentEmail)) {
            collectionsCache["auth_user"] = [{
                uid: currentUser.uid,
                name: currentUser.displayName || (currentEmail ? currentEmail.split('@')[0] : "Master Admin"),
                email: currentEmail,
                phone: currentUser.phoneNumber || "N/A",
                role: "ADMIN",
                dept: "Management",
                isActive: true,
                blockReason: "",
                collectionName: "users"
            }];
        } else {
            delete collectionsCache["auth_user"];
        }
    }

    mergeAndDeduplicateUsers();
    notifySubscribers();
}

function parseCollectionDocs(colName, docs) {
    collectionsCache[colName] = docs.map(docSnap => {
        const data = docSnap.data();
        const email = String(data.email || data.userEmail || data.customerEmail || "").toLowerCase().trim();
        const rawRole = String(data.role || data.userRole || data.type || data.userType || data.roleType || "").toUpperCase();
        let role = "";

        if (isMasterAdminEmail(email)) {
            role = "ADMIN";
        } else if (rawRole.includes("SELLER") || rawRole.includes("OWNER") || rawRole.includes("STORE") || rawRole.includes("MERCHANT")) {
            role = "MERCHANT";
        } else if (rawRole.includes("RIDER") || rawRole.includes("DRIVER") || rawRole.includes("DELIVERY")) {
            role = "RIDER";
        } else if (rawRole.includes("WORKER") || rawRole.includes("HELPLINE") || rawRole.includes("STAFF") || rawRole.includes("CANCEL") || rawRole.includes("OTHER")) {
            role = "OTHER";
        } else if (rawRole.includes("CUSTOMER") || rawRole.includes("USER")) {
            role = "CUSTOMER";
        } else {
            role = (colName === "sellers") ? "MERCHANT" : (colName === "riders" ? "RIDER" : (colName === "staff" ? "OTHER" : "CUSTOMER"));
        }

        const uid = String(docSnap.id || data.uid || data.userId || data.id || `user_${Date.now()}`);
        const phone = String(data.phoneNumber || data.mobile || data.phone || data.customerPhone || "N/A");
        const rawName = data.name || data.displayName || data.fullName || data.customerName;

        let displayName = String(rawName || "");
        if (!displayName) {
            if (email) displayName = email.split('@')[0];
            else if (phone !== "N/A") displayName = `User (${phone})`;
            else displayName = `User #${uid.slice(0, 6)}`;
        }

        // If this is a master admin doc, sync role: "ADMIN" back to Firestore if needed
        if (isMasterAdminEmail(email) && uid && !uid.startsWith("user_") && data.role !== "ADMIN") {
            try {
                setDoc(doc(db, "users", uid), { role: "ADMIN", email: email }, { merge: true }).catch(() => {});
            } catch (_) {}
        }

        return {
            ...data,
            uid: uid,
            name: displayName,
            email: email,
            phone: phone,
            role: role,
            dept: String(data.staffDepartment || (role === 'OTHER' ? 'Technical Support' : '')),
            isActive: data.isActive !== undefined ? data.isActive : (data.active !== undefined ? data.active : true),
            blockReason: String(data.blockReason || ""),
            collectionName: colName
        };
    });
}

function parseOrdersDocs(docs) {
    collectionsCache["orders_customers"] = docs.map(docSnap => {
        const data = docSnap.data();
        const phone = String(data.customerPhone || data.userPhone || data.phone || data.mobile || "N/A");
        const name = String(data.customerName || data.userName || data.name || data.customer || "");
        const email = String(data.customerEmail || data.userEmail || data.email || "");
        const uid = String(data.customerId || data.userId || data.customerUid || data.uid || (phone !== "N/A" ? `order_cust_${phone}` : `order_cust_${docSnap.id}`));

        if (phone === "N/A" && !name && !data.customerId && !data.userId) return null;

        return {
            uid: uid,
            name: name || (phone !== "N/A" ? `Customer (${phone})` : "Customer"),
            email: email,
            phone: phone,
            role: "CUSTOMER",
            dept: "",
            isActive: true,
            blockReason: "",
            collectionName: "customers"
        };
    }).filter(Boolean);
}

/**
 * Starts real-time Firestore listeners across all user account collections & order customers.
 */
export function startUsersService() {
    stopUsersService();

    // 1. One-time fetch immediately
    fetchAllUsersFromFirestore();

    // 2. Attach real-time onSnapshot listeners
    COLLECTIONS.forEach(colName => {
        try {
            const colRef = collection(db, colName);
            const unsub = onSnapshot(colRef, (snapshot) => {
                parseCollectionDocs(colName, snapshot.docs);
                mergeAndDeduplicateUsers();
                notifySubscribers();
            }, (err) => {
                console.warn(`[UserService] Firestore listener notice for ${colName}:`, err.message || err);
            });

            activeUnsubscribers.push(unsub);
        } catch (e) {
            console.error(`[UserService] Error attaching listener to ${colName}:`, e);
        }
    });

    try {
        const ordersRef = collection(db, "orders");
        const unsubOrders = onSnapshot(ordersRef, (snapshot) => {
            parseOrdersDocs(snapshot.docs);
            mergeAndDeduplicateUsers();
            notifySubscribers();
        }, (err) => {
            console.warn("[UserService] Orders listener notice:", err);
        });
        activeUnsubscribers.push(unsubOrders);
    } catch (e) {
        console.error("[UserService] Error attaching orders listener for customers:", e);
    }
}

/**
 * Stops all active Firestore listeners cleanly.
 */
export function stopUsersService() {
    activeUnsubscribers.forEach(unsub => {
        if (typeof unsub === 'function') unsub();
    });
    activeUnsubscribers = [];
}

function mergeAndDeduplicateUsers() {
    const mergedList = Object.values(collectionsCache).flat();
    const deduplicatedMap = new Map();

    mergedList.forEach((user, idx) => {
        if (!user) return;
        const emailLower = String(user.email || "").toLowerCase().trim();
        const phoneClean = String(user.phone || "").replace(/\s+/g, "").trim();

        // Primary key strategy: Email > Phone > UID to prevent duplicate entries for same user
        let key = "";
        if (emailLower) {
            key = `email_${emailLower}`;
        } else if (phoneClean && phoneClean !== "N/A") {
            key = `phone_${phoneClean}`;
        } else {
            key = user.uid || `user_idx_${idx}`;
        }

        const isMaster = isMasterAdminEmail(emailLower);
        if (isMaster) {
            user.role = "ADMIN";
        } else if (user.role === "ADMIN") {
            user.role = "OTHER"; // Force non-whitelisted admin back to staff/other
        }

        if (deduplicatedMap.has(key)) {
            const existing = deduplicatedMap.get(key);

            // Prefer real Firebase Auth UID over synthetic 'master_admin_...' or 'user_...'
            const preferredUid = (existing.uid && !existing.uid.startsWith("master_admin_") && !existing.uid.startsWith("user_"))
                ? existing.uid
                : user.uid;

            user = { ...existing, ...user };
            user.uid = preferredUid;
            user.role = isMaster ? "ADMIN" : (user.role || existing.role || "OTHER");
            user.name = (user.name && user.name !== "User" && user.name !== "Customer" && !user.name.startsWith("User (")) ? user.name : existing.name;
            user.email = user.email || existing.email;
            user.phone = (user.phone && user.phone !== "N/A") ? user.phone : existing.phone;
        }
        deduplicatedMap.set(key, user);
    });

    // Ensure BOTH master admin accounts are present as ADMINs and strictly only 1 entry per master email
    MASTER_ADMIN_EMAILS.forEach(masterEmail => {
        const key = `email_${masterEmail}`;
        if (!deduplicatedMap.has(key)) {
            const name = masterEmail.split('@')[0];
            const formattedName = name.charAt(0).toUpperCase() + name.slice(1) + " (Admin)";
            deduplicatedMap.set(key, {
                uid: `master_admin_${masterEmail.replace(/[^a-zA-Z0-9]/g, '_')}`,
                name: formattedName,
                email: masterEmail,
                phone: "N/A",
                role: "ADMIN",
                dept: "Management",
                isActive: true,
                blockReason: "",
                collectionName: "users"
            });
        } else {
            const existing = deduplicatedMap.get(key);
            existing.role = "ADMIN";
        }
    });

    let resultList = Array.from(deduplicatedMap.values());

    // FINAL STRICT CHECK: Ensure role "ADMIN" is ONLY present for the 2 whitelisted master emails
    resultList.forEach(u => {
        const emailLower = String(u.email || "").toLowerCase().trim();
        if (isMasterAdminEmail(emailLower)) {
            u.role = "ADMIN";
        } else if (u.role === "ADMIN") {
            u.role = "OTHER";
        }
    });

    if (resultList.length === 0) {
        resultList = [
            { uid: "usr_cust_1", name: "Rahul Sharma", email: "rahul.sharma@gmail.com", phone: "+91 9812345678", role: "CUSTOMER", dept: "", isActive: true, collectionName: "customers" },
            { uid: "usr_cust_2", name: "Pooja Verma", email: "pooja.v@gmail.com", phone: "+91 9898765432", role: "CUSTOMER", dept: "", isActive: true, collectionName: "customers" },
            { uid: "usr_merch_1", name: "Fresh Mart Dark Store", email: "store1@flash-o.com", phone: "+91 9823456789", role: "MERCHANT", dept: "", isActive: true, collectionName: "sellers" },
            { uid: "usr_rider_1", name: "Amit Kumar (Express Rider)", email: "amit.rider@flash-o.com", phone: "+91 9834567890", role: "RIDER", dept: "", isActive: true, collectionName: "riders" },
            { uid: "usr_staff_1", name: "Neha Singh (Operations)", email: "neha.s@flash-o.com", phone: "+91 9845678901", role: "OTHER", dept: "Operations & Support", isActive: true, collectionName: "staff" }
        ];
    }

    unifiedUsersList = resultList;
}

export function getUnifiedUsersList() {
    return unifiedUsersList;
}

/**
 * Toggles account status (Active / Blocked) in Firestore
 */
export async function toggleUserStatusInDb(uid, colName, currentIsActive, reason = null) {
    const userInList = unifiedUsersList.find(u => u.uid === uid);
    if (userInList && isMasterAdminEmail(userInList.email)) {
        throw new Error("Master Admin accounts cannot be blocked.");
    }
    const userDocRef = doc(db, colName, uid);
    await updateDoc(userDocRef, {
        isActive: !currentIsActive,
        blockReason: currentIsActive ? (reason || "Blocked by Admin") : null
    });
}

/**
 * Sends a password reset email
 */
export async function sendUserPasswordResetEmail(email) {
    if (!email) throw new Error("No email registered for this account.");
    await sendPasswordResetEmail(auth, email);
}

/**
 * Permanently deletes a user document from Firestore
 */
export async function deleteUserDocFromDb(uid, colName) {
    const userInList = unifiedUsersList.find(u => u.uid === uid);
    if (userInList && isMasterAdminEmail(userInList.email)) {
        throw new Error("Master Admin accounts cannot be deleted.");
    }
    await deleteDoc(doc(db, colName, uid));
}

/**
 * Updates an existing user account profile & role in Firestore
 */
export async function updateUserAccountInDb({ uid, colName, name, phone, email, role, dept, isActive, blockReason, isOnline, darkStore }) {
    if (!uid) throw new Error("Invalid User UID");
    if (role === "ADMIN" && !isMasterAdminEmail(email)) {
        throw new Error("Security Violation: Cannot assign Admin role to non-whitelisted email.");
    }

    // Always map the target collection strictly based on the assigned role
    const newTargetCollection = role === "ADMIN" ? "users" : (role === "RIDER" ? "riders" : (role === "OTHER" ? "staff" : (role === "CUSTOMER" ? "customers" : "sellers")));

    const userDocRef = doc(db, newTargetCollection, uid);
    const updateData = {
        uid: uid,
        name: name,
        displayName: name,
        fullName: name,
        phoneNumber: phone,
        mobile: phone,
        phone: phone,
        email: email,
        userEmail: email,
        role: role,
        staffDepartment: role === "OTHER" ? dept : "",
        isActive: isActive,
        active: isActive,
        blockReason: !isActive ? (blockReason || "Blocked by Admin") : null,
        updatedAt: Date.now()
    };

    if (role === "RIDER") {
        if (isOnline !== undefined) updateData.isOnline = isOnline;
        if (darkStore !== undefined) {
            updateData.darkStore = darkStore;
            updateData.darkStoreArea = darkStore;
        }
    }

    await setDoc(userDocRef, updateData, { merge: true });

    // If the role change causes a collection migration, remove the document from the old collection
    if (colName && colName !== newTargetCollection) {
        try {
            await deleteDoc(doc(db, colName, uid));
        } catch (e) {
            console.warn(`Could not delete doc from old collection ${colName}:`, e);
        }
    }

    await fetchAllUsersFromFirestore();
    return newTargetCollection;
}

/**
 * Creates a new user account in Auth and Firestore without logging out current session
 */
export async function createNewUserAccountInDb({ name, phone, email, pass, role, dept }) {
    if (role === "ADMIN" && !isMasterAdminEmail(email)) {
        throw new Error("Security Violation: Cannot assign Admin role to non-whitelisted email.");
    }
    const targetCollection = role === "ADMIN" ? "users" : (role === "RIDER" ? "riders" : (role === "OTHER" ? "staff" : (role === "CUSTOMER" ? "customers" : "sellers")));

    let secondaryApp = null;
    try {
        secondaryApp = initializeApp(firebaseConfig, "SecondaryAuthApp_" + Date.now());
        const secondaryAuth = getAuth(secondaryApp);

        const userCred = await createUserWithEmailAndPassword(secondaryAuth, email, pass);
        const newUid = userCred.user.uid;

        await setDoc(doc(db, targetCollection, newUid), {
            uid: newUid,
            name: name,
            email: email,
            phoneNumber: phone,
            role: role,
            staffDepartment: role === "OTHER" ? dept : "",
            isActive: true,
            createdAt: Date.now()
        });

        await signOut(secondaryAuth);
        return { uid: newUid, collection: targetCollection };
    } finally {
        if (secondaryApp) {
            try { await deleteApp(secondaryApp); } catch (_) {}
        }
    }
}

// Auto-start listeners & one-time fetch immediately
startUsersService();
document.addEventListener("DOMContentLoaded", () => {
    startUsersService();
});
