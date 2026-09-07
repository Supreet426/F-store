/**
 * Global UI Helper Functions for Flash-O Web Admin Portal
 */

window.tabHistoryStack = [];
let currentActiveTab = 'overview';

export function switchTab(tabId, isBackAction = false) {
    if (tabId === 'riders') {
        if (typeof window.openUsersWithFilter === 'function') {
            window.openUsersWithFilter('RIDER');
        } else {
            switchTab('users', isBackAction);
            if (typeof window.setFilterPill === 'function') window.setFilterPill('RIDER');
        }
        return;
    }

    if (currentActiveTab && currentActiveTab !== tabId && !isBackAction) {
        window.tabHistoryStack.push(currentActiveTab);
    }
    currentActiveTab = tabId;

    const tabs = document.querySelectorAll('.tab-content');
    tabs.forEach(tab => tab.classList.add('hidden'));

    const navButtons = document.querySelectorAll('.nav-item');
    navButtons.forEach(btn => btn.classList.remove('active'));

    const targetContent = document.getElementById('tab-' + tabId);
    if (targetContent) {
        targetContent.classList.remove('hidden');
    }

    const activeNavBtn = document.querySelector(`.nav-item[onclick*="${tabId}"]`);
    if (activeNavBtn) {
        activeNavBtn.classList.add('active');
    }

    const titles = {
        'overview': 'Dashboard Overview',
        'merchant-home': 'Merchant Store Partner Portal',
        'user-profile': 'User Profile Details & Governance',
        'products': 'Product Catalog & Inventory',
        'orders': 'Live Orders Board',
        'dark-stores': 'Dark Store Fulfillment Hubs',
        'riders': 'Delivery Partner Hub',
        'users': 'User Management Portal',
        'coupons': 'Coupons & Promo Offers',
        'analytics': 'Business Analytics & Insights',
        'map': 'Realtime Operations Live Map',
        'settings': 'System Settings & Config'
    };

    const headerTitle = document.getElementById('page-title');
    if (headerTitle) {
        headerTitle.textContent = titles[tabId] || 'Admin Dashboard';
    }

    renderHeaderActions(tabId);


    if (tabId === 'dark-stores' && typeof window.renderDarkStoresGrid === 'function') {
        window.renderDarkStoresGrid();
    }

    const sidebar = document.getElementById('sidebar-drawer');
    if (sidebar && !sidebar.classList.contains('-translate-x-full')) {
        sidebar.classList.add('-translate-x-full');
    }
}
window.switchTab = switchTab;

export function renderHeaderActions(tabId) {
    const container = document.getElementById('header-actions-container');
    if (!container) return;

    if (tabId === 'products') {
        container.innerHTML = `
            <div class="relative w-32 sm:w-44 lg:w-56 shrink">
                <i class="fa-solid fa-magnifying-glass absolute left-3 top-2.5 text-slate-500 text-xs"></i>
                <input type="text" id="search-product" placeholder="Search products..." class="input-field pl-8 py-1.5 text-xs w-full bg-slate-950/90 border-slate-700/80 rounded-xl">
            </div>
            <button onclick="approveAllProductsWeb()" title="Approve All & Go Live" class="bg-emerald-600 hover:bg-emerald-500 text-white w-8 h-8 sm:w-9 sm:h-9 rounded-xl font-bold flex items-center justify-center cursor-pointer transition-colors shadow-md shrink-0">
                <i class="fa-solid fa-circle-check text-xs sm:text-sm"></i>
            </button>
            <button onclick="exportProductsCsv()" title="Export CSV" class="bg-slate-800 hover:bg-slate-700 text-emerald-400 w-8 h-8 sm:w-9 sm:h-9 rounded-xl border border-slate-700 flex items-center justify-center cursor-pointer transition-colors shrink-0">
                <i class="fa-solid fa-file-csv text-xs sm:text-sm"></i>
            </button>
            <button onclick="openCsvModal()" title="Import CSV" class="bg-slate-800 hover:bg-slate-700 text-sky-400 w-8 h-8 sm:w-9 sm:h-9 rounded-xl border border-slate-700 flex items-center justify-center cursor-pointer transition-colors shrink-0">
                <i class="fa-solid fa-file-import text-xs sm:text-sm"></i>
            </button>
            <button onclick="openCategoryModal()" title="Categories" class="bg-slate-800 hover:bg-slate-700 text-amber-400 w-8 h-8 sm:w-9 sm:h-9 rounded-xl border border-slate-700 flex items-center justify-center cursor-pointer transition-colors shrink-0">
                <i class="fa-solid fa-layer-group text-xs sm:text-sm"></i>
            </button>
            <button onclick="openProductModal()" title="Add Product" class="btn-primary text-xs px-2.5 sm:px-3 py-1.5 sm:py-2 flex items-center gap-1.5 cursor-pointer shadow-md shrink-0">
                <i class="fa-solid fa-plus text-xs"></i>
                <span class="hidden lg:inline font-bold">Add Product</span>
            </button>
        `;
    } else if (tabId === 'orders') {
        const currentFilter = window.activeStatusFilter || 'ALL';
        container.innerHTML = `
            <div class="relative w-32 sm:w-48 lg:w-60 shrink">
                <i class="fa-solid fa-magnifying-glass absolute left-3 top-2.5 text-slate-500 text-xs"></i>
                <input type="text" id="search-order-input" oninput="window.handleOrderSearch ? window.handleOrderSearch(this.value) : null" placeholder="Search orders..." class="input-field pl-8 py-1.5 text-xs w-full bg-slate-950/90 border-slate-700/80 rounded-xl">
            </div>
            <select id="filter-order-status" onchange="window.handleOrderStatusFilter ? window.handleOrderStatusFilter(this.value) : null" class="input-field text-xs py-1.5 px-2 bg-slate-950 border-slate-700 rounded-xl w-auto">
                <option value="ALL" ${currentFilter === 'ALL' ? 'selected' : ''}>All Orders</option>
                <option value="ACTIVE" ${currentFilter === 'ACTIVE' ? 'selected' : ''}>Active</option>
                <option value="PLACED" ${currentFilter === 'PLACED' ? 'selected' : ''}>PLACED</option>
                <option value="ACCEPTED" ${currentFilter === 'ACCEPTED' ? 'selected' : ''}>ACCEPTED</option>
                <option value="PREPARING" ${currentFilter === 'PREPARING' ? 'selected' : ''}>PREPARING</option>
                <option value="READY" ${currentFilter === 'READY' ? 'selected' : ''}>READY</option>
                <option value="DISPATCHED" ${currentFilter === 'DISPATCHED' ? 'selected' : ''}>DISPATCHED</option>
                <option value="DELIVERED" ${currentFilter === 'DELIVERED' ? 'selected' : ''}>DELIVERED</option>
                <option value="CANCELLED" ${currentFilter === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
            </select>
            <button onclick="exportOrdersCsv()" title="Export Orders CSV" class="bg-slate-800 hover:bg-slate-700 text-emerald-400 w-8 h-8 sm:w-9 sm:h-9 rounded-xl border border-slate-700 flex items-center justify-center cursor-pointer transition-colors shrink-0">
                <i class="fa-solid fa-file-csv text-xs sm:text-sm"></i>
            </button>
        `;
    } else if (tabId === 'users') {
        container.innerHTML = `
            <div class="relative w-32 sm:w-48 lg:w-64 shrink">
                <i class="fa-solid fa-magnifying-glass absolute left-3 top-2.5 text-slate-500 text-xs"></i>
                <input type="text" id="searchInput" placeholder="Search users..." class="input-field pl-8 py-1.5 text-xs w-full bg-slate-950/90 border-slate-700/80 rounded-xl">
            </div>
            <button onclick="exportUsersCsv()" title="Export Users CSV" class="bg-slate-800 hover:bg-slate-700 text-emerald-400 w-8 h-8 sm:w-9 sm:h-9 rounded-xl border border-slate-700 flex items-center justify-center cursor-pointer transition-colors shrink-0">
                <i class="fa-solid fa-file-csv text-xs sm:text-sm"></i>
            </button>
            <button onclick="openAddUserModal()" title="Add Account" class="btn-primary text-xs px-2.5 sm:px-3 py-1.5 sm:py-2 flex items-center gap-1.5 cursor-pointer shadow-md shrink-0">
                <i class="fa-solid fa-user-plus text-xs"></i>
                <span class="hidden lg:inline font-bold">Add Account</span>
            </button>
        `;
    } else if (tabId === 'dark-stores') {
        container.innerHTML = `
            <button onclick="openDarkStoreModal()" title="Add Dark Store" class="btn-primary text-xs px-3 py-2 flex items-center gap-1.5 cursor-pointer shadow-md shrink-0">
                <i class="fa-solid fa-plus text-xs"></i>
                <span class="hidden lg:inline font-bold">Add Dark Store</span>
            </button>
        `;
    } else {
        container.innerHTML = '';
    }
}
window.renderHeaderActions = renderHeaderActions;

export function goBack() {
    if (window.tabHistoryStack.length > 0) {
        const prevTab = window.tabHistoryStack.pop();
        switchTab(prevTab, true);
    } else {
        switchTab('overview', true);
    }
}
window.goBack = goBack;

export function toggleDrawer(forceState) {
    const sidebar = document.getElementById('sidebar-drawer');
    if (!sidebar) return;

    if (typeof forceState === 'boolean') {
        if (forceState) {
            sidebar.classList.remove('-translate-x-full');
        } else {
            sidebar.classList.add('-translate-x-full');
        }
    } else {
        sidebar.classList.toggle('-translate-x-full');
    }
}
window.toggleDrawer = toggleDrawer;

export function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    const bgClass = type === 'success' ? 'bg-emerald-950 border-emerald-800 text-emerald-300' :
                   type === 'error' ? 'bg-rose-950 border-rose-800 text-rose-300' :
                   'bg-slate-900 border-slate-700 text-slate-200';

    const iconClass = type === 'success' ? 'fa-circle-check text-emerald-400' :
                     type === 'error' ? 'fa-circle-exclamation text-rose-400' :
                     'fa-circle-info text-amber-400';

    toast.className = `p-4 rounded-xl border shadow-2xl flex items-start gap-3 transition-all duration-300 transform translate-x-10 opacity-0 pointer-events-auto ${bgClass}`;
    toast.innerHTML = `
        <i class="fa-solid ${iconClass} text-base mt-0.5 shrink-0"></i>
        <div class="text-xs font-medium leading-relaxed">${message}</div>
    `;

    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.remove('translate-x-10', 'opacity-0');
    });

    setTimeout(() => {
        toast.classList.add('opacity-0', 'translate-x-10');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
window.showToast = showToast;

// Close drawer when clicking outside (mobile and desktop)
document.addEventListener('click', (event) => {
    const sidebar = document.getElementById('sidebar-drawer');
    const toggleBtn = document.getElementById('btn-toggle-drawer');
    if (!sidebar) return;

    const isOpen = !sidebar.classList.contains('-translate-x-full');
    if (!isOpen) return;

    if (sidebar.contains(event.target) || (toggleBtn && toggleBtn.contains(event.target))) {
        return;
    }

    toggleDrawer(false);
});

