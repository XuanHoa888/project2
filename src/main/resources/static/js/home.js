/**
 * home.js - Logic trang chủ XH Store
 * Bao gồm: dữ liệu sản phẩm, flash sale, mẫu hot, gian hàng chủ đề, quản lý sale admin
 */

// DỮ LIỆU SẢN PHẨM MẪU

let PRODUCTS = [];

// TẢI SẢN PHẨM TỪ DATABASE

async function loadProductsFromDB() {
    try {
        const response = await fetch('/api/products');
        if (!response.ok) throw new Error('Cannot fetch products');
        const dbProducts = await response.json();
        
        // Cập nhật số lượng sản phẩm thật cho mỗi danh mục hiển thị trên UI
        updateCategoryCounts(dbProducts);
        
        const catMap = {
            'Thời Trang': 'fashion',
            'Mỹ Phẩm': 'beauty',
            'Trang Sức': 'jewelry',
            'Phụ Kiện': 'accessories',
            'Chăm Sóc Da': 'beauty'
        };

        const boothMap = {
            'Thời Trang': 'office',
            'Mỹ Phẩm': 'beauty',
            'Chăm Sóc Da': 'beauty',
            'Trang Sức': 'jewel',
            'Phụ Kiện': 'jewel'
        };

        PRODUCTS = dbProducts.map(p => {
            const catName = p.category ? p.category.name : '';
            const category = catMap[catName] || 'fashion';
            const booth = boothMap[catName] || 'office';
            
            const priceNum = p.price.doubleValue ? p.price.doubleValue : Number(p.price);
            const originalPrice = p.originalPrice ? (p.originalPrice.doubleValue ? p.originalPrice.doubleValue : Number(p.originalPrice)) : priceNum;

            // Gán badges dựa trên lượng bán
            const badges = [];
            if (p.soldCount > 5000) badges.push('bestseller');
            else if (p.soldCount > 1000) badges.push('hot');
            else badges.push('new');
            
            if (originalPrice > priceNum) badges.push('sale');

            const soldPercent = (p.soldCount + p.stock) > 0 ? Math.min(95, Math.round((p.soldCount / (p.soldCount + p.stock)) * 100)) : 0;

            return {
                id: p.id,
                category: category,
                brand: p.description && p.description.includes('thương hiệu') ? p.description.split('thương hiệu')[1].trim() : 'XH Store',
                name: p.name,
                price: priceNum,
                originalPrice: originalPrice,
                image: p.imageUrl || 'images/cosmetics_office_set_1779642650180.png',
                rating: (p.rating !== undefined && p.rating !== null) ? p.rating : 0.0,
                reviews: p.soldCount > 0 ? Math.round(p.soldCount * 0.3) : 0,
                sold: p.soldCount,
                badges: badges,
                isHot: p.soldCount > 1000 || p.id % 2 === 0,
                isFlashSale: p.id % 2 !== 0,
                booth: booth,
                soldPercent: soldPercent
            };
        });

        // Re-render UI components once products are loaded
        renderFlashSale();
        renderBestSeller();
        renderHotGrid('all');
        renderBooth('office', 'booth-office-products', 3);
        renderBooth('beauty', 'booth-beauty-products', 3);
        renderBooth('jewel', 'booth-jewel-products', 3);
        renderHotManageGrid();
        
        // Check URL parameters for search or category filtering
        const urlParams = new URLSearchParams(window.location.search);
        const searchParam = urlParams.get('search');
        const catParam = urlParams.get('cat');
        if (searchParam) {
            const searchInput = document.getElementById('search-input');
            if (searchInput) searchInput.value = searchParam;
            setTimeout(() => {
                doSearch();
            }, 100);
        } else if (catParam) {
            setTimeout(() => {
                filterByCategory(catParam);
            }, 100);
        }
        
    } catch (error) {
        console.error('Error loading products from DB:', error);
        showToast('Không thể tải dữ liệu sản phẩm từ máy chủ.', 'error');
    }
}

// DỮ LIỆU QUẢN LÝ SALE

let SALES = [
    {
        id: 1, name: 'Flash Sale Mỗi Ngày', discount: 30, status: 'active',
        categories: ['fashion', 'beauty'],
        startTime: '2026-06-01T08:00', endTime: '2026-06-30T23:59',
        totalProducts: 48, soldProducts: 35
    },
    {
        id: 2, name: 'Sale Mùa Hè - Thời Trang', discount: 50, status: 'active',
        categories: ['fashion'],
        startTime: '2026-06-15T00:00', endTime: '2026-07-15T23:59',
        totalProducts: 120, soldProducts: 88
    },
    {
        id: 3, name: 'Mega Sale Phụ Kiện Tháng 7', discount: 40, status: 'upcoming',
        categories: ['jewelry', 'accessories'],
        startTime: '2026-07-01T00:00', endTime: '2026-07-07T23:59',
        totalProducts: 65, soldProducts: 0
    },
    {
        id: 4, name: 'Clearance Sale Tháng 5', discount: 70, status: 'ended',
        categories: ['fashion', 'beauty', 'jewelry', 'accessories'],
        startTime: '2026-05-01T00:00', endTime: '2026-05-31T23:59',
        totalProducts: 200, soldProducts: 180
    },
];

let VOUCHERS = [
    { code: 'SUMMER30', discount: '30%', minOrder: 199000, desc: 'Giảm 30% cho đơn từ 199K', used: 234, limit: 500 },
    { code: 'NEWUSER', discount: '50K', minOrder: 99000, desc: 'Tặng 50K cho khách mới', used: 890, limit: 1000 },
    { code: 'VIP20', discount: '20%', minOrder: 299000, desc: 'Dành riêng cho VIP member', used: 45, limit: 100 },
    { code: 'FLASH15', discount: '15%', minOrder: 0, desc: 'Flash sale không giới hạn đơn', used: 1200, limit: 2000 },
];

// HERO CAROUSEL

let currentSlide = 0;
const slides = document.querySelectorAll('.hero-slide');
const dots = document.querySelectorAll('.hero-dot');
let autoplayTimer;

function goToSlide(index) {
    slides[currentSlide].classList.remove('active');
    dots[currentSlide].classList.remove('active');
    currentSlide = (index + slides.length) % slides.length;
    slides[currentSlide].classList.add('active');
    dots[currentSlide].classList.add('active');
    resetAutoplay();
}
function nextSlide() { goToSlide(currentSlide + 1); }
function prevSlide() { goToSlide(currentSlide - 1); }
function resetAutoplay() {
    clearInterval(autoplayTimer);
    autoplayTimer = setInterval(nextSlide, 5000);
}
resetAutoplay();

// COUNTDOWN TIMER

function startCountdown(endDate, hourEl, minEl, secEl) {
    function update() {
        const now = new Date();
        const diff = Math.max(0, endDate - now);
        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        if (hourEl) hourEl.textContent = String(h).padStart(2, '0');
        if (minEl)  minEl.textContent  = String(m).padStart(2, '0');
        if (secEl)  secEl.textContent  = String(s).padStart(2, '0');
    }
    update();
    return setInterval(update, 1000);
}

const flashEnd = new Date(Date.now() + 5 * 3600000 + 23 * 60000 + 45000);
startCountdown(flashEnd,
    document.getElementById('cd-hours'),
    document.getElementById('cd-mins'),
    document.getElementById('cd-secs')
);
startCountdown(flashEnd,
    document.getElementById('scd-h'),
    document.getElementById('scd-m'),
    document.getElementById('scd-s')
);

// RENDER PRODUCT CARD

function formatPrice(p) {
    return '₫' + Number(p).toLocaleString('vi-VN');
}

function calcDiscount(price, original) {
    if (!original || original <= price) return 0;
    return Math.round((1 - price / original) * 100);
}

function renderCard(product, compact = false) {
    const disc = calcDiscount(product.price, product.originalPrice);
    const badgeHtml = (product.badges || []).map(b => {
        const labels = { sale: 'SALE', hot: '🔥 HOT', new: 'MỚI', bestseller: '⭐ BẾN CHẠY' };
        const classes = { sale: 'badge-sale', hot: 'badge-hot', new: 'badge-new', bestseller: 'badge-bestseller' };
        return `<span class="badge ${classes[b] || 'badge-sale'}">${labels[b] || b}</span>`;
    }).slice(0, 1).join('');

    const soldBarHtml = (product.soldPercent !== undefined && product.soldPercent !== null)
        ? `<div class="product-sold-bar">
               <div class="sold-bar-track"><div class="sold-bar-fill" style="width:${product.soldPercent}%"></div></div>
               <span class="sold-label">Đã bán ${product.sold > 999 ? (product.sold/1000).toFixed(1)+'k' : product.sold}</span>
           </div>` : '';

    return `
    <div class="product-card" data-category="${product.category}" data-hot="${product.isHot}">
        <div class="product-img-wrap" onclick="window.location.href='product.html?id=${product.id}'" style="cursor: pointer;">
            <img src="${product.image}" alt="${product.name}" loading="lazy">
            ${badgeHtml}
            <div class="product-wish" onclick="toggleWish(event, ${product.id})" title="Yêu thích">
                <i class="fa-regular fa-heart"></i>
            </div>
        </div>
        <div class="product-info">
            <div class="product-brand">${product.brand}</div>
            <div class="product-name" onclick="window.location.href='product.html?id=${product.id}'" style="cursor: pointer;">${product.name}</div>
            <div class="product-rating">
                <span class="stars">${'★'.repeat(Math.floor(product.rating))}${'☆'.repeat(5 - Math.floor(product.rating))}</span>
                <span class="review-count">(${product.reviews.toLocaleString()})</span>
            </div>
            <div class="product-pricing">
                <span class="price-sale">${formatPrice(product.price)}</span>
                ${product.originalPrice && product.originalPrice > product.price ? `<span class="price-original">${formatPrice(product.originalPrice)}</span>` : ''}
                ${disc > 0 ? `<span class="discount-pct">-${disc}%</span>` : ''}
            </div>
            ${soldBarHtml}
            <button class="add-to-cart-btn" onclick="addToCart(event, ${product.id}, '${product.name.replace(/'/g,"\\'")}', ${product.price}, '${product.image}')">
                <i class="fa-solid fa-cart-plus"></i> Thêm giỏ hàng
            </button>
        </div>
    </div>`;
}

// RENDER SECTIONS

// Flash Sale: 4 sản phẩm có isFlashSale = true
function renderFlashSale() {
    const el = document.getElementById('flash-sale-grid');
    if (!el) return;
    const items = PRODUCTS.filter(p => p.isFlashSale).slice(0, 5);
    el.innerHTML = items.map(p => renderCard(p)).join('');
}

// Hot grid: tất cả sản phẩm, filter by tab
function renderHotGrid(filterCat = 'all') {
    const el = document.getElementById('hot-grid');
    if (!el) return;
    const items = filterCat === 'all'
        ? PRODUCTS.filter(p => p.isHot).slice(0, 8)
        : PRODUCTS.filter(p => p.isHot && p.category === filterCat).slice(0, 8);
    el.innerHTML = items.length
        ? items.map(p => renderCard(p)).join('')
        : '<p style="color:var(--text-gray);padding:20px;grid-column:1/-1">Không có sản phẩm hot trong danh mục này.</p>';
}

function renderBestSeller() {
    const el = document.getElementById('best-seller-grid');
    if (!el) return;
    const items = [...PRODUCTS].sort((a, b) => (b.sold || 0) - (a.sold || 0)).slice(0, 4);
    el.innerHTML = items.length
        ? items.map(p => renderCard(p)).join('')
        : '<p style="color:var(--text-gray);padding:20px;grid-column:1/-1">Chưa có dữ liệu best seller.</p>';
}

// Booth products
function renderBooth(boothId, targetId, maxItems = 3) {
    const el = document.getElementById(targetId);
    if (!el) return;
    const items = PRODUCTS.filter(p => p.booth === boothId).slice(0, maxItems);
    el.innerHTML = items.map(p => renderCard(p, true)).join('');
}

// FILTER FUNCTIONS

function filterHot(btn, cat) {
    document.querySelectorAll('#hot-filters .ftab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    renderHotGrid(cat);
}

function filterByCategory(cat) {
    scrollToSection('hot-section');
    // find & click the right tab
    const tabMap = { fashion: 'fashion', beauty: 'beauty', accessories: 'accessories', jewelry: 'jewelry', skincare: 'beauty' };
    const targetCat = tabMap[cat] || 'all';
    const tabs = document.querySelectorAll('#hot-filters .ftab');
    tabs.forEach(t => {
        if (t.dataset.filter === targetCat) {
            filterHot(t, targetCat);
        }
    });
}

function scrollToSection(id) {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// WISHLIST

function toggleWish(e, productId) {
    e.preventDefault(); e.stopPropagation();
    const btn = e.currentTarget;
    const item = PRODUCTS.find(p => p.id === productId);
    const wishlist = layWishlist();
    const idx = wishlist.findIndex(p => p.id === productId);
    if (idx >= 0) {
        wishlist.splice(idx, 1);
        btn.classList.remove('liked');
        btn.innerHTML = '<i class="fa-regular fa-heart"></i>';
        showToast('Đã xóa khỏi yêu thích', 'warning');
    } else {
        wishlist.push({ id: productId, name: item?.name, price: item?.price, image: item?.image });
        btn.classList.add('liked');
        btn.innerHTML = '<i class="fa-solid fa-heart"></i>';
        showToast('Đã thêm vào yêu thích!', 'success');
    }
    luuWishlist(wishlist);
}

function toggleWishlist() {
    window.location.href = 'profile.html?tab=wishlist';
}

// TOAST NOTIFICATION (override main.js)

function showToast(msg, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) { hienToast(msg); return; }
    const toast = document.createElement('div');
    toast.className = `toast-msg ${type}`;
    const icons = { success: 'fa-check-circle', error: 'fa-circle-xmark', warning: 'fa-triangle-exclamation' };
    toast.innerHTML = `<i class="fa-solid ${icons[type] || icons.success}"></i> ${msg}`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.animation = 'toastOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

// Override hienToast from main.js
function hienToast(msg) { showToast(msg, 'success'); }

// ADMIN PANEL

function toggleAdminPanel() {
    const panel = document.getElementById('admin-panel');
    const overlay = document.getElementById('admin-overlay');
    panel.classList.toggle('open');
    overlay.classList.toggle('active');
}

function switchAdminTab(btn, tabId) {
    document.querySelectorAll('.atab').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.admin-tab-content').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(tabId).classList.add('active');
}

async function renderAdminSaleList() {
    const el = document.getElementById('admin-sale-list');
    if (!el) return;
    try {
        const res = await fetch('/api/admin/booths');
        if (!res.ok) throw new Error('Cannot fetch booths');
        const dbBooths = await res.json();
        
        el.innerHTML = dbBooths.map(b => {
            const saleTag = b.saleTag || 'Không có';
            const subtitle = b.bannerSubtitle || '—';
            return `
            <div class="sale-item" id="booth-${b.id}" style="border: 1px solid #e2e8f0; padding:12px; border-radius:6px; margin-bottom:10px;">
                <div class="sale-item-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <span class="sale-item-name" style="font-weight:700; color:#1e293b;">${b.name} (${b.boothKey})</span>
                    <span class="sale-status status-active" style="background:#e8f5e9; color:#2e7d32; font-weight:600; padding:2px 8px; border-radius:20px; font-size:0.75rem;">🟢 Đang mở</span>
                </div>
                <div class="sale-item-info" style="display:flex; flex-direction:column; gap:4px; margin: 8px 0; font-size:0.82rem; color:#64748b;">
                    <span><i class="fa-solid fa-percent" style="color:var(--primary, #ee4d2d); margin-right:6px;"></i> Nhãn Khuyến Mãi: <strong>${saleTag}</strong></span>
                    <span><i class="fa-solid fa-heading" style="color:#3498db; margin-right:6px;"></i> Banner Title: ${b.bannerTitle || '—'}</span>
                    <span><i class="fa-solid fa-quote-left" style="color:#27ae60; margin-right:6px;"></i> Banner Subtitle: ${subtitle}</span>
                    <span><i class="fa-solid fa-info-circle" style="color:#7f8c8d; margin-right:6px;"></i> Mô tả: ${b.description || '—'}</span>
                </div>
                <div class="sale-item-actions" style="display:flex; gap:6px;">
                    <button class="btn-edit" onclick="openEditBooth(${b.id}, '${b.boothKey}', '${b.name.replace(/'/g, "\\'")}', '${saleTag.replace(/'/g, "\\'")}', '${(b.bannerTitle||'').replace(/'/g, "\\'")}', '${(b.bannerSubtitle||'').replace(/'/g, "\\'")}', '${(b.description||'').replace(/'/g, "\\'")}', '${(b.bannerImageUrl||'').replace(/'/g, "\\'")}')" style="background:#e3f2fd; color:#1565c0; border:none; padding:5px 10px; border-radius:4px; cursor:pointer; font-weight:600; font-size:0.8rem;"><i class="fa-solid fa-pen"></i> Thiết Lập Sale</button>
                </div>
            </div>`;
        }).join('');
    } catch (err) {
        console.error('Error fetching booths in admin list:', err);
        el.innerHTML = `<p style="font-size:0.85rem;color:var(--text-gray);padding:10px;">Vui lòng đăng nhập Admin để quản lý sale.</p>`;
    }
}

async function renderVoucherList() {
    const el = document.getElementById('voucher-list-admin');
    if (!el) return;
    try {
        const res = await fetch('/api/admin/vouchers');
        if (!res.ok) throw new Error('Cannot fetch vouchers');
        const dbVouchers = await res.json();
        el.innerHTML = dbVouchers.map(v => {
            const desc = v.discountType === 'PERCENT'
                ? `Giảm ${v.discountValue}% cho đơn từ ${formatPrice(v.minOrderValue)}`
                : `Giảm ${formatPrice(v.discountValue)} cho đơn từ ${formatPrice(v.minOrderValue)}`;
            const discountDisplay = v.discountType === 'PERCENT'
                ? `${v.discountValue}%`
                : `${Math.round(v.discountValue / 1000)}K`;
            return `
            <div class="voucher-item" style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid #f1f5f9;">
                <div>
                    <div class="voucher-code" style="font-weight:700; color:#1e293b;">${v.code}</div>
                    <div class="voucher-desc" style="font-size:0.8rem; color:#64748b;">${desc}</div>
                    <div style="font-size:0.72rem;color:var(--text-gray);margin-top:4px">
                        Đã dùng: ${v.usedCount}/${v.maxUses || '∞'} | Hạn: ${v.expiryDate ? new Date(v.expiryDate).toLocaleDateString('vi-VN') : 'vô hạn'}
                    </div>
                </div>
                <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
                    <div class="voucher-discount" style="font-weight:700; color:#ee4d2d;">
                        ${discountDisplay}
                        <small style="font-size:0.7rem; color:#94a3b8; font-weight:normal;">${v.discountType === 'PERCENT' ? 'giảm' : 'giảm thêm'}</small>
                    </div>
                    <button class="btn-delete" style="background:#fee2e2; color:#b91c1c; border:none; padding:3px 8px; border-radius:4px; font-size:0.75rem; cursor:pointer;" onclick="deleteVoucher(${v.id})"><i class="fa-solid fa-trash"></i> Xóa</button>
                </div>
            </div>`;
        }).join('');
    } catch (err) {
        console.error('Error fetching vouchers:', err);
        el.innerHTML = `<p style="font-size:0.85rem;color:var(--text-gray);padding:10px;">Vui lòng đăng nhập Admin để xem danh sách voucher.</p>`;
    }
}

function renderHotManageGrid() {
    const el = document.getElementById('hot-manage-grid');
    if (!el) return;
    const hotProducts = PRODUCTS.filter(p => p.isHot);
    el.innerHTML = hotProducts.map(p => `
    <div class="hot-manage-item">
        <img src="${p.image}" alt="${p.name}">
        <div class="hot-manage-info">
            <div class="hot-manage-name">${p.name}</div>
            <div class="hot-manage-price">${formatPrice(p.price)}</div>
        </div>
        <button class="hot-toggle on" id="hot-toggle-${p.id}" onclick="toggleHotProduct(${p.id}, this)"></button>
    </div>`).join('');
}

function openEditBooth(id, key, name, tag, title, subtitle, desc, image) {
    document.getElementById('edit-booth-id').value = id;
    document.getElementById('edit-booth-key').value = key;
    document.getElementById('edit-booth-name').value = name;
    document.getElementById('edit-booth-tag').value = tag;
    document.getElementById('edit-booth-title').value = title;
    document.getElementById('edit-booth-subtitle').value = subtitle;
    document.getElementById('edit-booth-desc').value = desc;
    document.getElementById('edit-booth-image').value = image;
    document.getElementById('modal-edit-sale').classList.add('open');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('open');
}

async function saveBoothConfig() {
    const id = document.getElementById('edit-booth-id').value;
    const key = document.getElementById('edit-booth-key').value;
    const name = document.getElementById('edit-booth-name').value.trim();
    const tag = document.getElementById('edit-booth-tag').value.trim();
    const title = document.getElementById('edit-booth-title').value.trim();
    const subtitle = document.getElementById('edit-booth-subtitle').value.trim();
    const desc = document.getElementById('edit-booth-desc').value.trim();
    const image = document.getElementById('edit-booth-image').value.trim();

    if (!name) { showToast('Vui lòng nhập tên gian hàng', 'error'); return; }

    const body = {
        id: parseInt(id),
        boothKey: key,
        name: name,
        saleTag: tag,
        bannerTitle: title,
        bannerSubtitle: subtitle,
        description: desc,
        bannerImageUrl: image
    };

    try {
        const res = await fetch(`/api/admin/booths/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error('Failed to update booth');
        
        showToast('✅ Cấu hình gian hàng và đợt sale thành công!', 'success');
        closeModal('modal-edit-sale');
        
        // Refresh booth list
        renderAdminSaleList();
        
        // Refresh homepage layout displays
        if (typeof loadBoothConfigs === 'function') {
            loadBoothConfigs();
        }
    } catch (err) {
        console.error(err);
        showToast('❌ Lỗi cập nhật gian hàng', 'error');
    }
}

// Voucher Actions
function openAddVoucher() {
    document.getElementById('v-code').value = '';
    document.getElementById('v-value').value = '';
    document.getElementById('v-min-order').value = '0';
    document.getElementById('v-max-uses').value = '';
    document.getElementById('v-expiry').value = '';
    document.getElementById('modal-add-voucher').classList.add('open');
}

async function saveNewVoucher() {
    const code = document.getElementById('v-code').value.trim().toUpperCase();
    const type = document.getElementById('v-type').value;
    const value = parseFloat(document.getElementById('v-value').value);
    const minOrder = parseFloat(document.getElementById('v-min-order').value) || 0;
    const maxUsesRaw = document.getElementById('v-max-uses').value.trim();
    const maxUses = maxUsesRaw ? parseInt(maxUsesRaw) : null;
    const expiry = document.getElementById('v-expiry').value;

    if (!code) { showToast('Vui lòng nhập mã voucher', 'error'); return; }
    if (isNaN(value) || value <= 0) { showToast('Vui lòng nhập giá trị giảm hợp lệ', 'error'); return; }

    const body = {
        code,
        discountType: type,
        discountValue: value,
        minOrderValue: minOrder,
        maxUses,
        expiryDate: expiry || null,
        isActive: true
    };

    try {
        const res = await fetch('/api/admin/vouchers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            const errData = await res.json();
            throw new Error(errData.error || 'Failed to save voucher');
        }
        
        showToast(`✅ Tạo voucher "${code}" thành công!`, 'success');
        closeModal('modal-add-voucher');
        renderVoucherList();
    } catch (err) {
        console.error(err);
        showToast(`❌ Lỗi: ${err.message}`, 'error');
    }
}

async function deleteVoucher(id) {
    if (!confirm('Xóa voucher này vĩnh viễn khỏi hệ thống?')) return;
    try {
        const res = await fetch(`/api/admin/vouchers/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete voucher');
        showToast('🗑 Đã xóa voucher', 'warning');
        renderVoucherList();
    } catch (err) {
        console.error(err);
        showToast('❌ Lỗi xóa voucher', 'error');
    }
}

function toggleHotProduct(id, btn) {
    btn.classList.toggle('on');
    const product = PRODUCTS.find(p => p.id === id);
    if (product) {
        product.isHot = btn.classList.contains('on');
        renderHotGrid();
        showToast(product.isHot ? `✅ "${product.name.slice(0, 20)}..." đã được đánh dấu hot` : `Đã bỏ hot: "${product.name.slice(0, 20)}..."`, product.isHot ? 'success' : 'warning');
    }
}

// Search
function doSearch() {
    const q = (document.getElementById('search-input')?.value || '').trim().toLowerCase();
    if (!q) return;
    showToast(`🔍 Đang tìm: "${q}"`, 'success');
    // Filter and render
    const results = PRODUCTS.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.brand.toLowerCase().includes(q) ||
        p.category.toLowerCase().includes(q)
    );
    const el = document.getElementById('hot-grid');
    if (el) {
        el.innerHTML = results.length
            ? results.map(p => renderCard(p)).join('')
            : '<p style="color:var(--text-gray);padding:20px;grid-column:1/-1;text-align:center"><i class="fa-solid fa-search" style="font-size:2rem;margin-bottom:10px;display:block;color:#ccc"></i>Không tìm thấy sản phẩm phù hợp</p>';
        scrollToSection('hot-section');
    }
}

// Search on Enter
document.getElementById('search-input')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') doSearch();
});



// AUTHENTICATION UI SYNC (home.js only handles admin widgets)

function updateAuthUI() {
    const adminFab = document.getElementById('admin-fab');
    if (adminFab) adminFab.style.display = 'none';

    const userRaw = localStorage.getItem('currentUser');
    if (userRaw) {
        try {
            const user = JSON.parse(userRaw);
            if (user && user.role === 'ADMIN') {
                if (adminFab) adminFab.style.display = 'flex';
                loadAdminStats();
            }
        } catch (e) {
            console.error('Error parsing user session in home.js', e);
        }
    }
}

async function loadAdminStats() {
    try {
        const res = await fetch('/api/admin/stats');
        if (!res.ok) throw new Error('Cannot fetch stats');
        const data = await res.json();
        
        const revEl = document.getElementById('stats-revenue');
        const ordEl = document.getElementById('stats-orders');
        const prodEl = document.getElementById('stats-products');
        const usrEl = document.getElementById('stats-users');
        
        if (revEl) revEl.textContent = formatPrice(data.revenue || 0);
        if (ordEl) ordEl.textContent = data.totalOrders || 0;
        if (prodEl) prodEl.textContent = data.totalProducts || 0;
        if (usrEl) usrEl.textContent = data.totalUsers || 0;
    } catch (err) {
        console.error('Failed to load admin stats:', err);
    }
}

function updateCategoryCounts(dbProducts) {
    const counts = {
        'Thời Trang': 0,
        'Mỹ Phẩm': 0,
        'Trang Sức': 0,
        'Phụ Kiện': 0,
        'Chăm Sóc Da': 0
    };

    dbProducts.forEach(p => {
        const catName = p.category ? p.category.name : '';
        if (counts[catName] !== undefined) {
            counts[catName]++;
        }
    });

    const elFashion = document.getElementById('cat-count-fashion');
    const elBeauty = document.getElementById('cat-count-beauty');
    const elJewelry = document.getElementById('cat-count-jewelry');
    const elAccessories = document.getElementById('cat-count-accessories');
    const elSkincare = document.getElementById('cat-count-skincare');

    if (elFashion) elFashion.textContent = `${counts['Thời Trang']} sản phẩm`;
    if (elBeauty) elBeauty.textContent = `${counts['Mỹ Phẩm']} sản phẩm`;
    if (elJewelry) elJewelry.textContent = `${counts['Trang Sức']} sản phẩm`;
    if (elAccessories) elAccessories.textContent = `${counts['Phụ Kiện']} sản phẩm`;
    if (elSkincare) elSkincare.textContent = `${counts['Chăm Sóc Da']} sản phẩm`;
}

function handleNavClick(el, actionType, arg) {
    // Cập nhật class active trong nav
    document.querySelectorAll('#main-nav .nav-item').forEach(item => item.classList.remove('active'));
    el.classList.add('active');
    
    // Thực hiện hành động
    if (actionType === 'scroll-top') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } else if (actionType === 'filter') {
        filterByCategory(arg);
    } else if (actionType === 'scroll-to') {
        scrollToSection(arg);
    }
}

async function loadBoothConfigs() {
    try {
        const response = await fetch('/api/products/booths');
        if (!response.ok) throw new Error('Cannot fetch booth configs');
        const configs = await response.json();
        
        configs.forEach(c => {
            const key = c.boothKey;
            
            const elName = document.getElementById(`booth-${key}-name`);
            const elDesc = document.getElementById(`booth-${key}-desc`);
            const elTag = document.getElementById(`booth-${key}-tag`);
            const elImg = document.getElementById(`booth-${key}-img`);
            const elSubtitle = document.getElementById(`booth-${key}-subtitle`);
            const elTitle = document.getElementById(`booth-${key}-title`);
            
            if (elName) elName.textContent = c.name;
            if (elDesc) elDesc.textContent = c.description;
            if (elTag) elTag.textContent = c.saleTag;
            if (elImg) {
                elImg.src = c.bannerImageUrl || 'images/office_fashion_set_1779642635158.png';
                elImg.alt = c.name;
            }
            if (elSubtitle) elSubtitle.textContent = c.bannerSubtitle;
            if (elTitle) elTitle.textContent = c.bannerTitle;
        });
    } catch (error) {
        console.error('Error loading booth configs:', error);
    }
}

// INIT

document.addEventListener('DOMContentLoaded', () => {
    loadProductsFromDB();
    loadBoothConfigs();
    renderAdminSaleList();
    renderVoucherList();
    updateAuthUI();
    
    // Update cart badge
    if (typeof capNhatBadge === 'function') capNhatBadge();
});
