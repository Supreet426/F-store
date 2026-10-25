import { db } from '../firebase-config.js';
import { collection, onSnapshot, doc, setDoc, deleteDoc } from 'https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js';

const PROMO_COLLECTION = collection(db, 'app_config', 'main', 'promo_cards');

export function listenToPromoCards(onUpdate) {
    onSnapshot(PROMO_COLLECTION, (snapshot) => {
        const cards = snapshot.docs.map(docSnap => ({
            id: docSnap.id,
            ...docSnap.data()
        })).sort((a, b) => (a.priority ?? 99) - (b.priority ?? 99));

        onUpdate(cards);
    });
}

export function renderPromoGrid(cards) {
    const grid = document.getElementById('promo-cards-grid');
    if (!grid) return;

    grid.innerHTML = cards.map(c => `
        <div class="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-lg">
            <div class="p-5 bg-gradient-to-br ${c.bgGradient} text-white flex justify-between items-center">
                <div>
                    <h4 class="font-extrabold text-base">${c.title}</h4>
                    <p class="text-[11px] opacity-90">${c.subtitle}</p>
                </div>
                <img src="${c.imageUrl}" class="w-12 h-12 object-contain">
            </div>
            <div class="p-4 bg-slate-900 border-t border-slate-800 text-right">
                <button onclick="window.deletePromoCard('${c.id}')" class="text-rose-400 font-bold text-xs">Remove</button>
            </div>
        </div>
    `).join('');
}
