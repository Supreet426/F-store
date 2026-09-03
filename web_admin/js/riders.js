import { db } from './firebase-config.js';
import {
    collection,
    onSnapshot,
    doc,
    updateDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { escapeHtml } from './utils.js';

let ridersList = [];
let darkStoresList = [];

export function initRidersListener() {
    onSnapshot(collection(db, "riders"), (snapshot) => {
        ridersList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));

        renderRidersGrid(ridersList);
    }, (err) => {
        console.error("Error listening to riders collection:", err);
    });

    onSnapshot(collection(db, "dark_stores"), (snapshot) => {
        darkStoresList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
        renderRidersGrid(ridersList);
    }, (err) => {
        console.error("Error listening to dark_stores collection:", err);
    });
}

function renderRidersGrid(riders) {
    const container = document.getElementById("riders-container");
    if (!container) return;

    if (riders.length === 0) {
        container.innerHTML = `
            <div class="col-span-full p-8 text-center bg-slate-900 rounded-2xl border border-slate-800 text-slate-400">
                <i class="fa-solid fa-motorcycle text-2xl mb-2 text-slate-600 block"></i>
                No delivery partners found in riders collection. Add a Rider from User Management.
            </div>
        `;
        return;
    }

    container.innerHTML = riders.map(r => {
        const safeId = escapeHtml(r.id);
        const safeName = escapeHtml(r.name || 'Partner Rider');
        const safePhone = escapeHtml(r.phoneNumber || r.phone || 'N/A');

        const matchedStore = darkStoresList.find(ds => ds.id === r.darkStoreId);
        const safeStoreName = matchedStore ? escapeHtml(matchedStore.name) : escapeHtml(r.darkStoreArea || r.darkStore || 'Unassigned / Main Hub');

        const safeActiveOrderId = r.activeOrderId ? escapeHtml(r.activeOrderId) : null;
        const safeCompletedCount = escapeHtml(r.completedOrdersCount || 0);

        const storeOptionsHtml = `
            <option value="">-- Main Zone (Unassigned) --</option>
            ${darkStoresList.map(ds => `
                <option value="${escapeHtml(ds.id)}" ${ds.id === r.darkStoreId ? 'selected' : ''}>${escapeHtml(ds.name)} (${escapeHtml(ds.area || 'Hub')})</option>
            `).join('')}
        `;

        return `
            <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg hover:border-slate-700 transition-all">
                <div class="flex justify-between items-center border-b border-slate-800 pb-3">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-slate-800 border border-slate-700 font-bold text-white flex items-center justify-center">
                            <i class="fa-solid fa-motorcycle text-sky-400"></i>
                        </div>
                        <div>
                            <h4 class="font-bold text-white text-sm">${safeName}</h4>
                            <p class="text-xs text-slate-400 font-mono">${safePhone}</p>
                        </div>
                    </div>
                    <span class="badge ${r.isOnline ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-400'}">
                        ${r.isOnline ? 'ONLINE' : 'OFFLINE'}
                    </span>
                </div>

                <div class="space-y-2 text-xs text-slate-300">
                    <div>
                        <strong class="text-slate-400 block mb-1">Assigned Dark Store Hub:</strong>
                        <select onchange="updateRiderHubAssignment('${safeId}', this.value)" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-1.5 text-xs text-sky-300 focus:border-sky-500">
                            ${storeOptionsHtml}
                        </select>
                    </div>
                    <p><strong class="text-slate-400">Current Order:</strong> ${safeActiveOrderId ? `<span class="font-mono text-amber-400 font-bold">#${safeActiveOrderId.slice(0, 8)}</span>` : '<span class="text-emerald-400">Available for Dispatch</span>'}</p>
                    <p><strong class="text-slate-400">Completed Deliveries:</strong> <span class="font-mono text-white font-bold">${safeCompletedCount}</span></p>
                </div>

                <div class="flex justify-between items-center pt-2 border-t border-slate-800 text-xs">
                    <button onclick="toggleRiderDuty('${safeId}', ${r.isOnline})" class="text-slate-300 hover:text-white font-medium flex items-center gap-1 cursor-pointer">
                        <i class="fa-solid fa-toggle-on ${r.isOnline ? 'text-emerald-400' : 'text-slate-500'}"></i>
                        ${r.isOnline ? 'Set Offline' : 'Set Online'}
                    </button>
                    <span class="text-[11px] text-slate-500"><i class="fa-solid fa-location-arrow text-sky-400"></i> ${safeStoreName}</span>
                </div>
            </div>
        `;
    }).join('');
}

window.updateRiderHubAssignment = async function(riderId, newStoreId) {
    try {
        const matchedStore = darkStoresList.find(ds => ds.id === newStoreId);
        const storeName = matchedStore ? matchedStore.name : "";

        await updateDoc(doc(db, "riders", riderId), {
            darkStoreId: newStoreId || null,
            darkStoreArea: storeName || null,
            darkStore: storeName || null,
            updatedAt: Date.now()
        });

        if (window.showToast) window.showToast(`Rider hub updated to '${storeName || 'Unassigned'}'`, "success");
    } catch (err) {
        console.error("Error updating rider hub assignment:", err);
        if (window.showToast) window.showToast("Failed to assign rider to store hub", "error");
    }
};

window.toggleRiderDuty = async function(riderId, currentOnline) {
    try {
        await updateDoc(doc(db, "riders", riderId), {
            isOnline: !currentOnline,
            updatedAt: Date.now()
        });
        if (window.showToast) window.showToast(`Rider duty state changed to ${!currentOnline ? 'Online' : 'Offline'}`, "success");
    } catch (err) {
        console.error("Error toggling rider duty:", err);
        if (window.showToast) window.showToast("Failed to update rider status", "error");
    }
};

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
        initRidersListener();
    });
} else {
    initRidersListener();
}
