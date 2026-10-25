import { db } from './firebase-config.js';
import {
    collection,
    doc,
    onSnapshot,
    setDoc,
    deleteDoc,
    getDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const COLLECTIONS = [
    { name: 'vendor_inquiries', type: 'VENDOR' },
    { name: 'supplier_inquiries', type: 'SUPPLIER' },
    { name: 'rider_inquiries', type: 'RIDER' },
    { name: 'franchise_inquiries', type: 'FRANCHISE' },
    { name: 'b2b_inquiries', type: 'B2B' },
    { name: 'refund_requests', type: 'REFUND' },
    { name: 'ad_inquiries', type: 'ADVERTISING' },
    { name: 'ip_reports', type: 'IP_REPORT' },
    { name: 'job_applications', type: 'CAREERS' }
];

let allInquiries = [];
let currentFilter = 'ALL';
const unsubscribers = [];

export function initInquiriesListener() {
    unsubscribers.forEach(unsub => unsub());
    unsubscribers.length = 0;

    COLLECTIONS.forEach(col => {
        const unsub = onSnapshot(collection(db, col.name), (snapshot) => {
            const items = snapshot.docs.map(docSnap => ({
                id: docSnap.id,
                _colName: col.name,
                type: col.type,
                ...docSnap.data()
            }));

            // Merge items into allInquiries replacing old collection items
            allInquiries = allInquiries.filter(item => item._colName !== col.name).concat(items);
            allInquiries.sort((a, b) => (b.submittedAt || 0) - (a.submittedAt || 0));

            updateBadgeCount();
            renderInquiries();
        }, (err) => {
            console.warn(`Error loading ${col.name}:`, err);
        });
        unsubscribers.push(unsub);
    });

    loadGlobalSupportConfig();
}

function updateBadgeCount() {
    const pendingCount = allInquiries.filter(i => (i.status || 'PENDING') === 'PENDING').length;
    const badgeEl = document.getElementById('badge-inquiries');
    if (badgeEl) badgeEl.textContent = pendingCount;
    const totalEl = document.getElementById('inquiries-total-count');
    if (totalEl) totalEl.textContent = allInquiries.length;
}

function renderInquiries() {
    const tbody = document.getElementById('inquiries-table-body');
    if (!tbody) return;

    const filtered = currentFilter === 'ALL'
        ? allInquiries
        : allInquiries.filter(i => i.type === currentFilter);

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="p-8 text-center text-slate-500">
                    <i class="fa-solid fa-folder-open text-2xl mb-2 block"></i>
                    No ${currentFilter === 'ALL' ? '' : currentFilter} inquiries found. Submissions from customer web & app will appear here in real time.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtered.map(item => {
        const dateStr = item.submittedAt ? new Date(item.submittedAt).toLocaleDateString() + ' ' + new Date(item.submittedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'N/A';
        const status = item.status || 'PENDING';

        let statusBadgeClass = 'bg-amber-500/20 text-amber-400 border-amber-500/30';
        if (status === 'APPROVED' || status === 'CONTACTED') statusBadgeClass = 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
        if (status === 'REJECTED') statusBadgeClass = 'bg-rose-500/20 text-rose-400 border-rose-500/30';

        const nameDisplay = item.brand || item.company || item.name || item.owner || item.contact || 'N/A';
        const contactDisplay = item.phone || item.email ? `<div><p class="font-bold text-white">${item.phone || ''}</p><p class="text-[11px] text-slate-400">${item.email || ''}</p></div>` : 'N/A';

        let detailsSummary = '';
        if (item.type === 'VENDOR') detailsSummary = `GST: ${item.gst || 'N/A'} â€¢ Cat: ${item.category || 'General'}`;
        else if (item.type === 'SUPPLIER') detailsSummary = `Items: ${item.items || 'N/A'} â€¢ Capacity: ${item.capacity || 'N/A'}`;
        else if (item.type === 'RIDER') detailsSummary = `Vehicle: ${item.vehicle || 'EV'} â€¢ City: ${item.city || 'N/A'}`;
        else if (item.type === 'FRANCHISE') detailsSummary = `Budget: ${item.budget || 'N/A'} â€¢ City: ${item.city || 'N/A'} â€¢ Space: ${item.space || 'N/A'} â€¢ Exp: ${item.retailExperience || 'No'}`;
        else if (item.type === 'B2B') detailsSummary = `Quantity: ${item.qty || 'N/A'}`;
        else if (item.type === 'REFUND') detailsSummary = `Order ID: ${item.orderId || 'N/A'} â€¢ Reason: ${item.reason || 'N/A'}`;
        else if (item.type === 'ADVERTISING') detailsSummary = `Budget: ${item.budget || 'N/A'}`;
        else if (item.type === 'IP_REPORT') detailsSummary = `Issue: ${item.desc || 'Fake Listing'}`;
        else detailsSummary = `Role: ${item.role || 'General'}`;

        return `
            <tr class="hover:bg-slate-800/40 transition">
                <td class="p-4 font-mono text-[11px]">
                    <span class="text-slate-400">${dateStr}</span>
                    <p class="text-[10px] text-slate-500 truncate w-24">${item.id}</p>
                </td>
                <td class="p-4">
                    <span class="bg-slate-800 text-slate-300 border border-slate-700 font-extrabold text-[10px] px-2.5 py-1 rounded-full uppercase">
                        ${item.type}
                    </span>
                </td>
                <td class="p-4">
                    <p class="font-black text-white text-xs">${nameDisplay}</p>
                    <p class="text-[11px] text-slate-400">${item.owner ? 'Owner: ' + item.owner : ''}</p>
                </td>
                <td class="p-4">${contactDisplay}</td>
                <td class="p-4 max-w-xs truncate text-slate-300 font-medium">${detailsSummary}</td>
                <td class="p-4">
                    <select onchange="window.updateInquiryStatus('${item._colName}', '${item.id}', this.value)" class="bg-slate-950 border ${statusBadgeClass} text-xs font-bold rounded-lg px-2.5 py-1 outline-none cursor-pointer">
                        <option value="PENDING" ${status === 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="CONTACTED" ${status === 'CONTACTED' ? 'selected' : ''}>CONTACTED</option>
                        <option value="APPROVED" ${status === 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                        <option value="REJECTED" ${status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                    </select>
                </td>
                <td class="p-4 text-right">
                    <button onclick="window.deleteInquiry('${item._colName}', '${item.id}')" class="text-rose-400 hover:text-rose-300 font-bold px-2 py-1 rounded hover:bg-rose-950/40 transition">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

window.filterInquiries = function(filterType) {
    currentFilter = filterType;
    document.querySelectorAll('#inquiry-filter-pills .inquiry-pill').forEach(btn => {
        btn.classList.remove('bg-brand-600', 'text-white');
        btn.classList.add('text-slate-400');
    });
    event.target.classList.remove('text-slate-400');
    event.target.classList.add('bg-brand-600', 'text-white');
    renderInquiries();
};

window.refreshInquiries = function() {
    initInquiriesListener();
    if (window.showToast) window.showToast("Inquiries refreshed", "success");
};

window.updateInquiryStatus = async function(colName, docId, newStatus) {
    try {
        await setDoc(doc(db, colName, docId), { status: newStatus, updatedAt: Date.now() }, { merge: true });
        if (window.showToast) window.showToast(`Status updated to ${newStatus}`, "success");
    } catch (err) {
        console.error("Error updating status:", err);
        if (window.showToast) window.showToast("Failed to update status", "error");
    }
};

window.deleteInquiry = async function(colName, docId) {
    if (!confirm("Are you sure you want to delete this inquiry?")) return;
    try {
        await deleteDoc(doc(db, colName, docId));
        if (window.showToast) window.showToast("Inquiry deleted", "success");
    } catch (err) {
        console.error("Error deleting inquiry:", err);
        if (window.showToast) window.showToast("Failed to delete", "error");
    }
};

// GLOBAL SUPPORT & HELP CENTER CONFIG SYNC
async function loadGlobalSupportConfig() {
    try {
        const docRef = doc(db, "settings", "store_config");
        const snap = await getDoc(docRef);
        if (snap.exists()) {
            const data = snap.data();
            if (data.helpline && document.getElementById('cfg-helpline')) document.getElementById('cfg-helpline').value = data.helpline;
            if (data.supportEmail && document.getElementById('cfg-email')) document.getElementById('cfg-email').value = data.supportEmail;
            if (data.minFreeDeliveryThreshold && document.getElementById('cfg-min-free-delivery')) document.getElementById('cfg-min-free-delivery').value = data.minFreeDeliveryThreshold;
            if (data.standardDeliveryFee && document.getElementById('cfg-delivery-fee')) document.getElementById('cfg-delivery-fee').value = data.standardDeliveryFee;
            if (data.promisedDeliveryMinutes && document.getElementById('cfg-delivery-time')) document.getElementById('cfg-delivery-time').value = data.promisedDeliveryMinutes;
            if (data.apkDownloadUrl && document.getElementById('cfg-apk-link')) document.getElementById('cfg-apk-link').value = data.apkDownloadUrl;
        }
    } catch (err) {
        console.warn("Error loading store_config:", err);
    }
}

window.saveGlobalSupportConfig = async function() {
    const helpline = document.getElementById('cfg-helpline')?.value.trim() || '1800-108-FLASH (3527)';
    const supportEmail = document.getElementById('cfg-email')?.value.trim() || 'support@flash-o.com';
    const minFreeDeliveryThreshold = parseFloat(document.getElementById('cfg-min-free-delivery')?.value) || 99;
    const standardDeliveryFee = parseFloat(document.getElementById('cfg-delivery-fee')?.value) || 15;
    const promisedDeliveryMinutes = parseInt(document.getElementById('cfg-delivery-time')?.value) || 10;
    const apkDownloadUrl = document.getElementById('cfg-apk-link')?.value.trim() || '';

    const payload = {
        helpline,
        supportEmail,
        minFreeDeliveryThreshold,
        standardDeliveryFee,
        promisedDeliveryMinutes,
        apkDownloadUrl,
        lastUpdated: Date.now()
    };

    try {
        await setDoc(doc(db, "settings", "store_config"), payload, { merge: true });
        await setDoc(doc(db, "settings", "footer_config"), payload, { merge: true });
        if (window.showToast) window.showToast("Global Support & Store Settings saved & synced to Web & Android App!", "success");
    } catch (err) {
        console.error("Error saving global config:", err);
        if (window.showToast) window.showToast("Error saving config", "error");
    }
};

// Initialize listener when module loads
initInquiriesListener();
loadGlobalSupportConfig();
window.loadGlobalSupportConfig = loadGlobalSupportConfig;
