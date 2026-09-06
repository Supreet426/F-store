import { db, storage } from './firebase-config.js';
import { showToast } from './ui.js';
import {
    doc,
    getDoc,
    setDoc,
    onSnapshot
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import {
    ref,
    uploadBytesResumable,
    getDownloadURL
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-storage.js";

// Firestore Document References
const APP_CONFIG_APP_REF = doc(db, 'app_config', 'config_app');
const APP_CONFIG_WEB_REF = doc(db, 'app_config', 'config_web');
const APP_CONFIG_GLOBAL_REF = doc(db, 'app_config', 'config_global');
const APP_CONFIG_LEGACY_REF = doc(db, 'app_config', 'config'); // Backward compatibility
const APP_UPDATE_REF = doc(db, 'app_config', 'app_update_info');

// In-Memory Configurations
let appConfig = {};
let webConfig = {};
let globalConfig = {};
let currentPlatform = 'app'; // 'app' | 'web' | 'global'
let currentBannerTarget = 'app'; // Target for banner creation modal

// =========================================================================
// TAB SWITCHING LOGIC
// =========================================================================
window.switchPlatformTab = function(platform) {
    currentPlatform = platform;

    // Reset button styles
    ['app', 'web', 'global'].forEach(p => {
        const btn = document.getElementById(`tab-btn-${p}`);
        const view = document.getElementById(`view-platform-${p}`);

        if (btn) {
            if (p === platform) {
                btn.className = 'platform-tab-btn active flex-1 sm:flex-initial px-4 py-2 rounded-lg text-xs font-bold text-white transition-all flex items-center justify-center gap-2 cursor-pointer bg-brand-600 shadow-md';
            } else {
                btn.className = 'platform-tab-btn flex-1 sm:flex-initial px-4 py-2 rounded-lg text-xs font-bold text-slate-400 hover:text-white transition-all flex items-center justify-center gap-2 cursor-pointer';
            }
        }

        if (view) {
            if (p === platform) {
                view.classList.remove('hidden');
            } else {
                view.classList.add('hidden');
            }
        }
    });
};

// =========================================================================
// REAL-TIME FIRESTORE LISTENERS
// =========================================================================

// 1. Mobile App Listener (`app_config/config_app`)
onSnapshot(APP_CONFIG_APP_REF, (docSnap) => {
    if (docSnap.exists()) {
        appConfig = docSnap.data();
    } else {
        console.warn("app_config/config_app missing. Migrating default from legacy config.");
        appConfig = getInitialDefaultAppConfig();
        setDoc(APP_CONFIG_APP_REF, appConfig, { merge: true });
    }
    renderAppControls(appConfig);
    updateKPIs();
}, (err) => {
    console.error("Error listening to config_app:", err);
});

// 2. Customer Web Listener (`app_config/config_web`)
onSnapshot(APP_CONFIG_WEB_REF, (docSnap) => {
    if (docSnap.exists()) {
        webConfig = docSnap.data();
    } else {
        console.warn("app_config/config_web missing. Initializing default web config.");
        webConfig = getInitialDefaultWebConfig();
        setDoc(APP_CONFIG_WEB_REF, webConfig, { merge: true });
    }
    renderWebControls(webConfig);
}, (err) => {
    console.error("Error listening to config_web:", err);
});

// 3. Global Settings Listener (`app_config/config_global`)
onSnapshot(APP_CONFIG_GLOBAL_REF, (docSnap) => {
    if (docSnap.exists()) {
        globalConfig = docSnap.data();
    } else {
        console.warn("app_config/config_global missing. Initializing default global config.");
        globalConfig = getInitialDefaultGlobalConfig();
        setDoc(APP_CONFIG_GLOBAL_REF, globalConfig, { merge: true });
    }
    renderGlobalControls(globalConfig);
}, (err) => {
    console.error("Error listening to config_global:", err);
});

// Legacy Fallback Listener
onSnapshot(APP_CONFIG_LEGACY_REF, (docSnap) => {
    if (docSnap.exists()) {
        const legacyData = docSnap.data();
        // If config_app is empty, populate from legacy
        if (!appConfig.bannerAds && legacyData.bannerAds) {
            appConfig = { ...legacyData, ...appConfig };
            renderAppControls(appConfig);
        }
    }
});

// =========================================================================
// DEFAULT CONFIG GENERATORS
// =========================================================================
function getInitialDefaultAppConfig() {
    return {
        bannerAds: [
            {
                id: 'banner_app_1',
                title: 'Welcome App Exclusive Offer',
                imageUrl: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=80',
                actionUrl: 'flash_sale',
                priority: 1,
                isActive: true
            }
        ],
        flashSale: {
            title: 'App Exclusive Flash Sale 50% Off',
            startTime: Date.now(),
            endTime: Date.now() + 86400000,
            productIds: [],
            discountPercentage: 50,
            isActive: true
        },
        flashOPass: {
            title: 'Flash-O Pass VIP',
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
            title: '🎉 App Exclusive Cashback!',
            message: 'Get ₹100 Cashback on your first 3 orders on the Flash-O App!',
            imageUrl: '',
            actionUrl: '',
            isActive: false
        }
    };
}

function getInitialDefaultWebConfig() {
    return {
        bannerAds: [
            {
                id: 'banner_web_1',
                title: 'Website Special Widescreen Hero',
                imageUrl: 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=1200&q=80',
                actionUrl: '/flash-sale',
                priority: 1,
                isActive: true
            }
        ],
        topAnnouncement: {
            text: '🚀 Free Delivery on all website orders above ₹199!',
            link: '/flash-sale',
            isActive: true
        },
        flashSale: {
            title: 'Website Mega Flash Sale 40% Off',
            startTime: Date.now(),
            endTime: Date.now() + 86400000,
            productIds: [],
            discountPercentage: 40,
            isActive: true
        },
        dealsButtonText: 'Flash Deals 🔥',
        dealsButtonAction: 'flash_sale'
    };
}

function getInitialDefaultGlobalConfig() {
    return {
        supportNumber: '9318312478',
        supportWhatsApp: '9318312478',
        supportEmail: 'support@flash-o.com',
        isLiveChatEnabled: true
    };
}

// =========================================================================
// RENDERING LOGIC
// =========================================================================
function renderAppControls(config) {
    // 1. Render App Banners
    renderBanners('app', config.bannerAds || []);

    // 2. Render App Flash Sale
    const fs = config.flashSale || {};
    const toggleFs = document.getElementById('toggle-flash-sale-app');
    if (toggleFs) toggleFs.checked = !!fs.isActive;
    const titleFs = document.getElementById('fs-title-app');
    if (titleFs) titleFs.value = fs.title || '';
    const discFs = document.getElementById('fs-discount-app');
    if (discFs) discFs.value = fs.discountPercentage || 0;

    const remainingHours = fs.endTime ? Math.max(0, Math.round((fs.endTime - Date.now()) / (1000 * 60 * 60))) : 24;
    const hoursFs = document.getElementById('fs-hours-app');
    if (hoursFs) hoursFs.value = remainingHours;

    // 3. Render App Popup Alert
    const popup = config.popupNotification || {};
    const activePopup = document.getElementById('popup-is-active-app');
    if (activePopup) activePopup.checked = !!popup.isActive;
    const titlePopup = document.getElementById('popup-title-app');
    if (titlePopup) titlePopup.value = popup.title || '';
    const imgPopup = document.getElementById('popup-image-app');
    if (imgPopup) imgPopup.value = popup.imageUrl || '';
    const msgPopup = document.getElementById('popup-message-app');
    if (msgPopup) msgPopup.value = popup.message || '';

    // 4. Render App Pass Tiers
    renderPassTiers('app', (config.flashOPass && config.flashOPass.tiers) || []);
}

function renderWebControls(config) {
    // 1. Render Web Banners
    renderBanners('web', config.bannerAds || []);

    // 2. Render Web Top Announcement
    const ann = config.topAnnouncement || {};
    const activeAnn = document.getElementById('web-announcement-active');
    if (activeAnn) activeAnn.checked = !!ann.isActive;
    const textAnn = document.getElementById('web-announcement-text');
    if (textAnn) textAnn.value = ann.text || '';
    const linkAnn = document.getElementById('web-announcement-link');
    if (linkAnn) linkAnn.value = ann.link || '';

    // 3. Render Web Flash Sale
    const fs = config.flashSale || {};
    const toggleFs = document.getElementById('toggle-flash-sale-web');
    if (toggleFs) toggleFs.checked = !!fs.isActive;
    const titleFs = document.getElementById('fs-title-web');
    if (titleFs) titleFs.value = fs.title || '';
    const discFs = document.getElementById('fs-discount-web');
    if (discFs) discFs.value = fs.discountPercentage || 0;

    const remainingHours = fs.endTime ? Math.max(0, Math.round((fs.endTime - Date.now()) / (1000 * 60 * 60))) : 24;
    const hoursFs = document.getElementById('fs-hours-web');
    if (hoursFs) hoursFs.value = remainingHours;

    // 4. Render Web Header Deals Button
    const txtDeals = document.getElementById('deals-button-text-web');
    if (txtDeals) txtDeals.value = config.dealsButtonText || 'Flash Deals 🔥';
    const actDeals = document.getElementById('deals-button-action-web');
    if (actDeals) actDeals.value = config.dealsButtonAction || 'flash_sale';
}

function renderGlobalControls(config) {
    const num = document.getElementById('support-number');
    if (num) num.value = config.supportNumber || '9318312478';
    const wa = document.getElementById('support-whatsapp');
    if (wa) wa.value = config.supportWhatsApp || '9318312478';
    const email = document.getElementById('support-email');
    if (email) email.value = config.supportEmail || 'support@flash-o.com';
    const chat = document.getElementById('live-chat-enabled');
    if (chat) chat.checked = config.isLiveChatEnabled !== false;
}

function updateKPIs() {
    const activeBanners = (appConfig.bannerAds || []).filter(b => b.isActive);
    const kpi = document.getElementById('kpi-banners');
    if (kpi) kpi.textContent = activeBanners.length;
}

// =========================================================================
// BANNERS MANAGEMENT
// =========================================================================
function renderBanners(platform, banners) {
    const container = document.getElementById(`banners-container-${platform}`);
    if (!container) return;

    if (banners.length === 0) {
        container.innerHTML = `
            <div class="col-span-full p-8 text-center bg-slate-950/40 border border-dashed border-slate-800 rounded-xl">
                <p class="text-slate-400 text-sm">No ${platform === 'app' ? 'Mobile App' : 'Website'} banners created yet.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = banners.map((banner) => `
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
                    <button onclick="toggleBannerActive('${platform}', '${banner.id}')" class="text-xs text-slate-300 hover:text-white font-medium">
                        <i class="fa-solid fa-power-off text-amber-400"></i> Toggle
                    </button>
                    <button onclick="deleteBanner('${platform}', '${banner.id}')" class="text-xs text-rose-400 hover:text-rose-300 font-medium">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

window.openBannerModal = function(targetPlatform = 'app') {
    currentBannerTarget = targetPlatform;
    const modal = document.getElementById('modal-banner');
    if (!modal) return;
    modal.classList.remove('hidden');
    document.getElementById('banner-id').value = '';
    document.getElementById('banner-title-input').value = '';
    document.getElementById('banner-image-input').value = '';
    document.getElementById('banner-action-input').value = '';
};

window.closeBannerModal = function() {
    const modal = document.getElementById('modal-banner');
    if (modal) modal.classList.add('hidden');
};

const formBanner = document.getElementById('form-banner');
if (formBanner) {
    formBanner.addEventListener('submit', async (e) => {
        e.preventDefault();
        const title = document.getElementById('banner-title-input').value.trim();
        const imageUrl = document.getElementById('banner-image-input').value.trim();
        const actionUrl = document.getElementById('banner-action-input').value.trim();
        const isActive = document.getElementById('banner-active-input').checked;

        const newBanner = {
            id: `banner_${currentBannerTarget}_` + Date.now(),
            title,
            imageUrl,
            actionUrl,
            priority: 1,
            isActive
        };

        if (currentBannerTarget === 'app') {
            const updatedBanners = [...(appConfig.bannerAds || []), newBanner];
            await setDoc(APP_CONFIG_APP_REF, { bannerAds: updatedBanners }, { merge: true });
            // Sync with legacy doc for older app clients
            await setDoc(APP_CONFIG_LEGACY_REF, { bannerAds: updatedBanners }, { merge: true });
            showToast("New Mobile App banner published!", "success");
        } else {
            const updatedBanners = [...(webConfig.bannerAds || []), newBanner];
            await setDoc(APP_CONFIG_WEB_REF, { bannerAds: updatedBanners }, { merge: true });
            showToast("New Website hero banner published!", "success");
        }

        window.closeBannerModal();
    });
}

window.toggleBannerActive = async function(platform, bannerId) {
    if (platform === 'app') {
        const updated = (appConfig.bannerAds || []).map(b => b.id === bannerId ? { ...b, isActive: !b.isActive } : b);
        await setDoc(APP_CONFIG_APP_REF, { bannerAds: updated }, { merge: true });
        await setDoc(APP_CONFIG_LEGACY_REF, { bannerAds: updated }, { merge: true });
    } else {
        const updated = (webConfig.bannerAds || []).map(b => b.id === bannerId ? { ...b, isActive: !b.isActive } : b);
        await setDoc(APP_CONFIG_WEB_REF, { bannerAds: updated }, { merge: true });
    }
    showToast(`${platform === 'app' ? 'Mobile App' : 'Website'} banner toggled!`, "success");
};

window.deleteBanner = async function(platform, bannerId) {
    if (!confirm(`Are you sure you want to delete this ${platform === 'app' ? 'Mobile App' : 'Website'} banner?`)) return;
    if (platform === 'app') {
        const updated = (appConfig.bannerAds || []).filter(b => b.id !== bannerId);
        await setDoc(APP_CONFIG_APP_REF, { bannerAds: updated }, { merge: true });
        await setDoc(APP_CONFIG_LEGACY_REF, { bannerAds: updated }, { merge: true });
    } else {
        const updated = (webConfig.bannerAds || []).filter(b => b.id !== bannerId);
        await setDoc(APP_CONFIG_WEB_REF, { bannerAds: updated }, { merge: true });
    }
    showToast("Banner deleted!", "success");
};

// =========================================================================
// FORM SUBMIT HANDLERS
// =========================================================================

// 1. Mobile App Flash Sale Form
const formFsApp = document.getElementById('form-flash-sale-app');
if (formFsApp) {
    formFsApp.addEventListener('submit', async (e) => {
        e.preventDefault();
        const isActive = document.getElementById('toggle-flash-sale-app').checked;
        const title = document.getElementById('fs-title-app').value.trim();
        const discountPercentage = parseInt(document.getElementById('fs-discount-app').value) || 0;
        const hours = parseFloat(document.getElementById('fs-hours-app').value) || 24;

        const flashSale = {
            title,
            startTime: Date.now(),
            endTime: Date.now() + (hours * 3600 * 1000),
            productIds: appConfig.flashSale?.productIds || [],
            discountPercentage,
            isActive
        };

        await setDoc(APP_CONFIG_APP_REF, { flashSale }, { merge: true });
        await setDoc(APP_CONFIG_LEGACY_REF, { flashSale }, { merge: true });
        showToast("Mobile App Flash Sale Published!", "success");
    });
}

// 2. Customer Web Flash Sale Form
const formFsWeb = document.getElementById('form-flash-sale-web');
if (formFsWeb) {
    formFsWeb.addEventListener('submit', async (e) => {
        e.preventDefault();
        const isActive = document.getElementById('toggle-flash-sale-web').checked;
        const title = document.getElementById('fs-title-web').value.trim();
        const discountPercentage = parseInt(document.getElementById('fs-discount-web').value) || 0;
        const hours = parseFloat(document.getElementById('fs-hours-web').value) || 24;

        const flashSale = {
            title,
            startTime: Date.now(),
            endTime: Date.now() + (hours * 3600 * 1000),
            productIds: webConfig.flashSale?.productIds || [],
            discountPercentage,
            isActive
        };

        await setDoc(APP_CONFIG_WEB_REF, { flashSale }, { merge: true });
        showToast("Website Flash Sale Published!", "success");
    });
}

// 3. Website Top Announcement Bar
const formWebAnn = document.getElementById('form-web-announcement');
if (formWebAnn) {
    formWebAnn.addEventListener('submit', async (e) => {
        e.preventDefault();
        const topAnnouncement = {
            isActive: document.getElementById('web-announcement-active').checked,
            text: document.getElementById('web-announcement-text').value.trim(),
            link: document.getElementById('web-announcement-link').value.trim()
        };

        await setDoc(APP_CONFIG_WEB_REF, { topAnnouncement }, { merge: true });
        showToast("Website Announcement Bar Updated!", "success");
    });
}

// 4. Customer App Popup Alert
const formPopupApp = document.getElementById('form-popup-app');
if (formPopupApp) {
    formPopupApp.addEventListener('submit', async (e) => {
        e.preventDefault();
        const popupNotification = {
            isActive: document.getElementById('popup-is-active-app').checked,
            title: document.getElementById('popup-title-app').value.trim(),
            imageUrl: document.getElementById('popup-image-app').value.trim(),
            message: document.getElementById('popup-message-app').value.trim(),
            actionUrl: ''
        };

        await setDoc(APP_CONFIG_APP_REF, { popupNotification }, { merge: true });
        await setDoc(APP_CONFIG_LEGACY_REF, { popupNotification }, { merge: true });
        showToast("Customer App Popup Alert Updated!", "success");
    });
}

// 5. Customer Web Deals Button
const formDealsWeb = document.getElementById('form-deals-button-web');
if (formDealsWeb) {
    formDealsWeb.addEventListener('submit', async (e) => {
        e.preventDefault();
        const dealsButtonText = document.getElementById('deals-button-text-web').value.trim();
        const dealsButtonAction = document.getElementById('deals-button-action-web').value;

        await setDoc(APP_CONFIG_WEB_REF, { dealsButtonText, dealsButtonAction }, { merge: true });
        showToast("Website Header Deals Button Configured!", "success");
    });
}

// 6. Global Customer Support Config Form
const formSupport = document.getElementById('form-support-config');
if (formSupport) {
    formSupport.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = e.target.querySelector('button');
        btn.disabled = true;
        btn.textContent = 'Updating...';

        try {
            const payload = {
                supportNumber: document.getElementById('support-number').value,
                supportWhatsApp: document.getElementById('support-whatsapp').value,
                supportEmail: document.getElementById('support-email').value,
                isLiveChatEnabled: document.getElementById('live-chat-enabled').checked,
                lastUpdated: Date.now()
            };

            await setDoc(APP_CONFIG_GLOBAL_REF, payload, { merge: true });
            await setDoc(APP_CONFIG_LEGACY_REF, payload, { merge: true }); // Mirror for legacy apps

            showToast('Global support configuration updated!');
        } catch (err) {
            console.error("Error updating support config:", err);
            showToast('Failed to update support config.', 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Update Support Contacts';
        }
    });
}

// 7. Forced App Update Form & Direct APK Upload
const fileInputApk = document.getElementById('apk-file-input');
const btnUploadApk = document.getElementById('btn-upload-apk');
const statusApkUpload = document.getElementById('apk-upload-status');
const progressContainerApk = document.getElementById('apk-progress-container');
const progressBarApk = document.getElementById('apk-progress-bar');

if (fileInputApk && btnUploadApk) {
    fileInputApk.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            if (!file.name.toLowerCase().endsWith('.apk')) {
                showToast("Please select a valid .apk file", "error");
                fileInputApk.value = '';
                btnUploadApk.disabled = true;
                if (statusApkUpload) statusApkUpload.textContent = 'Invalid file type. Please select a .apk file.';
                return;
            }
            btnUploadApk.disabled = false;
            const sizeMb = (file.size / (1024 * 1024)).toFixed(2);
            if (statusApkUpload) {
                statusApkUpload.textContent = `Selected: ${file.name} (${sizeMb} MB). Ready to upload!`;
                statusApkUpload.className = 'text-xs text-amber-400 font-medium';
            }
        } else {
            btnUploadApk.disabled = true;
            if (statusApkUpload) {
                statusApkUpload.textContent = 'Select a .apk file to upload directly';
                statusApkUpload.className = 'text-xs text-slate-400';
            }
        }
    });

    btnUploadApk.addEventListener('click', async () => {
        const file = fileInputApk.files[0];
        if (!file) return;

        const targetApp = document.getElementById('update-target-app')?.value || 'customer';
        const timestamp = Date.now();
        const safeFileName = file.name.replace(/[^a-zA-Z0-9.-]/g, '_');
        const storagePath = `apks/${targetApp}/${timestamp}_${safeFileName}`;
        const storageRef = ref(storage, storagePath);

        btnUploadApk.disabled = true;
        if (progressContainerApk) progressContainerApk.classList.remove('hidden');
        if (progressBarApk) progressBarApk.style.width = '0%';

        const uploadTask = uploadBytesResumable(storageRef, file);

        uploadTask.on('state_changed',
            (snapshot) => {
                const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
                if (progressBarApk) progressBarApk.style.width = `${progress.toFixed(1)}%`;
                if (statusApkUpload) {
                    statusApkUpload.textContent = `Uploading: ${progress.toFixed(0)}% completed...`;
                    statusApkUpload.className = 'text-xs text-brand-400 font-medium';
                }
            },
            (error) => {
                console.error("APK Upload Error:", error);
                showToast("Failed to upload APK: " + error.message, "error");
                btnUploadApk.disabled = false;
                if (statusApkUpload) {
                    statusApkUpload.textContent = 'Upload failed. Please try again.';
                    statusApkUpload.className = 'text-xs text-rose-400 font-medium';
                }
            },
            async () => {
                try {
                    const downloadUrl = await getDownloadURL(uploadTask.snapshot.ref);
                    const urlInput = document.getElementById('update-download-url');
                    if (urlInput) {
                        urlInput.value = downloadUrl;
                        urlInput.classList.add('border-emerald-500');
                    }
                    showToast("APK uploaded successfully! URL inserted into form.", "success");
                    if (statusApkUpload) {
                        statusApkUpload.textContent = '✅ Upload complete! URL automatically filled below.';
                        statusApkUpload.className = 'text-xs text-emerald-400 font-medium';
                    }
                } catch (err) {
                    console.error("Error getting download URL:", err);
                    showToast("Uploaded, but failed to retrieve URL.", "error");
                } finally {
                    fileInputApk.value = '';
                    btnUploadApk.disabled = true;
                }
            }
        );
    });
}

const formAppUpdate = document.getElementById('form-app-update');
if (formAppUpdate) {
    formAppUpdate.addEventListener('submit', async (e) => {
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
        showToast("Forced App Update Rule Published!", "success");
    });
}

// =========================================================================
// PASS TIERS RENDER
// =========================================================================
function renderPassTiers(platform, tiers) {
    const container = document.getElementById(`pass-tiers-container-${platform}`) || document.getElementById('pass-tiers-container');
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
