import {
    onUsersUpdated,
    getUnifiedUsersList,
    toggleUserStatusInDb,
    sendUserPasswordResetEmail,
    deleteUserDocFromDb,
    createNewUserAccountInDb,
    updateUserAccountInDb
} from './user-service.js';
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

    // Modal edit form submit (kept for safety but not used in primary flow)
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

    // Reset Save Bar
    const saveBar = document.getElementById("fp-save-bar");
    if (saveBar) saveBar.classList.add("hidden");
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
        window.openUserProfile(uid, colName); // Refresh view
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
