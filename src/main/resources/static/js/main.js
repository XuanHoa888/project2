/**
 * main.js — logic dùng chung cho toàn bộ trang
 *
 * Giỏ hàng lưu trong localStorage dạng:
 * [{ id, name, price, image, variant, qty }]

 */

// GIỎ HÀNG (localStorage)

function layGioKey() {
    const userRaw = localStorage.getItem('currentUser');
    if (userRaw) {
        try {
            const user = JSON.parse(userRaw);
            if (user && user.username) {
                return 'cart_' + user.username;
            }
        } catch (e) {
            console.error('Error parsing user session', e);
        }
    }
    return 'cart_guest';
}

function layGio() {
    const key = layGioKey();
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : [];
}

function layWishlistKey() {
    const userRaw = localStorage.getItem('currentUser');
    if (userRaw) {
        try {
            const user = JSON.parse(userRaw);
            if (user && user.username) return 'wishlist_' + user.username;
        } catch (e) {}
    }
    return 'wishlist_guest';
}

function layWishlist() {
    const raw = localStorage.getItem(layWishlistKey());
    return raw ? JSON.parse(raw) : [];
}

function luuWishlist(items) {
    localStorage.setItem(layWishlistKey(), JSON.stringify(items));
}

function luuDaXem(product) {
    if (!product || !product.id) return;
    const key = layGioKey().replace('cart_', 'recent_');
    const raw = localStorage.getItem(key);
    let items = raw ? JSON.parse(raw) : [];
    items = items.filter(p => p.id !== product.id);
    items.unshift({
        id: product.id,
        name: product.name,
        price: product.price,
        image: product.imageUrl || product.image || '',
        category: product.category?.name || product.category || ''
    });
    localStorage.setItem(key, JSON.stringify(items.slice(0, 8)));
}

function luuGio(cart) {
    const key = layGioKey();
    localStorage.setItem(key, JSON.stringify(cart));
    capNhatBadge();
}

function capNhatBadge() {
    const cart = layGio();
    const total = cart.reduce((s, item) => s + item.qty, 0);
    document.querySelectorAll('.cart-badge').forEach(el => el.innerText = total);

    const wish = layWishlist();
    const wishTotal = wish.length;
    document.querySelectorAll('.wishlist-badge').forEach(el => {
        el.innerText = wishTotal;
        el.style.display = wishTotal > 0 ? 'flex' : 'none';
    });
}

function dinhDangTien(price) {
    return '₫' + Number(price).toLocaleString('vi-VN');
}

/**
 * Thêm sản phẩm vào giỏ.
 * Gọi: addToCart(event, id, name, price, image, variant)
 */
function addToCart(e, id, name, price, image, variant = '') {
    if (e) { e.preventDefault(); e.stopPropagation(); }

    const cart = layGio();
    const existing = cart.find(item => item.id === id && item.variant === variant);

    // Đọc số lượng từ input #qty nếu có trang product.html
    const qtyInput = document.getElementById('qty');
    const qty = qtyInput ? (parseInt(qtyInput.value) || 1) : 1;

    if (existing) {
        existing.qty += qty;
    } else {
        cart.push({ id, name, price, image, variant, qty });
    }

    luuGio(cart);
    hienToast('Đã thêm vào giỏ hàng!');
}

function hienToast(msg) {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        toast.style.cssText = [
            'position:fixed', 'top:50%', 'left:50%',
            'transform:translate(-50%,-50%)',
            'background:rgba(0,0,0,0.8)', 'color:#fff',
            'padding:14px 28px', 'border-radius:6px',
            'z-index:9999', 'font-size:1rem', 'display:none'
        ].join(';');
        document.body.appendChild(toast);
    }
    toast.innerText = msg;
    toast.style.display = 'block';
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => toast.style.display = 'none', 2000);
}

// TRANG GIỎ HÀNG (cart.html)

function renderCart() {
    const box = document.getElementById('cart-items-container');
    const footer = document.getElementById('cart-summary-footer');
    if (!box) return;

    const cart = layGio();
    box.innerHTML = '';

    if (cart.length === 0) {
        box.innerHTML = `
            <div style="padding:3rem;text-align:center;background:#fff;border-radius:4px;box-shadow:var(--shadow-sm)">
                <i class="fa-solid fa-cart-shopping" style="font-size:4rem;color:#ccc"></i>
                <p style="margin:1rem 0">Giỏ hàng của bạn đang trống.</p>
                <a href="index.html" style="background:var(--primary-color);color:#fff;padding:10px 20px;border-radius:2px;text-decoration:none">Mua sắm ngay</a>
                <br>
                <a href="shop.html" style="display:inline-block;margin-top:0.75rem;color:var(--primary-color);text-decoration:none;font-size:0.9rem;">
                    <i class="fa-solid fa-arrow-left"></i> Tiếp tục xem sản phẩm
                </a>
            </div>`;
        if (footer) footer.style.display = 'none';
        return;
    }

    if (footer) footer.style.display = 'flex';

    let total = 0, totalQty = 0;

    cart.forEach((item, i) => {
        const subtotal = item.price * item.qty;
        total    += subtotal;
        totalQty += item.qty;

        box.innerHTML += `
            <div class="cart-item">
                <div class="cart-product">
                    <img src="${item.image}" alt="${item.name}">
                    <div class="cart-product-info">
                        <h3>${item.name}</h3>
                        ${item.variant ? `<div class="cart-product-variant">Phân loại: ${item.variant}</div>` : ''}
                    </div>
                </div>
                <div class="cart-price">${dinhDangTien(item.price)}</div>
                <div>
                    <div class="cart-qty">
                        <button onclick="doiSoLuong(${i}, -1)">-</button>
                        <input type="text" value="${item.qty}" readonly>
                        <button onclick="doiSoLuong(${i}, 1)">+</button>
                    </div>
                </div>
                <div class="cart-total-price">${dinhDangTien(subtotal)}</div>
                <div class="cart-action" onclick="xoaKhoi(${i})">Xóa</div>
            </div>`;
    });

    const elTotal = document.getElementById('cart-total-price');
    const elQty   = document.getElementById('cart-total-items');
    if (elTotal) elTotal.innerText = dinhDangTien(total);
    if (elQty)   elQty.innerText   = totalQty;
}

function doiSoLuong(i, delta) {
    const cart = layGio();
    if (!cart[i]) return;
    cart[i].qty += delta;
    if (cart[i].qty <= 0) cart.splice(i, 1);
    luuGio(cart);
    renderCart();
}

function xoaKhoi(i) {
    const cart = layGio();
    cart.splice(i, 1);
    luuGio(cart);
    renderCart();
}

// TRANG THANH TOÁN (checkout.html)

/**
 * Hiển thị tóm tắt đơn hàng trong sidebar.
 * Trả về tổng tiền để dùng tiếp.
 */
function renderCheckoutSummary() {
    const elTotal = document.getElementById('summary-total');
    if (!elTotal) return 0;

    const cart = layGio();
    if (cart.length === 0) {
        alert('Giỏ hàng trống!');
        window.location.href = 'index.html';
        return 0;
    }

    const total = cart.reduce((s, item) => s + item.price * item.qty, 0);

    // Cập nhật hiển thị tổng
    elTotal.innerText = dinhDangTien(total);
    const elBank = document.getElementById('bank-amount');
    if (elBank) elBank.innerText = dinhDangTien(total);

    return total;
}

/** Áp mã giảm giá gọi API kiểm tra trước, cập nhật UI. */
async function apVoucher() {
    const code  = (document.getElementById('voucher-code')?.value || '').trim();
    const total = layGio().reduce((s, i) => s + i.price * i.qty, 0);

    if (!code) return;

    const elMsg      = document.getElementById('voucher-msg');
    const elDiscount = document.getElementById('summary-discount');
    const elFinal    = document.getElementById('summary-final');

    try {
        const res  = await fetch(`/api/vouchers/check?code=${encodeURIComponent(code)}&total=${total}`);
        const data = await res.json();

        if (!res.ok) {
            if (elMsg) elMsg.textContent = '❌ ' + data.error;
            window.voucherOk = false;
            return;
        }

        if (elMsg)      elMsg.textContent = `✅ Giảm ${dinhDangTien(data.discount)}`;
        if (elDiscount) elDiscount.textContent = '-' + dinhDangTien(data.discount);
        if (elFinal)    elFinal.textContent = dinhDangTien(data.finalTotal);
        window.voucherOk   = true;
        window.voucherCode = code;
    } catch {
        if (elMsg) elMsg.textContent = '❌ Không thể kiểm tra mã lúc này';
        window.voucherOk = false;
    }
}

/**
 * Đặt hàng thật gọi POST /api/orders.
 * Bắt lỗi chưa đăng nhập → chuyển trang login.
 */
async function submitOrder() {
    const cart = layGio();
    if (cart.length === 0) {
        alert('Giỏ hàng trống!');
        return;
    }

    // Đọc form
    const name    = document.getElementById('receiver-name')?.value.trim();
    const phone   = document.getElementById('receiver-phone')?.value.trim();
    const address = document.getElementById('receiver-address')?.value.trim();
    const payment = document.querySelector('input[name="payment"]:checked')?.value || 'COD';
    const note    = document.getElementById('order-note')?.value.trim() || '';

    // Validate phía client trước để tránh roundtrip không cần thiết
    if (!name)    { alert('Vui lòng nhập tên người nhận'); return; }
    if (!phone)   { alert('Vui lòng nhập số điện thoại'); return; }
    if (!address) { alert('Vui lòng nhập địa chỉ giao hàng'); return; }

    const body = {
        receiverName:    name,
        receiverPhone:   phone,
        receiverAddress: address,
        paymentMethod:   payment,
        note:            note,
        voucherCode:     window.voucherOk ? window.voucherCode : null,
        items: cart.map(item => ({
            productId: item.id,
            variant:   item.variant || null,
            quantity:  item.qty
        }))
    };

    const btn = document.getElementById('place-order-btn');
    if (btn) { btn.disabled = true; btn.innerText = 'Đang xử lý...'; }

    try {
        const res  = await fetch('/api/orders', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body)
        });
        const data = await res.json();

        if (!res.ok) {
            // 401 = chưa đăng nhập
            if (res.status === 401) {
                alert('Vui lòng đăng nhập để đặt hàng!');
                window.location.href = 'index.html';
                return;
            }
            alert('Lỗi: ' + (data.error || 'Không thể đặt hàng'));
            return;
        }

        // Đặt hàng thành công
        localStorage.removeItem(layGioKey());
        capNhatBadge();

        // Hiện modal thành công với mã đơn thật từ server
        const elCode = document.getElementById('success-order-code');
        if (elCode) elCode.innerText = data.orderCode;

        const modal = document.getElementById('success-modal');
        if (modal) modal.style.display = 'flex';

    } catch {
        alert('Mất kết nối. Vui lòng thử lại.');
    } finally {
        if (btn) { btn.disabled = false; btn.innerText = 'Đặt Hàng'; }
    }
}

// HỖ TRỢ XÁC THỰC HEADER & TIỆN ÍCH DÙNG CHUNG

function formatVnd(price) {
    return '₫' + Number(price).toLocaleString('vi-VN');
}

function pageSearch() {
    const q = (document.getElementById('search-input')?.value || '').trim();
    if (q) {
        window.location.href = 'index.html?search=' + encodeURIComponent(q);
    }
}

function toggleWishlist() {
    window.location.href = 'profile.html?tab=wishlist';
}

function capNhatHeaderAuth() {
    const userDropdown = document.getElementById('user-dropdown');
    const userTrigger = document.querySelector('#user-menu-trigger span');
    const userMenu = document.getElementById('user-menu-trigger');
    if (!userMenu) return;

    // Reset admin button
    const adminBtn = document.getElementById('header-admin-btn');
    if (adminBtn) adminBtn.style.display = 'none';

    const userRaw = localStorage.getItem('currentUser');
    if (userRaw) {
        try {
            const user = JSON.parse(userRaw);
            if (userTrigger) userTrigger.textContent = user.username;
            
            let adminLink = '';
            if (user.role === 'ADMIN') {
                adminLink = `<a href="admin/index.html"><i class="fa-solid fa-user-gear"></i> Trang quản trị</a>`;
                if (adminBtn) adminBtn.style.display = 'flex';
            }

            if (userDropdown) {
                userDropdown.innerHTML = `
                    <div style="padding: 10px 16px; font-size: 0.85rem; font-weight: 600; color: var(--primary-color, #00a8ff); border-bottom: 1px solid var(--border-color, #e8e8e8); margin-bottom: 4px;">Chào, ${user.username}!</div>
                    ${adminLink}
                    <a href="profile.html?tab=profile"><i class="fa-solid fa-user"></i> Hồ sơ của tôi</a>
                    <a href="profile.html?tab=orders"><i class="fa-solid fa-box"></i> Đơn hàng của tôi</a>
                    <a href="profile.html?tab=wishlist"><i class="fa-solid fa-heart"></i> Danh sách yêu thích</a>
                    <a href="profile.html?tab=addresses"><i class="fa-solid fa-location-dot"></i> Sổ địa chỉ</a>
                    <div class="dropdown-divider"></div>
                    <a href="#" onclick="handleLogout(event)"><i class="fa-solid fa-right-from-bracket"></i> Đăng xuất</a>
                `;
            }
        } catch (e) {
            console.error('Error rendering auth UI', e);
        }
    } else {
        if (userTrigger) userTrigger.textContent = 'Tài khoản';
        if (userDropdown) {
            userDropdown.innerHTML = `
                <a href="login.html"><i class="fa-solid fa-user"></i> Đăng nhập</a>
                <a href="register.html"><i class="fa-solid fa-user-plus"></i> Đăng ký</a>
                <div class="dropdown-divider"></div>
                <a href="profile.html?tab=orders"><i class="fa-solid fa-box"></i> Đơn hàng của tôi</a>
            `;
        }
    }
}

async function handleLogout(e) {
    if (e) e.preventDefault();
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch (err) {
        console.warn('Logout API error', err);
    }
    localStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}

// KHỞI ĐỘNG
document.addEventListener('DOMContentLoaded', () => {
    capNhatBadge();
    capNhatHeaderAuth();
    renderCart();           // chỉ chạy nếu có #cart-items-container
    renderCheckoutSummary(); // chỉ chạy nếu có #summary-total

    document.getElementById('user-menu-trigger')?.addEventListener('click', (e) => {
        if (e.target.closest('#user-dropdown')) return; // Allow dropdown links click
        e.preventDefault();
        e.stopPropagation();
        const userRaw = localStorage.getItem('currentUser');
        if (userRaw) {
            window.location.href = 'profile.html';
        } else {
            window.location.href = 'login.html';
        }
    });
    document.addEventListener('click', () => {
        document.getElementById('user-dropdown')?.classList.remove('visible');
    });

    // Enter key support for search inputs on secondary pages
    document.getElementById('search-input')?.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            if (typeof doShopSearch === 'function') {
                e.preventDefault();
                doShopSearch();
            } else if (typeof doSearch === 'function') {
                e.preventDefault();
                doSearch();
            } else {
                pageSearch();
            }
        }
    });
});

// HỆ THỐNG CHAT TRỰC TUYẾN PHÍA KHÁCH HÀNG (LIVE CHAT)

(function initLiveChat() {
    // 1. Thêm CSS vào head
    const style = document.createElement('style');
    style.innerHTML = `
        .floating-chat-bubble {
            position: fixed; bottom: 24px; right: 24px;
            width: 60px; height: 60px; border-radius: 50%;
            background: linear-gradient(135deg, #ee4d2d, #ff7356);
            color: #fff; display: flex; align-items: center; justify-content: center;
            box-shadow: 0 4px 16px rgba(238, 77, 45, 0.4);
            cursor: pointer; z-index: 9999;
            transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            font-size: 1.5rem;
        }
        .floating-chat-bubble:hover {
            transform: scale(1.1) rotate(5deg);
            box-shadow: 0 6px 20px rgba(238, 77, 45, 0.5);
        }
        .floating-chat-window {
            position: fixed; bottom: 96px; right: 24px;
            width: 350px; height: 450px; background: #fff;
            border-radius: 12px; box-shadow: 0 8px 30px rgba(0,0,0,0.15);
            display: none; flex-direction: column; z-index: 9999;
            overflow: hidden; animation: slideUpChat 0.25s ease;
            border: 1px solid #e2e8f0;
        }
        @keyframes slideUpChat {
            from { transform: translateY(15px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
        .chat-window-header {
            background: linear-gradient(135deg, #1e293b, #334155);
            color: #fff; padding: 0.85rem 1rem;
            display: flex; justify-content: space-between; align-items: center;
        }
        .chat-window-header h4 {
            margin: 0; font-size: 0.92rem; font-weight: 700;
            display: flex; align-items: center; gap: 8px;
        }
        .chat-window-header .close-btn {
            background: none; border: none; color: #94a3b8;
            font-size: 1.2rem; cursor: pointer; line-height: 1;
        }
        .chat-window-header .close-btn:hover { color: #fff; }
        .chat-window-messages {
            flex: 1; padding: 1rem; overflow-y: auto;
            background: #f8fafc; display: flex; flex-direction: column; gap: 0.75rem;
        }
        .chat-msg {
            max-width: 80%; padding: 0.5rem 0.75rem;
            border-radius: 8px; font-size: 0.82rem; line-height: 1.4;
            word-break: break-word;
        }
        .chat-msg.self {
            align-self: flex-end; background: #ee4d2d; color: #fff;
            border-bottom-right-radius: 2px;
        }
        .chat-msg.other {
            align-self: flex-start; background: #fff; color: #1e293b;
            border-bottom-left-radius: 2px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);
            border: 1px solid #e2e8f0;
        }
        .chat-window-input {
            padding: 0.65rem 0.8rem; border-top: 1px solid #e2e8f0;
            display: flex; gap: 0.5rem; background: #fff; align-items: center;
        }
        .chat-window-input input {
            flex: 1; border: 1px solid #cbd5e1; border-radius: 20px;
            padding: 0.4rem 0.8rem; font-size: 0.82rem; outline: none;
            font-family: inherit;
        }
        .chat-window-input input:focus { border-color: #ee4d2d; }
        .chat-window-input button {
            background: #ee4d2d; color: #fff; border: none;
            width: 30px; height: 30px; border-radius: 50%;
            cursor: pointer; display: flex; align-items: center; justify-content: center;
            font-size: 0.85rem; flex-shrink: 0;
        }
        .chat-window-input button:hover { background: #d73211; }
        .chat-guest-view {
            padding: 2rem 1.5rem; text-align: center;
            display: flex; flex-direction: column; justify-content: center;
            height: 100%; align-items: center; gap: 1rem;
        }
        .chat-guest-view i { font-size: 2.8rem; color: #ee4d2d; opacity: 0.8; }
        .chat-guest-view p { font-size: 0.88rem; color: #64748b; line-height: 1.5; margin: 0; }
        .chat-guest-view a {
            background: #ee4d2d; color:#fff; text-decoration:none;
            padding: 0.45rem 1.2rem; border-radius: 20px; font-size: 0.82rem; font-weight: 600;
        }
        .chat-guest-view a:hover { background:#d73211; }
    `;
    document.head.appendChild(style);

    // 2. Tạo phần tử chat bubble
    const bubble = document.createElement('div');
    bubble.className = 'floating-chat-bubble';
    bubble.innerHTML = '<i class="fa-solid fa-comments"></i>';
    document.body.appendChild(bubble);

    // 3. Tạo phần tử chat window
    const win = document.createElement('div');
    win.className = 'floating-chat-window';
    document.body.appendChild(win);

    let socket = null;
    let currentUser = null;

    bubble.addEventListener('click', (e) => {
        e.stopPropagation();
        if (win.style.display === 'flex') {
            closeChatWindow();
        } else {
            openChatWindow();
        }
    });

    function closeChatWindow() {
        win.style.display = 'none';
        if (socket) {
            socket.close();
            socket = null;
        }
    }

    async function openChatWindow() {
        win.style.display = 'flex';
        
        const rawUser = localStorage.getItem('currentUser');
        if (!rawUser) {
            renderGuestView();
            return;
        }

        try {
            currentUser = JSON.parse(rawUser);
            renderChatView();
            await loadChatHistory();
            connectWebSocket();
        } catch (e) {
            renderGuestView();
        }
    }

    function renderGuestView() {
        const loginPath = window.location.pathname.includes('/admin/') ? '../login.html' : 'login.html';
        win.innerHTML = `
            <div class="chat-window-header">
                <h4><i class="fa-solid fa-headset"></i> Hỗ Trợ XH STORE</h4>
                <button class="close-btn" id="chat-close-btn-guest">&times;</button>
            </div>
            <div class="chat-guest-view">
                <i class="fa-solid fa-circle-user"></i>
                <p>Chào bạn! Vui lòng đăng nhập tài khoản khách hàng để sử dụng chat tư vấn trực tuyến.</p>
                <a href="${loginPath}">Đăng Nhập Ngay</a>
            </div>
        `;
        document.getElementById('chat-close-btn-guest').onclick = closeChatWindow;
    }

    function renderChatView() {
        win.innerHTML = `
            <div class="chat-window-header">
                <h4><i class="fa-solid fa-headset"></i> Tư vấn XH STORE</h4>
                <button class="close-btn" id="chat-close-btn">&times;</button>
            </div>
            <div class="chat-window-messages" id="chat-msg-container">
                <div style="color:#94a3b8;font-size:0.75rem;text-align:center;margin:1rem 0;">Đang kết nối & tải lịch sử chat...</div>
            </div>
            <div class="chat-window-input">
                <input type="text" id="chat-input-text" placeholder="Nhập nội dung tư vấn..." autocomplete="off">
                <button id="chat-send-btn"><i class="fa-solid fa-paper-plane"></i></button>
            </div>
        `;

        document.getElementById('chat-close-btn').onclick = closeChatWindow;
        
        const input = document.getElementById('chat-input-text');
        input.onkeydown = (e) => {
            if (e.key === 'Enter') sendChatMessage();
        };

        document.getElementById('chat-send-btn').onclick = sendChatMessage;
    }

    async function loadChatHistory() {
        try {
            const res = await fetch(`/api/chat/history?chatRoomId=${currentUser.id}`);
            if (!res.ok) return;
            const messages = await res.json();
            
            const container = document.getElementById('chat-msg-container');
            container.innerHTML = '';
            
            if (messages.length === 0) {
                container.innerHTML = '<div style="color:#94a3b8;font-size:0.75rem;text-align:center;margin:2rem 0;">Bắt đầu gửi tin nhắn để trò chuyện với chúng tôi!</div>';
            } else {
                messages.forEach(msg => {
                    appendMessageDOM(msg);
                });
            }
            scrollToBottom();
        } catch (e) {
            console.error('Lỗi tải lịch sử chat', e);
        }
    }

    function connectWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws/chat?userId=${currentUser.id}&role=CLIENT`;

        socket = new WebSocket(wsUrl);

        socket.onopen = () => {
            console.log('Connected to chat WebSocket');
        };

        socket.onmessage = (event) => {
            try {
                const msg = JSON.parse(event.data);
                const container = document.getElementById('chat-msg-container');
                if (container && container.innerText.includes('Bắt đầu gửi tin nhắn')) {
                    container.innerHTML = '';
                }
                appendMessageDOM(msg);
                scrollToBottom();
            } catch (e) {
                console.error('Lỗi nhận tin nhắn', e);
            }
        };

        socket.onclose = () => {
            console.log('Chat WebSocket closed');
        };
    }

    function sendChatMessage() {
        const input = document.getElementById('chat-input-text');
        const content = input.value.trim();
        if (!content || !socket || socket.readyState !== WebSocket.OPEN) return;

        const payload = {
            chatRoomId: currentUser.id,
            content: content,
            senderName: currentUser.username
        };

        socket.send(JSON.stringify(payload));
        input.value = '';
    }

    function appendMessageDOM(msg) {
        const container = document.getElementById('chat-msg-container');
        if (!container) return;

        const el = document.createElement('div');
        el.className = 'chat-msg ' + (msg.isAdmin ? 'other' : 'self');
        el.innerText = msg.content;
        container.appendChild(el);
    }

    function scrollToBottom() {
        const container = document.getElementById('chat-msg-container');
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    }
})();
