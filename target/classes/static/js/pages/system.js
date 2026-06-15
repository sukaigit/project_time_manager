// js/pages/system.js — 系统管理页面（操作日志）

const ACTION_LABELS = {
    CREATE: '新增',
    UPDATE: '修改',
    DELETE: '删除',
    LOGIN: '登录',
    APPROVE: '审批通过',
    REJECT: '驳回'
};
const ACTION_COLORS = {
    CREATE: '#d1fae5',
    UPDATE: '#dbeafe',
    DELETE: '#fee2e2',
    LOGIN: '#e0e7ff',
    APPROVE: '#d1fae5',
    REJECT: '#fee2e2'
};

let currentPage = 1;
let pageSize = 10;
let totalLogs = 0;
let filters = { userId: '', action: '', startDate: '', endDate: '' };

function renderPage() {
    fetchLogs();
}

// ====== 数据获取 ======
async function fetchLogs() {
    const content = document.getElementById('pageContent');
    content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)\">加载中...</div>';

    const params = new URLSearchParams({ page: currentPage, size: pageSize });
    if (filters.userId) params.set('userId', filters.userId);
    if (filters.action) params.set('action', filters.action);
    if (filters.startDate) params.set('startDate', filters.startDate);
    if (filters.endDate) params.set('endDate', filters.endDate);

    const res = await API.get(`/api/logs?${params}`);
    if (res.code !== 200) {
        content.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    totalLogs = res.data.total;
    renderTable(res.data.list);
}

// ====== 渲染表格 ======
function renderTable(logs) {
    const content = document.getElementById('pageContent');
    const totalPages = Math.max(1, Math.ceil(totalLogs / pageSize));

    content.innerHTML = `
        <div class="page-header">
            <h1>操作日志</h1>
        </div>

        <div style="margin-bottom:16px;display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
            <input class="form-input" id="filterUserId" placeholder="操作人ID" style="max-width:120px;"
                value="${filters.userId}" onkeydown="if(event.key==='Enter'){applyFilters();}">
            <select class="form-select" id="filterAction" style="max-width:140px;">
                <option value="">全部操作</option>
                <option value="CREATE" ${filters.action === 'CREATE' ? 'selected' : ''}>新增</option>
                <option value="UPDATE" ${filters.action === 'UPDATE' ? 'selected' : ''}>修改</option>
                <option value="DELETE" ${filters.action === 'DELETE' ? 'selected' : ''}>删除</option>
                <option value="LOGIN" ${filters.action === 'LOGIN' ? 'selected' : ''}>登录</option>
                <option value="APPROVE" ${filters.action === 'APPROVE' ? 'selected' : ''}>审批通过</option>
                <option value="REJECT" ${filters.action === 'REJECT' ? 'selected' : ''}>驳回</option>
            </select>
            <input class="form-input" id="filterStartDate" type="date" style="max-width:160px;"
                value="${filters.startDate}" placeholder="开始日期">
            <span style="color:var(--color-ink-mute);">至</span>
            <input class="form-input" id="filterEndDate" type="date" style="max-width:160px;"
                value="${filters.endDate}" placeholder="结束日期">
            <button class="btn btn-primary" onclick="applyFilters()">筛选</button>
            ${filters.userId || filters.action || filters.startDate || filters.endDate
                ? `<button class="btn btn-secondary" onclick="resetFilters()">清除</button>` : ''}
        </div>

        <table>
            <thead>
                <tr>
                    <th style="width:60px;">ID</th>
                    <th>操作人</th>
                    <th style="width:100px;">操作类型</th>
                    <th>操作目标</th>
                    <th>操作详情</th>
                    <th style="width:180px;">操作时间</th>
                </tr>
            </thead>
            <tbody>
                ${logs.length === 0
                    ? '<tr><td colspan="6" style="text-align:center;color:var(--color-ink-mute);padding:40px;">暂无日志</td></tr>'
                    : logs.map(log => `
                        <tr>
                            <td>${log.id}</td>
                            <td>${log.userName || '-'}</td>
                            <td><span class="tag" style="background:${ACTION_COLORS[log.action] || '#e2e8f0'};color:var(--color-ink);">${ACTION_LABELS[log.action] || log.action}</span></td>
                            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${escapeHtml(log.target || '')}">${escapeHtml(log.target || '-')}</td>
                            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${escapeHtml(log.detail || '')}">${escapeHtml(log.detail || '-')}</td>
                            <td>${formatTime(log.createTime)}</td>
                        </tr>
                    `).join('')}
            </tbody>
        </table>

        <div class="pagination">
            <span style="color:var(--color-ink-mute);font-size:13px;">共 ${totalLogs} 条</span>
            ${currentPage > 1 ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage - 1};fetchLogs();">&laquo; 上一页</a>` : ''}
            ${generatePageLinks(totalPages)}
            ${currentPage < totalPages ? `<a href="javascript:void(0)" onclick="currentPage=${currentPage + 1};fetchLogs();">下一页 &raquo;</a>` : ''}
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
        links += i === currentPage
            ? `<span class="active">${i}</span>`
            : `<a href="javascript:void(0)" onclick="currentPage=${i};fetchLogs();">${i}</a>`;
    }
    return links;
}

function applyFilters() {
    filters.userId = document.getElementById('filterUserId').value.trim();
    filters.action = document.getElementById('filterAction').value;
    filters.startDate = document.getElementById('filterStartDate').value;
    filters.endDate = document.getElementById('filterEndDate').value;
    currentPage = 1;
    fetchLogs();
}

function resetFilters() {
    filters = { userId: '', action: '', startDate: '', endDate: '' };
    currentPage = 1;
    fetchLogs();
}

function formatTime(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// 注册到全局
window.renderPage = renderPage;
