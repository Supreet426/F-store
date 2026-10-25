import { db } from './firebase-config.js';
import {
    collection,
    doc,
    onSnapshot,
    setDoc,
    deleteDoc,
    updateDoc,
    addDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

let vacanciesList = [];
let jobApplicationsList = [];
let careerEnquiriesList = [];
let editingVacancyId = null;

// INITIALIZE REAL-TIME LISTENERS
export function initCareersModule() {
    // 1. Listen to Vacancies Collection
    onSnapshot(collection(db, "vacancies"), (snapshot) => {
        vacanciesList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
        renderAdminVacancies();
        updateCareersMetrics();
    }, (err) => {
        console.warn("Vacancies snapshot warning:", err);
    });

    // 2. Listen to Job Applications Collection
    onSnapshot(collection(db, "job_applications"), (snapshot) => {
        jobApplicationsList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
        renderAdminApplications();
        updateCareersMetrics();
    }, (err) => {
        console.warn("Applications snapshot warning:", err);
    });

    // 3. Listen to Career HR Enquiries Collection
    onSnapshot(collection(db, "career_enquiries"), (snapshot) => {
        careerEnquiriesList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
        renderAdminEnquiries();
        updateCareersMetrics();
    }, (err) => {
        console.warn("Enquiries snapshot warning:", err);
    });
}

// UPDATE TOP METRIC CARDS & SIDEBAR BADGE
function updateCareersMetrics() {
    const totalJobsEl = document.getElementById('stat-careers-total-jobs');
    const totalVacanciesEl = document.getElementById('stat-careers-total-vacancies');
    const totalAppsEl = document.getElementById('stat-careers-applications');
    const totalEnqEl = document.getElementById('stat-careers-enquiries');
    const badgeAppsEl = document.getElementById('badge-careers-applicants');
    const subtabAppCountEl = document.getElementById('admin-subtab-app-count');
    const subtabEnqCountEl = document.getElementById('admin-subtab-enq-count');

    const totalOpenings = vacanciesList.reduce((acc, v) => acc + (Number(v.vacanciesCount) || 1), 0);
    const pendingApps = jobApplicationsList.filter(a => a.status === 'Pending' || !a.status).length;
    const newEnqs = careerEnquiriesList.filter(e => e.status === 'New' || !e.status).length;

    if (totalJobsEl) totalJobsEl.textContent = vacanciesList.length;
    if (totalVacanciesEl) totalVacanciesEl.textContent = totalOpenings;
    if (totalAppsEl) totalAppsEl.textContent = jobApplicationsList.length;
    if (totalEnqEl) totalEnqEl.textContent = careerEnquiriesList.length;

    if (badgeAppsEl) badgeAppsEl.textContent = pendingApps;
    if (subtabAppCountEl) subtabAppCountEl.textContent = jobApplicationsList.length;
    if (subtabEnqCountEl) subtabEnqCountEl.textContent = careerEnquiriesList.length;
}

// SUBTAB SWITCHING
window.switchCareersSubTab = (tabName) => {
    const panels = ['vacancies', 'applications', 'enquiries'];
    panels.forEach(p => {
        const panelEl = document.getElementById(`careers-subtab-panel-${p}`);
        const btnEl = document.getElementById(`btn-careers-subtab-${p}`);
        if (panelEl) {
            if (p === tabName) {
                panelEl.classList.remove('hidden');
            } else {
                panelEl.classList.add('hidden');
            }
        }
        if (btnEl) {
            if (p === tabName) {
                btnEl.className = "careers-subtab-btn active bg-brand-600 text-white font-bold px-4 py-2.5 rounded-xl transition cursor-pointer";
            } else {
                btnEl.className = "careers-subtab-btn bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold px-4 py-2.5 rounded-xl transition cursor-pointer";
            }
        }
    });
};

// RENDER ADMIN VACANCIES TABLE
function renderAdminVacancies() {
    const tbody = document.getElementById('admin-vacancies-tbody');
    if (!tbody) return;

    if (vacanciesList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="p-8 text-center text-slate-500 font-semibold space-y-2">
                    <i class="fa-solid fa-briefcase text-3xl text-slate-600"></i>
                    <p>No job vacancies created yet in Firestore database.</p>
                    <button onclick="window.openAddJobVacancyModal()" class="btn-primary text-xs px-4 py-2 mt-2">Create First Vacancy</button>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = vacanciesList.map(vac => `
        <tr class="hover:bg-slate-800/40 transition">
            <td class="p-4">
                <div class="font-bold text-white text-sm">${vac.title}</div>
                <div class="text-[11px] text-brand-400 font-semibold">${vac.department || 'General'} â€¢ ${vac.type || 'Full Time'}</div>
            </td>
            <td class="p-4">
                <span class="bg-emerald-950 text-emerald-300 border border-emerald-800 font-bold px-2.5 py-1 rounded-full text-[11px]">
                    ${vac.vacanciesCount || 1} Openings
                </span>
            </td>
            <td class="p-4 text-slate-300 font-semibold">
                <i class="fa-solid fa-location-dot text-rose-400 mr-1"></i> ${vac.location || 'Multiple Cities'}
            </td>
            <td class="p-4 text-emerald-400 font-bold">
                ${vac.salaryRange || 'Competitive Pay'}
            </td>
            <td class="p-4 max-w-xs text-slate-400 truncate text-[11px]" title="${(vac.termsAndConditions || '').replace(/"/g, '&quot;')}">
                ${vac.termsAndConditions || 'Standard hiring policies apply.'}
            </td>
            <td class="p-4">
                <button onclick="window.toggleVacancyStatus('${vac.id}', ${vac.active !== false})" class="px-2.5 py-1 rounded-full font-extrabold text-[10px] cursor-pointer ${vac.active !== false ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/10 text-rose-400 border border-rose-500/30'}">
                    ${vac.active !== false ? 'â— ACTIVE' : 'â—‹ CLOSED'}
                </button>
            </td>
            <td class="p-4 text-right">
                <div class="flex items-center justify-end gap-2">
                    <button onclick="window.openEditJobVacancyModal('${vac.id}')" class="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-brand-400 hover:text-white transition cursor-pointer" title="Edit Vacancy">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="window.deleteVacancy('${vac.id}')" class="p-2 rounded-lg bg-slate-800 hover:bg-rose-900/40 text-slate-400 hover:text-rose-400 transition cursor-pointer" title="Delete Vacancy">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// RENDER CANDIDATE APPLICATIONS LIST
window.renderAdminApplications = () => {
    const listContainer = document.getElementById('admin-applications-list');
    const filterVal = document.getElementById('admin-app-status-filter')?.value || 'All';
    if (!listContainer) return;

    let filtered = jobApplicationsList;
    if (filterVal !== 'All') {
        filtered = filtered.filter(a => a.status === filterVal);
    }

    if (filtered.length === 0) {
        listContainer.innerHTML = `
            <div class="col-span-2 text-center py-12 text-slate-500 font-semibold space-y-2">
                <i class="fa-solid fa-file-signature text-3xl text-slate-600"></i>
                <p>No candidate applications matching filter: <strong>${filterVal}</strong></p>
            </div>
        `;
        return;
    }

    listContainer.innerHTML = filtered.map(app => {
        const dateStr = app.appliedAt ? new Date(app.appliedAt).toLocaleString() : 'Recent';
        const statusColors = {
            'Pending': 'bg-amber-500/10 text-amber-400 border-amber-500/30',
            'Reviewed': 'bg-blue-500/10 text-blue-400 border-blue-500/30',
            'Shortlisted': 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
            'Interview Scheduled': 'bg-purple-500/10 text-purple-400 border-purple-500/30',
            'Hired': 'bg-green-500/20 text-green-300 border-green-500/50',
            'Rejected': 'bg-rose-500/10 text-rose-400 border-rose-500/30'
        };
        const statusClass = statusColors[app.status] || 'bg-slate-800 text-slate-300 border-slate-700';

        return `
            <div class="bg-slate-950 border border-slate-800 rounded-2xl p-5 space-y-3 relative shadow-md">
                <div class="flex justify-between items-start gap-2 border-b border-slate-800/80 pb-3">
                    <div>
                        <span class="text-[10px] font-extrabold text-brand-400 bg-brand-950 border border-brand-800 px-2 py-0.5 rounded">
                            ${app.department || 'GENERAL'}
                        </span>
                        <h5 class="text-base font-extrabold text-white mt-1">${app.applicantName || 'Applicant'}</h5>
                        <p class="text-xs font-bold text-slate-400">${app.jobTitle || 'General Position'}</p>
                    </div>

                    <div class="flex flex-col items-end gap-1">
                        <span class="text-[10px] font-extrabold px-2.5 py-1 rounded-full border ${statusClass}">
                            ${app.status || 'Pending'}
                        </span>
                        <span class="text-[10px] text-slate-500 font-medium">${dateStr}</span>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2 text-xs font-semibold text-slate-300">
                    <div><i class="fa-solid fa-phone text-slate-500 mr-1.5"></i> <a href="tel:${app.applicantPhone}" class="hover:text-brand-400 underline">${app.applicantPhone || 'N/A'}</a></div>
                    <div><i class="fa-solid fa-envelope text-slate-500 mr-1.5"></i> <a href="mailto:${app.applicantEmail}" class="hover:text-brand-400 underline truncate block">${app.applicantEmail || 'N/A'}</a></div>
                    <div><i class="fa-solid fa-location-dot text-slate-500 mr-1.5"></i> ${app.city || 'N/A'}</div>
                    <div><i class="fa-solid fa-briefcase text-slate-500 mr-1.5"></i> ${app.experience || 'N/A'}</div>
                </div>

                ${app.coverNote ? `
                    <div class="bg-slate-900/80 p-3 rounded-xl border border-slate-800/80 text-[11px] text-slate-300 space-y-1">
                        <span class="font-extrabold text-slate-400 uppercase text-[10px] block">Candidate Pitch / Cover Note:</span>
                        <p class="italic text-slate-300 font-medium leading-relaxed">${app.coverNote}</p>
                    </div>
                ` : ''}

                <div class="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/80">
                    ${app.resumeLink ? `
                        <a href="${app.resumeLink}" target="_blank" class="bg-brand-600 hover:bg-brand-500 text-white font-bold text-xs px-4 py-2 rounded-xl transition flex items-center gap-1.5 shadow-md">
                            <i class="fa-solid fa-file-pdf"></i> View Candidate Resume / CV
                        </a>
                    ` : '<span class="text-xs text-slate-500 italic">No Resume Link Provided</span>'}

                    <div class="flex items-center gap-2">
                        <label class="text-[11px] font-bold text-slate-400">Update Status:</label>
                        <select onchange="window.updateApplicationStatus('${app.id}', this.value)" class="bg-slate-900 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs font-bold text-white outline-none focus:border-brand-500 cursor-pointer">
                            <option value="Pending" ${app.status === 'Pending' ? 'selected' : ''}>Pending</option>
                            <option value="Reviewed" ${app.status === 'Reviewed' ? 'selected' : ''}>Reviewed</option>
                            <option value="Shortlisted" ${app.status === 'Shortlisted' ? 'selected' : ''}>Shortlisted</option>
                            <option value="Interview Scheduled" ${app.status === 'Interview Scheduled' ? 'selected' : ''}>Interview Scheduled</option>
                            <option value="Hired" ${app.status === 'Hired' ? 'selected' : ''}>Hired</option>
                            <option value="Rejected" ${app.status === 'Rejected' ? 'selected' : ''}>Rejected</option>
                        </select>
                    </div>
                </div>
            </div>
        `;
    }).join('');
};

// RENDER CANDIDATE ENQUIRIES
function renderAdminEnquiries() {
    const listContainer = document.getElementById('admin-enquiries-list');
    if (!listContainer) return;

    if (careerEnquiriesList.length === 0) {
        listContainer.innerHTML = `
            <div class="text-center py-8 text-slate-500 font-semibold space-y-1">
                <i class="fa-solid fa-headset text-3xl text-slate-600"></i>
                <p>No HR career enquiries submitted yet.</p>
            </div>
        `;
        return;
    }

    listContainer.innerHTML = careerEnquiriesList.map(enq => `
        <div class="bg-slate-950 border border-slate-800 rounded-2xl p-4 space-y-2 flex flex-col md:flex-row justify-between md:items-center gap-4">
            <div class="space-y-1">
                <div class="flex items-center gap-2">
                    <span class="font-extrabold text-white text-sm">${enq.name || 'Candidate'}</span>
                    <span class="text-[10px] font-bold px-2 py-0.5 rounded ${enq.status === 'Resolved' ? 'bg-emerald-950 text-emerald-400 border border-emerald-800' : 'bg-amber-950 text-amber-400 border border-amber-800'}">
                        ${enq.status || 'New'}
                    </span>
                </div>
                <div class="text-xs text-brand-400 font-bold">${enq.topic || 'General Inquiry'}</div>
                <p class="text-xs text-slate-300 font-medium">${enq.message || ''}</p>
                <div class="text-[11px] text-slate-500 font-semibold pt-1">
                    Contact: <a href="tel:${enq.phone}" class="text-slate-300 underline mr-3">${enq.phone}</a>
                    Email: <a href="mailto:${enq.email}" class="text-slate-300 underline">${enq.email}</a>
                </div>
            </div>

            <button onclick="window.toggleEnquiryStatus('${enq.id}', '${enq.status || 'New'}')" class="btn-primary text-xs px-4 py-2 shrink-0 cursor-pointer">
                ${enq.status === 'Resolved' ? 'Mark as New' : 'Mark as Resolved'}
            </button>
        </div>
    `).join('');
}

// MODAL HANDLERS FOR VACANCIES
window.openAddJobVacancyModal = () => {
    editingVacancyId = null;
    document.getElementById('modal-admin-job-title').textContent = "Add New Job Vacancy";
    document.getElementById('admin-job-id').value = "";
    document.getElementById('admin-job-title-input').value = "";
    document.getElementById('admin-job-dept-input').value = "Operations & Dark Store";
    document.getElementById('admin-job-loc-input').value = "";
    document.getElementById('admin-job-count-input').value = "1";
    document.getElementById('admin-job-salary-input').value = "";
    document.getElementById('admin-job-exp-input').value = "";
    document.getElementById('admin-job-overview-input').value = "";
    document.getElementById('admin-job-resp-input').value = "";
    document.getElementById('admin-job-tc-input').value = "Standard background verification required. 6-month probation period and shift flexibility required.";
    document.getElementById('admin-job-active-input').checked = true;

    const modal = document.getElementById('modal-admin-job-form');
    if (modal) modal.classList.remove('hidden');
};

window.openEditJobVacancyModal = (vacId) => {
    const vac = vacanciesList.find(v => v.id === vacId);
    if (!vac) return;

    editingVacancyId = vacId;
    document.getElementById('modal-admin-job-title').textContent = "Edit Job Vacancy";
    document.getElementById('admin-job-id').value = vacId;
    document.getElementById('admin-job-title-input').value = vac.title || "";
    document.getElementById('admin-job-dept-input').value = vac.department || "Operations & Dark Store";
    document.getElementById('admin-job-loc-input').value = vac.location || "";
    document.getElementById('admin-job-count-input').value = vac.vacanciesCount || 1;
    document.getElementById('admin-job-salary-input').value = vac.salaryRange || "";
    document.getElementById('admin-job-exp-input').value = vac.experienceRequired || "";
    document.getElementById('admin-job-overview-input').value = vac.overview || "";
    document.getElementById('admin-job-resp-input').value = Array.isArray(vac.responsibilities) ? vac.responsibilities.join('\n') : (vac.responsibilities || "");
    document.getElementById('admin-job-tc-input').value = vac.termsAndConditions || "";
    document.getElementById('admin-job-active-input').checked = vac.active !== false;

    const modal = document.getElementById('modal-admin-job-form');
    if (modal) modal.classList.remove('hidden');
};

window.closeAdminJobModal = () => {
    const modal = document.getElementById('modal-admin-job-form');
    if (modal) modal.classList.add('hidden');
};

window.saveAdminJobVacancy = async (event) => {
    event.preventDefault();

    const title = document.getElementById('admin-job-title-input').value.trim();
    const department = document.getElementById('admin-job-dept-input').value;
    const location = document.getElementById('admin-job-loc-input').value.trim();
    const vacanciesCount = Number(document.getElementById('admin-job-count-input').value) || 1;
    const salaryRange = document.getElementById('admin-job-salary-input').value.trim();
    const experienceRequired = document.getElementById('admin-job-exp-input').value.trim();
    const overview = document.getElementById('admin-job-overview-input').value.trim();
    const respRaw = document.getElementById('admin-job-resp-input').value.trim();
    const termsAndConditions = document.getElementById('admin-job-tc-input').value.trim();
    const active = document.getElementById('admin-job-active-input').checked;

    const responsibilities = respRaw.split('\n').map(s => s.trim()).filter(Boolean);

    const payload = {
        title,
        department,
        location,
        vacanciesCount,
        salaryRange,
        experienceRequired,
        overview,
        responsibilities,
        termsAndConditions,
        active,
        updatedAt: Date.now()
    };

    try {
        if (editingVacancyId) {
            await setDoc(doc(db, "vacancies", editingVacancyId), payload, { merge: true });
            if (window.showToast) window.showToast("Job vacancy updated successfully!", "success");
        } else {
            payload.createdAt = Date.now();
            await addDoc(collection(db, "vacancies"), payload);
            if (window.showToast) window.showToast("New job vacancy published to Firestore!", "success");
        }
        window.closeAdminJobModal();
    } catch (e) {
        console.error("Save vacancy error:", e);
        if (window.showToast) window.showToast("Failed to save vacancy: " + e.message, "error");
    }
};

window.toggleVacancyStatus = async (vacId, currentActive) => {
    try {
        await updateDoc(doc(db, "vacancies", vacId), { active: !currentActive });
        if (window.showToast) window.showToast(`Vacancy ${!currentActive ? 'Activated' : 'Closed'}!`, "success");
    } catch (e) {
        console.error("Toggle error:", e);
    }
};

window.deleteVacancy = async (vacId) => {
    if (!confirm("Are you sure you want to delete this job vacancy posting?")) return;
    try {
        await deleteDoc(doc(db, "vacancies", vacId));
        if (window.showToast) window.showToast("Job vacancy deleted!", "success");
    } catch (e) {
        console.error("Delete error:", e);
    }
};

window.updateApplicationStatus = async (appId, newStatus) => {
    try {
        await updateDoc(doc(db, "job_applications", appId), { status: newStatus });
        if (window.showToast) window.showToast(`Application status updated to: ${newStatus}`, "success");
    } catch (e) {
        console.error("Update application status error:", e);
    }
};

window.toggleEnquiryStatus = async (enqId, currentStatus) => {
    const newStatus = currentStatus === 'Resolved' ? 'New' : 'Resolved';
    try {
        await updateDoc(doc(db, "career_enquiries", enqId), { status: newStatus });
        if (window.showToast) window.showToast(`HR Enquiry marked as ${newStatus}`, "success");
    } catch (e) {
        console.error("Toggle enquiry status error:", e);
    }
};

// AUTO INITIALIZE MODULE ON LOAD
document.addEventListener('DOMContentLoaded', () => {
    initCareersModule();
});
