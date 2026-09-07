import { db } from './firebase-config.js';
import {
    collection,
    onSnapshot,
    doc,
    updateDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { escapeHtml, exportToCsv } from './utils.js';

let ordersList = [];
let availableRidersList = [];
let activeStatusFilter = "ALL";
window.activeStatusFilter = activeStatusFilter;

window.handleOrderStatusFilter = function(status) {
    activeStatusFilter = status;
    window.activeStatusFilter = status;
    filterAndRenderOrders();
};

window.handleOrderSearch = function(query) {
    filterAndRenderOrders();
};

window.openOrdersWithFilter = function(status = 'ALL') {
    activeStatusFilter = status;
    window.activeStatusFilter = status;
    if (typeof window.switchTab === 'function') {
        window.switchTab('orders');
    }
    const selectEl = document.getElementById("filter-order-status");
    if (selectEl) {
        selectEl.value = status;
    }
    filterAndRenderOrders();
};

export function initOrdersListener() {
    onSnapshot(collection(db, "orders"), (snapshot) => {
        ordersList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));

        updateOrdersKPIs(ordersList);
        filterAndRenderOrders();
    }, (err) => {
        console.error("Error listening to orders collection:", err);
    });

    onSnapshot(collection(db, "riders"), (snapshot) => {
        availableRidersList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        }));
    }, (err) => {
        console.warn("Could not fetch riders for reassignment:", err);
    });
}

function updateOrdersKPIs(orders) {
    const today = new Date().setHours(0, 0, 0, 0);

    const activeOrders = orders.filter(o => {
        const st = (o.orderStatus || o.status || 'PLACED').toUpperCase();
        return st !== 'DELIVERED' && st !== 'CANCELLED';
    });

    const todayRevenue = orders
        .filter(o => {
            const st = (o.orderStatus || o.status || 'PLACED').toUpperCase();
            return (o.createdAt || 0) >= today && st === 'DELIVERED';
        })
        .reduce((sum, o) => sum + (o.totalAmount || o.grandTotal || 0), 0);

    const kpiRevenue = document.getElementById("kpi-revenue");
    const kpiOrders = document.getElementById("kpi-orders");
    const badgeLiveOrders = document.getElementById("badge-live-orders");

    if (kpiRevenue) kpiRevenue.textContent = `₹${todayRevenue.toLocaleString('en-IN')}`;
    if (kpiOrders) kpiOrders.textContent = activeOrders.length;
    if (badgeLiveOrders) badgeLiveOrders.textContent = activeOrders.length;

    const aovEl = document.getElementById("analytics-aov");
    if (aovEl && orders.length > 0) {
        const totalRev = orders.reduce((s, o) => s + (o.totalAmount || o.grandTotal || 0), 0);
        aovEl.textContent = `₹${Math.round(totalRev / orders.length)}`;
    }
}

export function filterAndRenderOrders() {
    const searchVal = document.getElementById("search-order-input")?.value.toLowerCase().trim() || "";
    const currentFilter = activeStatusFilter || window.activeStatusFilter || "ALL";

    const filtered = ordersList.filter(o => {
        const status = (o.orderStatus || o.status || "PLACED").toUpperCase();

        const matchesStatus = currentFilter === "ALL" ||
            (currentFilter === "ACTIVE" && status !== "DELIVERED" && status !== "CANCELLED") ||
            status === currentFilter;

        const matchesSearch = !searchVal ||
            (o.id || "").toLowerCase().includes(searchVal) ||
            (o.customerName || "").toLowerCase().includes(searchVal) ||
            (o.customerPhone || "").toLowerCase().includes(searchVal) ||
            (o.assignedRiderName || o.riderName || "").toLowerCase().includes(searchVal) ||
            (o.storeName || o.merchantName || "").toLowerCase().includes(searchVal) ||
            (o.deliveryAddress || "").toLowerCase().includes(searchVal);

        return matchesStatus && matchesSearch;
    });

    renderOrdersGrid(filtered);
}
window.filterAndRenderOrders = filterAndRenderOrders;

function renderOrdersGrid(orders) {
    const container = document.getElementById("orders-list-container");
    if (!container) return;

    if (orders.length === 0) {
        container.innerHTML = `
            <div class="col-span-full p-12 text-center bg-slate-900 rounded-2xl border border-slate-800">
                <i class="fa-solid fa-receipt text-3xl text-slate-600 mb-2 block"></i>
                <p class="text-slate-400 font-semibold text-sm">No live orders found matching filters.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = orders.map(o => {
        const safeId = escapeHtml(o.id);
        const safeCustomerName = escapeHtml(o.customerName || 'Customer');
        const safeCustomerPhone = escapeHtml(o.customerPhone || 'N/A');
        const safeMerchantName = escapeHtml(o.storeName || o.merchantName || 'Flash-O Store');
        const safeMerchantPhone = escapeHtml(o.storePhone || o.merchantPhone || o.sellerPhone || 'N/A');
        const safeRiderName = escapeHtml(o.assignedRiderName || o.riderName || 'Not Assigned');
        const safeRiderPhone = escapeHtml(o.assignedRiderPhone || o.riderPhone || 'N/A');
        const safeDeliveryAddress = escapeHtml(o.deliveryAddress || 'Standard Delivery Zone');
        const safeLat = escapeHtml(o.deliveryLatitude || o.latitude || 28.6139);
        const safeLng = escapeHtml(o.deliveryLongitude || o.longitude || 77.2090);
        const status = (o.orderStatus || o.status || 'PLACED').toUpperCase();
        const isCancelled = status === 'CANCELLED';

        const itemsHtml = (o.items || []).map(i => {
            const safeQty = escapeHtml(i.quantity || 1);
            const safeItemName = escapeHtml(i.name || i.title || 'Item');
            const safePrice = escapeHtml(i.price || 0);
            return `<div class="flex justify-between text-slate-300"><span>${safeQty}x ${safeItemName}</span><span class="font-mono">₹${safePrice}</span></div>`;
        }).join('');

        return `
            <div class="bg-slate-900 border border-slate-800/80 rounded-2xl p-5 space-y-4 shadow-lg hover:border-slate-700 transition-all">
                <div class="flex justify-between items-start border-b border-slate-800 pb-3">
                    <div>
                        <span class="text-[10px] font-mono text-slate-400 block">ORDER ID</span>
                        <h4 class="font-mono font-bold text-white text-sm flex items-center gap-2">
                            #${safeId.slice(0, 8)}
                            <button onclick="openPrintInvoiceModal('${safeId}')" class="px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[10px] font-bold hover:bg-amber-500/20 cursor-pointer">
                                <i class="fa-solid fa-file-invoice mr-1"></i> Invoice
                            </button>
                        </h4>
                    </div>
                    ${getOrderStatusBadge(status)}
                </div>

                <!-- CONTACT BAR: CALL CUSTOMER / MERCHANT / RIDER -->
                <div class="flex flex-wrap items-center gap-1.5 pt-1">
                    <a href="${safeCustomerPhone !== 'N/A' ? 'tel:' + safeCustomerPhone : 'javascript:void(0)'}" class="px-2 py-1 bg-slate-800 hover:bg-slate-700 rounded-lg text-[11px] text-emerald-400 font-bold flex items-center gap-1 border border-slate-700/80 cursor-pointer" title="Call Customer">
                        <i class="fa-solid fa-phone"></i> Cust: ${safeCustomerPhone}
                    </a>
                    <a href="${safeMerchantPhone !== 'N/A' ? 'tel:' + safeMerchantPhone : 'javascript:void(0)'}" class="px-2 py-1 bg-slate-800 hover:bg-slate-700 rounded-lg text-[11px] text-amber-400 font-bold flex items-center gap-1 border border-slate-700/80 cursor-pointer" title="Call Merchant Store">
                        <i class="fa-solid fa-store"></i> ${safeMerchantName.slice(0, 10)}
                    </a>
                    <a href="${safeRiderPhone !== 'N/A' ? 'tel:' + safeRiderPhone : 'javascript:void(0)'}" class="px-2 py-1 bg-slate-800 hover:bg-slate-700 rounded-lg text-[11px] text-sky-400 font-bold flex items-center gap-1 border border-slate-700/80 cursor-pointer" title="Call Delivery Rider">
                        <i class="fa-solid fa-motorcycle"></i> ${safeRiderName.slice(0, 10)}
                    </a>
                </div>

                <div class="space-y-1.5 text-xs text-slate-300">
                    <p><i class="fa-solid fa-user text-slate-400 w-4"></i> <strong class="text-white">${safeCustomerName}</strong></p>
                    <p><i class="fa-solid fa-location-dot text-slate-400 w-4"></i> <span class="truncate block">${safeDeliveryAddress}</span></p>
                    ${isCancelled && o.cancellationReason ? `<p class="p-2 bg-rose-950/40 rounded-lg text-rose-300 border border-rose-900/50 text-[11px]"><i class="fa-solid fa-triangle-exclamation mr-1"></i> <strong>Cancelled:</strong> ${escapeHtml(o.cancellationReason)}</p>` : ''}
                </div>

                <div class="bg-slate-950 p-3 rounded-xl border border-slate-800 space-y-1">
                    <p class="text-[10px] font-bold text-slate-400 uppercase">Items (${(o.items || []).length})</p>
                    <div class="max-h-24 overflow-y-auto text-xs space-y-1">
                        ${itemsHtml}
                    </div>
                </div>

                <!-- DYNAMIC CONTROL BAR: REASSIGN RIDER / TRACK MAP / CANCEL -->
                <div class="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-800">
                    <div class="flex items-center gap-1.5">
                        <button onclick="openReassignRiderModal('${safeId}')" class="px-2.5 py-1.5 bg-sky-950/80 hover:bg-sky-900 border border-sky-800 rounded-lg text-xs font-bold text-sky-300 flex items-center gap-1 cursor-pointer">
                            <i class="fa-solid fa-user-plus"></i> ${safeRiderName !== 'Not Assigned' ? 'Reassign Rider' : 'Assign Rider'}
                        </button>
                        <button onclick="openOrderMapTracking('${safeId}', ${safeLat}, ${safeLng})" class="px-2.5 py-1.5 bg-indigo-950/80 hover:bg-indigo-900 border border-indigo-800 rounded-lg text-xs font-bold text-indigo-300 flex items-center gap-1 cursor-pointer">
                            <i class="fa-solid fa-map-location-dot"></i> Track
                        </button>
                    </div>

                    <div class="flex items-center gap-2">
                        <strong class="text-emerald-400 font-bold text-base">₹${escapeHtml(o.totalAmount || o.grandTotal || 0)}</strong>
                        <select onchange="handleOrderStatusChange('${safeId}', this.value)" class="bg-slate-800 border border-slate-700 text-xs font-bold text-white rounded-lg px-2 py-1.5 focus:outline-none focus:border-brand-500">
                            <option value="PLACED" ${status === 'PLACED' ? 'selected' : ''}>PLACED</option>
                            <option value="ACCEPTED" ${status === 'ACCEPTED' ? 'selected' : ''}>ACCEPTED</option>
                            <option value="PREPARING" ${status === 'PREPARING' ? 'selected' : ''}>PREPARING</option>
                            <option value="READY" ${status === 'READY' ? 'selected' : ''}>READY</option>
                            <option value="DISPATCHED" ${status === 'DISPATCHED' ? 'selected' : ''}>DISPATCHED</option>
                            <option value="DELIVERED" ${status === 'DELIVERED' ? 'selected' : ''}>DELIVERED</option>
                            <option value="CANCELLED" ${status === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                        </select>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function getOrderStatusBadge(status) {
    const safeStatus = escapeHtml(status || 'PLACED').toUpperCase();
    switch (safeStatus) {
        case 'DELIVERED':
            return `<span class="badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-bold">DELIVERED</span>`;
        case 'CANCELLED':
            return `<span class="badge bg-rose-500/20 text-rose-400 border border-rose-500/30 font-bold">CANCELLED</span>`;
        case 'DISPATCHED':
            return `<span class="badge bg-sky-500/20 text-sky-400 border border-sky-500/30 font-bold">DISPATCHED</span>`;
        default:
            return `<span class="badge bg-amber-500/20 text-amber-400 border border-amber-500/30 font-bold animate-pulse">${safeStatus}</span>`;
    }
}

window.handleOrderStatusChange = function(orderId, newStatus) {
    if (newStatus === 'CANCELLED') {
        window.openCancelOrderModal(orderId);
    } else {
        window.updateOrderStatus(orderId, newStatus);
    }
};

window.updateOrderStatus = async function(orderId, newStatus) {
    try {
        await updateDoc(doc(db, "orders", orderId), {
            orderStatus: newStatus,
            status: newStatus,
            updatedAt: Date.now()
        });
        if (window.showToast) window.showToast(`Order #${orderId.slice(0,6)} status changed to ${newStatus}`, "success");
    } catch (err) {
        console.error("Error updating order status:", err);
        if (window.showToast) window.showToast("Failed to update order status", "error");
    }
};

window.openPrintInvoiceModal = function(orderId) {
    const order = ordersList.find(o => o.id === orderId);
    if (!order) return;

    const modal = document.getElementById("modal-print-invoice");
    const container = document.getElementById("invoice-slip-content");
    if (!modal || !container) return;

    const itemsHtml = (order.items || []).map(i => `
        <div class="flex justify-between py-1 border-b border-slate-800/60">
            <span>${escapeHtml(i.quantity || 1)}x ${escapeHtml(i.name || i.title || 'Item')}</span>
            <span class="font-bold">₹${escapeHtml((i.price || 0) * (i.quantity || 1))}</span>
        </div>
    `).join('');

    const totalAmt = order.totalAmount || order.grandTotal || 0;
    const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleString('en-IN') : new Date().toLocaleString('en-IN');

    container.innerHTML = `
        <div class="text-center border-b border-slate-700 pb-3">
            <h2 class="text-base font-extrabold text-white uppercase tracking-widest">FLASH-O EXPRESS</h2>
            <p class="text-[10px] text-slate-400">Hyper-Local Quick Commerce</p>
            <p class="text-[10px] text-amber-400 mt-1 font-bold">TAX INVOICE / PACKING SLIP</p>
        </div>

        <div class="space-y-1 text-[11px] pt-1">
            <p><strong>Order ID:</strong> #${escapeHtml(order.id)}</p>
            <p><strong>Date & Time:</strong> ${escapeHtml(dateStr)}</p>
            <p><strong>Customer:</strong> ${escapeHtml(order.customerName || 'N/A')} (${escapeHtml(order.customerPhone || 'N/A')})</p>
            <p><strong>Address:</strong> ${escapeHtml(order.deliveryAddress || 'Standard Delivery')}</p>
            <p><strong>Assigned Rider:</strong> ${escapeHtml(order.assignedRiderName || 'Unassigned')}</p>
        </div>

        <div class="py-2">
            <p class="font-bold text-[10px] text-slate-400 uppercase border-b border-slate-700 pb-1">Items Summary</p>
            ${itemsHtml}
        </div>

        <div class="border-t border-slate-700 pt-2 space-y-1 text-right">
            <p class="text-sm font-extrabold text-emerald-400">GRAND TOTAL: ₹${escapeHtml(totalAmt)}</p>
            <p class="text-[9px] text-slate-400 uppercase">Payment Mode: ${escapeHtml(order.paymentMode || 'COD / Online')}</p>
        </div>

        <div class="text-center text-[9px] text-slate-500 pt-3 border-t border-slate-800">
            Thank you for ordering with Flash-O!
        </div>
    `;

    modal.classList.remove("hidden");
};

window.closePrintInvoiceModal = function() {
    const modal = document.getElementById("modal-print-invoice");
    if (modal) modal.classList.add("hidden");
};

window.openCancelOrderModal = function(orderId) {
    const modal = document.getElementById("modal-cancel-order");
    const idInput = document.getElementById("cancel-order-id");
    if (modal && idInput) {
        idInput.value = orderId;
        modal.classList.remove("hidden");
    }
};

window.closeCancelOrderModal = function() {
    const modal = document.getElementById("modal-cancel-order");
    if (modal) modal.classList.add("hidden");
};

window.submitCancelOrder = async function(e) {
    e.preventDefault();
    const orderId = document.getElementById("cancel-order-id")?.value;
    const reasonSelect = document.getElementById("cancel-reason-select")?.value;
    const reasonCustom = document.getElementById("cancel-reason-custom")?.value.trim();
    const finalReason = reasonCustom ? `${reasonSelect} - ${reasonCustom}` : reasonSelect;

    if (!orderId) return;

    try {
        await updateDoc(doc(db, "orders", orderId), {
            orderStatus: "CANCELLED",
            status: "CANCELLED",
            cancellationReason: finalReason,
            cancelledAt: Date.now(),
            updatedAt: Date.now()
        });
        if (window.showToast) window.showToast(`Order #${orderId.slice(0,6)} cancelled`, "success");
        window.closeCancelOrderModal();
    } catch (err) {
        console.error("Error cancelling order:", err);
        if (window.showToast) window.showToast("Failed to cancel order", "error");
    }
};

window.openReassignRiderModal = function(orderId) {
    const modal = document.getElementById("modal-reassign-rider");
    const idInput = document.getElementById("reassign-order-id");
    const select = document.getElementById("reassign-rider-select");

    if (!modal || !idInput || !select) return;

    idInput.value = orderId;

    if (availableRidersList.length === 0) {
        select.innerHTML = `<option value="">No riders registered in system</option>`;
    } else {
        select.innerHTML = availableRidersList.map(r => `
            <option value="${escapeHtml(r.id)}|${escapeHtml(r.name || r.fullName || 'Rider')}|${escapeHtml(r.phone || r.mobile || 'N/A')}">
                ${escapeHtml(r.name || r.fullName || 'Rider')} (${r.isOnline !== false ? 'ONLINE' : 'OFFLINE'}) - ${escapeHtml(r.phone || r.mobile || 'N/A')}
            </option>
        `).join('');
    }

    modal.classList.remove("hidden");
};

window.closeReassignRiderModal = function() {
    const modal = document.getElementById("modal-reassign-rider");
    if (modal) modal.classList.add("hidden");
};

window.submitReassignRider = async function(e) {
    e.preventDefault();
    const orderId = document.getElementById("reassign-order-id")?.value;
    const selectedVal = document.getElementById("reassign-rider-select")?.value;

    if (!orderId || !selectedVal) {
        if (window.showToast) window.showToast("Please select a valid rider", "error");
        return;
    }

    const [riderId, riderName, riderPhone] = selectedVal.split('|');

    try {
        await updateDoc(doc(db, "orders", orderId), {
            assignedRiderId: riderId,
            assignedRiderName: riderName,
            assignedRiderPhone: riderPhone,
            orderStatus: "DISPATCHED",
            status: "DISPATCHED",
            updatedAt: Date.now()
        });
        if (window.showToast) window.showToast(`Order assigned to ${riderName}`, "success");
        window.closeReassignRiderModal();
    } catch (err) {
        console.error("Error reassigning rider:", err);
        if (window.showToast) window.showToast("Failed to assign rider", "error");
    }
};

window.openOrderMapTracking = function(orderId, lat, lng) {
    if (typeof window.switchTab === 'function') {
        window.switchTab('map');
        if (window.showToast) window.showToast(`Tracking Order #${orderId.slice(0,6)} on map`, "info");
    } else {
        window.open(`https://www.google.com/maps?q=${lat},${lng}`, '_blank');
    }
};

window.exportOrdersCsv = function() {
    const headers = ["Order ID", "Customer Name", "Customer Phone", "Rider Name", "Address", "Total Amount", "Status", "Created At", "Item Summary"];
    const rows = ordersList.map(o => {
        const itemSummary = (o.items || []).map(i => `${i.quantity || 1}x ${i.name || i.title || ''}`).join('; ');
        const createdDate = o.createdAt ? new Date(o.createdAt).toLocaleString() : 'N/A';

        return [
            o.id || '',
            o.customerName || '',
            o.customerPhone || '',
            o.assignedRiderName || '',
            o.deliveryAddress || '',
            o.totalAmount || o.grandTotal || 0,
            o.orderStatus || o.status || 'PLACED',
            createdDate,
            itemSummary
        ];
    });

    const dateStr = new Date().toISOString().slice(0, 10);
    exportToCsv(`orders_report_${dateStr}.csv`, headers, rows);
};

// Document-level delegated listeners ensure dynamically re-injected header inputs trigger re-filtering
document.addEventListener("change", (e) => {
    if (e.target && e.target.id === "filter-order-status") {
        window.handleOrderStatusFilter(e.target.value);
    }
});

document.addEventListener("input", (e) => {
    if (e.target && e.target.id === "search-order-input") {
        window.handleOrderSearch(e.target.value);
    }
});

document.addEventListener("DOMContentLoaded", () => {
    initOrdersListener();
});
