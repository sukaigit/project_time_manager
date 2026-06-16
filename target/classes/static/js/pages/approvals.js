// js/pages/approvals.js — 工时审批管理

// ====== State ======
let currentTab = 'pending'; // 'pending' | 'history'
let pendingPage = 1;
let historyPage = 1;
const pageSize = 10;
let pendingTotal = 0;
let historyTotal = 0;
let pendingList = [];
let selectedIds = new Set();

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
    renderTabs();
    if (currentTab === 'pending') {
        fetchPending();
    } else {
        fetchHistory();
    }
}

function renderTabs() {
    const content = document.getElementById('pageContent');
    content.innerHTML = `
        <div class="page-header">
            <h1>工时审批</h1>
        </div>
        <div style="margin-bottom:16px;display:flex;gap:0;border-bottom:2px solid var(--color-border);">
            <button class="tab-btn ${currentTab === 'pending' ? 'active' : ''}" onclick="switchTab('pending')">待审批</button>
            <button class="tab-btn ${currentTab === 'history' ? 'active' : ''}" onclick="switchTab('history')">审批历史</button>
        </div>
        <div id="approvalContent"></div>
    `;
    // Add tab styles if not present
    if (!document.getElementById('approvalTabStyles')) {
        const style = document.createElement('style');
        style.id = 'approvalTabStyles';
        style.textContent = `
            .tab-btn {
                padding: 8px 20px;
                border: none;
                background: none;
                cursor: pointer;
                font-size: 14px;
                color: var(--color-ink-mute);
                border-bottom: 2px solid transparent;
                margin-bottom: -2px;
                transition: all 0.2s;
            }
            .tab-btn.active {
                color: var(--color-primary);
                border-bottom-color: var(--color-primary);
                font-weight: 500;
            }
            .tab-btn:hover {
                color: var(--color-ink);
            }
            .batch-bar {
                display: flex;
                gap: 8px;
                align-items: center;
                padding: 8px 0;
                margin-bottom: 12px;
            }
            .approval-item {
                border: 1px solid var(--color-border);
                border-radius: 8px;
                padding: 12px 16px;
                margin-bottom: 8px;
                display: flex;
                align-items: flex-start;
                gap: 12px;
                transition: box-shadow 0.2s;
            }
            .approval-item:hover {
                box-shadow: var(--shadow-floating);
            }
            .approval-item.selected {
                border-color: var(--color-primary);
                background: rgba(59, 130, 246, 0.04);
            }
            .approval-item .checkbox-col {
                padding-top: 2px;
            }
            .approval-item .info-col {
                flex: 1;
                min-width: 0;
            }
            .approval-item .info-row {
                display: flex;
                flex-wrap: wrap;
                gap: 4px 16px;
                margin-bottom: 4px;
                align-items: center;
            }
            .approval-item .info-row .label {
                font-size: 12px;
                color: var(--color-ink-mute);
            }
            .approval-item .info-row .value {
                font-size: 14px;
            }
            .approval-item .action-col {
                display: flex;
                gap: 6px;
                flex-shrink: 0;
            }
            .approval-item .content-text {
                font-size: 13px;
                color: var(--color-ink-mute);
                margin-top: 2px;
                line-height: 1.4;
            }
            .history-table th {
                white-space: nowrap;
            }
        `;
        document.head.appendChild(style);
    }
}

function switchTab(tab) {
    currentTab = tab;
    selectedIds.clear();
    renderTabs();
}

// ====== 待审批列表 ======

async function fetchPending() {
    const container = document.getElementById('approvalContent');
    container.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    const params = new URLSearchParams({ page: pendingPage, size: pageSize });
    const res = await API.get(`/api/approvals/pending?${params}`);

    if (res.code !== 200) {
        container.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    pendingTotal = res.data.total;
    pendingList = res.data.list || [];
    selectedIds.clear();
    renderPending();
}

function renderPending() {
    const container = document.getElementById('approvalContent');
    const totalPages = Math.max(1, Math.ceil(pendingTotal / pageSize));

    if (pendingList.length === 0) {
        container.innerHTML = `
            <div style="text-align:center;padding:60px 20px;color:var(--color-ink-mute);">
                <div style="font-size:48px;margin-bottom:12px;">✅</div>
                <p>暂无待审批的工时记录</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="batch-bar">
            <input type="checkbox" id="selectAll" onchange="toggleSelectAll()" ${pendingList.every(w => selectedIds.has(w.id)) ? 'checked' : ''}>
            <label for="selectAll" style="font-size:13px;cursor:pointer;">全选</label>
            <span style="font-size:13px;color:var(--color-ink-mute);margin-left:4px;">已选 ${selectedIds.size} 条</span>
            <button class="btn btn-primary btn-sm" onclick="batchApprove()" ${selectedIds.size === 0 ? 'disabled' : ''}>批量通过</button>
            <button class="btn btn-danger btn-sm" onclick="batchReject()" ${selectedIds.size === 0 ? 'disabled' : ''}>批量驳回</button>
        </div>
        <div id="pendingList">
            ${pendingList.map(w => renderPendingItem(w)).join('')}
        </div>
        <div class="pagination">
            <span style="color:var(--color-ink-mute);font-size:13px;">共 ${pendingTotal} 条</span>
            ${pendingPage > 1 ? `<a href="javascript:void(0)" onclick="pendingPage--;fetchPending();">&laquo; 上一页</a>` : ''}
            ${generatePageLinks(totalPages, pendingPage, 'pendingPage', 'fetchPending')}
            ${pendingPage < totalPages ? `<a href="javascript:void(0)" onclick="pendingPage++;fetchPending();">下一页 &raquo;</a>` : ''}
        </div>
    `;
}

function renderPendingItem(w) {
    const checked = selectedIds.has(w.id) ? 'checked' : '';
    const selectedClass = selectedIds.has(w.id) ? 'selected' : '';
    return `
        <div class="approval-item ${selectedClass}" onclick="toggleItem(${w.id})">
            <div class="checkbox-col" onclick="event.stopPropagation()">
                <input type="checkbox" ${checked} onchange="toggleItem(${w.id})">
            </div>
            <div class="info-col">
                <div class="info-row">
                    <span class="label">提交人：</span><span class="value"><strong>${w.userName || '-'}</strong></span>
                    <span class="label">项目：</span><span class="value">${w.projectName || '-'}</span>
                    <span class="label">模块：</span><span class="value">${w.moduleName || '-'}</span>
                </div>
                <div class="info-row">
                    <span class="label">日期：</span><span class="value">${w.workDate || '-'}</span>
                    <span class="label">工时：</span><span class="value"><strong>${w.hours}</strong>h</span>
                    <span class="label">填报时间：</span><span class="value" style="font-size:12px;color:var(--color-ink-mute);">${w.createTime ? w.createTime.substring(0, 16) : '-'}</span>
                </div>
                ${w.content ? `<div class="content-text">${w.content}</div>` : ''}
            </div>
            <div class="action-col">
                <button class="btn btn-primary btn-sm" onclick="event.stopPropagation();approveSingle(${w.id})">通过</button>
                <button class="btn btn-danger btn-sm" onclick="event.stopPropagation();rejectSingle(${w.id})">驳回</button>
            </div>
        </div>
    `;
}

function toggleItem(id) {
    if (selectedIds.has(id)) {
        selectedIds.delete(id);
    } else {
        selectedIds.add(id);
    }
    renderPending();
}

function toggleSelectAll() {
    const selectAll = document.getElementById('selectAll');
    if (selectAll.checked) {
        pendingList.forEach(w => selectedIds.add(w.id));
    } else {
        selectedIds.clear();
    }
    renderPending();
}

async function approveSingle(id) {
    const comment = prompt('审批意见（可选）：');
    if (comment === null) return; // cancelled
    await doBatch([{ id, status: 'APPROVED', comment: comment || '' }]);
}

async function rejectSingle(id) {
    const comment = prompt('驳回原因：');
    if (comment === null) return;
    if (!comment || !comment.trim()) {
        showToast('请输入驳回原因', 'error');
        return;
    }
    await doBatch([{ id, status: 'REJECTED', comment: comment.trim() }]);
}

async function batchApprove() {
    if (selectedIds.size === 0) { showToast('请选择要审批的工时记录', 'error'); return; }
    const comment = prompt('审批意见（可选）：');
    if (comment === null) return;
    const items = Array.from(selectedIds).map(id => ({ id, status: 'APPROVED', comment: comment || '' }));
    await doBatch(items);
}

async function batchReject() {
    if (selectedIds.size === 0) { showToast('请选择要审批的工时记录', 'error'); return; }
    const comment = prompt('驳回原因（可选）：');
    if (comment === null) return;
    const items = Array.from(selectedIds).map(id => ({ id, status: 'REJECTED', comment: comment || '' }));
    await doBatch(items);
}

async function doBatch(items) {
    const res = await API.put('/api/approvals/batch', { items });
    if (res.code === 200) {
        showToast('审批完成', 'success');
        selectedIds.clear();
        fetchPending();
    } else {
        showToast(res.message || '审批失败', 'error');
    }
}

// ====== 审批历史 ======

async function fetchHistory() {
    const container = document.getElementById('approvalContent');
    container.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    const params = new URLSearchParams({ page: historyPage, size: pageSize });
    const res = await API.get(`/api/approvals/history?${params}`);

    if (res.code !== 200) {
        container.innerHTML = `<div class="alert alert-error">加载失败：${res.message}</div>`;
        return;
    }

    historyTotal = res.data.total;
    renderHistory(res.data.list || []);
}

function renderHistory(list) {
    const container = document.getElementById('approvalContent');
    const totalPages = Math.max(1, Math.ceil(historyTotal / pageSize));

    if (list.length === 0) {
        container.innerHTML = `
            <div style="text-align:center;padding:60px 20px;color:var(--color-ink-mute);">
                <div style="font-size:48px;margin-bottom:12px;">📋</div>
                <p>暂无审批历史</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <table class="history-table">
            <thead>
                <tr>
                    <th>工时记录ID</th>
                    <th>审批人</th>
                    <th>审批结果</th>
                    <th>审批意见</th>
                    <th>审批时间</th>
                </tr>
            </thead>
            <tbody>
                ${list.map(a => `
                    <tr>
                        <td>#${a.workHourId}</td>
                        <td>${a.approverName || '-'}</td>
                        <td><span class="tag" style="background:${STATUS_COLORS[a.status] || '#e2e8f0'};color:var(--color-ink);">${STATUS_LABELS[a.status] || a.status}</span></td>
                        <td style="max-width:200px;">${a.comment || '-'}</td>
                        <td style="font-size:12px;color:var(--color-ink-mute);">${a.approveTime ? a.approveTime.substring(0, 16) : '-'}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
        <div class="pagination">
            <span style="color:var(--color-ink-mute);font-size:13px;">共 ${historyTotal} 条</span>
            ${historyPage > 1 ? `<a href="javascript:void(0)" onclick="historyPage--;fetchHistory();">&laquo; 上一页</a>` : ''}
            ${generatePageLinks(totalPages, historyPage, 'historyPage', 'fetchHistory')}
            ${historyPage < totalPages ? `<a href="javascript:void(0)" onclick="historyPage++;fetchHistory();">下一页 &raquo;</a>` : ''}
        </div>
    `;
}

// ====== 通用 ======

function generatePageLinks(totalPages, currentPage, pageVar, fetchFn) {
    let links = '';
    const maxShow = 7;
    let start = Math.max(1, currentPage - Math.floor(maxShow / 2));
    let end = Math.min(totalPages, start + maxShow - 1);
    if (end - start < maxShow - 1) start = Math.max(1, end - maxShow + 1);

    for (let i = start; i <= end; i++) {
        links += i === currentPage
            ? `<span class="active">${i}</span>`
            : `<a href="javascript:void(0)" onclick="${pageVar}=${i};${fetchFn}();">${i}</a>`;
    }
    return links;
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

// Register to global
window.renderPage = renderPage;
