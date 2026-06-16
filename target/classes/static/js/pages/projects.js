// js/pages/projects.js — 项目管理 + 任务模块管理

// ====== Project State ======
let currentPage = 1;
let pageSize = 10;
let totalProjects = 0;
let keyword = '';
let editingId = null;

// ====== Module State ======
let moduleProjectId = null;
let moduleProjectName = '';

const STATUS_OPTIONS = [
    { value: 'ACTIVE', label: '进行中' },
    { value: 'FINISHED', label: '已结束' }
];

const STATUS_COLORS = {
    ACTIVE: '#d1fae5',
    FINISHED: '#e2e8f0'
};

const STATUS_LABELS = {
    ACTIVE: '进行中',
    FINISHED: '已结束'
};

let pmUsers = [];

function renderPage() {
    fetchPMUsers();
    fetchProjects();
}

// ====== PM Users ======
async function fetchPMUsers() {
    const res = await API.get('/api/users/pm-list');
    if (res.code === 200) {
        pmUsers = res.data || [];
    }
}

// ====== 弹窗 ======
function showModal(title, content) {
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);
        display:flex;align-items:center;justify-content:center;z-index:1000;
    `;
    overlay.innerHTML = `
        <div class="card" style="width:520px;max-width:90vw;max-height:90vh;overflow-y:auto;">
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

// ====== 项目：数据获取 ======
async function fetchProjects() {
    const content = document.getElementById('pageContent');
    content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    const params = new URLSearchParams({ page: currentPage, size: pageSize });
    if (keyword) params.set('keyword', keyword);

    const res = await API.get(`/api/projects?${params}`);
    if (res.code !== 200) {
        content.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    totalProjects = res.data.total;
    renderTable(res.data.list);
}

// ====== 项目：渲染表格 ======
function renderTable(projects) {
    const content = document.getElementById('pageContent');
    const totalPages = Math.max(1, Math.ceil(totalProjects / pageSize));

    content.innerHTML = `
        <div class="page-header">
            <h1>项目管理</h1>
            <button class="btn btn-primary" onclick="openAddProjectModal()">+ 新增项目</button>
        </div>

        <div style="margin-bottom:16px;display:flex;gap:8px;">
            <input class="form-input" id="searchInput" placeholder="搜索项目名称..." style="max-width:300px;"
                value="${keyword}" onkeydown="if(event.key==='Enter'){keyword=this.value;currentPage=1;fetchProjects();}">
            <button class="btn btn-primary" onclick="keyword=document.getElementById('searchInput').value;currentPage=1;fetchProjects();">搜索</button>
            ${keyword ? `<button class="btn btn-secondary" onclick="keyword='';document.getElementById('searchInput').value='';currentPage=1;fetchProjects();">清除</button>` : ''}
        </div>

        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>项目名称</th>
                    <th>项目经理</th>
                    <th>开始日期</th>
                    <th>结束日期</th>
                    <th>状态</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                ${projects.length === 0 ? '<tr><td colspan="7" style="text-align:center;color:var(--color-ink-mute);padding:40px;">暂无数据</td></tr>' :
                    projects.map(p => `
                        <tr>
                            <td>${p.id}</td>
                            <td><strong>${p.name}</strong></td>
                            <td>${p.managerName || '-'}</td>
                            <td>${p.startDate || '-'}</td>
                            <td>${p.endDate || '-'}</td>
                            <td><span class="tag" style="background:${STATUS_COLORS[p.status] || '#e2e8f0'};color:var(--color-ink);">${STATUS_LABELS[p.status] || p.status}</span></td>
                            <td style="white-space:nowrap;display:flex;gap:4px;">
                                <button class="btn btn-secondary btn-sm" onclick="openEditProjectModal(${p.id})">编辑</button>
                                <button class="btn btn-secondary btn-sm" onclick="showModules(${p.id}, '${p.name.replace(/'/g, "\\'")}')">模块</button>
                                <button class="btn btn-danger btn-sm" onclick="deleteProject(${p.id})">删除</button>
                            </td>
                        </tr>
                    `).join('')}
            </tbody>
        </table>

        <div class="pagination">
            <span style="color:var(--color-ink-mute);font-size:13px;">共 ${totalProjects} 条</span>
            ${currentPage > 1 ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage - 1};fetchProjects();">&laquo; 上一页</a>` : ''}
            ${generatePageLinks(totalPages)}
            ${currentPage < totalPages ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage + 1};fetchProjects();">下一页 &raquo;</a>` : ''}
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
        links += i === currentPage ? `<span class="active">${i}</span>` : `<a href="javascript:void(0)" onclick="currentPage=${i};fetchProjects();">${i}</a>`;
    }
    return links;
}

// ====== 项目：新增 ======
function openAddProjectModal() {
    editingId = null;
    const pmOptions = pmUsers.map(u => `<option value="${u.id}">${u.name} (${u.username})</option>`).join('');
    const statusOpts = STATUS_OPTIONS.map(s => `<option value="${s.value}">${s.label}</option>`).join('');

    showModal('新增项目', `
        <div class="form-group"><label class="form-label">项目名称 *</label><input class="form-input" id="f_name" placeholder="项目名称"></div>
        <div class="form-group"><label class="form-label">项目经理 *</label><select class="form-select" id="f_managerId">
            <option value="">请选择项目经理</option>
            ${pmOptions}
        </select></div>
        <div class="form-group"><label class="form-label">开始日期</label><input class="form-input" id="f_startDate" type="date"></div>
        <div class="form-group"><label class="form-label">结束日期</label><input class="form-input" id="f_endDate" type="date"></div>
        <div class="form-group"><label class="form-label">状态</label><select class="form-select" id="f_status">${statusOpts}</select></div>
        <div class="form-group"><label class="form-label">项目描述</label><textarea class="form-input" id="f_description" rows="3" placeholder="项目描述"></textarea></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitProject()">保存</button>
        </div>
    `);
}

// ====== 项目：编辑 ======
async function openEditProjectModal(id) {
    editingId = id;
    const res = await API.get(`/api/projects?page=1&size=1000`);
    if (res.code !== 200) { showToast('获取项目信息失败', 'error'); return; }
    const project = res.data.list.find(p => p.id === id);
    if (!project) { showToast('项目不存在', 'error'); return; }

    const pmOptions = pmUsers.map(u =>
        `<option value="${u.id}" ${project.managerId === u.id ? 'selected' : ''}>${u.name} (${u.username})</option>`
    ).join('');
    const statusOpts = STATUS_OPTIONS.map(s =>
        `<option value="${s.value}" ${project.status === s.value ? 'selected' : ''}>${s.label}</option>`
    ).join('');

    showModal('编辑项目', `
        <div class="form-group"><label class="form-label">项目名称 *</label><input class="form-input" id="f_name" value="${project.name || ''}"></div>
        <div class="form-group"><label class="form-label">项目经理 *</label><select class="form-select" id="f_managerId">
            <option value="">请选择项目经理</option>
            ${pmOptions}
        </select></div>
        <div class="form-group"><label class="form-label">开始日期</label><input class="form-input" id="f_startDate" type="date" value="${project.startDate || ''}"></div>
        <div class="form-group"><label class="form-label">结束日期</label><input class="form-input" id="f_endDate" type="date" value="${project.endDate || ''}"></div>
        <div class="form-group"><label class="form-label">状态</label><select class="form-select" id="f_status">${statusOpts}</select></div>
        <div class="form-group"><label class="form-label">项目描述</label><textarea class="form-input" id="f_description" rows="3" placeholder="项目描述">${project.description || ''}</textarea></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitProject()">保存</button>
        </div>
    `);
}

// ====== 项目：提交 ======
async function submitProject() {
    const data = {
        name: document.getElementById('f_name')?.value,
        description: document.getElementById('f_description')?.value || null,
        managerId: document.getElementById('f_managerId')?.value ? parseInt(document.getElementById('f_managerId').value) : null,
        startDate: document.getElementById('f_startDate')?.value || null,
        endDate: document.getElementById('f_endDate')?.value || null,
        status: document.getElementById('f_status')?.value || 'ACTIVE'
    };

    if (!data.name) { showToast('项目名称不能为空', 'error'); return; }
    if (!data.managerId) { showToast('请选择项目经理', 'error'); return; }

    let res;
    if (editingId) {
        res = await API.put(`/api/projects/${editingId}`, data);
    } else {
        res = await API.post('/api/projects', data);
    }

    if (res.code === 200) {
        showToast(editingId ? '修改成功' : '新增成功', 'success');
        closeModal();
        fetchProjects();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

// ====== 项目：删除 ======
async function deleteProject(id) {
    const overlay = showModal('确认删除', `
        <p style="color:var(--color-ink-mute);">确定要删除该项目吗？</p>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-danger" id="confirmDeleteBtn">确认删除</button>
        </div>
    `);
    document.getElementById('confirmDeleteBtn').onclick = async () => {
        const res = await API.del(`/api/projects/${id}`);
        if (res.code === 200) {
            showToast('删除成功', 'success');
            closeModal();
            if ((currentPage - 1) * pageSize >= totalProjects - 1 && currentPage > 1) currentPage--;
            fetchProjects();
        } else {
            showToast(res.message || '删除失败', 'error');
        }
    };
}

// ====================================================================
//  任务模块管理（子页面）
// ====================================================================

// ====== 模块状态 ======
let modulePage = 1;
let moduleSize = 100; // 模块通常较少，一次加载
let moduleEditingId = null;

// ====== 显示模块管理 ======
async function showModules(projectId, projectName) {
    moduleProjectId = projectId;
    moduleProjectName = projectName;
    moduleEditingId = null;

    // 更新页面标题
    document.getElementById('pageTitle').textContent = `项目管理 > ${projectName} - 模块管理`;

    const content = document.getElementById('pageContent');
    content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    const res = await API.get(`/api/projects/${projectId}/modules`);
    if (res.code !== 200) {
        content.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    renderModules(res.data || []);
}

// ====== 返回项目列表 ======
function backToProjects() {
    document.getElementById('pageTitle').textContent = '项目管理';
    currentPage = 1;
    fetchProjects();
}

// ====== 渲染模块列表 ======
function renderModules(modules) {
    const content = document.getElementById('pageContent');
    const totalHours = modules.reduce((sum, m) => sum + (m.usedHours || 0), 0);

    content.innerHTML = `
        <div style="margin-bottom:16px;display:flex;justify-content:space-between;align-items:center;">
            <div>
                <a href="javascript:void(0)" onclick="backToProjects()" style="color:var(--color-primary);text-decoration:none;font-size:14px;">&larr; 返回项目列表</a>
                <h2 style="margin:8px 0 0 0;font-size:20px;font-weight:500;">${moduleProjectName} - 任务模块</h2>
                <p style="margin:4px 0 0 0;font-size:13px;color:var(--color-ink-mute);">已报工时合计：${totalHours.toFixed(1)} 小时</p>
            </div>
            <button class="btn btn-primary" onclick="openAddModuleModal()">+ 新增模块</button>
        </div>

        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>模块名称</th>
                    <th>描述</th>
                    <th>预算工时</th>
                    <th>已用工时</th>
                    <th>使用率</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                ${modules.length === 0 ? '<tr><td colspan="7" style="text-align:center;color:var(--color-ink-mute);padding:40px;">暂无模块，请新增</td></tr>' :
                    modules.map(m => {
                        const used = m.usedHours || 0;
                        const estimated = m.estimatedHours || 0;
                        const ratio = estimated > 0 ? (used / estimated * 100) : 0;
                        const ratioColor = ratio >= 100 ? '#fee2e2' : ratio >= 80 ? '#fef3c7' : '#d1fae5';
                        return `
                        <tr>
                            <td>${m.id}</td>
                            <td><strong>${m.name}</strong></td>
                            <td>${m.description || '-'}</td>
                            <td>${estimated} 小时</td>
                            <td>${used.toFixed(1)} 小时</td>
                            <td>
                                <span class="tag" style="background:${ratioColor};color:var(--color-ink);">
                                    ${ratio.toFixed(0)}%
                                </span>
                            </td>
                            <td style="white-space:nowrap;display:flex;gap:4px;">
                                <button class="btn btn-secondary btn-sm" onclick="openEditModuleModal(${m.id}, '${m.name.replace(/'/g, "\\'")}', '${(m.description || '').replace(/'/g, "\\'")}', ${estimated})">编辑</button>
                                <button class="btn btn-danger btn-sm" onclick="deleteModule(${m.id})">删除</button>
                            </td>
                        </tr>
                    `}).join('')}
            </tbody>
        </table>
    `;
}

// ====== 模块：新增 ======
function openAddModuleModal() {
    moduleEditingId = null;
    showModal('新增任务模块', `
        <div class="form-group"><label class="form-label">模块名称 *</label><input class="form-input" id="mf_name" placeholder="模块名称"></div>
        <div class="form-group"><label class="form-label">预算工时（小时）</label><input class="form-input" id="mf_hours" type="number" min="0" value="0" placeholder="预估工时"></div>
        <div class="form-group"><label class="form-label">模块描述</label><textarea class="form-input" id="mf_description" rows="3" placeholder="模块描述"></textarea></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitModule()">保存</button>
        </div>
    `);
}

// ====== 模块：编辑 ======
function openEditModuleModal(id, name, description, hours) {
    moduleEditingId = id;
    showModal('编辑任务模块', `
        <div class="form-group"><label class="form-label">模块名称 *</label><input class="form-input" id="mf_name" value="${name}"></div>
        <div class="form-group"><label class="form-label">预算工时（小时）</label><input class="form-input" id="mf_hours" type="number" min="0" value="${hours}"></div>
        <div class="form-group"><label class="form-label">模块描述</label><textarea class="form-input" id="mf_description" rows="3">${description}</textarea></div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitModule()">保存</button>
        </div>
    `);
}

// ====== 模块：提交 ======
async function submitModule() {
    const data = {
        name: document.getElementById('mf_name')?.value,
        description: document.getElementById('mf_description')?.value || null,
        estimatedHours: parseInt(document.getElementById('mf_hours')?.value || '0')
    };

    if (!data.name) { showToast('模块名称不能为空', 'error'); return; }

    let res;
    if (moduleEditingId) {
        res = await API.put(`/api/modules/${moduleEditingId}`, data);
    } else {
        res = await API.post(`/api/projects/${moduleProjectId}/modules`, data);
    }

    if (res.code === 200) {
        showToast(moduleEditingId ? '修改成功' : '新增成功', 'success');
        closeModal();
        showModules(moduleProjectId, moduleProjectName);
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

// ====== 模块：删除 ======
async function deleteModule(id) {
    if (!confirm('确定要删除该模块吗？')) return;
    const res = await API.del(`/api/modules/${id}`);
    if (res.code === 200) {
        showToast('删除成功', 'success');
        showModules(moduleProjectId, moduleProjectName);
    } else {
        showToast(res.message || '删除失败', 'error');
    }
}

// 注册到全局
window.renderPage = renderPage;
window.showModules = showModules;
