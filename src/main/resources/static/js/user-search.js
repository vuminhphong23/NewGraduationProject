(() => {
    const input = document.getElementById('userSearchInput');
    const btn = document.getElementById('userSearchBtn');
    const results = document.getElementById('userSearchResults');

    if (!input || !btn || !results) return;

    let currentPage = 0;
    const size = 8;

    async function searchUsers(page = 0) {
        const q = (input.value || '').trim();
        const params = new URLSearchParams({ q, page, size });
        results.innerHTML = '<div class="text-center py-2 text-muted">Đang tìm...</div>';
        try {
            const resp = await authenticatedFetch(`/api/users/search?${params.toString()}`);
            if (!resp || !resp.ok) throw new Error('Search failed');
            const data = await resp.json();
            renderResults(data.items || []);
            currentPage = data.page || 0;
        } catch (e) {
            console.error(e);
            results.innerHTML = '<div class="text-danger small py-2">Không thể tìm kiếm</div>';
        }
    }

    function renderResults(items) {
        if (!items.length) {
            results.innerHTML = '<div class="text-muted small py-2">Không có kết quả</div>';
            return;
        }
        results.innerHTML = '';
        items.forEach(u => {
            const row = document.createElement('div');
            row.className = 'list-group-item d-flex align-items-center justify-content-between gap-2';
            
            // Xử lý avatar - sử dụng từ database hoặc ảnh mặc định
            let avatarSrc = 'https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'; // Ảnh placeholder trống
            if (u.avatar && u.avatar.trim() !== '') {
                avatarSrc = u.avatar; // Sử dụng avatar từ Cloudinary
            }
            row.innerHTML = `
                <div class="d-flex align-items-center gap-2">
                    <img src="${avatarSrc}" width="28" height="28" class="rounded-circle"
                         onerror="this.src='https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png'"
                         alt="Avatar của ${escapeHtml(u.username || '')}"/>
                    <div>
                        <div class="fw-semibold">${escapeHtml(u.username || '')}</div>
                        <div class="text-muted">${escapeHtml((u.firstName || '') + ' ' + (u.lastName || ''))}</div>
                    </div>
                </div>
                <div>
                    ${renderActionBtns(u)}
                </div>
            `;
            results.appendChild(row);
        });
        wireActionButtons(results);
    }

    function renderActionBtns(u) {
        const status = u.friendshipStatus || 'NONE';
        const requestedByMe = !!u.requestedByMe;
        const id = u.id;
        if (status === 'ACCEPTED') {
            return `<button class="btn btn-sm btn-danger" data-action="unfriend" data-user-id="${id}">Hủy kết bạn</button>`;
        }
        if (status === 'PENDING') {
            if (requestedByMe) {
                return `<button class="btn btn-sm btn-danger" data-action="cancel" data-user-id="${id}">Hủy yêu cầu</button>`;
            } else {
                return `
                    <button class="btn btn-sm btn-success me-1" data-action="accept" data-user-id="${id}">Chấp nhận</button>
                    <button class="btn btn-sm btn-danger" data-action="decline" data-user-id="${id}">Từ chối</button>
                `;
            }
        }
        if (status === 'BLOCKED') {
            return `<span class="badge bg-secondary">Đã chặn</span>`;
        }
        return `<button class="btn btn-sm btn-primary" data-action="request" data-user-id="${id}">Kết bạn</button>`;
    }

    function wireActionButtons(container) {
        container.querySelectorAll('[data-action]')?.forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-user-id');
                const action = btn.getAttribute('data-action');
                try {
                    if (action === 'request') {
                        await callApi('POST', `/api/friends/request/${id}`);
                    } else if (action === 'accept') {
                        await callApi('POST', `/api/friends/accept/${id}`);
                    } else if (action === 'decline') {
                        await callApi('POST', `/api/friends/decline/${id}`);
                    } else if (action === 'cancel') {
                        await callApi('POST', `/api/friends/cancel/${id}`);
                    } else if (action === 'unfriend') {
                        await callApi('DELETE', `/api/friends/${id}`);
                    }
                    await searchUsers(currentPage);
                } catch (e) {
                    console.error(e);
                    // Use toastManager for error notifications
                    if (window.toastManager) {
                        window.toastManager.error('Thao tác thất bại');
                    } else {
                        alert('Thao tác thất bại');
                    }
                }
            });
        });
    }

    async function callApi(method, url) {
        const resp = await authenticatedFetch(url, { method });
        if (!resp || !resp.ok) throw new Error('API failed');
        return resp.json().catch(() => ({}));
    }

    function escapeHtml(str) {
        return String(str).replace(/[&<>"]+/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[s]));
    }

    btn.addEventListener('click', () => searchUsers(0));
    input.addEventListener('keypress', e => { if (e.key === 'Enter') { e.preventDefault(); searchUsers(0); } });
})();


