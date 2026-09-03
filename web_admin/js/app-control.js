import { db } from './firebase-config.js';
import { showToast } from './ui.js';
import {
    doc,
    getDoc,
    setDoc,
    onSnapshot
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const APP_CONFIG_REF = doc(db, 'app_config', 'config');
const APP_UPDATE_REF = doc(db, 'app_config', 'app_update_info');

let currentConfig = {};

// Subscribe to real-time App Config updates
onSnapshot(APP_CONFIG_REF, (docSnap) => {
    if (docSnap.exists()) {
        currentConfig = docSnap.data();
        renderAppControls(currentConfig);
        updateKPIs(currentConfig);
    } else {
        console.warn("app_config/config document does not exist yet. Initializing default.");
        currentConfig = getInitialDefaultConfig();
        setDoc(APP_CONFIG_REF, currentConfig, { merge: true });
    }
}, (err) => {
    console.error("Error listening to app_config/config:", err);
});

function getInitialDefaultConfig() {
    return {
        bannerAds: [
            {
                id: 'banner_1',
                title: 'Welcome Special Offers',
                imageUrl: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=80',
                actionUrl: 'flash_sale',
                priority: 1,
                isActive: true
            }
        ],
        flashSale: {
            title: 'Flash Sale 50% Off',
            startTime: Date.now(),
            endTime: Date.now() + 86400000,
            productIds: [],
            discountPercentage: 50,
            isActive: true
        },
        flashOPass: {
            title: 'Flash-O Pass',
            isActive: true,
            tiers: [
                {
                    id: 'basic_99',
                    title: 'Flash-O Pass 99',
                    subtitle: 'Free delivery on ₹199+',
                    price: 99.0,
                    minOrderForFreeDelivery: 199.0,
                    benefits: ['Free Delivery on 1000+ items', 'Standard Support'],
                    isActive: true
                },
                {
                    id: 'premium_199',
                    title: 'Flash-O Premium 199',
                    subtitle: 'Free delivery on ₹99+',
                    price: 199.0,
                    minOrderForFreeDelivery: 99.0,
                    benefits: ['Free Delivery on 1000+ items', 'Priority Delivery', 'Max Discounts'],
                    isActive: true
                }
            ]
        },
        popupNotification: {
            title: '🎉 Welcome to Flash-O!',
            message: 'Order fresh food and groceries delivered in 10 minutes!',
            imageUrl: '',
            actionUrl: '',
            isActive: false
        },
        dealsButtonText: 'Flash-O Deals',
        dealsButtonAction: 'flash_sale',
        dealsButtonIcon: 'https://cdn-icons-png.flaticon.com/512/706/706164.png'
    };
}

function renderAppControls(config) {
    // 1. Render Banners
    renderBanners(config.bannerAds || []);

    // 2. Render Flash Sale
    const fs = config.flashSale || {};
    document.getElementById('toggle-flash-sale').checked = !!fs.isActive;
    document.getElementById('fs-title').value = fs.title || '';
    document.getElementById('fs-discount').value = fs.discountPercentage || 0;

    const remainingHours = fs.endTime ? Math.max(0, Math.round((fs.endTime - Date.now()) / (1000 * 60 * 60))) : 24;
    document.getElementById('fs-hours').value = remainingHours;

    // 3. Render Flash-O Pass
    renderPassTiers((config.flashOPass && config.flashOPass.tiers) || []);

    // 4. Render Popup Notification
    const popup = config.popupNotification || {};
    document.getElementById('popup-is-active').checked = !!popup.isActive;
    document.getElementById('popup-title').value = popup.title || '';
    document.getElementById('popup-image').value = popup.imageUrl || '';
    document.getElementById('popup-message').value = popup.message || '';

    // 5. Header Deals Button
    document.getElementById('deals-button-text').value = config.dealsButtonText || 'Flash-O Deals';
    document.getElementById('deals-button-action').value = config.dealsButtonAction || 'flash_sale';

    // 6. Support Configuration
    document.getElementById('support-number').value = config.supportNumber || '9318312478';
    document.getElementById('support-whatsapp').value = config.supportWhatsApp || '9318312478';
    document.getElementById('support-email').value = config.supportEmail || 'ranjanamishra1117@gmail.com';
    document.getElementById('live-chat-enabled').checked = config.isLiveChatEnabled !== false;
}

// Add event listener for support config form
document.getElementById('form-support-config').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    btn.disabled = true;
    btn.textContent = 'Updating...';

    try {
        await setDoc(APP_CONFIG_REF, {
            supportNumber: document.getElementById('support-number').value,
            supportWhatsApp: document.getElementById('support-whatsapp').value,
            supportEmail: document.getElementById('support-email').value,
            isLiveChatEnabled: document.getElementById('live-chat-enabled').checked,
            lastUpdated: Date.now()
        }, { merge: true });

        showToast('Support configuration updated successfully!');
    } catch (err) {
        console.error("Error updating support config:", err);
        showToast('Failed to update support config.', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Update Support Details';
    }
});

function updateKPIs(config) {
    const banners = (config.bannerAds || []).filter(b => b.isActive);
    document.getElementById('kpi-banners').textContent = banners.length;

    // Status Badges on Overview
    const fsStatus = document.getElementById('status-flash-sale');
    if (fsStatus) {
        if (config.flashSale && config.flashSale.isActive) {
            fsStatus.className = 'badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30';
            fsStatus.textContent = 'Active Now';
        } else {
            fsStatus.className = 'badge bg-slate-800 text-slate-400 border border-slate-700';
            fsStatus.textContent = 'Inactive';
        }
    }

    const passStatus = document.getElementById('status-pass-active');
    if (passStatus) {
        if (config.flashOPass && config.flashOPass.isActive) {
            passStatus.className = 'badge bg-emerald-500/20 text-emerald-400 border border-emerald-500/30';
            passStatus.textContent = 'Active';
        } else {
            passStatus.className = 'badge bg-slate-800 text-slate-400 border border-slate-700';
            passStatus.textContent = 'Inactive';
        }
    }

    const popupStatus = document.getElementById('status-popup-active');
    if (popupStatus) {
        if (config.popupNotification && config.popupNotification.isActive) {
            popupStatus.className = 'badge bg-amber-500/20 text-amber-400 border border-amber-500/30';
            popupStatus.textContent = 'Broadcasting';
        } else {
            popupStatus.className = 'badge bg-slate-800 text-slate-400 border border-slate-700';
            popupStatus.textContent = 'None';
        }
    }
}

// BANNERS RENDER & ACTIONS
function renderBanners(banners) {
    const container = document.getElementById('banners-container');
    if (!container) return;

    if (banners.length === 0) {
        container.innerHTML = `
            <div class="col-span-full p-8 text-center bg-slate-950/40 border border-dashed border-slate-800 rounded-xl">
                <p class="text-slate-400 text-sm">No promotional banners created yet.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = banners.map((banner, index) => `
        <div class="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden group hover:border-slate-700 transition-all">
            <div class="h-36 bg-slate-800 relative overflow-hidden">
                <img src="${banner.imageUrl}" alt="${banner.title}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" onerror="this.src='https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=80'">
                <div class="absolute top-2 right-2">
                    <span class="badge ${banner.isActive ? 'bg-emerald-500/80 text-white' : 'bg-slate-800/90 text-slate-400'} backdrop-blur-md">
                        ${banner.isActive ? 'Active' : 'Disabled'}
                    </span>
                </div>
            </div>
            <div class="p-4 space-y-2">
                <h4 class="font-bold text-white text-sm truncate">${banner.title}</h4>
                <p class="text-xs text-slate-400 truncate"><i class="fa-solid fa-link text-slate-500"></i> ${banner.actionUrl || 'No action'}</p>
                <div class="flex items-center justify-between pt-2 border-t border-slate-800">
                    <button onclick="toggleBannerActive('${banner.id}')" class="text-xs text-slate-300 hover:text-white font-medium">
                        <i class="fa-solid fa-power-off text-amber-400"></i> Toggle State
                    </button>
                    <button onclick="deleteBanner('${banner.id}')" class="text-xs text-rose-400 hover:text-rose-300 font-medium">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

window.openBannerModal = function() {
    document.getElementById('modal-banner').classList.remove('hidden');
    document.getElementById('banner-id').value = '';
    document.getElementById('banner-title-input').value = '';
    document.getElementById('banner-image-input').value = '';
    document.getElementById('banner-action-input').value = '';
};

window.closeBannerModal = function() {
    document.getElementById('modal-banner').classList.add('hidden');
};

document.getElementById('form-banner').addEventListener('submit', async (e) => {
    e.preventDefault();
    const title = document.getElementById('banner-title-input').value.trim();
    const imageUrl = document.getElementById('banner-image-input').value.trim();
    const actionUrl = document.getElementById('banner-action-input').value.trim();
    const isActive = document.getElementById('banner-active-input').checked;

    const newBanner = {
        id: 'banner_' + Date.now(),
        title,
        imageUrl,
        actionUrl,
        priority: 1,
        isActive
    };

    const updatedBanners = [...(currentConfig.bannerAds || []), newBanner];
    await setDoc(APP_CONFIG_REF, { bannerAds: updatedBanners }, { merge: true });
    window.closeBannerModal();
    window.showToast("New banner added live to Customer App!", "success");
});

window.toggleBannerActive = async function(bannerId) {
    const updated = (currentConfig.bannerAds || []).map(b => {
        if (b.id === bannerId) return { ...b, isActive: !b.isActive };
        return b;
    });
    await setDoc(APP_CONFIG_REF, { bannerAds: updated }, { merge: true });
    window.showToast("Banner status updated!", "success");
};

window.deleteBanner = async function(bannerId) {
    if (!confirm("Are you sure you want to delete this banner?")) return;
    const updated = (currentConfig.bannerAds || []).filter(b => b.id !== bannerId);
    await setDoc(APP_CONFIG_REF, { bannerAds: updated }, { merge: true });
    window.showToast("Banner removed!", "success");
};

// FLASH SALE SUBMIT
document.getElementById('form-flash-sale').addEventListener('submit', async (e) => {
    e.preventDefault();
    const isActive = document.getElementById('toggle-flash-sale').checked;
    const title = document.getElementById('fs-title').value.trim();
    const discountPercentage = parseInt(document.getElementById('fs-discount').value) || 0;
    const hours = parseFloat(document.getElementById('fs-hours').value) || 24;

    const flashSale = {
        title,
        startTime: Date.now(),
        endTime: Date.now() + (hours * 3600 * 1000),
        productIds: currentConfig.flashSale?.productIds || [],
        discountPercentage,
        isActive
    };

    await setDoc(APP_CONFIG_REF, { flashSale }, { merge: true });
    window.showToast("Flash Sale configuration published!", "success");
});

document.getElementById('toggle-flash-sale').addEventListener('change', async (e) => {
    const isActive = e.target.checked;
    const flashSale = { ...(currentConfig.flashSale || {}), isActive };
    await setDoc(APP_CONFIG_REF, { flashSale }, { merge: true });
    window.showToast(`Flash sale is now ${isActive ? 'ACTIVE' : 'INACTIVE'}`, "success");
});

// PASS TIERS RENDER
function renderPassTiers(tiers) {
    const container = document.getElementById('pass-tiers-container');
    if (!container) return;

    container.innerHTML = tiers.map(tier => `
        <div class="p-5 bg-slate-950 border border-slate-800 rounded-xl space-y-3">
            <div class="flex justify-between items-center">
                <h4 class="font-bold text-white text-base">${tier.title}</h4>
                <span class="text-amber-400 font-extrabold text-lg">₹${tier.price}</span>
            </div>
            <p class="text-xs text-slate-400">${tier.subtitle}</p>
            <div class="text-xs text-slate-300 space-y-1 pt-2 border-t border-slate-800">
                ${(tier.benefits || []).map(b => `<p><i class="fa-solid fa-check text-emerald-400 mr-1.5"></i> ${b}</p>`).join('')}
            </div>
            <p class="text-[11px] text-slate-500">Min Order for Free Delivery: ₹${tier.minOrderForFreeDelivery}</p>
        </div>
    `).join('');
}

// POPUP NOTIFICATION SUBMIT
document.getElementById('form-popup-notification').addEventListener('submit', async (e) => {
    e.preventDefault();
    const popupNotification = {
        isActive: document.getElementById('popup-is-active').checked,
        title: document.getElementById('popup-title').value.trim(),
        imageUrl: document.getElementById('popup-image').value.trim(),
        message: document.getElementById('popup-message').value.trim(),
        actionUrl: ''
    };

    await setDoc(APP_CONFIG_REF, { popupNotification }, { merge: true });
    window.showToast("Customer App Popup Alert updated!", "success");
});

// APP UPDATE SUBMIT
document.getElementById('form-app-update').addEventListener('submit', async (e) => {
    e.preventDefault();
    const appId = document.getElementById('update-target-app').value;
    const versionCode = parseInt(document.getElementById('update-version-code').value) || 1;
    const versionName = document.getElementById('update-version-name').value.trim();
    const downloadUrl = document.getElementById('update-download-url').value.trim();
    const isMandatory = document.getElementById('update-is-mandatory').checked;

    const appUpdateInfo = {
        appId,
        appName: appId === 'customer' ? 'Flash-O Customer' : appId === 'partner' ? 'Flash-O Partner' : 'F-Store',
        versionCode,
        versionName,
        downloadUrl,
        releaseNotes: 'Performance improvements and bug fixes.',
        isMandatory,
        timestamp: Date.now()
    };

    await setDoc(APP_UPDATE_REF, appUpdateInfo, { merge: true });
    window.showToast("Forced App Update Rule Published!", "success");
});

// HEADER DEALS BUTTON SUBMIT
document.getElementById('form-deals-button').addEventListener('submit', async (e) => {
    e.preventDefault();
    const dealsButtonText = document.getElementById('deals-button-text').value.trim();
    const dealsButtonAction = document.getElementById('deals-button-action').value;

    await setDoc(APP_CONFIG_REF, { dealsButtonText, dealsButtonAction }, { merge: true });
    window.showToast("Header Deals Button Configured!", "success");
});
