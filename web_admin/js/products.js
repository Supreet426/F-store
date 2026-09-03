import { db } from './firebase-config.js';
import {
    collection,
    doc,
    onSnapshot,
    setDoc,
    deleteDoc
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

const PRODUCTS_COLLECTION = collection(db, 'products');

let allProducts = [];

// Real-time listener for Product Catalog
onSnapshot(PRODUCTS_COLLECTION, (snapshot) => {
    allProducts = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    renderProductsTable(allProducts);
    updateProductKPIs(allProducts);
}, (err) => {
    console.error("Error fetching products:", err);
});

function renderProductsTable(products) {
    const tableBody = document.getElementById('products-table-body');
    if (!tableBody) return;

    if (products.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="p-8 text-center text-slate-400">
                    <i class="fa-solid fa-box-open text-2xl mb-2 text-slate-600"></i>
                    <p>No products found in catalog. Click "Add Product" to create one.</p>
                </td>
            </tr>
        `;
        return;
    }

    tableBody.innerHTML = products.map(p => {
        const isStock = p.inStock !== false;
        const discountPct = p.mrp > p.price ? Math.round(((p.mrp - p.price) / p.mrp) * 100) : 0;

        return `
            <tr class="hover:bg-slate-950/60 transition-colors">
                <td class="p-4">
                    <div class="flex items-center gap-3">
                        <img src="${p.imageUrl}" alt="${p.name}" class="w-10 h-10 rounded-lg object-cover bg-slate-800" onerror="this.src='https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=150&q=80'">
                        <div>
                            <p class="font-bold text-white text-sm">${p.name || 'Unnamed Product'}</p>
                            <p class="text-[11px] text-slate-500 font-mono">ID: ${p.id}</p>
                        </div>
                    </div>
                </td>
                <td class="p-4">
                    <span class="badge bg-slate-800 text-slate-300 font-medium">${p.category || 'General'}</span>
                </td>
                <td class="p-4">
                    <div class="font-extrabold text-white text-sm">₹${p.price || 0}</div>
                    ${p.mrp && p.mrp > p.price ? `<div class="text-[11px] text-slate-500 line-through">MRP ₹${p.mrp} (${discountPct}% Off)</div>` : ''}
                </td>
                <td class="p-4 text-xs text-slate-400">${p.quantity || '1 Unit'}</td>
                <td class="p-4">
                    <button onclick="toggleProductStock('${p.id}', ${!isStock})" class="badge cursor-pointer ${isStock ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">
                        <i class="fa-solid ${isStock ? 'fa-check-circle' : 'fa-circle-xmark'} mr-1"></i>
                        ${isStock ? 'In Stock' : 'Out of Stock'}
                    </button>
                </td>
                <td class="p-4 text-right space-x-2">
                    <button onclick="editProduct('${p.id}')" class="p-2 text-slate-400 hover:text-white transition-colors" title="Edit">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="deleteProduct('${p.id}')" class="p-2 text-rose-400 hover:text-rose-300 transition-colors" title="Delete">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function updateProductKPIs(products) {
    document.getElementById('kpi-products').textContent = products.length;
    const inStockCount = products.filter(p => p.inStock !== false).length;
    document.getElementById('kpi-in-stock-subtext').textContent = `${inStockCount} In Stock`;
}

// Search Filter
document.getElementById('search-product')?.addEventListener('input', (e) => {
    const query = e.target.value.toLowerCase().trim();
    const filtered = allProducts.filter(p =>
        (p.name && p.name.toLowerCase().includes(query)) ||
        (p.category && p.category.toLowerCase().includes(query))
    );
    renderProductsTable(filtered);
});

// Toggle Stock
window.toggleProductStock = async function(productId, newStockState) {
    try {
        const prodRef = doc(db, 'products', productId);
        await setDoc(prodRef, { inStock: newStockState }, { merge: true });
        window.showToast(`Product stock updated to ${newStockState ? 'In Stock' : 'Out of Stock'}`, "success");
    } catch (err) {
        console.error("Error toggling stock:", err);
        window.showToast("Failed to update stock state", "error");
    }
};

// Open Product Modal
window.openProductModal = function() {
    document.getElementById('modal-product').classList.remove('hidden');
    document.getElementById('modal-product-title').textContent = 'Add New Product';
    document.getElementById('product-id').value = '';
    document.getElementById('product-name').value = '';
    document.getElementById('product-category').value = '';
    document.getElementById('product-price').value = '';
    document.getElementById('product-mrp').value = '';
    document.getElementById('product-quantity').value = '';
    document.getElementById('product-image').value = '';
    document.getElementById('product-description').value = '';
    document.getElementById('product-in-stock').checked = true;
};

window.closeProductModal = function() {
    document.getElementById('modal-product').classList.add('hidden');
};

// Edit Product
window.editProduct = function(productId) {
    const p = allProducts.find(item => item.id === productId);
    if (!p) return;

    document.getElementById('modal-product').classList.remove('hidden');
    document.getElementById('modal-product-title').textContent = 'Edit Product';
    document.getElementById('product-id').value = p.id;
    document.getElementById('product-name').value = p.name || '';
    document.getElementById('product-category').value = p.category || '';
    document.getElementById('product-price').value = p.price || '';
    document.getElementById('product-mrp').value = p.mrp || '';
    document.getElementById('product-quantity').value = p.quantity || '';
    document.getElementById('product-image').value = p.imageUrl || '';
    document.getElementById('product-description').value = p.description || '';
    document.getElementById('product-in-stock').checked = p.inStock !== false;
};

// Save Product Form
document.getElementById('form-product')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const existingId = document.getElementById('product-id').value;
    const productId = existingId || 'prod_' + Date.now();

    const price = parseFloat(document.getElementById('product-price').value) || 0;
    const mrp = parseFloat(document.getElementById('product-mrp').value) || price;
    const discountPercentage = mrp > price ? Math.round(((mrp - price) / mrp) * 100) : 0;

    const isChecked = document.getElementById('product-in-stock').checked;
    const productData = {
        id: productId,
        name: document.getElementById('product-name').value.trim(),
        category: document.getElementById('product-category').value.trim(),
        price,
        mrp,
        discountPercentage,
        quantity: document.getElementById('product-quantity').value.trim(),
        imageUrl: document.getElementById('product-image').value.trim() || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80',
        description: document.getElementById('product-description').value.trim(),
        inStock: isChecked,
        isInStock: isChecked,
        isAvailable: isChecked,
        isVerified: true,
        stockQuantity: isChecked ? 50 : 0,
        sellerId: 'seller_1',
        storeId: 'store_flash_o_1',
        updatedAt: Date.now()
    };

    try {
        await setDoc(doc(db, 'products', productId), productData, { merge: true });
        window.closeProductModal();
        window.showToast(`Product ${existingId ? 'updated' : 'added'} successfully!`, "success");
    } catch (err) {
        console.error("Error saving product:", err);
        window.showToast("Failed to save product", "error");
    }
});

// Delete Product
window.deleteProduct = async function(productId) {
    if (!confirm("Are you sure you want to delete this product from Customer App catalog?")) return;
    try {
        await deleteDoc(doc(db, 'products', productId));
        window.showToast("Product deleted from catalog", "success");
    } catch (err) {
        console.error("Error deleting product:", err);
        window.showToast("Failed to delete product", "error");
    }
};
