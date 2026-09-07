/**
 * Component Loader
 * Dynamically fetches and inserts HTML component partials into tab, layout, and modal container elements.
 * Uses a Singleton Promise Guard to prevent duplicate fetches or DOM overwrites on page refresh.
 */

let isComponentsLoaded = false;
let componentsLoadPromise = null;

async function loadComponent(elementId, filepath) {
    const el = document.getElementById(elementId);
    if (!el) {
        console.warn(`[ComponentLoader] Container element '#${elementId}' not found.`);
        return;
    }
    try {
        const cacheBustUrl = `${filepath}?_v=${Date.now()}`;
        const response = await fetch(cacheBustUrl, { cache: 'no-store' });
        if (response.ok) {
            const content = await response.text();
            if (content.trim().startsWith('<!DOCTYPE html>') || content.includes('Flash-O | Admin Control Portal')) {
                console.warn(`[ComponentLoader] Rewrite fallback returned index.html for ${filepath}. Skipping insertion.`);
                return;
            }
            el.innerHTML = content;
            if (elementId === 'tab-dark-stores' && typeof window.renderDarkStoresGrid === 'function') {
                window.renderDarkStoresGrid();
            }
        } else {
            console.error(`[ComponentLoader] Failed to fetch ${filepath}: status ${response.status}`);
        }
    } catch (err) {
        console.error(`[ComponentLoader] Error loading ${filepath}:`, err);
    }
}

export function loadAllComponents() {
    if (isComponentsLoaded) {
        return Promise.resolve();
    }
    if (componentsLoadPromise) {
        return componentsLoadPromise;
    }

    componentsLoadPromise = Promise.all([
        // Layout & Overlays
        loadComponent('login-overlay-container', 'components/login-overlay.html'),
        loadComponent('header-container', 'components/header.html'),
        loadComponent('sidebar-container', 'components/sidebar.html'),

        // Tabs
        loadComponent('tab-overview', 'components/tabs/overview.html'),
        loadComponent('tab-merchant-home', 'components/tabs/merchant-home.html'),
        loadComponent('tab-user-profile', 'components/tabs/user-profile.html'),
        loadComponent('tab-products', 'components/tabs/products.html'),
        loadComponent('tab-orders', 'components/tabs/orders.html'),
        loadComponent('tab-dark-stores', 'components/tabs/dark-stores.html'),
        loadComponent('tab-riders', 'components/tabs/riders.html'),
        loadComponent('tab-users', 'components/tabs/users.html'),
        loadComponent('tab-coupons', 'components/tabs/coupons.html'),
        loadComponent('tab-analytics', 'components/tabs/analytics.html'),
        loadComponent('tab-map', 'components/tabs/map.html'),
        loadComponent('tab-settings', 'components/tabs/settings.html'),

        // Modals
        loadComponent('modal-product-container', 'components/modals/product-modal.html'),
        loadComponent('modal-dark-store-container', 'components/modals/dark-store-modal.html'),
        loadComponent('modal-coupon-container', 'components/modals/coupon-modal.html'),
        loadComponent('modal-add-user-container', 'components/modals/add-user-modal.html'),
        loadComponent('modal-edit-user-container', 'components/modals/edit-user-modal.html'),
        loadComponent('modal-invoice-container', 'components/modals/invoice-modal.html'),
        loadComponent('modal-csv-container', 'components/modals/csv-modal.html'),
        loadComponent('modal-category-container', 'components/modals/category-modal.html'),
        loadComponent('modal-fcm-container', 'components/modals/fcm-modal.html'),
        loadComponent('modal-merge-container', 'components/modals/merge-modal.html'),
        loadComponent('modal-cancel-order-container', 'components/modals/cancel-order-modal.html'),
        loadComponent('modal-reassign-rider-container', 'components/modals/reassign-rider-modal.html')
    ]).then(() => {
        isComponentsLoaded = true;
        if (typeof window.renderHeaderActions === 'function') {
            window.renderHeaderActions('products');
        }
        if (typeof window.renderDarkStoresGrid === 'function') {
            window.renderDarkStoresGrid();
        }
    }).catch((err) => {
        console.error("[ComponentLoader] Error during component load:", err);
        componentsLoadPromise = null;
    });

    return componentsLoadPromise;
}
window.loadAllComponents = loadAllComponents;

// Auto-initialize component loading immediately on script execution
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadAllComponents);
} else {
    loadAllComponents();
}
