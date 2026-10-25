import { db } from './firebase-config.js';
import {
    collection,
    doc,
    onSnapshot,
    setDoc,
    deleteDoc,
    updateDoc,
    addDoc,
    getDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

let vacanciesList = [];
let jobApplicationsList = [];
let franchiseInquiriesList = [];
let allInquiriesList = [];
let currentInquiryFilter = 'ALL';
let editingVacancyId = null;

// SUBTAB SWITCHING
window.switchFlashoSubTab = function(subtabName) {
    const panels = ['vacancies', 'franchise', 'inquiries', 'config', 'content'];
    panels.forEach(p => {
        const panelEl = document.getElementById(`flasho-subtab-panel-${p}`);
        const btnEl = document.getElementById(`btn-flasho-subtab-${p}`);
        if (panelEl) {
            if (p === subtabName) panelEl.classList.remove('hidden');
            else panelEl.classList.add('hidden');
        }
        if (btnEl) {
            if (p === subtabName) {
                btnEl.className = "flasho-subtab-btn bg-amber-500 text-slate-950 font-black text-xs px-4 py-2.5 rounded-xl transition cursor-pointer flex items-center gap-2";
            } else {
                btnEl.className = "flasho-subtab-btn bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs px-4 py-2.5 rounded-xl transition cursor-pointer flex items-center gap-2";
            }
        }
    });
};

// REAL-TIME LISTENERS INITIALIZATION
export function initFlashoWebsiteModule() {
    // 1. Listen to Vacancies
    onSnapshot(collection(db, "vacancies"), (snapshot) => {
        vacanciesList = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderVacancies();
        updateCareersMetrics();
    }, (err) => console.warn("Vacancies listener warning:", err));

    // 2. Listen to Job Applications
    onSnapshot(collection(db, "job_applications"), (snapshot) => {
        jobApplicationsList = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderApplications();
        updateCareersMetrics();
    }, (err) => console.warn("Applications listener warning:", err));

    // 3. Listen to Franchise Inquiries
    onSnapshot(collection(db, "franchise_inquiries"), (snapshot) => {
        franchiseInquiriesList = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderFranchiseInquiries();
    }, (err) => console.warn("Franchise inquiries warning:", err));

    // 4. Listen to Inquiries Collections
    const inquiryCols = [
        { name: 'vendor_inquiries', type: 'VENDOR' },
        { name: 'supplier_inquiries', type: 'SUPPLIER' },
        { name: 'rider_inquiries', type: 'RIDER' },
        { name: 'b2b_inquiries', type: 'B2B' },
        { name: 'refund_requests', type: 'REFUND' },
        { name: 'ad_inquiries', type: 'ADVERTISING' }
    ];

    inquiryCols.forEach(col => {
        onSnapshot(collection(db, col.name), (snapshot) => {
            const items = snapshot.docs.map(d => ({ id: d.id, _colName: col.name, type: col.type, ...d.data() }));
            allInquiriesList = allInquiriesList.filter(i => i._colName !== col.name).concat(items);
            allInquiriesList.sort((a, b) => (b.submittedAt || 0) - (a.submittedAt || 0));
            renderFlashoInquiriesTable();
        }, (err) => console.warn(`Inquiries ${col.name} warning:`, err));
    });

    // 5. Load Global Store Config & Franchise Config
    loadGlobalStoreConfigs();
    loadFaqs();
    loadOtherApps();
}

// METRICS UPDATE
function updateCareersMetrics() {
    const totalJobsEl = document.getElementById('flasho-stat-total-jobs');
    const totalVacanciesEl = document.getElementById('flasho-stat-openings-count');
    const totalAppsEl = document.getElementById('flasho-stat-applications-count');
    const pendingAppsEl = document.getElementById('flasho-stat-pending-apps');
    const badgeVacanciesEl = document.getElementById('badge-vacancies-count');

    const totalOpenings = vacanciesList.reduce((acc, v) => acc + (Number(v.vacanciesCount) || 1), 0);
    const pendingApps = jobApplicationsList.filter(a => a.status === 'Pending' || a.status === 'PENDING' || !a.status).length;

    if (totalJobsEl) totalJobsEl.textContent = vacanciesList.length;
    if (totalVacanciesEl) totalVacanciesEl.textContent = totalOpenings;
    if (totalAppsEl) totalAppsEl.textContent = jobApplicationsList.length;
    if (pendingAppsEl) pendingAppsEl.textContent = pendingApps;
    if (badgeVacanciesEl) badgeVacanciesEl.textContent = vacanciesList.length;
}

// RENDER VACANCIES TABLE
function renderVacancies() {
    const tbody = document.getElementById('flasho-vacancies-tbody');
    if (!tbody) return;

    if (vacanciesList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="p-8 text-center text-slate-500 font-semibold space-y-2">
                    <i class="fa-solid fa-briefcase text-3xl text-slate-600 block"></i>
                    <p>No job vacancies posted yet.</p>
                    <button onclick="window.openFlashoAddJobModal()" class="bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs px-4 py-2 rounded-xl mt-2">Post First Vacancy</button>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = vacanciesList.map(vac => `
        <tr class="hover:bg-slate-800/40 transition">
            <td class="p-3">
                <div class="font-bold text-white text-sm">${vac.title}</div>
                <div class="text-[11px] text-amber-400 font-semibold">${vac.department || 'General'}</div>
            </td>
            <td class="p-3">
                <span class="bg-emerald-950 text-emerald-300 border border-emerald-800 font-bold px-2.5 py-1 rounded-full text-[10px]">
                    ${vac.vacanciesCount || 1} Openings
                </span>
            </td>
            <td class="p-3 text-slate-300 font-semibold">
                <i class="fa-solid fa-location-dot text-rose-400 mr-1"></i> ${vac.location || 'Multiple Cities'}
            </td>
            <td class="p-3 text-emerald-400 font-bold">
                ${vac.salaryRange || 'Competitive Pay'}
            </td>
            <td class="p-3">
                <button onclick="window.toggleFlashoVacancyStatus('${vac.id}', ${vac.active !== false})" class="px-2.5 py-1 rounded-full font-extrabold text-[10px] cursor-pointer ${vac.active !== false ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/10 text-rose-400 border border-rose-500/30'}">
                    ${vac.active !== false ? 'â— ACTIVE' : 'â—‹ CLOSED'}
                </button>
            </td>
            <td class="p-3 text-right">
                <div class="flex items-center justify-end gap-2">
                    <button onclick="window.openFlashoEditJobModal('${vac.id}')" class="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-amber-400 hover:text-white transition cursor-pointer" title="Edit">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="window.deleteFlashoVacancy('${vac.id}')" class="p-2 rounded-lg bg-slate-800 hover:bg-rose-900/40 text-slate-400 hover:text-rose-400 transition cursor-pointer" title="Delete">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// RENDER CANDIDATE APPLICATIONS
window.renderFlashoApplications = function() {
    const listContainer = document.getElementById('flasho-applications-list');
    const filterVal = document.getElementById('flasho-app-status-filter')?.value || 'All';
    if (!listContainer) return;

    let filtered = jobApplicationsList;
    if (filterVal !== 'All') {
        filtered = filtered.filter(a => (a.status || 'Pending').toLowerCase() === filterVal.toLowerCase());
    }

    if (filtered.length === 0) {
        listContainer.innerHTML = `
            <div class="col-span-2 text-center py-10 text-slate-500 font-semibold space-y-2">
                <i class="fa-solid fa-file-signature text-3xl text-slate-600 block"></i>
                <p>No candidate applications matching filter: <strong>${filterVal}</strong></p>
            </div>
        `;
        return;
    }

    listContainer.innerHTML = filtered.map(app => {
        const dateStr = app.appliedAt || app.submittedAt ? new Date(app.appliedAt || app.submittedAt).toLocaleDateString() : 'Recent';
        const name = app.applicantName || app.name || 'Candidate';
        const phone = app.applicantPhone || app.phone || 'N/A';
        const email = app.applicantEmail || app.email || 'N/A';
        const position = app.jobTitle || app.position || 'General Position';
        const resumeLink = app.resumeLink || app.portfolio || '';

        return `
            <div class="bg-slate-950 border border-slate-800 rounded-2xl p-5 space-y-3 relative shadow-md">
                <div class="flex justify-between items-start gap-2 border-b border-slate-800/80 pb-3">
                    <div>
                        <span class="text-[10px] font-extrabold text-amber-400 bg-amber-950 border border-amber-800 px-2 py-0.5 rounded uppercase">
                            ${app.department || 'GENERAL'}
                        </span>
                        <h5 class="text-base font-extrabold text-white mt-1">${name}</h5>
                        <p class="text-xs font-bold text-slate-400">${position}</p>
                    </div>

                    <div class="flex flex-col items-end gap-1">
                        <span class="text-[10px] font-extrabold px-2.5 py-1 rounded-full border bg-amber-500/10 text-amber-400 border-amber-500/30">
                            ${app.status || 'Pending'}
                        </span>
                        <span class="text-[10px] text-slate-500 font-medium">${dateStr}</span>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2 text-xs font-semibold text-slate-300">
                    <div><i class="fa-solid fa-phone text-slate-500 mr-1.5"></i> <a href="tel:${phone}" class="hover:text-amber-400 underline">${phone}</a></div>
                    <div><i class="fa-solid fa-envelope text-slate-500 mr-1.5"></i> <a href="mailto:${email}" class="hover:text-amber-400 underline truncate block">${email}</a></div>
                </div>

                ${app.coverNote ? `
                    <div class="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 text-[11px] text-slate-300 italic">
                        "${app.coverNote}"
                    </div>
                ` : ''}

                <div class="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/80">
                    ${resumeLink ? `
                        <a href="${resumeLink}" target="_blank" class="bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs px-3.5 py-1.5 rounded-xl transition flex items-center gap-1.5 shadow">
                            <i class="fa-solid fa-file-pdf"></i> View Resume / CV
                        </a>
                    ` : '<span class="text-xs text-slate-500 italic">No Resume Link</span>'}

                    <div class="flex items-center gap-2">
                        <select onchange="window.updateFlashoApplicationStatus('${app.id}', this.value)" class="bg-slate-900 border border-slate-700 rounded-xl px-2.5 py-1 text-xs font-bold text-white outline-none cursor-pointer">
                            <option value="Pending" ${app.status === 'Pending' ? 'selected' : ''}>Pending</option>
                            <option value="Reviewed" ${app.status === 'Reviewed' ? 'selected' : ''}>Reviewed</option>
                            <option value="Shortlisted" ${app.status === 'Shortlisted' ? 'selected' : ''}>Shortlisted</option>
                            <option value="Hired" ${app.status === 'Hired' ? 'selected' : ''}>Hired</option>
                            <option value="Rejected" ${app.status === 'Rejected' ? 'selected' : ''}>Rejected</option>
                        </select>
                    </div>
                </div>
            </div>
        `;
    }).join('');
};

// RENDER FRANCHISE INQUIRIES
function renderFranchiseInquiries() {
    const tbody = document.getElementById('flasho-franchise-inquiries-tbody');
    if (!tbody) return;

    if (franchiseInquiriesList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="p-8 text-center text-slate-500 font-semibold">
                    <i class="fa-solid fa-crown text-3xl text-slate-600 mb-2 block"></i>
                    No franchise applications submitted yet.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = franchiseInquiriesList.map(item => `
        <tr class="hover:bg-slate-800/40 transition">
            <td class="p-3 font-bold text-white text-sm">${item.name || 'Applicant'}</td>
            <td class="p-3 text-xs">
                <p class="font-bold text-slate-200">${item.phone || 'N/A'}</p>
                <p class="text-[11px] text-slate-400">${item.email || ''}</p>
            </td>
            <td class="p-3 font-semibold text-amber-400"><i class="fa-solid fa-location-dot mr-1"></i> ${item.city || 'N/A'}</td>
            <td class="p-3 font-bold text-emerald-400">${item.budget || 'N/A'}</td>
            <td class="p-3">
                <select onchange="window.updateFlashoFranchiseStatus('${item.id}', this.value)" class="bg-slate-950 border border-slate-700 text-xs font-bold text-white rounded-lg px-2 py-1 outline-none cursor-pointer">
                    <option value="PENDING" ${(item.status || 'PENDING') === 'PENDING' ? 'selected' : ''}>PENDING</option>
                    <option value="CONTACTED" ${item.status === 'CONTACTED' ? 'selected' : ''}>CONTACTED</option>
                    <option value="APPROVED" ${item.status === 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                    <option value="REJECTED" ${item.status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                </select>
            </td>
            <td class="p-3 text-right">
                <button onclick="window.deleteFlashoFranchiseInquiry('${item.id}')" class="text-rose-400 hover:text-rose-300 font-bold p-1">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

// RENDER ALL INQUIRIES TABLE
function renderFlashoInquiriesTable() {
    const tbody = document.getElementById('flasho-inquiries-table-body');
    const badgeTotal = document.getElementById('badge-inquiries-total-count');
    if (badgeTotal) badgeTotal.textContent = allInquiriesList.length;
    if (!tbody) return;

    const filtered = currentInquiryFilter === 'ALL'
        ? allInquiriesList
        : allInquiriesList.filter(i => i.type === currentInquiryFilter);

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="p-8 text-center text-slate-500 font-semibold">
                    <i class="fa-solid fa-folder-open text-3xl text-slate-600 mb-2 block"></i>
                    No ${currentInquiryFilter} submissions found.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtered.map(item => {
        const dateStr = item.submittedAt ? new Date(item.submittedAt).toLocaleDateString() : 'N/A';
        const nameDisplay = item.brand || item.company || item.name || item.owner || 'N/A';

        return `
            <tr class="hover:bg-slate-800/40 transition">
                <td class="p-3 text-slate-400 text-[11px]">${dateStr}</td>
                <td class="p-3"><span class="bg-slate-800 text-slate-300 font-bold text-[10px] px-2 py-0.5 rounded border border-slate-700">${item.type}</span></td>
                <td class="p-3 font-bold text-white">${nameDisplay}</td>
                <td class="p-3 text-xs"><p class="text-slate-200 font-bold">${item.phone || ''}</p><p class="text-slate-400 text-[11px]">${item.email || ''}</p></td>
                <td class="p-3 text-slate-300 text-xs font-medium max-w-xs truncate">${item.category || item.city || item.budget || item.reason || 'N/A'}</td>
                <td class="p-3">
                    <select onchange="window.updateInquiryStatus('${item._colName}', '${item.id}', this.value)" class="bg-slate-950 border border-slate-700 text-xs font-bold text-white rounded-lg px-2 py-1 outline-none cursor-pointer">
                        <option value="PENDING" ${(item.status || 'PENDING') === 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="CONTACTED" ${item.status === 'CONTACTED' ? 'selected' : ''}>CONTACTED</option>
                        <option value="APPROVED" ${item.status === 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                        <option value="REJECTED" ${item.status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                    </select>
                </td>
                <td class="p-3 text-right">
                    <button onclick="window.deleteInquiry('${item._colName}', '${item.id}')" class="text-rose-400 hover:text-rose-300 font-bold p-1">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

window.filterFlashoInquiry = function(filterType) {
    currentInquiryFilter = filterType;
    document.querySelectorAll('#flasho-inquiry-pills .flasho-inquiry-pill').forEach(btn => {
        btn.classList.remove('bg-brand-600', 'text-white');
        btn.classList.add('text-slate-400');
    });
    event.target.classList.remove('text-slate-400');
    event.target.classList.add('bg-brand-600', 'text-white');
    renderFlashoInquiriesTable();
};

// LOAD GLOBAL STORE & FRANCHISE CONFIGS
async function loadGlobalStoreConfigs() {
    try {
        const snap = await getDoc(doc(db, "settings", "store_config"));
        if (snap.exists()) {
            const data = snap.data();
            if (document.getElementById('flasho-cfg-helpline')) document.getElementById('flasho-cfg-helpline').value = data.helpline || '';
            if (document.getElementById('flasho-cfg-email')) document.getElementById('flasho-cfg-email').value = data.supportEmail || '';
            if (document.getElementById('flasho-cfg-min-free')) document.getElementById('flasho-cfg-min-free').value = data.minFreeDeliveryThreshold || 99;
            if (document.getElementById('flasho-cfg-delivery-fee')) document.getElementById('flasho-cfg-delivery-fee').value = data.standardDeliveryFee || 15;
            if (document.getElementById('flasho-cfg-promised-time')) document.getElementById('flasho-cfg-promised-time').value = data.promisedDeliveryMinutes || 10;
            if (document.getElementById('flasho-cfg-apk-link')) document.getElementById('flasho-cfg-apk-link').value = data.apkDownloadUrl || '';
            if (document.getElementById('flasho-cfg-hero-banner')) document.getElementById('flasho-cfg-hero-banner').value = data.heroBannerText || '';
        }

        const franSnap = await getDoc(doc(db, "settings", "franchise_config"));
        if (franSnap.exists()) {
            const fData = franSnap.data();
            if (document.getElementById('cfg-franchise-fee')) document.getElementById('cfg-franchise-fee').value = fData.fee || '';
            if (document.getElementById('cfg-min-investment')) document.getElementById('cfg-min-investment').value = fData.minCapital || '';
            if (document.getElementById('cfg-expected-roi')) document.getElementById('cfg-expected-roi').value = fData.expectedRoi || '';
            if (document.getElementById('cfg-franchise-sla')) document.getElementById('cfg-franchise-sla').value = fData.slaMins || 10;
            if (document.getElementById('cfg-franchise-phone')) document.getElementById('cfg-franchise-phone').value = fData.phone || '';
            if (document.getElementById('cfg-franchise-email')) document.getElementById('cfg-franchise-email').value = fData.email || '';
        }
    } catch (e) {
        console.warn("Config load error:", e);
    }
}

window.saveGlobalFlashoConfig = async function() {
    const helpline = document.getElementById('flasho-cfg-helpline')?.value.trim();
    const supportEmail = document.getElementById('flasho-cfg-email')?.value.trim();
    const minFreeDeliveryThreshold = Number(document.getElementById('flasho-cfg-min-free')?.value) || 99;
    const standardDeliveryFee = Number(document.getElementById('flasho-cfg-delivery-fee')?.value) || 15;
    const promisedDeliveryMinutes = Number(document.getElementById('flasho-cfg-promised-time')?.value) || 10;
    const apkDownloadUrl = document.getElementById('flasho-cfg-apk-link')?.value.trim();
    const heroBannerText = document.getElementById('flasho-cfg-hero-banner')?.value.trim();

    const payload = { helpline, supportEmail, minFreeDeliveryThreshold, standardDeliveryFee, promisedDeliveryMinutes, apkDownloadUrl, heroBannerText, updatedAt: Date.now() };

    try {
        await setDoc(doc(db, "settings", "store_config"), payload, { merge: true });
        await setDoc(doc(db, "settings", "website_config"), payload, { merge: true });
        if (window.showToast) window.showToast("Global Flasho Website configuration saved & synced live!", "success");
    } catch (e) {
        console.error("Save config error:", e);
    }
};

window.saveFlashoFranchiseConfig = async function() {
    const fee = document.getElementById('cfg-franchise-fee')?.value.trim();
    const minCapital = document.getElementById('cfg-min-investment')?.value.trim();
    const expectedRoi = document.getElementById('cfg-expected-roi')?.value.trim();
    const slaMins = Number(document.getElementById('cfg-franchise-sla')?.value) || 10;
    const phone = document.getElementById('cfg-franchise-phone')?.value.trim();
    const email = document.getElementById('cfg-franchise-email')?.value.trim();

    const payload = { fee, minCapital, expectedRoi, slaMins, phone, email, updatedAt: Date.now() };

    try {
        await setDoc(doc(db, "settings", "franchise_config"), payload, { merge: true });
        if (window.showToast) window.showToast("Franchise configuration saved live!", "success");
    } catch (e) {
        console.error("Save franchise config error:", e);
    }
};

// MODALS & CRUD FOR VACANCIES
window.openFlashoAddJobModal = function() {
    editingVacancyId = null;
    document.getElementById('modal-admin-job-title').textContent = "Post New Job Vacancy";
    document.getElementById('admin-job-id').value = "";
    document.getElementById('admin-job-title-input').value = "";
    document.getElementById('admin-job-loc-input').value = "";
    document.getElementById('admin-job-count-input').value = "1";
    document.getElementById('admin-job-salary-input').value = "";
    document.getElementById('admin-job-overview-input').value = "";
    document.getElementById('admin-job-active-input').checked = true;

    const modal = document.getElementById('modal-admin-job-form');
    if (modal) modal.classList.remove('hidden');
};

window.openFlashoEditJobModal = function(vacId) {
    const vac = vacanciesList.find(v => v.id === vacId);
    if (!vac) return;

    editingVacancyId = vacId;
    document.getElementById('modal-admin-job-title').textContent = "Edit Job Vacancy";
    document.getElementById('admin-job-id').value = vacId;
    document.getElementById('admin-job-title-input').value = vac.title || "";
    document.getElementById('admin-job-dept-input').value = vac.department || "Engineering & Tech";
    document.getElementById('admin-job-loc-input').value = vac.location || "";
    document.getElementById('admin-job-count-input').value = vac.vacanciesCount || 1;
    document.getElementById('admin-job-salary-input').value = vac.salaryRange || "";
    document.getElementById('admin-job-overview-input').value = vac.overview || "";
    document.getElementById('admin-job-active-input').checked = vac.active !== false;

    const modal = document.getElementById('modal-admin-job-form');
    if (modal) modal.classList.remove('hidden');
};

window.closeAdminJobModal = function() {
    const modal = document.getElementById('modal-admin-job-form');
    if (modal) modal.classList.add('hidden');
};

window.saveAdminJobVacancy = async function(event) {
    event.preventDefault();

    const title = document.getElementById('admin-job-title-input').value.trim();
    const department = document.getElementById('admin-job-dept-input').value;
    const location = document.getElementById('admin-job-loc-input').value.trim();
    const vacanciesCount = Number(document.getElementById('admin-job-count-input').value) || 1;
    const salaryRange = document.getElementById('admin-job-salary-input').value.trim();
    const overview = document.getElementById('admin-job-overview-input').value.trim();
    const active = document.getElementById('admin-job-active-input').checked;

    const payload = { title, department, location, vacanciesCount, salaryRange, overview, active, updatedAt: Date.now() };

    try {
        if (editingVacancyId) {
            await setDoc(doc(db, "vacancies", editingVacancyId), payload, { merge: true });
            if (window.showToast) window.showToast("Vacancy updated!", "success");
        } else {
            payload.createdAt = Date.now();
            await addDoc(collection(db, "vacancies"), payload);
            if (window.showToast) window.showToast("New job vacancy published to Customer Website!", "success");
        }
        window.closeAdminJobModal();
    } catch (e) {
        console.error("Save vacancy error:", e);
    }
};

window.toggleFlashoVacancyStatus = async function(vacId, currentActive) {
    try {
        await updateDoc(doc(db, "vacancies", vacId), { active: !currentActive });
        if (window.showToast) window.showToast(`Vacancy ${!currentActive ? 'Activated' : 'Closed'}!`, "success");
    } catch (e) {
        console.error("Toggle vacancy error:", e);
    }
};

window.deleteFlashoVacancy = async function(vacId) {
    if (!confirm("Are you sure you want to delete this vacancy?")) return;
    try {
        await deleteDoc(doc(db, "vacancies", vacId));
        if (window.showToast) window.showToast("Vacancy deleted!", "success");
    } catch (e) {
        console.error("Delete vacancy error:", e);
    }
};

window.updateFlashoApplicationStatus = async function(appId, newStatus) {
    try {
        await updateDoc(doc(db, "job_applications", appId), { status: newStatus });
        if (window.showToast) window.showToast(`Application status set to ${newStatus}`, "success");
    } catch (e) {
        console.error("Update application status error:", e);
    }
};

window.updateFlashoFranchiseStatus = async function(inqId, newStatus) {
    try {
        await updateDoc(doc(db, "franchise_inquiries", inqId), { status: newStatus });
        if (window.showToast) window.showToast(`Franchise inquiry status set to ${newStatus}`, "success");
    } catch (e) {
        console.error("Update franchise status error:", e);
    }
};

window.deleteFlashoFranchiseInquiry = async function(inqId) {
    if (!confirm("Are you sure you want to delete this franchise inquiry?")) return;
    try {
        await deleteDoc(doc(db, "franchise_inquiries", inqId));
        if (window.showToast) window.showToast("Inquiry deleted!", "success");
    } catch (e) {
        console.error("Delete inquiry error:", e);
    }
};

// FAQS & OTHER APPS
function loadFaqs() {
    onSnapshot(collection(db, "faqs"), (snapshot) => {
        const container = document.getElementById('flasho-faqs-list');
        if (!container) return;
        const faqs = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));

        if (faqs.length === 0) {
            container.innerHTML = `<p class="text-xs text-slate-500 font-semibold italic">No custom FAQs added yet.</p>`;
            return;
        }

        container.innerHTML = faqs.map(f => `
            <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 flex justify-between items-start gap-4">
                <div>
                    <h5 class="font-extrabold text-white text-xs">${f.question}</h5>
                    <p class="text-xs text-slate-300 font-medium mt-1">${f.answer}</p>
                </div>
                <button onclick="window.deleteFaq('${f.id}')" class="text-rose-400 hover:text-rose-300 p-1 font-bold">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        `).join('');
    });
}

function loadOtherApps() {
    onSnapshot(collection(db, "other_apps"), (snapshot) => {
        const container = document.getElementById('flasho-other-apps-grid');
        if (!container) return;
        const apps = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));

        if (apps.length === 0) {
            container.innerHTML = `<p class="col-span-3 text-xs text-slate-500 font-semibold italic">Default apps active on customer site.</p>`;
            return;
        }

        container.innerHTML = apps.map(app => `
            <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2 flex flex-col justify-between">
                <div>
                    <div class="flex items-center justify-between">
                        <span class="font-black text-white text-sm">${app.name}</span>
                        <span class="text-[10px] bg-amber-500/20 text-amber-400 px-2 py-0.5 rounded font-bold">${app.category || 'App'}</span>
                    </div>
                    <p class="text-xs text-slate-400 font-medium mt-1">${app.subtitle || ''}</p>
                </div>
                <div class="pt-2 border-t border-slate-800 flex items-center justify-between">
                    <a href="${app.downloadUrl || '#'}" target="_blank" class="text-xs text-brand-400 font-bold underline">Link</a>
                    <button onclick="window.deleteOtherApp('${app.id}')" class="text-rose-400 hover:text-rose-300 p-1 font-bold"><i class="fa-solid fa-trash"></i></button>
                </div>
            </div>
        `).join('');
    });
}

window.openAddFaqModal = async function() {
    const question = prompt("Enter FAQ Question:");
    if (!question) return;
    const answer = prompt("Enter FAQ Answer:");
    if (!answer) return;

    try {
        await addDoc(collection(db, "faqs"), { question, answer, createdAt: Date.now() });
        if (window.showToast) window.showToast("FAQ added!", "success");
    } catch (e) {
        console.error(e);
    }
};

window.deleteFaq = async function(id) {
    if (!confirm("Delete FAQ?")) return;
    try {
        await deleteDoc(doc(db, "faqs", id));
        if (window.showToast) window.showToast("FAQ deleted!", "success");
    } catch (e) {
        console.error(e);
    }
};

window.openAddOtherAppModal = async function() {
    const name = prompt("Enter App Name:");
    if (!name) return;
    const category = prompt("Enter Category (e.g., Media, Healthcare, Cabs):") || "General";
    const subtitle = prompt("Enter Short Description:");
    const downloadUrl = prompt("Enter APK / Download URL:");

    try {
        await addDoc(collection(db, "other_apps"), { name, category, subtitle, downloadUrl, createdAt: Date.now() });
        if (window.showToast) window.showToast("App added to Ecosystem!", "success");
    } catch (e) {
        console.error(e);
    }
};

window.deleteOtherApp = async function(id) {
    if (!confirm("Delete App?")) return;
    try {
        await deleteDoc(doc(db, "other_apps", id));
        if (window.showToast) window.showToast("App removed!", "success");
    } catch (e) {
        console.error(e);
    }
};

window.refreshFlashoWebsiteData = function() {
    initFlashoWebsiteModule();
    if (window.showToast) window.showToast("Flasho Website data refreshed!", "success");
};

// AUTO-INITIALIZE MODULE ON DOM/EVENT READY
document.addEventListener('DOMContentLoaded', () => {
    initFlashoWebsiteModule();
});
