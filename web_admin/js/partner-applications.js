/**
 * Partner Applications Management Controller for Web Admin
 * Handles real-time listening to Firestore "partner_applications", filtering, modal status updates & admin feedback notes.
 */

import { db } from './firebase-config.js';
import {
    collection,
    doc,
    onSnapshot,
    updateDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { escapeHtml, exportToCsv } from './utils.js';

let partnerApplicationsList = [];

// Initialize Firestore Listener for partner_applications collection
export function initPartnerApplicationsListener() {
    const colRef = collection(db, "partner_applications");

    onSnapshot(colRef, (snapshot) => {
        partnerApplicationsList = snapshot.docs.map(docSnap => {
            const data = docSnap.data();
            return {
                id: docSnap.id,
                userId: data.userId || "",
                partnerType: data.partnerType || "SELL_ON_FLASHO",
                applicantName: data.applicantName || "N/A",
                phone: data.phone || "N/A",
                email: data.email || "N/A",
                businessName: data.businessName || "",
                gstNumber: data.gstNumber || "",
                fssaiNumber: data.fssaiNumber || "",
                address: data.address || "",
                city: data.city || "",
                category: data.category || "",
                supplyCapacity: data.supplyCapacity || "",
                trademarkNumber: data.trademarkNumber || "",
                socialMediaHandle: data.socialMediaHandle || "",
                payoutUpiOrBank: data.payoutUpiOrBank || "",
                additionalNotes: data.additionalNotes || "",
                adminNotes: data.adminNotes || "",
                status: data.status || "PENDING",
                timestamp: data.timestamp || Date.now()
            };
        });

        // Sort latest first
        partnerApplicationsList.sort((a, b) => b.timestamp - a.timestamp);

        updatePartnerKPIs(partnerApplicationsList);
        renderPartnerApplicationsTable();
    }, (err) => {
        console.warn("Partner Applications listener warning:", err);
    });
}

function updatePartnerKPIs(apps) {
    const total = apps.length;
    const sellCount = apps.filter(a => a.partnerType === 'SELL_ON_FLASHO').length;
    const supplierCount = apps.filter(a => a.partnerType === 'DARK_STORE_SUPPLIER').length;
    const brandCount = apps.filter(a => a.partnerType === 'BRAND_PROTECTION').length;
    const affiliateCount = apps.filter(a => a.partnerType === 'AFFILIATE_PARTNER').length;
    const pendingCount = apps.filter(a => a.status === 'PENDING' || a.status === 'UNDER_REVIEW').length;

    const elTotal = document.getElementById('kpi-partner-total');
    const elSell = document.getElementById('kpi-partner-sell');
    const elSupplier = document.getElementById('kpi-partner-supplier');
    const elBrand = document.getElementById('kpi-partner-brand');
    const elAffiliate = document.getElementById('kpi-partner-affiliate');
    const elPending = document.getElementById('kpi-partner-pending');
    const elBadge = document.getElementById('badge-partner-apps');

    if (elTotal) elTotal.textContent = total;
    if (elSell) elSell.textContent = sellCount;
    if (elSupplier) elSupplier.textContent = supplierCount;
    if (elBrand) elBrand.textContent = brandCount;
    if (elAffiliate) elAffiliate.textContent = affiliateCount;
    if (elPending) elPending.textContent = pendingCount;

    if (elBadge) {
        if (pendingCount > 0) {
            elBadge.textContent = pendingCount;
            elBadge.classList.remove('hidden');
        } else {
            elBadge.classList.add('hidden');
        }
    }
}

export function renderPartnerApplicationsTable() {
    const tbody = document.getElementById('partner-apps-table-body');
    if (!tbody) return;

    const typeFilter = document.getElementById('partner-filter-type')?.value || 'ALL';
    const statusFilter = document.getElementById('partner-filter-status')?.value || 'ALL';
    const searchQuery = (document.getElementById('partner-search-input')?.value || '').toLowerCase().trim();

    let filtered = partnerApplicationsList.filter(app => {
        if (typeFilter !== 'ALL' && app.partnerType !== typeFilter) return false;
        if (statusFilter !== 'ALL' && app.status !== statusFilter) return false;

        if (searchQuery) {
            const matchesSearch =
                app.applicantName.toLowerCase().includes(searchQuery) ||
                app.businessName.toLowerCase().includes(searchQuery) ||
                app.phone.toLowerCase().includes(searchQuery) ||
                app.email.toLowerCase().includes(searchQuery) ||
                app.gstNumber.toLowerCase().includes(searchQuery) ||
                app.city.toLowerCase().includes(searchQuery) ||
                app.id.toLowerCase().includes(searchQuery);

            if (!matchesSearch) return false;
        }

        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="px-5 py-8 text-center text-slate-500">
                    <i class="fa-solid fa-inbox text-2xl mb-2 text-slate-600 block"></i>
                    No partner applications found matching selected criteria.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtered.map(app => {
        const formattedDate = new Date(app.timestamp).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
        });

        const [typeBadgeClass, typeLabel] = getTypeBadge(app.partnerType);
        const [statusBadgeClass, statusLabel] = getStatusBadge(app.status);

        const businessText = app.businessName ? escapeHtml(app.businessName) : escapeHtml(app.applicantName);
        const legalDetails = [
            app.gstNumber ? `GST: ${escapeHtml(app.gstNumber)}` : '',
            app.fssaiNumber ? `FSSAI: ${escapeHtml(app.fssaiNumber)}` : '',
            app.trademarkNumber ? `TM: ${escapeHtml(app.trademarkNumber)}` : '',
            app.socialMediaHandle ? `Social: ${escapeHtml(app.socialMediaHandle)}` : '',
            app.city ? `City: ${escapeHtml(app.city)}` : ''
        ].filter(Boolean).join(' | ') || 'N/A';

        return `
            <tr class="hover:bg-slate-800/40 transition-colors">
                <td class="px-5 py-3.5">
                    <div class="font-bold text-slate-200">#${escapeHtml(app.id.slice(-8))}</div>
                    <div class="text-[10px] text-slate-500">${formattedDate}</div>
                </td>
                <td class="px-5 py-3.5">
                    <div class="font-bold text-white text-sm">${businessText}</div>
                    <div class="text-[11px] text-slate-400">Applicant: ${escapeHtml(app.applicantName)}</div>
                </td>
                <td class="px-5 py-3.5">
                    <span class="px-2.5 py-1 rounded-lg text-[10px] font-extrabold uppercase border ${typeBadgeClass}">
                        ${typeLabel}
                    </span>
                </td>
                <td class="px-5 py-3.5">
                    <div class="text-slate-200"><i class="fa-solid fa-phone text-slate-500 mr-1 text-[10px]"></i>${escapeHtml(app.phone)}</div>
                    <div class="text-slate-400 text-[11px]"><i class="fa-solid fa-envelope text-slate-500 mr-1 text-[10px]"></i>${escapeHtml(app.email)}</div>
                </td>
                <td class="px-5 py-3.5 max-w-xs truncate text-[11px] text-slate-300">
                    ${legalDetails}
                </td>
                <td class="px-5 py-3.5 text-center">
                    <span class="px-2.5 py-1 rounded-full text-[10px] font-bold border ${statusBadgeClass}">
                        ${statusLabel}
                    </span>
                </td>
                <td class="px-5 py-3.5 text-right">
                    <button onclick="openManagePartnerModal('${escapeHtml(app.id)}')" class="bg-purple-600/20 hover:bg-purple-600 text-purple-300 hover:text-white border border-purple-500/40 px-3 py-1.5 rounded-xl font-bold text-[11px] transition-all cursor-pointer">
                        <i class="fa-solid fa-pen-to-square mr-1"></i> Manage
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function getTypeBadge(type) {
    switch (type) {
        case 'SELL_ON_FLASHO':
            return ['bg-purple-500/20 text-purple-300 border-purple-500/40', 'Sell on Flash-O'];
        case 'DARK_STORE_SUPPLIER':
            return ['bg-sky-500/20 text-sky-300 border-sky-500/40', 'Dark Store Vendor'];
        case 'BRAND_PROTECTION':
            return ['bg-emerald-500/20 text-emerald-300 border-emerald-500/40', 'Brand Registry'];
        case 'AFFILIATE_PARTNER':
            return ['bg-amber-500/20 text-amber-300 border-amber-500/40', 'Affiliate Creator'];
        default:
            return ['bg-slate-700 text-slate-300 border-slate-600', type];
    }
}

function getStatusBadge(status) {
    switch (status) {
        case 'PENDING':
            return ['bg-amber-500/20 text-amber-300 border-amber-500/40', 'PENDING'];
        case 'UNDER_REVIEW':
            return ['bg-sky-500/20 text-sky-300 border-sky-500/40', 'UNDER REVIEW'];
        case 'APPROVED':
            return ['bg-emerald-500/20 text-emerald-300 border-emerald-500/40', 'APPROVED'];
        case 'REJECTED':
            return ['bg-rose-500/20 text-rose-300 border-rose-500/40', 'REJECTED'];
        default:
            return ['bg-slate-700 text-slate-300 border-slate-600', status];
    }
}

window.openManagePartnerModal = function(appId) {
    const app = partnerApplicationsList.find(a => a.id === appId);
    if (!app) return;

    document.getElementById('partner-modal-app-id').value = app.id;
    document.getElementById('partner-modal-ref-id').textContent = `Ref ID: ${app.id}`;
    document.getElementById('partner-modal-status-select').value = app.status;
    document.getElementById('partner-modal-admin-notes').value = app.adminNotes || '';

    const detailsBox = document.getElementById('partner-modal-details-box');
    if (detailsBox) {
        detailsBox.innerHTML = `
            <div class="flex justify-between border-b border-slate-800 pb-1">
                <span class="text-slate-400">Applicant:</span>
                <span class="font-bold text-white">${escapeHtml(app.applicantName)}</span>
            </div>
            <div class="flex justify-between border-b border-slate-800 pb-1">
                <span class="text-slate-400">Business Name:</span>
                <span class="font-bold text-purple-300">${escapeHtml(app.businessName || 'N/A')}</span>
            </div>
            <div class="flex justify-between border-b border-slate-800 pb-1">
                <span class="text-slate-400">Phone & Email:</span>
                <span class="text-slate-200">${escapeHtml(app.phone)} | ${escapeHtml(app.email)}</span>
            </div>
            ${app.gstNumber ? `<div class="flex justify-between border-b border-slate-800 pb-1"><span class="text-slate-400">GSTIN:</span><span class="font-mono text-emerald-400">${escapeHtml(app.gstNumber)}</span></div>` : ''}
            ${app.fssaiNumber ? `<div class="flex justify-between border-b border-slate-800 pb-1"><span class="text-slate-400">FSSAI:</span><span class="font-mono text-sky-400">${escapeHtml(app.fssaiNumber)}</span></div>` : ''}
            ${app.trademarkNumber ? `<div class="flex justify-between border-b border-slate-800 pb-1"><span class="text-slate-400">Trademark No:</span><span class="font-mono text-amber-400">${escapeHtml(app.trademarkNumber)}</span></div>` : ''}
            ${app.socialMediaHandle ? `<div class="flex justify-between border-b border-slate-800 pb-1"><span class="text-slate-400">Social Handle:</span><span class="text-indigo-300">${escapeHtml(app.socialMediaHandle)}</span></div>` : ''}
            ${app.payoutUpiOrBank ? `<div class="flex justify-between border-b border-slate-800 pb-1"><span class="text-slate-400">Payout UPI/Bank:</span><span class="font-mono text-emerald-300">${escapeHtml(app.payoutUpiOrBank)}</span></div>` : ''}
            ${app.address || app.city ? `<div class="flex justify-between border-b border-slate-800 pb-1"><span class="text-slate-400">Address/City:</span><span class="text-slate-300">${escapeHtml(app.address)} ${escapeHtml(app.city)}</span></div>` : ''}
            ${app.additionalNotes ? `<div class="pt-1"><span class="text-slate-400 block">User Notes:</span><span class="text-slate-300 italic">${escapeHtml(app.additionalNotes)}</span></div>` : ''}
        `;
    }

    const modal = document.getElementById('modal-manage-partner-app');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
};

function closeManagePartnerModal() {
    const modal = document.getElementById('modal-manage-partner-app');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

// Expose listener globally
window.initPartnerApplicationsListener = initPartnerApplicationsListener;

// Initialize on DOMReady or immediately
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPartnerApplicationsListener);
} else {
    initPartnerApplicationsListener();
}

    document.getElementById('partner-filter-type')?.addEventListener('change', renderPartnerApplicationsTable);
    document.getElementById('partner-filter-status')?.addEventListener('change', renderPartnerApplicationsTable);
    document.getElementById('partner-search-input')?.addEventListener('input', renderPartnerApplicationsTable);

    document.getElementById('btn-close-partner-modal')?.addEventListener('click', closeManagePartnerModal);
    document.getElementById('btn-cancel-partner-modal')?.addEventListener('click', closeManagePartnerModal);

    document.getElementById('form-update-partner-app')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const appId = document.getElementById('partner-modal-app-id').value;
        const newStatus = document.getElementById('partner-modal-status-select').value;
        const adminNotes = document.getElementById('partner-modal-admin-notes').value;

        if (!appId) return;

        try {
            const docRef = doc(db, "partner_applications", appId);
            await updateDoc(docRef, {
                status: newStatus,
                adminNotes: adminNotes,
                lastUpdatedByAdmin: Date.now()
            });

            if (typeof window.showToast === 'function') {
                window.showToast(`Partner application ${newStatus} successfully!`, 'success');
            }
            closeManagePartnerModal();
        } catch (err) {
            console.error("Failed to update partner application:", err);
            if (typeof window.showToast === 'function') {
                window.showToast("Failed to update application: " + err.message, 'error');
            }
        }
    });

    document.getElementById('btn-export-partner-apps')?.addEventListener('click', () => {
        if (partnerApplicationsList.length === 0) {
            if (typeof window.showToast === 'function') window.showToast('No data to export', 'error');
            return;
        }

        const exportData = partnerApplicationsList.map(a => ({
            ID: a.id,
            Type: a.partnerType,
            Applicant: a.applicantName,
            Business: a.businessName,
            Phone: a.phone,
            Email: a.email,
            GST: a.gstNumber,
            FSSAI: a.fssaiNumber,
            City: a.city,
            Status: a.status,
            AdminNotes: a.adminNotes,
            Date: new Date(a.timestamp).toISOString()
        }));

        exportToCsv('FlashO_Partner_Applications.csv', exportData);
    });
});
