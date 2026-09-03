import { db } from './firebase-config.js';
import {
    collection,
    onSnapshot
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

let leafletMap = null;
let markersGroup = null;

export function initLiveMap() {
    const mapDiv = document.getElementById('live-map-container');
    if (!mapDiv) return;

    if (!leafletMap && window.L) {
        leafletMap = L.map('live-map-container').setView([28.6139, 77.2090], 12);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(leafletMap);

        markersGroup = L.layerGroup().addTo(leafletMap);
        subscribeToMapEntities();
    }
}

function subscribeToMapEntities() {
    if (!leafletMap || !markersGroup) return;

    onSnapshot(collection(db, 'dark_stores'), (snapshot) => {
        markersGroup.clearLayers();

        snapshot.docs.forEach(docSnap => {
            const ds = docSnap.data();
            if (ds.latitude && ds.longitude) {
                const storeMarker = L.circleMarker([ds.latitude, ds.longitude], {
                    radius: 10,
                    fillColor: '#8b5cf6',
                    color: '#ffffff',
                    weight: 2,
                    opacity: 1,
                    fillOpacity: 0.9
                });

                storeMarker.bindPopup(`
                    <div class="text-slate-900 font-sans p-1">
                        <strong class="text-sm">${ds.name || 'Dark Store'}</strong><br>
                        <span class="text-xs text-slate-600">${ds.area || 'Hub'}</span>
                    </div>
                `);

                markersGroup.addLayer(storeMarker);
            }
        });
    });

    onSnapshot(collection(db, 'riders'), (snapshot) => {
        snapshot.docs.forEach(docSnap => {
            const r = docSnap.data();
            if (r.latitude && r.longitude && r.isOnline) {
                const riderMarker = L.circleMarker([r.latitude, r.longitude], {
                    radius: 8,
                    fillColor: '#0ea5e9',
                    color: '#ffffff',
                    weight: 2,
                    opacity: 1,
                    fillOpacity: 0.9
                });

                riderMarker.bindPopup(`
                    <div class="text-slate-900 font-sans p-1">
                        <strong class="text-sm">🛵 Rider: ${r.name || 'Partner'}</strong><br>
                        <span class="text-xs text-slate-600">Status: ${r.activeOrderId ? 'Delivering Order' : 'Available'}</span>
                    </div>
                `);

                markersGroup.addLayer(riderMarker);
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const mapTabBtn = document.querySelector('[data-tab="map"]');
    if (mapTabBtn) {
        mapTabBtn.addEventListener('click', () => {
            setTimeout(initLiveMap, 200);
        });
    }
});
