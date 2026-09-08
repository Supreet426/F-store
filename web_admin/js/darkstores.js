import { db } from './firebase-config.js';
import {
    collection,
    onSnapshot,
    doc,
    setDoc,
    deleteDoc,
    updateDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { escapeHtml } from './utils.js';

let darkStoresList = [];
let sellersList = [];
let ridersList = [];

let currentSelectedMerchantIds = new Set();
let currentSelectedRiderIds = new Set();

export function initDarkStoresListener() {
    // 1. Listen to Dark Stores
    onSnapshot(collection(db, "dark_stores"), (snapshot) => {
        darkStoresList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));

        updateDarkStoresKPIs(darkStoresList);
        renderDarkStoresGrid(darkStoresList);
    }, (err) => {
        console.error("Error listening to dark_stores collection:", err);
    });

    // 2. Listen to Sellers
    onSnapshot(collection(db, "sellers"), (snapshot) => {
        sellersList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
        renderModalMerchantsList();
        renderDarkStoresGrid(darkStoresList);
    }, (err) => {
        console.error("Error listening to sellers collection:", err);
    });

    // 3. Listen to Riders
    onSnapshot(collection(db, "riders"), (snapshot) => {
        ridersList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
        renderModalRidersList();
        renderDarkStoresGrid(darkStoresList);
    }, (err) => {
        console.error("Error listening to riders collection:", err);
    });
}

function updateDarkStoresKPIs(stores) {
    const activeStores = stores.filter(s => s.isActive !== false).length;
    const el = document.getElementById("kpi-active-dark-stores");
    if (el) el.textContent = `${activeStores} / ${stores.length} Active Hubs`;
}

window.renderDarkStoresGrid = function() {
    updateDarkStoresKPIs(darkStoresList);
    renderDarkStoresGrid(darkStoresList);
};

function renderDarkStoresGrid(stores) {
    const container = document.getElementById("dark-stores-container");
    if (!container) return;

    if (stores.length === 0) {
        container.innerHTML = `
            <div class="col-span-full p-8 text-center bg-slate-900 rounded-2xl border border-slate-800 text-slate-400">
                <i class="fa-solid fa-store text-2xl mb-2 text-slate-600 block"></i>
                No dark stores configured. Click "Add Dark Store" to set up a fulfillment hub.
            </div>
        `;
        return;
    }

    container.innerHTML = stores.map(ds => {
        try {
            const safeId = escapeHtml(ds.id);
            const safeName = escapeHtml(ds.name || 'Dark Store Hub');
            const safeArea = escapeHtml(ds.area || 'Fulfillment Zone');
            const safeAddress = escapeHtml(ds.address || 'N/A');
            const safeOpen = escapeHtml(ds.openTime || '07:00');
            const safeClose = escapeHtml(ds.closeTime || '23:00');
            const safeLat = escapeHtml(ds.centerLatitude || ds.latitude || 0);
            const safeLng = escapeHtml(ds.centerLongitude || ds.longitude || 0);
            const safeManager = escapeHtml(ds.managerName || 'Not Assigned');
            const safeDelRad = escapeHtml(ds.deliveryRadiusKm || 5.0);
            const safeVisRad = escapeHtml(ds.visibilityRadiusKm || 5.0);
            const safeMerRad = escapeHtml(ds.merchantRadiusKm || 5.0);
            const safeMinVal = escapeHtml(ds.minOrderValue || 99);
            const safeNote = escapeHtml(ds.statusNote || 'Normal Operation');

            // Linked merchants & riders count
            const merchantIds = Array.isArray(ds.assignedMerchantIds) ? ds.assignedMerchantIds : [];
            const riderIds = Array.isArray(ds.assignedRiderIds) ? ds.assignedRiderIds : [];

            const linkedMerchants = (sellersList || []).filter(s => s.darkStoreId === ds.id || merchantIds.includes(s.id));
            const linkedRiders = (ridersList || []).filter(r => r.darkStoreId === ds.id || riderIds.includes(r.id));

            return `
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-lg hover:border-slate-700 transition-all">
                    <div class="flex justify-between items-start border-b border-slate-800 pb-3">
                        <div>
                            <h4 class="font-bold text-white text-base">${safeName}</h4>
                            <p class="text-xs text-slate-400 flex items-center gap-1 mt-0.5"><i class="fa-solid fa-location-dot text-brand-400"></i> ${safeArea} (${safeAddress})</p>
                        </div>
                        <span class="badge ${ds.isActive !== false ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'}">
                            ${ds.isActive !== false ? 'Active Hub' : 'Inactive'}
                        </span>
                    </div>

                    <div class="space-y-2 text-xs text-slate-300">
                        <p><strong class="text-slate-400">Manager:</strong> <span class="text-indigo-400">${safeManager}</span></p>
                        <p><strong class="text-slate-400">Radii (KM):</strong> ${safeDelRad} (Delivery) | ${safeVisRad} (Visib) | <span class="text-amber-400 font-bold">${safeMerRad} (Merchant)</span></p>

                        <div class="flex items-center gap-2 pt-1">
                            <span class="badge bg-purple-500/10 text-purple-300 border border-purple-500/20 text-[11px]"><i class="fa-solid fa-store mr-1"></i> ${linkedMerchants.length} Merchants Linked</span>
                            <span class="badge bg-sky-500/10 text-sky-300 border border-sky-500/20 text-[11px]"><i class="fa-solid fa-motorcycle mr-1"></i> ${linkedRiders.length} Riders Linked</span>
                        </div>

                        <p><strong class="text-slate-400">Operating:</strong> <span class="font-mono text-amber-400">${safeOpen} - ${safeClose}</span> | Min ₹${safeMinVal}</p>
                        <p><strong class="text-slate-400">Coords:</strong> <code class="text-slate-400 font-mono">${safeLat}, ${safeLng}</code></p>
                        ${ds.statusNote ? `<p class="p-2 bg-slate-950/60 rounded-lg text-slate-300 border border-slate-800 text-[11px]"><i class="fa-solid fa-bullhorn text-amber-400 mr-1"></i> ${safeNote}</p>` : ''}
                    </div>

                    <div class="flex items-center justify-between pt-3 border-t border-slate-800 text-xs">
                        <button onclick="toggleDarkStoreActive('${safeId}', ${ds.isActive !== false})" class="text-slate-300 hover:text-white font-medium flex items-center gap-1 cursor-pointer">
                            <i class="fa-solid fa-power-off ${ds.isActive !== false ? 'text-emerald-400' : 'text-slate-500'}"></i>
                            ${ds.isActive !== false ? 'Disable Hub' : 'Enable Hub'}
                        </button>
                        <div class="flex items-center gap-2">
                            <button onclick="editDarkStore('${safeId}')" class="text-amber-400 hover:text-amber-300 font-medium cursor-pointer">Edit & Manage</button>
                            <button onclick="deleteDarkStore('${safeId}')" class="text-rose-400 hover:text-rose-300 font-medium cursor-pointer">Safe Delete</button>
                        </div>
                    </div>
                </div>
            `;
        } catch (e) {
            console.error("Error rendering dark store card:", e, ds);
            return '';
        }
    }).join('');
}

function renderModalMerchantsList() {
    const listEl = document.getElementById("ds-merchants-list");
    if (!listEl) return;

    const query = (document.getElementById("ds-merchant-search")?.value || "").toLowerCase().trim();

    const filtered = sellersList.filter(s => {
        const name = (s.tradeName || s.name || "").toLowerCase();
        const email = (s.email || "").toLowerCase();
        return !query || name.includes(query) || email.includes(query);
    });

    if (filtered.length === 0) {
        listEl.innerHTML = `<p class="text-slate-500 text-center py-2">No merchants found</p>`;
        return;
    }

    listEl.innerHTML = filtered.map(m => {
        const checked = currentSelectedMerchantIds.has(m.id);
        const name = escapeHtml(m.tradeName || m.name || m.id);
        const email = escapeHtml(m.email || '');

        return `
            <label class="flex items-center justify-between p-2 rounded-lg bg-slate-950/40 hover:bg-slate-800/60 border border-slate-800 cursor-pointer">
                <div class="flex items-center gap-2">
                    <input type="checkbox" onchange="toggleWebMerchantSelection('${m.id}', this.checked)" ${checked ? 'checked' : ''} class="w-4 h-4 rounded text-purple-600">
                    <div>
                        <span class="font-semibold text-white">${name}</span>
                        ${email ? `<span class="text-[10px] text-slate-400 block">${email}</span>` : ''}
                    </div>
                </div>
                ${checked ? '<span class="text-[10px] text-emerald-400 font-bold">LINKED</span>' : ''}
            </label>
        `;
    }).join('');

    const badge = document.getElementById("ds-merchants-count-badge");
    if (badge) badge.textContent = `${currentSelectedMerchantIds.size} Selected`;
}

function renderModalRidersList() {
    const listEl = document.getElementById("ds-riders-list");
    if (!listEl) return;

    const query = (document.getElementById("ds-rider-search")?.value || "").toLowerCase().trim();

    const filtered = ridersList.filter(r => {
        const name = (r.name || "").toLowerCase();
        const phone = (r.phoneNumber || r.phone || "").toLowerCase();
        return !query || name.includes(query) || phone.includes(query);
    });

    if (filtered.length === 0) {
        listEl.innerHTML = `<p class="text-slate-500 text-center py-2">No delivery riders found</p>`;
        return;
    }

    listEl.innerHTML = filtered.map(r => {
        const checked = currentSelectedRiderIds.has(r.id);
        const name = escapeHtml(r.name || r.id);
        const phone = escapeHtml(r.phoneNumber || r.phone || '');

        return `
            <label class="flex items-center justify-between p-2 rounded-lg bg-slate-950/40 hover:bg-slate-800/60 border border-slate-800 cursor-pointer">
                <div class="flex items-center gap-2">
                    <input type="checkbox" onchange="toggleWebRiderSelection('${r.id}', this.checked)" ${checked ? 'checked' : ''} class="w-4 h-4 rounded text-sky-600">
                    <div>
                        <span class="font-semibold text-white">${name}</span>
                        ${phone ? `<span class="text-[10px] text-slate-400 block">${phone}</span>` : ''}
                    </div>
                </div>
                ${checked ? '<span class="text-[10px] text-sky-400 font-bold">LINKED</span>' : ''}
            </label>
        `;
    }).join('');

    const badge = document.getElementById("ds-riders-count-badge");
    if (badge) badge.textContent = `${currentSelectedRiderIds.size} Selected`;
}

window.toggleWebMerchantSelection = function(merchantId, isChecked) {
    if (isChecked) {
        currentSelectedMerchantIds.add(merchantId);
    } else {
        currentSelectedMerchantIds.delete(merchantId);
    }
    renderModalMerchantsList();
};

window.toggleWebRiderSelection = function(riderId, isChecked) {
    if (isChecked) {
        currentSelectedRiderIds.add(riderId);
    } else {
        currentSelectedRiderIds.delete(riderId);
    }
    renderModalRidersList();
};

window.filterWebDarkStoreMerchants = function() {
    renderModalMerchantsList();
};

window.filterWebDarkStoreRiders = function() {
    renderModalRidersList();
};

window.openDarkStoreModal = function() {
    const modal = document.getElementById("modal-dark-store");
    if (modal) modal.classList.remove("hidden");
};

window.closeDarkStoreModal = function() {
    const modal = document.getElementById("modal-dark-store");
    if (modal) modal.classList.add("hidden");
    const form = document.getElementById("form-dark-store");
    if (form) form.reset();
    const idInput = document.getElementById("ds-id");
    if (idInput) idInput.value = "";
    currentSelectedMerchantIds.clear();
    currentSelectedRiderIds.clear();
};

window.setCurrentLocation = function() {
    if (navigator.geolocation) {
        if (window.showToast) window.showToast("Fetching current location...", "info");
        navigator.geolocation.getCurrentPosition((position) => {
            document.getElementById("ds-lat").value = position.coords.latitude.toFixed(6);
            document.getElementById("ds-lng").value = position.coords.longitude.toFixed(6);
            if (window.showToast) window.showToast("Location updated", "success");
        }, (err) => {
            console.error("Geolocation error:", err);
            if (window.showToast) window.showToast("Permission denied or location unavailable", "error");
        });
    } else {
        if (window.showToast) window.showToast("Geolocation not supported by this browser", "error");
    }
};

window.toggleDarkStoreActive = async function(id, currentActive) {
    try {
        await updateDoc(doc(db, "dark_stores", id), {
            isActive: !currentActive,
            updatedAt: Date.now()
        });
        if (window.showToast) window.showToast(`Hub ${!currentActive ? 'enabled' : 'disabled'} successfully`, "success");
    } catch (err) {
        console.error("Error toggling dark store active status:", err);
        if (window.showToast) window.showToast("Failed to update status", "error");
    }
};

window.deleteDarkStore = async function(id) {
    if (!confirm("Are you sure you want to delete this Dark Store Hub?\n\n🛡️ SAFE PROTECTION: All attached merchants and riders will be safely unassigned without losing any data.")) return;
    try {
        // Safe unassign attached merchants
        const attachedSellers = sellersList.filter(s => s.darkStoreId === id);
        for (const seller of attachedSellers) {
            try {
                await updateDoc(doc(db, "sellers", seller.id), { darkStoreId: null });
            } catch (_) {}
        }

        // Safe unassign attached riders
        const attachedRiders = ridersList.filter(r => r.darkStoreId === id);
        for (const rider of attachedRiders) {
            try {
                await updateDoc(doc(db, "riders", rider.id), { darkStoreId: null, darkStoreArea: null, darkStore: null });
            } catch (_) {}
        }

        await deleteDoc(doc(db, "dark_stores", id));
        if (window.showToast) window.showToast("Dark Store Hub deleted & attached partners safely unassigned!", "success");
    } catch (err) {
        console.error("Error deleting dark store:", err);
        if (window.showToast) window.showToast("Failed to delete dark store hub", "error");
    }
};

window.editDarkStore = function(id) {
    const ds = darkStoresList.find(item => item.id === id);
    if (!ds) return;

    const merchantIds = Array.isArray(ds.assignedMerchantIds) ? ds.assignedMerchantIds : [];
    const riderIds = Array.isArray(ds.assignedRiderIds) ? ds.assignedRiderIds : [];

    currentSelectedMerchantIds = new Set(
        merchantIds.concat(sellersList.filter(s => s.darkStoreId === id).map(s => s.id))
    );

    currentSelectedRiderIds = new Set(
        riderIds.concat(ridersList.filter(r => r.darkStoreId === id).map(r => r.id))
    );

    window.openDarkStoreModal();

    const setValue = (elemId, val) => {
        const el = document.getElementById(elemId);
        if (el) el.value = val !== undefined && val !== null ? val : '';
    };

    const setChecked = (elemId, val) => {
        const el = document.getElementById(elemId);
        if (el) el.checked = !!val;
    };

    setValue("ds-id", ds.id);
    setValue("ds-name", ds.name || '');
    setValue("ds-area", ds.area || '');
    setValue("ds-address", ds.address || '');
    setValue("ds-lat", ds.centerLatitude || ds.latitude || '');
    setValue("ds-lng", ds.centerLongitude || ds.longitude || '');
    setValue("ds-delivery-radius", ds.deliveryRadiusKm || 5.0);
    setValue("ds-visibility-radius", ds.visibilityRadiusKm || 5.0);
    setValue("ds-merchant-radius", ds.merchantRadiusKm || 5.0);

    setValue("ds-min-order-val", ds.minOrderValue !== undefined ? ds.minOrderValue : 99);
    setValue("ds-max-orders-per-rider", ds.maxOrdersPerRider !== undefined ? ds.maxOrdersPerRider : 3);
    setValue("ds-surge-multiplier", ds.surgeFeeMultiplier !== undefined ? ds.surgeFeeMultiplier : 1.0);
    setChecked("ds-auto-dispatch", ds.autoDispatchRiders !== false);
    setValue("ds-status-note", ds.statusNote || '');

    setValue("ds-manager-name", ds.managerName || '');
    setValue("ds-manager-mobile", ds.managerMobile || '');
    setValue("ds-manager-aadhar", ds.aadharNumber || '');

    setValue("ds-bank-name", ds.bankName || '');
    setValue("ds-bank-account", ds.bankAccountNumber || '');
    setValue("ds-bank-ifsc", ds.bankIfsc || '');

    setValue("ds-open", ds.openTime || '07:00');
    setValue("ds-close", ds.closeTime || '23:00');
    setChecked("ds-active", ds.isActive !== false);

    renderModalMerchantsList();
    renderModalRidersList();
};

async function handleDarkStoreSubmit(e) {
    e.preventDefault();
    const getValue = (elemId) => document.getElementById(elemId)?.value?.trim() || '';
    const getFloat = (elemId, fallback = 0) => parseFloat(document.getElementById(elemId)?.value) || fallback;
    const getInt = (elemId, fallback = 0) => parseInt(document.getElementById(elemId)?.value) || fallback;
    const getChecked = (elemId) => document.getElementById(elemId)?.checked ?? true;

    const existingId = getValue("ds-id");
    const dsId = existingId || 'ds_' + Date.now();
    const storeName = getValue("ds-name");

    const storeData = {
        id: dsId,
        name: storeName,
        area: getValue("ds-area"),
        address: getValue("ds-address"),
        centerLatitude: getFloat("ds-lat"),
        centerLongitude: getFloat("ds-lng"),
        latitude: getFloat("ds-lat"),
        longitude: getFloat("ds-lng"),
        deliveryRadiusKm: getFloat("ds-delivery-radius", 5.0),
        visibilityRadiusKm: getFloat("ds-visibility-radius", 5.0),
        merchantRadiusKm: getFloat("ds-merchant-radius", 5.0),
        minOrderValue: getFloat("ds-min-order-val", 99),
        maxOrdersPerRider: getInt("ds-max-orders-per-rider", 3),
        surgeFeeMultiplier: getFloat("ds-surge-multiplier", 1.0),
        autoDispatchRiders: getChecked("ds-auto-dispatch"),
        statusNote: getValue("ds-status-note"),
        managerName: getValue("ds-manager-name"),
        managerMobile: getValue("ds-manager-mobile"),
        aadharNumber: getValue("ds-manager-aadhar"),
        bankName: getValue("ds-bank-name"),
        bankAccountNumber: getValue("ds-bank-account"),
        bankIfsc: getValue("ds-bank-ifsc"),
        openTime: getValue("ds-open") || '07:00',
        closeTime: getValue("ds-close") || '23:00',
        isActive: getChecked("ds-active"),
        assignedMerchantIds: Array.from(currentSelectedMerchantIds),
        assignedRiderIds: Array.from(currentSelectedRiderIds),
        updatedAt: Date.now()
    };

    try {
        await setDoc(doc(db, "dark_stores", dsId), storeData, { merge: true });

        // Sync sellers (merchants)
        for (const seller of sellersList) {
            const shouldBeAssigned = currentSelectedMerchantIds.has(seller.id);
            const isCurrentlyAssigned = seller.darkStoreId === dsId;
            if (shouldBeAssigned && !isCurrentlyAssigned) {
                await updateDoc(doc(db, "sellers", seller.id), { darkStoreId: dsId });
            } else if (!shouldBeAssigned && isCurrentlyAssigned) {
                await updateDoc(doc(db, "sellers", seller.id), { darkStoreId: null });
            }
        }

        // Sync riders
        for (const rider of ridersList) {
            const shouldBeAssigned = currentSelectedRiderIds.has(rider.id);
            const isCurrentlyAssigned = rider.darkStoreId === dsId;
            if (shouldBeAssigned && !isCurrentlyAssigned) {
                await updateDoc(doc(db, "riders", rider.id), { darkStoreId: dsId, darkStoreArea: storeName, darkStore: storeName });
            } else if (!shouldBeAssigned && isCurrentlyAssigned) {
                await updateDoc(doc(db, "riders", rider.id), { darkStoreId: null, darkStoreArea: null, darkStore: null });
            }
        }

        if (window.showToast) window.showToast(`Dark Store Hub ${existingId ? 'updated' : 'created'} successfully!`, "success");
        window.closeDarkStoreModal();
    } catch (err) {
        console.error("Error saving dark store:", err);
        if (window.showToast) window.showToast("Failed to save dark store", "error");
    }
}

// Global document submit listener delegation for dynamic component loading
document.addEventListener("submit", (e) => {
    if (e.target && e.target.id === "form-dark-store") {
        handleDarkStoreSubmit(e);
    }
});

export function initDarkStoresModule() {
    initDarkStoresListener();
}

// Immediate execution on module load
initDarkStoresListener();
