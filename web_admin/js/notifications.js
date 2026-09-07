import { db } from './firebase-config.js';
import { showToast } from './ui.js';
import {
    collection,
    addDoc,
    query,
    orderBy,
    limit,
    onSnapshot,
    serverTimestamp,
    doc,
    updateDoc,
    getDoc,
    deleteDoc,
    getDocs
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

// DOM Elements - Push
const formPush = document.getElementById('form-broadcast-push');
const historyList = document.getElementById('notification-history-list');
const previewTitle = document.getElementById('preview-title');
const previewBody = document.getElementById('preview-body');
const previewImage = document.getElementById('preview-image');
const previewImageContainer = document.getElementById('preview-image-container');

// DOM Elements - Popup
const formPopup = document.getElementById('form-popup-broadcast');
const popupTargetLabel = document.getElementById('popup-target-label');
const popupActive = document.getElementById('notif-popup-active');
const popupTitle = document.getElementById('notif-popup-title');
const popupMessage = document.getElementById('notif-popup-message');
const popupImage = document.getElementById('notif-popup-image');
const popupAction = document.getElementById('notif-popup-action');

let currentPopupTarget = 'app'; // 'app' or 'web'

// =========================================================================
// PUSH NOTIFICATION BROADCAST
// =========================================================================

if (formPush) {
    formPush.addEventListener('submit', async (e) => {
        e.preventDefault();

        const target = document.getElementById('push-target').value;
        const title = document.getElementById('push-title').value;
        const body = document.getElementById('push-body').value;
        const imageUrl = document.getElementById('push-image').value;
        const actionUrl = document.getElementById('push-action').value;
        const saveHistory = document.getElementById('push-save-history').checked;

        try {
            const notification = {
                type: 'BROADCAST',
                target: target,
                title: title,
                body: body,
                imageUrl: imageUrl || null,
                actionUrl: actionUrl || null,
                timestamp: Date.now(),
                status: 'pending',
                saveToHistory: saveHistory
            };

            await addDoc(collection(db, 'notifications'), notification);
            showToast('Broadcast Push Queued Successfully', 'success');
            formPush.reset();
            updatePreview();
        } catch (error) {
            console.error("Error sending push:", error);
            showToast('Failed to queue broadcast', 'error');
        }
    });

    // Real-time Preview Logic
    ['push-title', 'push-body', 'push-image'].forEach(id => {
        document.getElementById(id).addEventListener('input', updatePreview);
    });
}

function updatePreview() {
    const title = document.getElementById('push-title').value || "Notification Title";
    const body = document.getElementById('push-body').value || "Message body will appear here...";
    const image = document.getElementById('push-image').value;

    previewTitle.textContent = title;
    previewBody.textContent = body;

    if (image) {
        previewImage.src = image;
        previewImageContainer.classList.remove('hidden');
    } else {
        previewImageContainer.classList.add('hidden');
    }
}

// =========================================================================
// POPUP BROADCAST ALERTS
// =========================================================================

window.switchPopupTab = function(target) {
    currentPopupTarget = target;

    // UI Update
    const btnApp = document.getElementById('popup-tab-app');
    const btnWeb = document.getElementById('popup-tab-web');

    if (target === 'app') {
        btnApp.className = 'popup-tab-btn active px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-wider transition-all bg-brand-600 text-white shadow-md';
        btnWeb.className = 'popup-tab-btn px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-wider transition-all bg-slate-800 text-slate-400 hover:text-white';
        popupTargetLabel.textContent = 'Target: Mobile App';
    } else {
        btnWeb.className = 'popup-tab-btn active px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-wider transition-all bg-brand-600 text-white shadow-md';
        btnApp.className = 'popup-tab-btn px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-wider transition-all bg-slate-800 text-slate-400 hover:text-white';
        popupTargetLabel.textContent = 'Target: Web Portal';
    }

    loadPopupConfig(target);
};

async function loadPopupConfig(target) {
    const docPath = target === 'app' ? 'config_app' : 'config_web';
    const docRef = doc(db, 'app_config', docPath);

    try {
        const docSnap = await getDoc(docRef);
        if (docSnap.exists()) {
            const data = docSnap.data().popupNotification || {};
            popupActive.checked = data.isActive || false;
            popupTitle.value = data.title || '';
            popupMessage.value = data.message || '';
            popupImage.value = data.imageUrl || '';
            popupAction.value = data.actionUrl || '';
        } else {
            formPopup.reset();
        }
    } catch (error) {
        console.error("Error loading popup config:", error);
    }
}

if (formPopup) {
    formPopup.addEventListener('submit', async (e) => {
        e.preventDefault();

        const docPath = currentPopupTarget === 'app' ? 'config_app' : 'config_web';
        const docRef = doc(db, 'app_config', docPath);

        const popupData = {
            popupNotification: {
                isActive: popupActive.checked,
                title: popupTitle.value,
                message: popupMessage.value,
                imageUrl: popupImage.value,
                actionUrl: popupAction.value,
                lastUpdated: Date.now()
            }
        };

        try {
            await updateDoc(docRef, popupData);
            showToast(`Popup Alert Updated for ${currentPopupTarget.toUpperCase()}`, 'success');
        } catch (error) {
            console.error("Error updating popup:", error);
            showToast('Failed to update popup', 'error');
        }
    });
}

// =========================================================================
// BROADCAST HISTORY
// =========================================================================

function initHistoryListener() {
    if (!historyList) return;

    const q = query(
        collection(db, 'notifications'),
        orderBy('timestamp', 'desc'),
        limit(20)
    );

    onSnapshot(q, (snapshot) => {
        historyList.innerHTML = '';
        if (snapshot.empty) {
            historyList.innerHTML = '<div class="p-8 text-center text-slate-500 italic text-xs">No recent broadcasts found</div>';
            return;
        }

        snapshot.forEach(docSnap => {
            const data = docSnap.data();
            const date = new Date(data.timestamp).toLocaleString();

            const card = document.createElement('div');
            card.className = 'p-3 bg-slate-950/40 border border-slate-800 rounded-xl space-y-2';
            card.innerHTML = `
                <div class="flex justify-between items-start">
                    <span class="text-[9px] font-bold text-slate-500 uppercase tracking-tighter">${date}</span>
                    <span class="text-[9px] font-black ${data.status === 'sent' ? 'text-emerald-500' : 'text-amber-500'} uppercase">${data.status}</span>
                </div>
                <h4 class="text-xs font-bold text-white truncate">${data.title}</h4>
                <p class="text-[10px] text-slate-400 line-clamp-2">${data.body}</p>
                ${data.imageUrl ? `<div class="h-10 w-full rounded bg-slate-800 overflow-hidden"><img src="${data.imageUrl}" class="w-full h-full object-cover opacity-50"></div>` : ''}
            `;
            historyList.appendChild(card);
        });
    });
}

window.clearNotificationHistory = async function() {
    if (!confirm("Are you sure you want to clear the notification history? This will delete records from Firestore.")) return;

    try {
        const q = query(collection(db, 'notifications'), limit(50));
        const querySnapshot = await getDocs(q);

        const deletePromises = [];
        querySnapshot.forEach((docSnap) => {
            deletePromises.push(deleteDoc(docSnap.ref));
        });

        await Promise.all(deletePromises);
        showToast('History Cleared', 'success');
    } catch (error) {
        console.error("Error clearing history:", error);
        showToast('Failed to clear history', 'error');
    }
};

// =========================================================================
// INITIALIZATION
// =========================================================================

document.addEventListener('DOMContentLoaded', () => {
    initHistoryListener();
    loadPopupConfig('app');
});
