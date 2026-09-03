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

let couponsList = [];

export function initCouponsListener() {
    onSnapshot(collection(db, "coupons"), (snapshot) => {
        couponsList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));

        renderCouponsGrid(couponsList);
    }, (err) => {
        console.error("Error listening to coupons collection:", err);
    });
}

function renderCouponsGrid(coupons) {
    const container = document.getElementById("coupons-container");
    if (!container) return;

    if (coupons.length === 0) {
        container.innerHTML = `
            <div class="col-span-full p-8 text-center bg-slate-900 rounded-2xl border border-slate-800 text-slate-400">
                <i class="fa-solid fa-ticket text-2xl mb-2 text-slate-600 block"></i>
                No discount coupons found. Click "Add Coupon" to launch a promotional code.
            </div>
        `;
        return;
    }

    container.innerHTML = coupons.map(c => {
        const safeId = escapeHtml(c.id);
        const safeCode = escapeHtml(c.code || 'COUPON');
        const safeTitle = escapeHtml(c.title || 'Discount Promo');
        const safeDiscountVal = escapeHtml(c.discountValue || 0);
        const safeMinOrder = escapeHtml(c.minOrderValue || 0);
        const safeMaxDiscount = c.maxDiscount ? escapeHtml(c.maxDiscount) : 'No Limit';

        return `
            <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-lg hover:border-slate-700 transition-all">
                <div class="flex justify-between items-center border-b border-slate-800 pb-3">
                    <div class="flex items-center gap-2">
                        <span class="font-mono font-extrabold text-white text-base tracking-wider bg-slate-800 px-3 py-1 rounded-lg border border-slate-700">${safeCode}</span>
                    </div>
                    <span class="badge ${c.isActive !== false ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-400'}">
                        ${c.isActive !== false ? 'ACTIVE' : 'EXPIRED'}
                    </span>
                </div>

                <p class="text-xs text-slate-300 font-semibold">${safeTitle}</p>

                <div class="grid grid-cols-3 gap-2 bg-slate-950 p-2.5 rounded-xl border border-slate-800 text-center text-xs">
                    <div>
                        <span class="text-[10px] text-slate-400 block">DISCOUNT</span>
                        <strong class="text-emerald-400 font-bold">${c.discountType === 'FLAT' ? '₹' + safeDiscountVal : safeDiscountVal + '%'}</strong>
                    </div>
                    <div>
                        <span class="text-[10px] text-slate-400 block">MIN ORDER</span>
                        <strong class="text-white font-bold">₹${safeMinOrder}</strong>
                    </div>
                    <div>
                        <span class="text-[10px] text-slate-400 block">MAX DISC</span>
                        <strong class="text-amber-400 font-bold">${c.maxDiscount ? '₹' + safeMaxDiscount : safeMaxDiscount}</strong>
                    </div>
                </div>

                <div class="flex justify-between items-center pt-2 border-t border-slate-800 text-xs">
                    <button onclick="toggleCouponActive('${safeId}', ${c.isActive !== false})" class="text-slate-300 hover:text-white font-medium flex items-center gap-1 cursor-pointer">
                        <i class="fa-solid fa-power-off ${c.isActive !== false ? 'text-emerald-400' : 'text-slate-500'}"></i>
                        ${c.isActive !== false ? 'Deactivate' : 'Activate'}
                    </button>
                    <div class="flex items-center gap-2">
                        <button onclick="editCoupon('${safeId}')" class="text-amber-400 hover:text-amber-300 font-medium cursor-pointer">Edit</button>
                        <button onclick="deleteCoupon('${safeId}')" class="text-rose-400 hover:text-rose-300 font-medium cursor-pointer">Delete</button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

window.toggleCouponActive = async function(id, currentActive) {
    try {
        await updateDoc(doc(db, "coupons", id), {
            isActive: !currentActive,
            updatedAt: Date.now()
        });
        if (window.showToast) window.showToast(`Coupon status updated`, "success");
    } catch (err) {
        console.error("Error toggling coupon:", err);
        if (window.showToast) window.showToast("Failed to update coupon status", "error");
    }
};

window.openCouponModal = function() {
    const modal = document.getElementById("modal-coupon");
    if (modal) modal.classList.remove("hidden");
    document.getElementById("coupon-id").value = "";
    document.getElementById("form-coupon").reset();
};

window.closeCouponModal = function() {
    const modal = document.getElementById("modal-coupon");
    if (modal) modal.classList.add("hidden");
};

window.editCoupon = function(id) {
    const c = couponsList.find(item => item.id === id);
    if (!c) return;

    window.openCouponModal();
    document.getElementById("coupon-id").value = c.id;
    document.getElementById("coupon-code").value = c.code || '';
    document.getElementById("coupon-type").value = c.discountType || 'PERCENTAGE';
    document.getElementById("coupon-title").value = c.title || '';
    document.getElementById("coupon-value").value = c.discountValue || 0;
    document.getElementById("coupon-min-order").value = c.minOrderValue || 0;
    document.getElementById("coupon-max-discount").value = c.maxDiscount || '';
    document.getElementById("coupon-active").checked = c.isActive !== false;
};

window.deleteCoupon = async function(id) {
    if (confirm("Delete this promo coupon?")) {
        try {
            await deleteDoc(doc(db, "coupons", id));
            if (window.showToast) window.showToast("Coupon deleted", "success");
        } catch (err) {
            console.error("Error deleting coupon:", err);
            if (window.showToast) window.showToast("Failed to delete coupon", "error");
        }
    }
};

function initCouponsModule() {
    initCouponsListener();

    const form = document.getElementById("form-coupon");
    if (form) {
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const existingId = document.getElementById("coupon-id").value;
            const couponId = existingId || 'cpn_' + Date.now();

            const couponData = {
                id: couponId,
                code: document.getElementById("coupon-code").value.trim().toUpperCase(),
                discountType: document.getElementById("coupon-type").value,
                title: document.getElementById("coupon-title").value.trim(),
                discountValue: parseFloat(document.getElementById("coupon-value").value) || 0,
                minOrderValue: parseFloat(document.getElementById("coupon-min-order").value) || 0,
                maxDiscount: parseFloat(document.getElementById("coupon-max-discount").value) || 0,
                isActive: document.getElementById("coupon-active").checked,
                createdAt: Date.now()
            };

            try {
                await setDoc(doc(db, "coupons", couponId), couponData, { merge: true });
                if (window.showToast) window.showToast(`Coupon ${existingId ? 'updated' : 'created'}!`, "success");
                window.closeCouponModal();
            } catch (err) {
                console.error("Error saving coupon:", err);
                if (window.showToast) window.showToast("Failed to save coupon", "error");
            }
        });
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initCouponsModule);
} else {
    initCouponsModule();
}
