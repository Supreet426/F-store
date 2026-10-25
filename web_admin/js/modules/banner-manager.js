import { db } from '../firebase-config.js';
import { collection, onSnapshot, doc, setDoc, deleteDoc } from 'https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js';

const BANNERS_COLLECTION = collection(db, 'app_config', 'main', 'banners');
let heroBannersList = [];

export function listenToBanners(onUpdate) {
    onSnapshot(BANNERS_COLLECTION, (snapshot) => {
        heroBannersList = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        })).sort((a, b) => (a.priority ?? 99) - (b.priority ?? 99));

        onUpdate(heroBannersList);
    });
}

export async function deleteBanner(id) {
    if (!confirm("Are you sure you want to permanently delete this banner?")) return;
    try {
        await deleteDoc(doc(db, 'app_config', 'main', 'banners', id));
        if (window.showToast) window.showToast("Banner deleted successfully", "success");
    } catch (err) {
        console.error("Error deleting banner:", err);
    }
}

export function renderBannersGrid(banners) {
    const grid = document.getElementById('hero-banners-grid');
    if (!grid) return;

    grid.innerHTML = banners.map(b => `
        <div class="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-lg group">
            <div class="p-5 bg-gradient-to-r ${b.bgGradient || 'from-indigo-600 to-blue-700'} text-white min-h-[140px] flex justify-between items-center">
                <div class="space-y-1">
                    <h3 class="font-extrabold text-lg">${b.title}</h3>
                    <p class="text-xs opacity-90">${b.subtitle}</p>
                </div>
                <img src="${b.imageUrl}" class="w-16 h-16 object-cover rounded-xl shadow-md">
            </div>
            <div class="p-4 bg-slate-900 border-t border-slate-800 flex justify-between items-center">
                <span class="text-[10px] font-bold text-slate-400 uppercase">Priority #${b.priority || 1}</span>
                <div class="flex gap-2">
                    <button onclick="window.deleteHeroBanner('${b.id}')" class="text-rose-400 hover:text-rose-300 text-xs font-bold">Delete</button>
                </div>
            </div>
        </div>
    `).join('');
}
