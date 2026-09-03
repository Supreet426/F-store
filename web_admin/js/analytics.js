import { db } from './firebase-config.js';
import {
    collection,
    onSnapshot,
    query,
    where,
    limit
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

// Chart instances
let salesChart = null;
let hourlySpikesChart = null;
let categoryChart = null;
let funnelChart = null;
let paymentChart = null;

// Search tab state
let currentSearchTab = 'top';

document.addEventListener('DOMContentLoaded', () => {
    initAnalyticsModule();
});

function initAnalyticsModule() {
    initCharts();
    renderSearchAnalyticsList();
    listenToFirestoreAnalytics();

    // Attach global window handlers
    window.updateAnalyticsFilters = updateAnalyticsFilters;
    window.refreshAnalyticsData = refreshAnalyticsData;
    window.exportAnalyticsReport = exportAnalyticsReport;
    window.switchSearchTab = switchSearchTab;
}

function initCharts() {
    // Chart 1: Sales & Revenue Trend
    const ctxSales = document.getElementById('chart-sales-trend')?.getContext('2d');
    if (ctxSales) {
        if (salesChart) salesChart.destroy();
        salesChart = new Chart(ctxSales, {
            type: 'line',
            data: {
                labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                datasets: [
                    {
                        label: 'GMV Revenue (₹)',
                        data: [42500, 58900, 64000, 78100, 92000, 118000, 139000],
                        borderColor: '#f43f5e',
                        backgroundColor: 'rgba(244, 63, 94, 0.12)',
                        tension: 0.35,
                        fill: true,
                        pointRadius: 4,
                        yAxisID: 'y'
                    },
                    {
                        label: 'Orders Count',
                        data: [120, 165, 180, 220, 260, 330, 390],
                        borderColor: '#3b82f6',
                        backgroundColor: 'rgba(59, 130, 246, 0.12)',
                        tension: 0.35,
                        fill: false,
                        pointRadius: 3,
                        borderDash: [4, 4],
                        yAxisID: 'y1'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: { color: '#94a3b8', font: { size: 11 }, boxWidth: 12 }
                    },
                    tooltip: { mode: 'index', intersect: false }
                },
                scales: {
                    x: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } },
                    y: {
                        type: 'linear',
                        display: true,
                        position: 'left',
                        grid: { color: 'rgba(255, 255, 255, 0.05)' },
                        ticks: { color: '#94a3b8', callback: (v) => '₹' + (v / 1000) + 'k' }
                    },
                    y1: {
                        type: 'linear',
                        display: true,
                        position: 'right',
                        grid: { drawOnChartArea: false },
                        ticks: { color: '#60a5fa' }
                    }
                }
            }
        });
    }

    // Chart 2: Hourly Order Demand Spikes
    const ctxHourly = document.getElementById('chart-hourly-spikes')?.getContext('2d');
    if (ctxHourly) {
        if (hourlySpikesChart) hourlySpikesChart.destroy();
        hourlySpikesChart = new Chart(ctxHourly, {
            type: 'bar',
            data: {
                labels: ['7 AM', '9 AM', '11 AM', '1 PM', '3 PM', '5 PM', '7 PM', '9 PM', '11 PM'],
                datasets: [{
                    label: 'Orders / Hour',
                    data: [45, 95, 140, 110, 85, 160, 310, 280, 90],
                    backgroundColor: [
                        '#38bdf8', '#38bdf8', '#818cf8', '#818cf8', '#38bdf8', '#818cf8',
                        '#f43f5e', '#f43f5e', '#38bdf8'
                    ],
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { grid: { display: false }, ticks: { color: '#94a3b8' } },
                    y: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } }
                }
            }
        });
    }

    // Chart 3: Category Revenue Share
    const ctxCategory = document.getElementById('chart-category-breakdown')?.getContext('2d');
    if (ctxCategory) {
        if (categoryChart) categoryChart.destroy();
        categoryChart = new Chart(ctxCategory, {
            type: 'doughnut',
            data: {
                labels: ['Snacks & Chaat', 'Dairy & Eggs', 'Beverages', 'Bakery', 'Fresh Produce', 'Instant Food'],
                datasets: [{
                    data: [32, 24, 18, 12, 8, 6],
                    backgroundColor: ['#f43f5e', '#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ec4899'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: { color: '#94a3b8', font: { size: 11 }, boxWidth: 10 }
                    }
                }
            }
        });
    }

    // Chart 4: GA4 E-Commerce Conversion Funnel
    const ctxFunnel = document.getElementById('chart-conversion-funnel')?.getContext('2d');
    if (ctxFunnel) {
        if (funnelChart) funnelChart.destroy();
        funnelChart = new Chart(ctxFunnel, {
            type: 'bar',
            data: {
                labels: ['App Sessions', 'Product Views', 'Cart Added', 'Checkout Init', 'Paid Order'],
                datasets: [{
                    label: 'Conversion Count',
                    data: [25000, 18500, 8400, 3200, 1700],
                    backgroundColor: ['#38bdf8', '#818cf8', '#a855f7', '#f43f5e', '#10b981'],
                    borderRadius: 6
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const val = context.raw;
                                const max = 25000;
                                const pct = ((val / max) * 100).toFixed(1);
                                return ` Users: ${val.toLocaleString()} (${pct}% of total sessions)`;
                            }
                        }
                    }
                },
                scales: {
                    x: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8' } },
                    y: { grid: { display: false }, ticks: { color: '#94a3b8', font: { size: 11 } } }
                }
            }
        });
    }

    // Chart 5: Payment Method Distribution
    const ctxPayment = document.getElementById('chart-payment-methods')?.getContext('2d');
    if (ctxPayment) {
        if (paymentChart) paymentChart.destroy();
        paymentChart = new Chart(ctxPayment, {
            type: 'doughnut',
            data: {
                labels: ['UPI (GPay / PhonePe)', 'Flash Wallet', 'Credit / Debit Card', 'Cash on Delivery'],
                datasets: [{
                    data: [64, 18, 12, 6],
                    backgroundColor: ['#10b981', '#f43f5e', '#6366f1', '#f59e0b'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { color: '#94a3b8', font: { size: 10 }, boxWidth: 10 }
                    }
                }
            }
        });
    }
}

function listenToFirestoreAnalytics() {
    // Real-time listener for Orders to calculate dynamic GMV & AOV
    try {
        onSnapshot(collection(db, 'orders'), (snapshot) => {
            const orders = snapshot.docs.map(doc => doc.data());
            updateKpisFromOrders(orders);
        });
    } catch (e) {
        console.warn('Firestore real-time listener unconfigured, using benchmark defaults.', e);
        updateKpisFromOrders([]);
    }
}

function updateKpisFromOrders(orders) {
    const totalCount = orders.length > 0 ? orders.length : 1720;
    const totalGmv = orders.length > 0
        ? orders.reduce((acc, o) => acc + (parseFloat(o.totalAmount || o.total || 0)), 0)
        : 592500;

    const aov = totalCount > 0 ? Math.round(totalGmv / totalCount) : 344;

    const elGmv = document.getElementById('kpi-gmv');
    if (elGmv) elGmv.textContent = '₹' + totalGmv.toLocaleString('en-IN');

    const elOrders = document.getElementById('kpi-total-orders');
    if (elOrders) elOrders.textContent = totalCount.toLocaleString();

    const elAov = document.getElementById('kpi-aov');
    if (elAov) elAov.textContent = '₹' + aov;

    const elAnalyticsAov = document.getElementById('analytics-aov');
    if (elAnalyticsAov) elAnalyticsAov.textContent = '₹' + aov;
}

function switchSearchTab(tab) {
    currentSearchTab = tab;
    const btnTop = document.getElementById('btn-search-top');
    const btnZero = document.getElementById('btn-search-zero');

    if (tab === 'top') {
        if (btnTop) btnTop.className = "text-xs font-semibold px-2.5 py-1 rounded-lg bg-brand-600 text-white cursor-pointer";
        if (btnZero) btnZero.className = "text-xs font-semibold px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer";
    } else {
        if (btnTop) btnTop.className = "text-xs font-semibold px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300 hover:text-white cursor-pointer";
        if (btnZero) btnZero.className = "text-xs font-semibold px-2.5 py-1 rounded-lg bg-brand-600 text-white cursor-pointer";
    }

    renderSearchAnalyticsList();
}

function renderSearchAnalyticsList() {
    const container = document.getElementById('search-analytics-list');
    if (!container) return;

    const topSearches = [
        { query: 'Aloo Tikki Chaat', count: 1482, conversion: '84.2%', badge: 'High Volume' },
        { query: 'Paneer Roll Instant', count: 1120, conversion: '78.5%', badge: 'Trending' },
        { query: 'Amul Taaza Milk 500ml', count: 990, conversion: '92.1%', badge: 'Essential' },
        { query: 'Thums Up 750ml', count: 810, conversion: '71.0%', badge: 'Beverage' },
        { query: 'Organic Wild Honey', count: 645, conversion: '64.8%', badge: 'Premium' }
    ];

    const zeroResultSearches = [
        { query: 'Eno Lemon 100g', count: 340, impact: 'High OOS Demand', action: 'Add SKU' },
        { query: 'Almond Milk Unsweetened', count: 285, impact: 'Category Gap', action: 'Add SKU' },
        { query: 'Haldiram Soan Papdi', count: 210, impact: 'Festival Spike', action: 'Stock Now' },
        { query: 'Blueberry Greek Yogurt', count: 195, impact: 'Diet Query', action: 'Add SKU' },
        { query: 'Zero Sugar Energy Drink', count: 160, impact: 'Trending Demand', action: 'Procure' }
    ];

    const data = currentSearchTab === 'top' ? topSearches : zeroResultSearches;

    container.innerHTML = data.map(item => {
        if (currentSearchTab === 'top') {
            return `
                <div class="flex items-center justify-between p-3 bg-slate-950/60 rounded-xl border border-slate-800 hover:border-slate-700 transition">
                    <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-brand-500/10 text-brand-400 flex items-center justify-center font-bold text-xs shrink-0">
                            <i class="fa-solid fa-magnifying-glass"></i>
                        </div>
                        <div>
                            <p class="text-sm font-bold text-white flex items-center gap-2">
                                ${item.query}
                                <span class="text-[9px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-medium">${item.badge}</span>
                            </p>
                            <p class="text-[11px] text-slate-400">${item.count.toLocaleString()} searches this period</p>
                        </div>
                    </div>
                    <div class="text-right">
                        <span class="text-xs font-extrabold text-emerald-400">${item.conversion}</span>
                        <p class="text-[10px] text-slate-500">Cart Conv. Rate</p>
                    </div>
                </div>
            `;
        } else {
            return `
                <div class="flex items-center justify-between p-3 bg-slate-950/60 rounded-xl border border-amber-500/20 hover:border-amber-500/40 transition">
                    <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center font-bold text-xs shrink-0">
                            <i class="fa-solid fa-circle-xmark"></i>
                        </div>
                        <div>
                            <p class="text-sm font-bold text-white flex items-center gap-2">
                                ${item.query}
                                <span class="text-[9px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 font-medium">${item.impact}</span>
                            </p>
                            <p class="text-[11px] text-slate-400">${item.count} unfulfilled customer queries</p>
                        </div>
                    </div>
                    <button class="text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white px-2.5 py-1 rounded-lg transition cursor-pointer">
                        <i class="fa-solid fa-plus text-[10px] mr-1"></i>${item.action}
                    </button>
                </div>
            `;
        }
    }).join('');
}

function updateAnalyticsFilters() {
    const tf = document.getElementById('analytics-timeframe')?.value;
    const hub = document.getElementById('analytics-hub-filter')?.value;
    const cat = document.getElementById('analytics-category-filter')?.value;

    console.log(`[Analytics Filter Changed] Timeframe: ${tf}, Hub: ${hub}, Category: ${cat}`);

    // Update sales chart with multiplier based on timeframe
    if (salesChart) {
        let multiplier = 1;
        if (tf === 'today') multiplier = 0.2;
        if (tf === '30d') multiplier = 3.5;
        if (tf === 'ytd') multiplier = 12;

        const baseSales = [42500, 58900, 64000, 78100, 92000, 118000, 139000];
        salesChart.data.datasets[0].data = baseSales.map(v => Math.round(v * multiplier));
        salesChart.update();
    }

    if (window.showToast) {
        window.showToast(`Analytics view updated (${tf.toUpperCase()} - ${hub.toUpperCase()})`, 'info');
    }
}

function refreshAnalyticsData() {
    initCharts();
    renderSearchAnalyticsList();
    if (window.showToast) {
        window.showToast('Analytics & Real-time metrics resynced!', 'success');
    }
}

function exportAnalyticsReport() {
    const csvContent = "data:text/csv;charset=utf-8,"
        + "Metric,Value,Benchmark,Status\n"
        + "GMV Revenue,₹592500,₹500000+,Passing\n"
        + "Total Orders,1720,1500+,Passing\n"
        + "Average Order Value (AOV),₹344,₹350,On Target\n"
        + "Delivery SLA (Min),11.2,12.0,Passing\n"
        + "30D Repeat Rate,74.2%,70.0%+,Passing\n"
        + "Out-Of-Stock Loss,₹14280,<₹20000,Passing\n";

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", "FlashO_Executive_Analytics_Report.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    if (window.showToast) {
        window.showToast('Analytics report exported as CSV!', 'success');
    }
}
