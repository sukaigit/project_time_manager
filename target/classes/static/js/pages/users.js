// js/pages/users.js — 用户管理页面

const ROLE_OPTIONS = [
    { value: 'USER', label: '普通用户' },
    { value: 'PM', label: '项目经理' },
    { value: 'DEPT_MANAGER', label: '部门经理' },
    { value: 'ADMIN', label: '管理员' }
];

let currentPage = 1;
let pageSize = 10;
let totalUsers = 0;
let keyword = '';
let editingId = null;

function renderPage() {
    fetchUsers();
}

// ====== 弹窗 ======
function showModal(title, content) {
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);
        display:flex;align-items:center;justify-content:center;z-index:1000;
    `;
    overlay.innerHTML = `
        <div class="card" style="width:480px;max-width:90vw;max-height:90vh;overflow-y:auto;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
                <h3 style="margin:0;font-size:18px;font-weight:500;">${title}</h3>
                <button onclick="this.closest('div[style]').parentElement.remove()" style="background:none;border:none;font-size:20px;cursor:pointer;color:var(--color-ink-mute);">&times;</button>
            </div>
            ${content}
        </div>
    `;
    overlay.addEventListener('click', (e) => { if (e.target === overlay) overlay.remove(); });
    document.body.appendChild(overlay);
    return overlay;
}

function closeModal() {
    const modals = document.querySelectorAll('div[style*="position:fixed"]');
    modals.forEach(m => m.remove());
}

function showToast(msg, type) {
    const toast = document.createElement('div');
    toast.className = `alert alert-${type}`;
    toast.style.cssText = `
        position:fixed;top:20px;right:20px;z-index:2000;min-width:280px;
        box-shadow:var(--shadow-floating);animation:fadeIn 0.2s;
    `;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, 2500);
}

// ====== 数据获取 ======
async function fetchUsers() {
    const content = document.getElementById('pageContent');
    content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    const params = new URLSearchParams({ page: currentPage, size: pageSize });
    if (keyword) params.set('keyword', keyword);

    const res = await API.get(`/api/users?${params}`);
    if (res.code !== 200) {
        content.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    totalUsers = res.data.total;
    renderTable(res.data.list);
}

// ====== 渲染表格 ======
function renderTable(users) {
    const content = document.getElementById('pageContent');
    const totalPages = Math.max(1, Math.ceil(totalUsers / pageSize));

    const roleLabels = { USER: '普通用户', PM: '项目经理', DEPT_MANAGER: '部门经理', ADMIN: '管理员' };
    const statusLabels = { 1: '启用', 0: '禁用' };
    const statusColors = { 1: '#d1fae5', 0: '#fee2e2' };
    const roleColors = {
        USER: '#dbeafe',
        PM: '#fef3c7',
        DEPT_MANAGER: '#e0e7ff',
        ADMIN: '#fce7f3'
    };

    content.innerHTML = `
        <div class="page-header">
            <h1>用户管理</h1>
            <button class="btn btn-primary" onclick="openAddModal()">+ 新增用户</button>
        </div>

        <div style="margin-bottom:16px;display:flex;gap:8px;">
            <input class="form-input" id="searchInput" placeholder="搜索用户名或姓名..." style="max-width:300px;"
                value="${keyword}" onkeydown="if(event.key==='Enter'){keyword=this.value;currentPage=1;fetchUsers();}">
            <button class="btn btn-primary" onclick="keyword=document.getElementById('searchInput').value;currentPage=1;fetchUsers();">搜索</button>
            ${keyword ? `<button class="btn btn-secondary" onclick="keyword='';document.getElementById('searchInput').value='';currentPage=1;fetchUsers();">清除</button>` : ''}
        </div>

        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>用户名</th>
                    <th>姓名</th>
                    <th>邮箱</th>
                    <th>手机</th>
                    <th>部门</th>
                    <th>角色</th>
                    <th>状态</th>
                    <th>首次登录</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                ${users.length === 0 ? '<tr><td colspan="10" style="text-align:center;color:var(--color-ink-mute);padding:40px;">暂无数据</td></tr>' :
                    users.map(u => `
                        <tr>
                            <td>${u.id}</td>
                            <td>${u.username}</td>
                            <td>${u.name}</td>
                            <td>${u.email || '-'}</td>
                            <td>${u.phone || '-'}</td>
                            <td>${u.department || '-'}</td>
                            <td><span class="tag" style="background:${roleColors[u.role] || '#e2e8f0'};color:var(--color-ink);">${roleLabels[u.role] || u.role}</span></td>
                            <td><span class="tag" style="background:${statusColors[u.status]};color:var(--color-ink);">${statusLabels[u.status] || '-'}</span></td>
                            <td>${u.firstLogin === 1 ? '<span class="tag tag-pending">是</span>' : '<span class="tag" style="background:#e2e8f0;color:#64748b;">否</span>'}</td>
                            <td style="white-space:nowrap;display:flex;gap:4px;">
                                <button class="btn btn-secondary btn-sm" onclick="openEditModal(${u.id})">编辑</button>
                                <button class="btn btn-secondary btn-sm" onclick="openRoleModal(${u.id})">角色</button>
                                <button class="btn btn-secondary btn-sm" onclick="resetPassword(${u.id})">重置密码</button>
                                <button class="btn btn-danger btn-sm" onclick="deleteUser(${u.id})">删除</button>
                            </td>
                        </tr>
                    `).join('')}
            </tbody>
        </table>

        <div class="pagination">
            <span style="color:var(--color-ink-mute);font-size:13px;">共 ${totalUsers} 条</span>
            ${currentPage > 1 ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage - 1};fetchUsers();">&laquo; 上一页</a>` : ''}
            ${generatePageLinks(totalPages)}
            ${currentPage < totalPages ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage + 1};fetchUsers();">下一页 &raquo;</a>` : ''}
        </div>
    `;
}

function generatePageLinks(totalPages) {
    let links = '';
    const maxShow = 7;
    let start = Math.max(1, currentPage - Math.floor(maxShow / 2));
    let end = Math.min(totalPages, start + maxShow - 1);
    if (end - start < maxShow - 1) start = Math.max(1, end - maxShow + 1);

    for (let i = start; i <= end; i++) {
        links += i === currentPage ? `<span class="active">${i}</span>` : `<a href="javascript:void(0)" onclick="currentPage=${i};fetchUsers();">${i}</a>`;
    }
    return links;
}

// ====== 新增 ======
function openAddModal() {
    editingId = null;
    const roles = ROLE_OPTIONS.map(r => `<option value="${r.value}">${r.label}</option>`).join('');
    showModal('新增用户', `
        <div class="form-group"><label class="form-label">用户名 *</label><input class="form-input" id="f_username" placeholder="用户名"></div>
        <div class="form-group"><label class="form-label">姓名 *</label><input class="form-input" id="f_name" placeholder="姓名"></div>
        <div class="form-group"><label class="form-label">邮箱</label><input class="form-input" id="f_email" placeholder="邮箱"></div>
        <div class="form-group"><label class="form-label">手机</label><input class="form-input" id="f_phone" placeholder="手机号"></div>
        <div class="form-group"><label class="form-label">部门</label><input class="form-input" id="f_department" placeholder="部门" value="研发与交付中心"></div>
        <div class="form-group"><label class="form-label">角色</label><select class="form-select" id="f_role">${roles}</select></div>
        <div class="form-group"><label class="form-label">状态</label><select class="form-select" id="f_status"><option value="1">启用</option><option value="0">禁用</option></select></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitUser()">保存</button>
        </div>
        <div style="margin-top:8px;font-size:12px;color:var(--color-ink-mute);">密码默认为 123456，首次登录需强制修改</div>
    `);
}

// ====== 编辑 ======
async function openEditModal(id) {
    editingId = id;
    const res = await API.get(`/api/users?page=1&size=1000`);
    if (res.code !== 200) { showToast('获取用户信息失败', 'error'); return; }
    const user = res.data.list.find(u => u.id === id);
    if (!user) { showToast('用户不存在', 'error'); return; }

    const roles = ROLE_OPTIONS.map(r =>
        `<option value="${r.value}" ${user.role === r.value ? 'selected' : ''}>${r.label}</option>`
    ).join('');

    showModal('编辑用户', `
        <div class="form-group"><label class="form-label">用户名</label><input class="form-input" id="f_username" value="${user.username}" disabled style="background:#f1f5f9;"></div>
        <div class="form-group"><label class="form-label">姓名</label><input class="form-input" id="f_name" value="${user.name || ''}"></div>
        <div class="form-group"><label class="form-label">邮箱</label><input class="form-input" id="f_email" value="${user.email || ''}"></div>
        <div class="form-group"><label class="form-label">手机</label><input class="form-input" id="f_phone" value="${user.phone || ''}"></div>
        <div class="form-group"><label class="form-label">部门</label><input class="form-input" id="f_department" value="${user.department || ''}"></div>
        <div class="form-group"><label class="form-label">角色</label><select class="form-select" id="f_role">${roles}</select></div>
        <div class="form-group"><label class="form-label">状态</label><select class="form-select" id="f_status">
            <option value="1" ${user.status === 1 ? 'selected' : ''}>启用</option>
            <option value="0" ${user.status === 0 ? 'selected' : ''}>禁用</option>
        </select></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitUser()">保存</button>
        </div>
    `);
}

// ====== 提交用户 ======
async function submitUser() {
    const data = {
        username: document.getElementById('f_username')?.value,
        name: document.getElementById('f_name')?.value,
        email: document.getElementById('f_email')?.value || null,
        phone: document.getElementById('f_phone')?.value || null,
        department: document.getElementById('f_department')?.value || null,
        role: document.getElementById('f_role')?.value || 'USER',
        status: parseInt(document.getElementById('f_status')?.value || '1')
    };

    if (!data.name) { showToast('姓名不能为空', 'error'); return; }

    let res;
    if (editingId) {
        res = await API.put(`/api/users/${editingId}`, { name: data.name, email: data.email, phone: data.phone, department: data.department, role: data.role, status: data.status });
    } else {
        if (!data.username) { showToast('用户名不能为空', 'error'); return; }
        res = await API.post('/api/users', data);
    }

    if (res.code === 200) {
        showToast(editingId ? '修改成功' : '新增成功', 'success');
        closeModal();
        fetchUsers();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

// ====== 删除 ======
async function deleteUser(id) {
    const overlay = showModal('确认删除', `
        <p style="color:var(--color-ink-mute);">确定要删除该用户吗？此操作为逻辑删除，可恢复。</p>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-danger" id="confirmDeleteBtn">确认删除</button>
        </div>
    `);
    document.getElementById('confirmDeleteBtn').onclick = async () => {
        const res = await API.del(`/api/users/${id}`);
        if (res.code === 200) {
            showToast('删除成功', 'success');
            closeModal();
            if ((currentPage - 1) * pageSize >= totalUsers - 1 && currentPage > 1) currentPage--;
            fetchUsers();
        } else {
            showToast(res.message || '删除失败', 'error');
        }
    };
}

// ====== 修改角色 ======
function openRoleModal(id) {
    const roles = ROLE_OPTIONS.map(r => `<option value="${r.value}">${r.label}</option>`).join('');
    showModal('分配角色', `
        <div class="form-group"><label class="form-label">选择角色</label><select class="form-select" id="f_role_select">${roles}</select></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitRole(${id})">确认</button>
        </div>
    `);
}

async function submitRole(id) {
    const role = document.getElementById('f_role_select').value;
    const res = await API.put(`/api/users/${id}/role`, { role });
    if (res.code === 200) {
        showToast('角色修改成功', 'success');
        closeModal();
        fetchUsers();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

// ====== 重置密码 ======
async function resetPassword(id) {
    if (!confirm('确定要重置该用户的密码为 123456 吗？')) return;
    const res = await API.put(`/api/users/${id}/reset-password`);
    if (res.code === 200) {
        showToast('密码已重置为 123456', 'success');
    } else {
        showToast(res.message || '重置失败', 'error');
    }
}

// 注册到全局（app.js 通过 window.renderPage 调用）
window.renderPage = renderPage;
