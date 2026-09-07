import {
    onUsersUpdated,
    getUnifiedUsersList,
    toggleUserStatusInDb,
    sendUserPasswordResetEmail,
    deleteUserDocFromDb,
    createNewUserAccountInDb,
    updateUserAccountInDb
} from './user-service.js';
import { db } from './firebase-config.js';
import { doc, setDoc } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { escapeHtml, exportToCsv } from './utils.js';

let activeRoleFilter = "ALL";
let currentUsersCache = [];
let isUserUIInitialized = false;

// Keeps track of initial user state for "discard" functionality in full profile view
let currentlyEditingUser = null;

export function initUserUI() {
    if (isUserUIInitialized) return;
    isUserUIInitialized = true;

    onUsersUpdated((users) => {
        currentUsersCache = users;
        updateUserKPIs(users);
        filterAndRenderUsersTable();
    });

    const searchInput = document.getElementById("searchInput");
    if (searchInput) {
        searchInput.addEventListener("input", filterAndRenderUsersTable);
    }

    const formAdd = document.getElementById("form-add-user");
    if (formAdd) {
        formAdd.addEventListener("submit", handleAddUserFormSubmit);
    }

    // Full profile form submit
    const formProfile = document.getElementById("form-full-profile");
    if (formProfile) {
        formProfile.addEventListener("submit", handleFullProfileFormSubmit);
    }

    // Modal edit form submit
    const formEdit = document.getElementById("form-edit-user");
    if (formEdit) {
        formEdit.addEventListener("submit", handleEditUserFormSubmit);
    }
}

export function updateUserKPIs(users) {
    const kpiTotal = document.getElementById("kpi-total-users");
    const kpiOverviewTotal = document.getElementById("kpi-users");

    if (kpiTotal) kpiTotal.textContent = users.length;
    if (kpiOverviewTotal) kpiOverviewTotal.textContent = users.length;

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

export function filterAndRenderUsersTable() {
    const searchVal = document.getElementById("searchInput")?.value.toLowerCase().trim() || "";

    const filtered = currentUsersCache.filter(user => {
        const matchesRole = activeRoleFilter === "ALL" ||
            user.role === activeRoleFilter ||
            (activeRoleFilter === "MERCHANT" && user.role === "STORE_OWNER");

        const matchesSearch = !searchVal ||
            String(user.name || "").toLowerCase().includes(searchVal) ||
            String(user.email || "").toLowerCase().includes(searchVal) ||
            String(user.phone || "").toLowerCase().includes(searchVal) ||
            String(user.dept || "").toLowerCase().includes(searchVal);

        return matchesRole && matchesSearch;
    });

    renderUsersTable(filtered);
}

function renderUsersTable(users) {
    const tbody = document.getElementById("userTableBody");
    if (!tbody) return;

    if (users.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="p-8 text-center text-slate-500 font-medium">
                    <i class="fa-solid fa-user-slash text-2xl mb-2 text-slate-600 block"></i>
                    No accounts found matching search or filter criteria.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = users.map(u => {
        const safeName = escapeHtml(String(u.name || "User"));
        const safeEmail = escapeHtml(String(u.email || ""));
        const safePhone = escapeHtml(String(u.phone || "N/A"));
        const safeDept = escapeHtml(String(u.dept || ""));
        const safeUid = escapeHtml(String(u.uid || ""));
        const safeColName = escapeHtml(String(u.collectionName || "users"));

        const initial = String(u.name || "U").charAt(0).toUpperCase() || "U";
        const deptText = u.role === 'OTHER' && safeDept ? `<br><span class="text-[10px] font-semibold text-purple-400">Dept: ${safeDept}</span>` : '';
        const isRider = u.role === 'RIDER';
        const isRiderOnline = u.isOnline !== false;
        const riderDutyBadge = isRider ? `<br><span class="mt-1 inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded ${isRiderOnline ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-400'}"><i class="fa-solid fa-power-off mr-1"></i> ${isRiderOnline ? 'ONLINE' : 'OFFLINE'}</span>` : '';

        return `
            <tr onclick="openUserProfile('${safeUid}', '${safeColName}')" class="hover:bg-slate-800/40 transition-colors cursor-pointer" title="Click anywhere to view full profile & manage account">
                <td class="p-4">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-slate-800 border border-slate-700 font-bold text-white flex items-center justify-center shrink-0 shadow-sm">
                            ${escapeHtml(initial)}
                        </div>
                        <div class="overflow-hidden">
                            <p class="font-bold text-white truncate text-sm">${safeName}</p>
                            <p class="text-xs text-slate-400 truncate">${safeEmail || safePhone}</p>
                        </div>
                    </div>
                </td>
                <td class="p-4 font-mono text-xs font-bold text-slate-300">${safePhone}</td>
                <td class="p-4">
                    ${getRoleBadgeHTML(u.role)}
                    ${deptText}
                    ${riderDutyBadge}
                </td>
                <td class="p-4">
                    <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold ${u.isActive ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'}">
                        <span class="w-1.5 h-1.5 rounded-full ${u.isActive ? 'bg-emerald-400' : 'bg-rose-400'} mr-1.5"></span>
                        ${u.isActive ? 'Active' : 'Blocked'}
                    </span>
                </td>
                <td class="p-4 text-right">
                    <div class="flex justify-end items-center gap-1.5" onclick="event.stopPropagation()">
                        ${isRider ? `
                            <button onclick="event.stopPropagation(); quickToggleRiderDuty('${safeUid}', '${safeColName}', ${isRiderOnline})" title="Toggle Duty (Online/Offline)" class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-sky-400 flex items-center justify-center transition-colors cursor-pointer border border-slate-700/60">
                                <i class="fa-solid fa-power-off ${isRiderOnline ? 'text-emerald-400' : 'text-slate-500'}"></i>
                            </button>
                        ` : ''}
                        <button onclick="event.stopPropagation(); toggleUserStatus('${safeUid}', '${safeColName}', ${u.isActive})" title="Toggle Block/Active" class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer border border-slate-700/60">
                            <i class="fa-solid ${u.isActive ? 'fa-ban text-rose-400' : 'fa-circle-check text-emerald-400'}"></i>
                        </button>
                        <button onclick="event.stopPropagation(); sendPasswordReset('${safeEmail}')" title="Send Password Reset Email" class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer border border-slate-700/60">
                            <i class="fa-solid fa-key text-amber-400"></i>
                        </button>
                        <button onclick="event.stopPropagation(); deleteUserAccount('${safeUid}', '${safeColName}')" title="Delete Account" class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-rose-900/40 text-slate-400 hover:text-rose-400 flex items-center justify-center transition-colors cursor-pointer border border-slate-700/60">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getRoleBadgeHTML(role) {
    switch (role) {
        case 'ADMIN':
            return `<span class="badge bg-indigo-500/20 text-indigo-400 border border-indigo-500/30"><i class="fa-solid fa-shield-halved mr-1"></i> ADMIN</span>`;
        case 'MERCHANT':
        case 'STORE_OWNER':
            return `<span class="badge bg-amber-500/20 text-amber-400 border border-amber-500/30"><i class="fa-solid fa-store mr-1"></i> MERCHANT</span>`;
        case 'RIDER':
            return `<span class="badge bg-sky-500/20 text-sky-400 border border-sky-500/30"><i class="fa-solid fa-motorcycle mr-1"></i> RIDER</span>`;
        case 'CUSTOMER':
            return `<span class="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/20"><i class="fa-solid fa-user mr-1"></i> CUSTOMER</span>`;
        default:
            return `<span class="badge bg-purple-500/20 text-purple-400 border border-purple-500/30"><i class="fa-solid fa-headset mr-1"></i> ${escapeHtml(role)}</span>`;
    }
}

/* DIRECT COMMUNICATION ACTION SHORTCUTS */

window.contactUserViaPhone = function() {
    if (!currentlyEditingUser || !currentlyEditingUser.phone || currentlyEditingUser.phone === 'N/A') {
        if (window.showToast) window.showToast("No mobile number available for this account", "error");
        return;
    }
    window.open(`tel:${currentlyEditingUser.phone}`, '_self');
};

window.contactUserViaWhatsApp = function() {
    if (!currentlyEditingUser || !currentlyEditingUser.phone || currentlyEditingUser.phone === 'N/A') {
        if (window.showToast) window.showToast("No mobile number available for this account", "error");
        return;
    }
    const cleanPhone = String(currentlyEditingUser.phone).replace(/[^0-9]/g, '');
    const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;
    window.open(`https://api.whatsapp.com/send?phone=${formattedPhone}`, '_blank');
};

window.contactUserViaEmail = function() {
    if (!currentlyEditingUser || !currentlyEditingUser.email) {
        if (window.showToast) window.showToast("No email address available for this account", "error");
        return;
    }
    window.open(`mailto:${currentlyEditingUser.email}`, '_self');
};

/* GLOBAL WINDOW HANDLERS FOR HTML EVENT BINDING */

window.openUserProfile = function(uid, colName) {
    const users = getUnifiedUsersList();
    const user = users.find(u => u.uid === uid || u.email === uid || u.phone === uid);
    if (!user) {
        if (window.showToast) window.showToast("User details not found", "error");
        return;
    }

    currentlyEditingUser = { ...user };
    window.switchTab('user-profile');

    // Populate identity elements
    const avatar = document.getElementById("profile-avatar");
    if (avatar) avatar.textContent = String(user.name || "U").charAt(0).toUpperCase();

    const dispName = document.getElementById("profile-display-name");
    if (dispName) dispName.textContent = user.name || "Unnamed User";

    const dispEmail = document.getElementById("profile-display-email");
    if (dispEmail) dispEmail.textContent = user.email || "no-email@flash-o.com";

    const dispPhone = document.getElementById("profile-display-phone");
    if (dispPhone) dispPhone.textContent = user.phone || "N/A";

    const dispUid = document.getElementById("profile-display-uid");
    if (dispUid) dispUid.textContent = user.uid || "Unknown UID";

    // Populate Badge Status
    const roleBadge = document.getElementById("profile-role-badge");
    if (roleBadge) roleBadge.innerHTML = getRoleBadgeHTML(user.role);

    const statusBadge = document.getElementById("profile-status-badge");
    if (statusBadge) {
        statusBadge.innerHTML = `<span class="badge ${user.isActive ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border-rose-500/20'}">${user.isActive ? 'Active Account' : 'Blocked / Frozen'}</span>`;
    }

    // Populate Form Inputs
    const uidInput = document.getElementById("full-profile-uid");
    if (uidInput) uidInput.value = user.uid || "";

    const colInput = document.getElementById("full-profile-col");
    if (colInput) colInput.value = user.collectionName || colName || "users";

    const nameInput = document.getElementById("fp-name");
    if (nameInput) nameInput.value = user.name || "";

    const emailInput = document.getElementById("fp-email");
    if (emailInput) emailInput.value = user.email || "";

    const phoneInput = document.getElementById("fp-phone");
    if (phoneInput) phoneInput.value = (user.phone && user.phone !== "N/A") ? user.phone : "";

    const colDisplay = document.getElementById("fp-col-display");
    if (colDisplay) colDisplay.textContent = user.collectionName || "users";

    const uidDisplay = document.getElementById("fp-uid-display");
    if (uidDisplay) uidDisplay.textContent = user.uid || "";

    const roleSelect = document.getElementById("fp-role");
    if (roleSelect) roleSelect.value = user.role || "CUSTOMER";

    const approvedSelect = document.getElementById("fp-approved");
    if (approvedSelect) approvedSelect.value = user.isApproved ? "true" : "false";

    const activeSelect = document.getElementById("fp-active");
    if (activeSelect) activeSelect.value = user.isActive ? "true" : "false";

    const deptWrap = document.getElementById("fp-dept-wrap");
    if (deptWrap) deptWrap.style.display = user.role === "OTHER" ? "block" : "none";

    const deptSelect = document.getElementById("fp-dept");
    if (deptSelect && user.dept) deptSelect.value = user.dept;

    // Rider details card visibility & population
    const riderWrap = document.getElementById("fp-rider-wrap");
    if (riderWrap) riderWrap.style.display = user.role === "RIDER" ? "block" : "none";

    const riderOnline = document.getElementById("fp-rider-online");
    if (riderOnline) riderOnline.value = user.isOnline !== false ? "true" : "false";

    const riderStore = document.getElementById("fp-rider-store");
    if (riderStore) riderStore.value = user.darkStoreArea || user.darkStore || "Central Zone";

    const riderActiveOrder = document.getElementById("fp-rider-active-order");
    if (riderActiveOrder) riderActiveOrder.textContent = user.activeOrderId ? `#${user.activeOrderId.slice(0, 8)}` : "None (Available)";

    const riderCompletedCount = document.getElementById("fp-rider-completed-count");
    if (riderCompletedCount) riderCompletedCount.textContent = `${user.completedOrdersCount || 0} orders`;

    const reasonWrap = document.getElementById("fp-reason-wrap");
    if (reasonWrap) reasonWrap.style.display = !user.isActive ? "block" : "none";

    const reasonInput = document.getElementById("fp-reason");
    if (reasonInput) reasonInput.value = user.blockReason || "";

    const freezeBtnText = document.getElementById("lbl-profile-freeze");
    if (freezeBtnText) freezeBtnText.textContent = user.isActive ? "Freeze" : "Reactivate";

    // Admin Internal Notes Textarea
    const adminNotesInput = document.getElementById("fp-admin-notes");
    if (adminNotesInput) adminNotesInput.value = user.blockReason || user.adminNotes || "";

    // Toggle Role Sections Visibility
    const isMerchant = user.role === "MERCHANT" || user.role === "STORE_OWNER";
    const isCustomer = user.role === "CUSTOMER";

    const merchWrap = document.getElementById("merchant-details-wrap");
    if (merchWrap) merchWrap.style.display = isMerchant ? "block" : "none";

    const custWrap = document.getElementById("customer-details-wrap");
    if (custWrap) custWrap.style.display = isCustomer ? "block" : "none";

    // Populate Merchant Details
    if (isMerchant) {
        const now = Date.now();
        const isVacation = user.vacationStart && user.vacationEnd && now >= user.vacationStart && now <= user.vacationEnd;
        const statusPill = document.getElementById("merchant-store-status-pill");
        if (statusPill) {
            if (!user.isActive) {
                statusPill.innerHTML = `<span class="badge bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-ban mr-1"></i> STORE BLOCKED</span>`;
            } else if (isVacation) {
                statusPill.innerHTML = `<span class="badge bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-umbrella-beach mr-1"></i> ON VACATION</span>`;
            } else {
                statusPill.innerHTML = `<span class="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-circle-check mr-1"></i> OPEN / LIVE</span>`;
            }
        }

        const prepTime = document.getElementById("m-prep-time");
        if (prepTime) prepTime.textContent = `${user.averagePreparationTime || 15} Mins`;

        const darkstore = document.getElementById("m-darkstore-hub");
        if (darkstore) darkstore.textContent = user.darkStoreId || user.darkStoreArea || "Central Main Hub";

        const vacWin = document.getElementById("m-vacation-window");
        if (vacWin) {
            if (user.vacationStart && user.vacationEnd) {
                const s = new Date(user.vacationStart).toLocaleDateString();
                const e = new Date(user.vacationEnd).toLocaleDateString();
                vacWin.textContent = `${s} - ${e}`;
            } else {
                vacWin.textContent = "None Scheduled";
            }
        }

        const tradeName = document.getElementById("m-trade-name");
        if (tradeName) tradeName.textContent = user.tradeName || user.name || "N/A";

        const legalName = document.getElementById("m-legal-name");
        if (legalName) legalName.textContent = user.legalName || "N/A";

        const bizType = document.getElementById("m-biz-type");
        if (bizType) bizType.textContent = user.businessType || "Retail Merchant";

        const gstin = document.getElementById("m-gstin");
        if (gstin) gstin.textContent = user.gstin || "N/A";

        const fssai = document.getElementById("m-fssai");
        if (fssai) fssai.textContent = user.fssaiNumber || "N/A";

        const pan = document.getElementById("m-pan");
        if (pan) pan.textContent = user.panNumber || "N/A";

        const bankName = document.getElementById("m-bank-name");
        if (bankName) bankName.textContent = user.bankName || "N/A";

        const bankHolder = document.getElementById("m-bank-holder");
        if (bankHolder) bankHolder.textContent = user.bankAccountName || "N/A";

        const bankAcc = document.getElementById("m-bank-acc");
        if (bankAcc) bankAcc.textContent = user.bankAccountNumber || "N/A";

        const bankIfsc = document.getElementById("m-bank-ifsc");
        if (bankIfsc) bankIfsc.textContent = user.bankIfscCode || "N/A";

        const storeAddress = document.getElementById("m-store-address");
        if (storeAddress) storeAddress.textContent = user.storeAddress || user.address || "N/A";

        const coinsVal = document.getElementById("m-coins-val");
        if (coinsVal) coinsVal.textContent = user.flashOCoins || 0;

        const feeDiscountInput = document.getElementById("m-platform-discount-input");
        if (feeDiscountInput) feeDiscountInput.value = user.platformFeeDiscount !== undefined ? user.platformFeeDiscount : 0;

        const refCode = document.getElementById("m-ref-code");
        if (refCode) refCode.textContent = user.merchantReferralCode || "N/A";

        const refCount = document.getElementById("m-ref-count");
        if (refCount) refCount.textContent = `${user.referralCount || 0} Leads`;
    }

    // Populate Customer Details
    if (isCustomer) {
        const now = Date.now();
        const lastOrderTs = user.lastOrderDate || user.lastLogin;
        const daysSinceLastOrder = lastOrderTs ? Math.floor((now - lastOrderTs) / (1000 * 60 * 60 * 24)) : null;

        const churnBadge = document.getElementById("customer-churn-badge");
        if (churnBadge) {
            if (daysSinceLastOrder === null) {
                churnBadge.innerHTML = `<span class="badge bg-sky-500/20 text-sky-400 border border-sky-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-user-plus mr-1"></i> NEW USER (NO ORDERS)</span>`;
            } else if (daysSinceLastOrder <= 1) {
                churnBadge.innerHTML = `<span class="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-bolt mr-1"></i> ACTIVE TODAY</span>`;
            } else if (daysSinceLastOrder <= 7) {
                churnBadge.innerHTML = `<span class="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-check mr-1"></i> ACTIVE THIS WEEK</span>`;
            } else if (daysSinceLastOrder <= 30) {
                churnBadge.innerHTML = `<span class="badge bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-clock-rotate-left mr-1"></i> REGULAR ACTIVE (${daysSinceLastOrder} DAYS AGO)</span>`;
            } else {
                churnBadge.innerHTML = `<span class="badge bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs px-3 py-1 font-bold animate-pulse"><i class="fa-solid fa-triangle-exclamation mr-1"></i> AT CHURN RISK (${daysSinceLastOrder} DAYS INACTIVE)</span>`;
            }
        }

        const lifetimeSpend = document.getElementById("c-lifetime-spend");
        if (lifetimeSpend) lifetimeSpend.textContent = `₹${user.lifetimeSpending || 0}`;

        const walletBal = document.getElementById("c-wallet-balance");
        if (walletBal) walletBal.textContent = `₹${user.walletBalance || 0}`;

        const passTitle = document.getElementById("c-pass-title");
        const passDesc = document.getElementById("c-pass-desc");
        if (passTitle && passDesc) {
            if (user.isSubscribed && user.subscriptionExpiry) {
                const isExpired = user.subscriptionExpiry < Date.now();
                if (isExpired) {
                    passTitle.textContent = "PASS EXPIRED";
                    passTitle.className = "font-bold text-sm text-rose-400";
                    passDesc.textContent = `Expired on ${new Date(user.subscriptionExpiry).toLocaleDateString()}`;
                } else {
                    passTitle.textContent = `ACTIVE PASS: ${String(user.subscriptionTier || 'VIP').toUpperCase()}`;
                    passTitle.className = "font-bold text-sm text-purple-400";
                    passDesc.textContent = `Valid until ${new Date(user.subscriptionExpiry).toLocaleDateString()}`;
                }
            } else if (user.isTrialEligible) {
                passTitle.textContent = "28-DAY TRIAL PACK ELIGIBLE";
                passTitle.className = "font-bold text-sm text-amber-400";
                passDesc.textContent = "Trial pack will activate automatically on first order checkout.";
            } else {
                passTitle.textContent = "No Active Pass Subscription";
                passTitle.className = "font-bold text-sm text-slate-300";
                passDesc.textContent = "Customer has no active Flash-O Pass plan.";
            }
        }

        const acqSource = document.getElementById("c-acq-source");
        if (acqSource) acqSource.textContent = user.acquisitionSource || "Direct / Mobile App";

        const prefPay = document.getElementById("c-preferred-payment");
        if (prefPay) prefPay.textContent = user.preferredPaymentMethod || "COD / UPI";

        const devModel = document.getElementById("c-device-model");
        if (devModel) devModel.textContent = user.device || "Android Device";

        const appVer = document.getElementById("c-app-version");
        if (appVer) appVer.textContent = user.appVersion || "v1.0.0";

        const delAddr = document.getElementById("c-delivery-address");
        if (delAddr) delAddr.textContent = user.address || "Not provided";

        const primaryAddr = document.getElementById("c-primary-address");
        if (primaryAddr) primaryAddr.textContent = user.address || "Not provided";

        const delNotes = document.getElementById("c-delivery-notes");
        if (delNotes) delNotes.innerHTML = `<i class="fa-solid fa-motorcycle mr-1.5"></i> ${user.defaultDeliveryInstructions || "Leave at door / Call on arrival"}`;

        const totalRefunded = document.getElementById("c-total-refunded");
        if (totalRefunded) totalRefunded.textContent = `₹${user.totalRefundedAmount || 0}`;

        const refundCount = document.getElementById("c-refund-count");
        if (refundCount) refundCount.textContent = `${user.totalRefundsCount || 0} Claims`;

        const ticketsCount = document.getElementById("c-tickets-count");
        if (ticketsCount) ticketsCount.textContent = `${user.openSupportTicketsCount || 0} Open`;

        const refundRiskBadge = document.getElementById("c-refund-risk-badge");
        if (refundRiskBadge) {
            const isHighRisk = (user.totalRefundsCount || 0) > 3 || (user.totalOrders > 0 && ((user.cancelledOrders || 0) / user.totalOrders) > 0.3);
            if (isHighRisk) {
                refundRiskBadge.innerHTML = `<span class="badge bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs px-3 py-1 font-bold animate-pulse"><i class="fa-solid fa-triangle-exclamation mr-1"></i> HIGH REFUND RISK</span>`;
            } else {
                refundRiskBadge.innerHTML = `<span class="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs px-3 py-1 font-bold"><i class="fa-solid fa-shield-check mr-1"></i> LOW DISPUTE RISK</span>`;
            }
        }

        const cartValue = document.getElementById("c-cart-value");
        if (cartValue) cartValue.textContent = `₹${user.cartAbandonedTotalValue || 0} (${user.cartAbandonedItemsCount || 0} Items)`;

        const cartStatusBadge = document.getElementById("c-cart-status-badge");
        if (cartStatusBadge) {
            if ((user.cartAbandonedItemsCount || 0) > 0) {
                cartStatusBadge.innerHTML = `<span class="badge bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs px-2.5 py-0.5 font-bold"><i class="fa-solid fa-cart-arrow-down mr-1"></i> CART PENDING</span>`;
            } else {
                cartStatusBadge.innerHTML = `<span class="badge bg-slate-800 text-slate-400 border border-slate-700 text-xs px-2.5 py-0.5 font-bold">NO PENDING CART</span>`;
            }
        }

        const pushStatus = document.getElementById("c-push-status");
        if (pushStatus) {
            pushStatus.textContent = user.pushNotificationEnabled !== false ? "Enabled" : "Disabled";
            pushStatus.className = user.pushNotificationEnabled !== false ? "font-bold text-emerald-400" : "font-bold text-rose-400";
        }

        const waStatus = document.getElementById("c-wa-status");
        if (waStatus) {
            waStatus.textContent = user.whatsappUpdatesOptIn !== false ? "Opted In" : "Opted Out";
            waStatus.className = user.whatsappUpdatesOptIn !== false ? "font-bold text-emerald-400" : "font-bold text-rose-400";
        }

        const ratingScore = document.getElementById("c-rating-score");
        if (ratingScore) ratingScore.textContent = `⭐ ${user.avgCustomerRating || 5.0} / 5.0`;
    }

    // Reset Save Bar
    const saveBar = document.getElementById("fp-save-bar");
    if (saveBar) saveBar.classList.add("hidden");
};

window.saveMerchantPlatformDiscount = async function() {
    if (!currentlyEditingUser) return;
    const discountVal = parseFloat(document.getElementById("m-platform-discount-input")?.value || "0");
    try {
        const docRef = doc(db, currentlyEditingUser.collectionName || "sellers", currentlyEditingUser.uid);
        await setDoc(docRef, { platformFeeDiscount: discountVal }, { merge: true });
        currentlyEditingUser.platformFeeDiscount = discountVal;
        if (window.showToast) window.showToast(`Platform Fee Discount saved as ${discountVal}%`, "success");
    } catch (err) {
        console.error("Error saving platform fee discount:", err);
        if (window.showToast) window.showToast("Failed to save discount", "error");
    }
};

window.saveAdminInternalNoteFromProfile = async function() {
    if (!currentlyEditingUser) return;
    const noteVal = document.getElementById("fp-admin-notes")?.value.trim() || "";
    try {
        const docRef = doc(db, currentlyEditingUser.collectionName || "users", currentlyEditingUser.uid);
        await setDoc(docRef, { blockReason: noteVal, adminNotes: noteVal }, { merge: true });
        currentlyEditingUser.blockReason = noteVal;
        currentlyEditingUser.adminNotes = noteVal;
        if (window.showToast) window.showToast("Admin internal note saved successfully!", "success");
    } catch (err) {
        console.error("Error saving internal note:", err);
        if (window.showToast) window.showToast("Failed to save internal note", "error");
    }
};

window.grantCustomerPassModal = async function() {
    if (!currentlyEditingUser) return;
    const tier = prompt("Enter Flash-O Pass Tier to grant (e.g. SILVER, GOLD, VIP):", "VIP");
    if (!tier) return;
    const expiryDays = 30;
    const expiryTs = Date.now() + (expiryDays * 24 * 60 * 60 * 1000);
    try {
        const docRef = doc(db, currentlyEditingUser.collectionName || "customers", currentlyEditingUser.uid);
        await setDoc(docRef, {
            isSubscribed: true,
            subscriptionTier: tier,
            subscriptionExpiry: expiryTs
        }, { merge: true });
        if (window.showToast) window.showToast(`Granted 30-Day ${tier} Flash-O Pass!`, "success");
        window.openUserProfile(currentlyEditingUser.uid, currentlyEditingUser.collectionName);
    } catch (err) {
        console.error("Error granting pass:", err);
        if (window.showToast) window.showToast("Failed to grant pass", "error");
    }
};

window.sendCustomerCouponModal = function() {
    if (!currentlyEditingUser) return;
    const code = prompt("Enter Coupon Code to send to customer:", "FLASHO100");
    if (!code) return;
    if (window.showToast) window.showToast(`Targeted coupon ${code} sent to ${currentlyEditingUser.name}!`, "success");
};

window.onProfileInputChange = function() {
    const saveBar = document.getElementById("fp-save-bar");
    if (saveBar) saveBar.classList.remove("hidden");
};

window.onProfileRoleChange = function(val) {
    const deptWrap = document.getElementById("fp-dept-wrap");
    if (deptWrap) deptWrap.style.display = val === "OTHER" ? "block" : "none";
    const riderWrap = document.getElementById("fp-rider-wrap");
    if (riderWrap) riderWrap.style.display = val === "RIDER" ? "block" : "none";
    window.onProfileInputChange();
};

window.quickToggleRiderDuty = async function(uid, colName, currentOnline) {
    const userInList = currentUsersCache.find(u => u.uid === uid);
    try {
        await updateUserAccountInDb({
            uid,
            colName: colName || "riders",
            name: userInList?.name || "",
            phone: userInList?.phone || "",
            email: userInList?.email || "",
            role: "RIDER",
            isActive: userInList ? userInList.isActive : true,
            isOnline: !currentOnline,
            darkStore: userInList?.darkStore || "Central Zone"
        });
        if (window.showToast) window.showToast(`Rider duty changed to ${!currentOnline ? 'ONLINE' : 'OFFLINE'}`, "success");
    } catch (err) {
        console.error("Error toggling rider duty:", err);
        if (window.showToast) window.showToast("Failed to toggle duty", "error");
    }
};

window.toggleRiderDutyDirect = async function() {
    const uid = document.getElementById("full-profile-uid")?.value;
    const colName = document.getElementById("full-profile-col")?.value || "riders";
    const onlineSelect = document.getElementById("fp-rider-online");
    const currentOnline = onlineSelect?.value === "true";
    const newOnline = !currentOnline;

    try {
        await updateUserAccountInDb({
            uid,
            colName,
            name: document.getElementById("fp-name")?.value || "",
            phone: document.getElementById("fp-phone")?.value || "",
            email: document.getElementById("fp-email")?.value || "",
            role: "RIDER",
            isActive: document.getElementById("fp-active")?.value === "true",
            isOnline: newOnline,
            darkStore: document.getElementById("fp-rider-store")?.value
        });
        if (onlineSelect) onlineSelect.value = newOnline ? "true" : "false";
        if (window.showToast) window.showToast(`Rider duty changed to ${newOnline ? 'ONLINE' : 'OFFLINE'}`, "success");
        window.openUserProfile(uid, colName);
    } catch (err) {
        console.error("Error toggling rider duty:", err);
        if (window.showToast) window.showToast("Failed to toggle duty", "error");
    }
};

window.onProfileActiveChange = function(val) {
    const reasonWrap = document.getElementById("fp-reason-wrap");
    if (reasonWrap) reasonWrap.style.display = val === "false" ? "block" : "none";
    window.onProfileInputChange();
};

window.resetProfileInputs = function() {
    if (currentlyEditingUser) {
        window.openUserProfile(currentlyEditingUser.uid, currentlyEditingUser.collectionName);
    }
};

window.handleFullProfileFormSubmit = async function(e) {
    e.preventDefault();
    const uid = document.getElementById("full-profile-uid")?.value || "";
    const colName = document.getElementById("full-profile-col")?.value || "users";
    const name = document.getElementById("fp-name")?.value.trim() || "";
    const email = document.getElementById("fp-email")?.value.trim() || "";
    const phone = document.getElementById("fp-phone")?.value.trim() || "";
    const role = document.getElementById("fp-role")?.value || "CUSTOMER";
    const dept = document.getElementById("fp-dept")?.value || "";
    const isActive = document.getElementById("fp-active")?.value === "true";
    const blockReason = document.getElementById("fp-reason")?.value.trim() || "";

    try {
        await updateUserAccountInDb({ uid, colName, name, phone, email, role, dept, isActive, blockReason });
        if (window.showToast) window.showToast("User profile updated successfully!", "success");
        window.openUserProfile(uid, colName);
    } catch (err) {
        console.error("Error updating user profile:", err);
        if (window.showToast) window.showToast("Failed to update profile: " + err.message, "error");
    }
};

window.sendPasswordResetFromProfile = function() {
    const email = document.getElementById("fp-email")?.value;
    window.sendPasswordReset(email);
};

window.toggleFreezeFromProfile = function() {
    const uid = document.getElementById("full-profile-uid")?.value;
    const colName = document.getElementById("full-profile-col")?.value;
    const isActive = document.getElementById("fp-active")?.value === "true";
    window.toggleUserStatus(uid, colName, isActive);
};

window.deleteAccountFromProfile = async function() {
    const uid = document.getElementById("full-profile-uid")?.value;
    const colName = document.getElementById("full-profile-col")?.value;
    if (confirm("Permanently delete this account? This cannot be undone.")) {
        try {
            await deleteUserDocFromDb(uid, colName);
            if (window.showToast) window.showToast("Account deleted successfully", "success");
            window.switchTab('users');
        } catch (err) {
            console.error("Error deleting account:", err);
            if (window.showToast) window.showToast("Failed to delete account", "error");
        }
    }
};

// Legacy Modal Support (Optional Fallback)
window.openEditUserModal = window.openUserProfile;

window.closeEditUserModal = function() {
    const modal = document.getElementById("modal-edit-user");
    if (modal) modal.classList.add("hidden");
};

async function handleEditUserFormSubmit(e) {
    e.preventDefault();
    const uid = document.getElementById("edit-user-uid")?.value || "";
    const colName = document.getElementById("edit-user-col")?.value || "users";
    const name = document.getElementById("edit-user-name")?.value.trim() || "";
    const phone = document.getElementById("edit-user-phone")?.value.trim() || "";
    const email = document.getElementById("edit-user-email")?.value.trim() || "";
    const role = document.getElementById("edit-user-role")?.value || "CUSTOMER";
    const dept = document.getElementById("edit-user-dept")?.value || "";
    const isActive = document.getElementById("edit-user-active")?.value === "true";
    const blockReason = document.getElementById("edit-user-reason")?.value.trim() || "";

    try {
        await updateUserAccountInDb({ uid, colName, name, phone, email, role, dept, isActive, blockReason });
        if (window.showToast) window.showToast("User details updated successfully!", "success");
        window.closeEditUserModal();
    } catch (err) {
        console.error("Error updating user details:", err);
        if (window.showToast) window.showToast("Failed to update user: " + err.message, "error");
    }
}

window.setFilterPill = function(role, element) {
    document.querySelectorAll('.role-pill').forEach(pill => {
        pill.className = "role-pill px-4 py-2 rounded-xl text-xs font-bold border border-slate-800 bg-slate-900 text-slate-400 cursor-pointer hover:text-white transition-all";
    });

    const targetPill = element || document.querySelector(`.role-pill[data-role="${role}"]`);
    if (targetPill) {
        targetPill.className = "role-pill px-4 py-2 rounded-xl text-xs font-bold bg-brand-600 text-white border border-brand-500 cursor-pointer shadow-lg transition-all";
    }

    activeRoleFilter = role;
    filterAndRenderUsersTable();
};

window.openUsersWithFilter = function(role) {
    if (typeof window.switchTab === 'function') {
        window.switchTab('users');
    }
    window.setFilterPill(role);
    document.getElementById('tab-users')?.scrollIntoView({ behavior: 'smooth' });
};

window.toggleUserStatus = async function(uid, colName, currentIsActive, reasonInput = null) {
    let reason = reasonInput;
    if (currentIsActive && !reason) {
        reason = prompt("Enter reason for blocking this account:");
        if (reason === null) return;
    }

    try {
        await toggleUserStatusInDb(uid, colName, currentIsActive, reason);
        if (window.showToast) window.showToast(`Account status updated to ${!currentIsActive ? 'Active' : 'Blocked'}`, "success");

        // Refresh profile if open
        const openUid = document.getElementById("full-profile-uid")?.value;
        if (openUid === uid) {
            window.openUserProfile(uid, colName);
        }
    } catch (err) {
        console.error("Error toggling user status:", err);
        if (window.showToast) window.showToast("Failed to update user status", "error");
    }
};

window.sendPasswordReset = async function(email) {
    if (!email) {
        if (window.showToast) window.showToast("No email address registered for this account", "error");
        return;
    }
    if (confirm(`Send password reset link to ${email}?`)) {
        try {
            await sendUserPasswordResetEmail(email);
            if (window.showToast) window.showToast(`Password reset link sent to ${email}`, "success");
        } catch (err) {
            console.error("Error sending reset email:", err);
            if (window.showToast) window.showToast("Failed to send reset email: " + err.message, "error");
        }
    }
};

window.deleteUserAccount = async function(uid, colName) {
    if (confirm("Are you sure you want to PERMANENTLY delete this user account?")) {
        try {
            await deleteUserDocFromDb(uid, colName);
            if (window.showToast) window.showToast("User account permanently deleted", "success");
        } catch (err) {
            console.error("Error deleting user account:", err);
            if (window.showToast) window.showToast("Failed to delete user account", "error");
        }
    }
};

window.exportUsersCsv = function() {
    const headers = ["User UID", "Full Name", "Email", "Phone", "Role", "Department", "Account Status", "Firestore Collection"];
    const users = getUnifiedUsersList();
    const rows = users.map(u => [
        u.uid || '',
        u.name || '',
        u.email || '',
        u.phone || '',
        u.role || '',
        u.dept || '',
        u.isActive ? 'ACTIVE' : 'BLOCKED',
        u.collectionName || ''
    ]);

    const dateStr = new Date().toISOString().slice(0, 10);
    exportToCsv(`user_accounts_${dateStr}.csv`, headers, rows);
};

window.openAddUserModal = function() {
    const modal = document.getElementById("modal-add-user");
    if (modal) modal.classList.remove("hidden");
};

window.closeAddUserModal = function() {
    const modal = document.getElementById("modal-add-user");
    if (modal) modal.classList.add("hidden");
};

async function handleAddUserFormSubmit(e) {
    e.preventDefault();
    const name = document.getElementById("add-user-name")?.value.trim() || "";
    const phone = document.getElementById("add-user-phone")?.value.trim() || "";
    const email = document.getElementById("add-user-email")?.value.trim() || "";
    const pass = document.getElementById("add-user-pass")?.value || "";
    const role = document.getElementById("add-user-role")?.value || "CUSTOMER";
    const dept = document.getElementById("add-user-dept")?.value || "";

    try {
        await createNewUserAccountInDb({ name, phone, email, pass, role, dept });
        if (window.showToast) window.showToast("User account created successfully!", "success");
        window.closeAddUserModal();
        e.target.reset();
    } catch (err) {
        console.error("Error creating user account:", err);
        if (window.showToast) window.showToast("Failed to create account: " + err.message, "error");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    initUserUI();
});
