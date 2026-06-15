// js/pages/workhours.js — 工时填报管理

// ====== State ======
let currentPage = 1;
let pageSize = 10;
let totalRecords = 0;
let editingId = null;

let filterProjectId = '';
let filterStartDate = '';
let filterEndDate = '';
let filterStatus = '';

let projects = [];
let modules = [];

const STATUS_COLORS = {
    PENDING: '#fef3c7',
    APPROVED: '#d1fae5',
    REJECTED: '#fee2e2'
};

const STATUS_LABELS = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回'
};

// ====== Render ======
function renderPage() {
    fetchProjects();
    fetchWorkHours();
}

// ====== 获取项目列表 ======
async function fetchProjects() {
    const res = await API.get('/api/projects?page=1&size=1000');
    if (res.code === 200) {
        projects = res.data.list || [];
    }
}

// ====== 获取模块列表 ======
async function fetchModules(projectId) {
    if (!projectId) { modules = []; return; }
    const res = await API.get(`/api/projects/${projectId}/modules`);
    if (res.code === 200) {
        modules = res.data || [];
    }
}

// ====== 获取工时列表 ======
async function fetchWorkHours() {
    const content = document.getElementById('pageContent');
    content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    const params = new URLSearchParams({ page: currentPage, size: pageSize });
    if (filterProjectId) params.set('projectId', filterProjectId);
    if (filterStartDate) params.set('startDate', filterStartDate);
    if (filterEndDate) params.set('endDate', filterEndDate);
    if (filterStatus) params.set('status', filterStatus);

    const res = await API.get(`/api/work-hours?${params}`);
    if (res.code !== 200) {
        content.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    totalRecords = res.data.total;
    renderTable(res.data.list);
}

// ====== 渲染表格 ======
function renderTable(list) {
    const content = document.getElementById('pageContent');
    const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
    const role = getRole();

    // 项目筛选选项
    const projectOptions = projects.map(p =>
        `<option value="${p.id}" ${p.id == filterProjectId ? 'selected' : ''}>${p.name}</option>`
    ).join('');

    content.innerHTML = `
        <div class="page-header">
            <h1>我的工时</h1>
            <button class="btn btn-primary" onclick="openAddModal()">+ 填报工时</button>
        </div>

        <div style="margin-bottom:16px;display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
            <select class="form-select" id="filterProjectId" style="max-width:180px;" onchange="filterProjectId=this.value||'';currentPage=1;fetchWorkHours();">
                <option value="">全部项目</option>
                ${projectOptions}
            </select>
            <input class="form-input" id="filterStartDate" type="date" style="max-width:150px;" value="${filterStartDate}" placeholder="开始日期">
            <span style="color:var(--color-ink-mute);font-size:13px;">至</span>
            <input class="form-input" id="filterEndDate" type="date" style="max-width:150px;" value="${filterEndDate}" placeholder="结束日期">
            <select class="form-select" id="filterStatus" style="max-width:130px;" onchange="filterStatus=this.value||'';currentPage=1;fetchWorkHours();">
                <option value="">全部状态</option>
                <option value="PENDING" ${filterStatus === 'PENDING' ? 'selected' : ''}>待审批</option>
                <option value="APPROVED" ${filterStatus === 'APPROVED' ? 'selected' : ''}>已通过</option>
                <option value="REJECTED" ${filterStatus === 'REJECTED' ? 'selected' : ''}>已驳回</option>
            </select>
            <button class="btn btn-primary btn-sm" onclick="filterStartDate=document.getElementById('filterStartDate').value;filterEndDate=document.getElementById('filterEndDate').value;currentPage=1;fetchWorkHours();">查询</button>
            ${(filterProjectId || filterStartDate || filterEndDate || filterStatus) ? `<button class="btn btn-secondary btn-sm" onclick="filterProjectId='';filterStartDate='';filterEndDate='';filterStatus='';currentPage=1;fetchWorkHours();document.getElementById('filterProjectId').value='';document.getElementById('filterStartDate').value='';document.getElementById('filterEndDate').value='';document.getElementById('filterStatus').value='';">清除</button>` : ''}
        </div>

        <table>
            <thead>
                <tr>
                    <th>项目</th>
                    <th>模块</th>
                    <th>工作日期</th>
                    <th>工时</th>
                    <th>工作内容</th>
                    <th>状态</th>
                    <th>填报时间</th>
                    ${role === 'ADMIN' ? '<th>填报人</th>' : ''}
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                ${list.length === 0 ? '<tr><td colspan="9" style="text-align:center;color:var(--color-ink-mute);padding:40px;">暂无工时记录</td></tr>' :
                    list.map(w => `
                        <tr>
                            <td>${w.projectName || '-'}</td>
                            <td>${w.moduleName || '-'}</td>
                            <td>${w.workDate}</td>
                            <td><strong>${w.hours}</strong></td>
                            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${w.content || ''}">${w.content || '-'}</td>
                            <td><span class="tag" style="background:${STATUS_COLORS[w.status] || '#e2e8f0'};color:var(--color-ink);">${STATUS_LABELS[w.status] || w.status}</span></td>
                            <td style="font-size:12px;color:var(--color-ink-mute);">${w.createTime ? w.createTime.substring(0, 16) : '-'}</td>
                            ${role === 'ADMIN' ? `<td>${w.userName || '-'}</td>` : ''}
                            <td style="white-space:nowrap;display:flex;gap:4px;">
                                ${w.status !== 'APPROVED' ? `
                                    <button class="btn btn-secondary btn-sm" onclick="openEditModal(${w.id})">编辑</button>
                                    <button class="btn btn-danger btn-sm" onclick="deleteWorkHour(${w.id})">删除</button>
                                ` : `
                                    <span style="font-size:12px;color:var(--color-ink-mute);">—</span>
                                `}
                            </td>
                        </tr>
                    `).join('')}
            </tbody>
        </table>

        <div class="pagination">
            <span style="color:var(--color-ink-mute);font-size:13px;">共 ${totalRecords} 条</span>
            ${currentPage > 1 ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage - 1};fetchWorkHours();">&laquo; 上一页</a>` : ''}
            ${generatePageLinks(totalPages)}
            ${currentPage < totalPages ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage + 1};fetchWorkHours();">下一页 &raquo;</a>` : ''}
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
        links += i === currentPage ? `<span class="active">${i}</span>` : `<a href="javascript:void(0)" onclick="currentPage=${i};fetchWorkHours();">${i}</a>`;
    }
    return links;
}

// ====== 弹窗工具 ======
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

// ====== 填报 ======
function openAddModal() {
    editingId = null;
    renderFormModal('填报工时', null);
}

// ====== 编辑 ======
async function openEditModal(id) {
    editingId = id;
    const res = await API.get(`/api/work-hours?page=1&size=1000`);
    if (res.code !== 200) { showToast('获取数据失败', 'error'); return; }
    const wh = res.data.list.find(w => w.id === id);
    if (!wh) { showToast('工时记录不存在', 'error'); return; }

    // 加载对应项目的模块
    await fetchModules(wh.projectId);

    renderFormModal('编辑工时', wh);
}

// ====== 渲染表单弹窗 ======
function renderFormModal(title, wh) {
    const projectOptions = projects.map(p => {
        const selected = wh && p.id === wh.projectId ? 'selected' : '';
        return `<option value="${p.id}" ${selected}>${p.name}</option>`;
    }).join('');
    const moduleOptions = modules.map(m => {
        const selected = wh && m.id === wh.moduleId ? 'selected' : '';
        return `<option value="${m.id}" ${selected}>${m.name}（预算：${m.estimatedHours}h）</option>`;
    }).join('');

    showModal(title, `
        <div class="form-group">
            <label class="form-label">项目 *</label>
            <select class="form-select" id="f_projectId" onchange="onProjectChange(this.value)">
                <option value="">请选择项目</option>
                ${projectOptions}
            </select>
        </div>
        <div class="form-group">
            <label class="form-label">任务模块</label>
            <select class="form-select" id="f_moduleId">
                <option value="">请选择模块（PM可不选）</option>
                ${moduleOptions}
            </select>
            <div id="budgetInfo" style="margin-top:4px;font-size:12px;color:var(--color-ink-mute);display:none;"></div>
        </div>
        <div class="form-group">
            <label class="form-label">工作日期 *</label>
            <input class="form-input" id="f_workDate" type="date" value="${wh ? wh.workDate : new Date().toISOString().substring(0, 10)}">
        </div>
        <div class="form-group">
            <label class="form-label">工时（小时）*</label>
            <input class="form-input" id="f_hours" type="number" step="0.1" min="0.1" max="999.9" value="${wh ? wh.hours : ''}" placeholder="例如：4.0">
        </div>
        <div class="form-group">
            <label class="form-label">工作内容</label>
            <textarea class="form-input" id="f_content" rows="3" placeholder="描述今天的工作内容">${wh ? (wh.content || '') : ''}</textarea>
        </div>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-primary" onclick="submitWorkHour()">保存</button>
        </div>
    `);
}

// ====== 项目切换时加载模块 ======
window.onProjectChange = async function(projectId) {
    if (!projectId) return;
    const res = await API.get(`/api/projects/${projectId}/modules`);
    if (res.code === 200) {
        modules = res.data || [];
        const sel = document.getElementById('f_moduleId');
        sel.innerHTML = '<option value="">请选择模块</option>' +
            modules.map(m => `<option value="${m.id}">${m.name}（预算：${m.estimatedHours}h）</option>`).join('');
    }
};

// ====== 提交 ======
async function submitWorkHour() {
    const projectId = document.getElementById('f_projectId')?.value;
    const moduleId = document.getElementById('f_moduleId')?.value || null;
    const workDate = document.getElementById('f_workDate')?.value;
    const hours = parseFloat(document.getElementById('f_hours')?.value || '0');
    const content = document.getElementById('f_content')?.value || null;

    if (!projectId) { showToast('请选择项目', 'error'); return; }
    if (!workDate) { showToast('请选择工作日期', 'error'); return; }
    if (!hours || hours <= 0) { showToast('请输入有效的工时', 'error'); return; }

    const data = { projectId: parseInt(projectId), workDate, hours, content };
    if (moduleId) data.moduleId = parseInt(moduleId);

    let res;
    if (editingId) {
        res = await API.put(`/api/work-hours/${editingId}`, data);
    } else {
        res = await API.post('/api/work-hours', data);
    }

    if (res.code === 200) {
        showToast(editingId ? '修改成功' : '填报成功', 'success');
        closeModal();
        fetchWorkHours();
    } else {
        showToast(res.message || '操作失败', 'error');
    }
}

// ====== 删除 ======
async function deleteWorkHour(id) {
    const overlay = showModal('确认删除', `
        <p style="color:var(--color-ink-mute);">确定要删除该工时记录吗？</p>
        <p style="font-size:13px;color:var(--color-ink-mute);">已审批的工时不可删除。</p>
        <div style="margin-top:20px;display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn btn-secondary" onclick="closeModal()">取消</button>
            <button class="btn btn-danger" id="confirmDeleteBtn">确认删除</button>
        </div>
    `);
    document.getElementById('confirmDeleteBtn').onclick = async () => {
        const res = await API.del(`/api/work-hours/${id}`);
        if (res.code === 200) {
            showToast('删除成功', 'success');
            closeModal();
            if ((currentPage - 1) * pageSize >= totalRecords - 1 && currentPage > 1) currentPage--;
            fetchWorkHours();
        } else {
            showToast(res.message || '删除失败', 'error');
        }
    };
}

// 注册到全局
window.renderPage = renderPage;
