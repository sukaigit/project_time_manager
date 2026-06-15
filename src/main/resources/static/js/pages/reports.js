// js/pages/reports.js — 统计报表

let currentReportTab = 'personal';
let reportYear = new Date().getFullYear();
let reportMonth = new Date().getMonth() + 1;
let reportProjectId = null;

function renderPage() {
    const content = document.getElementById('pageContent');
    content.innerHTML = `
        <div class="page-header">
            <h1>统计报表</h1>
            <div class="report-controls">
                <label>年份：
                    <select id="reportYear" class="form-select">
                        ${generateYearOptions()}
                    </select>
                </label>
                <label>月份：
                    <select id="reportMonth" class="form-select">
                        ${generateMonthOptions()}
                    </select>
                </label>
                <button class="btn btn-primary" onclick="loadReport()">查询</button>
                <button class="btn btn-success" onclick="exportExcel()">📥 导出Excel</button>
            </div>
        </div>
        <div class="report-tabs">
            <button class="tab-btn ${currentReportTab === 'personal' ? 'active' : ''}" data-tab="personal" onclick="switchTab('personal')">个人统计</button>
            <button class="tab-btn ${currentReportTab === 'project' ? 'active' : ''}" data-tab="project" onclick="switchTab('project')">项目统计</button>
            <button class="tab-btn ${currentReportTab === 'department' ? 'active' : ''}" data-tab="department" onclick="switchTab('department')">部门统计</button>
        </div>
        <div class="report-summary" id="reportSummary"></div>
        <div class="report-table-wrap"><table class="data-table" id="reportTable"><thead></thead><tbody></tbody></table></div>
        <div id="reportStyles"></div>
    `;

    // Inject styles
    if (!document.getElementById('reportCss')) {
        const style = document.createElement('style');
        style.id = 'reportCss';
        style.textContent = `
            .page-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
            .report-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
            .report-controls label { font-size: 13px; color: var(--color-ink-mute); display: flex; align-items: center; gap: 4px; }
            .form-select { padding: 6px 10px; border: 1px solid var(--color-hairline); border-radius: var(--rounded-md); font-size: 13px; background: var(--color-canvas); color: var(--color-ink); }
            .report-tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid var(--color-hairline); padding-bottom: 0; }
            .tab-btn { padding: 8px 20px; border: none; background: transparent; cursor: pointer; font-size: 14px; color: var(--color-ink-mute); border-bottom: 2px solid transparent; transition: all 0.15s; }
            .tab-btn:hover { color: var(--color-ink); }
            .tab-btn.active { color: var(--color-primary, #2563eb); border-bottom-color: var(--color-primary, #2563eb); font-weight: 500; }
            .btn { padding: 6px 16px; border: none; border-radius: var(--rounded-md); cursor: pointer; font-size: 13px; transition: all 0.15s; }
            .btn-primary { background: #2563eb; color: #fff; }
            .btn-primary:hover { background: #1d4ed8; }
            .btn-success { background: #16a34a; color: #fff; }
            .btn-success:hover { background: #15803d; }
            .report-summary { display: flex; gap: 24px; margin-bottom: 16px; padding: 12px 16px; background: var(--color-canvas); border-radius: var(--rounded-lg); border: 1px solid var(--color-hairline); }
            .report-summary-item { text-align: center; }
            .report-summary-item .label { font-size: 12px; color: var(--color-ink-mute); margin-bottom: 2px; }
            .report-summary-item .value { font-size: 20px; font-weight: 600; color: var(--color-ink); }
            .report-table-wrap { overflow-x: auto; }
            .data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
            .data-table th { background: #f8fafc; padding: 10px 12px; text-align: center; border-bottom: 2px solid var(--color-hairline); font-weight: 600; color: var(--color-ink); white-space: nowrap; }
            .data-table td { padding: 10px 12px; text-align: center; border-bottom: 1px solid var(--color-hairline); color: var(--color-ink); }
            .data-table tr:hover td { background: #f1f5f9; }
            .data-table .total-row td { font-weight: 600; background: #f0f9ff; }
            .project-select-wrap { display: inline-flex; align-items: center; gap: 4px; }
        `;
        document.head.appendChild(style);
    }

    loadReport();
}

function generateYearOptions() {
    const currentYear = new Date().getFullYear();
    let html = '';
    for (let y = currentYear - 2; y <= currentYear + 1; y++) {
        html += `<option value="${y}" ${y === reportYear ? 'selected' : ''}>${y}年</option>`;
    }
    return html;
}

function generateMonthOptions() {
    let html = '';
    for (let m = 1; m <= 12; m++) {
        html += `<option value="${m}" ${m === reportMonth ? 'selected' : ''}>${m}月</option>`;
    }
    return html;
}

function switchTab(tab) {
    currentReportTab = tab;
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.toggle('active', btn.dataset.tab === tab));
    loadReport();
}

async function loadReport() {
    reportYear = parseInt(document.getElementById('reportYear').value);
    reportMonth = parseInt(document.getElementById('reportMonth').value);

    try {
        switch (currentReportTab) {
            case 'personal':
                await loadPersonalReport();
                break;
            case 'project':
                await loadProjectReport();
                break;
            case 'department':
                await loadDepartmentReport();
                break;
        }
    } catch (e) {
        console.error('Report load error:', e);
        document.getElementById('reportTable').querySelector('tbody').innerHTML =
            '<tr><td colspan="5" style="padding:32px;color:var(--color-ink-mute)">数据加载失败</td></tr>';
    }
}

// ====== 个人统计 ======

async function loadPersonalReport() {
    const res = await API.get(`/api/reports/personal?year=${reportYear}&month=${reportMonth}`);
    if (res.code !== 200) return;
    renderSummary([{ label: '总工时', value: (res.data.totalHours ?? 0) + 'h' }]);
    renderTable(
        ['项目名称', '工时(小时)', '占比'],
        res.data.details || [],
        ['projectName', 'hours', 'ratio'],
        (d) => d.ratio !== undefined ? (Math.round(d.ratio * 100) + '%') : '—'
    );
}

// ====== 项目统计 ======

async function loadProjectReport() {
    // 需要先获取项目列表，让用户选择
    const projectsRes = await API.get(`/api/projects?page=1&size=200`);
    const projects = projectsRes.code === 200 ? (projectsRes.data.list || []) : [];

    if (!reportProjectId && projects.length > 0) {
        reportProjectId = projects[0].id;
    }

    // 在标签页下方显示项目选择器
    const summaryEl = document.getElementById('reportSummary');
    summaryEl.innerHTML = `
        <div class="project-select-wrap">
            <label style="font-size:13px;color:var(--color-ink-mute);display:flex;align-items:center;gap:4px;">
                选择项目：
                <select id="reportProjectSelect" class="form-select" onchange="onProjectChange()">
                    ${projects.map(p => `<option value="${p.id}" ${p.id === reportProjectId ? 'selected' : ''}>${p.name}</option>`).join('')}
                </select>
            </label>
        </div>
    `;

    if (!reportProjectId) {
        document.getElementById('reportTable').querySelector('thead').innerHTML = '';
        document.getElementById('reportTable').querySelector('tbody').innerHTML = '<tr><td style="padding:32px;color:var(--color-ink-mute)">请先创建项目</td></tr>';
        return;
    }

    const res = await API.get(`/api/reports/project?year=${reportYear}&month=${reportMonth}&projectId=${reportProjectId}`);
    if (res.code !== 200) return;

    renderTable(
        ['模块名称', '预估工时(小时)', '实际工时(小时)', '完成率'],
        res.data.details || [],
        ['moduleName', 'estimatedHours', 'actualHours', 'ratio'],
        (d) => d.ratio !== undefined ? (Math.round(d.ratio * 100) + '%') : '—'
    );
}

function onProjectChange() {
    reportProjectId = parseInt(document.getElementById('reportProjectSelect').value);
    loadProjectReportData();
}

async function loadProjectReportData() {
    if (!reportProjectId) return;
    const res = await API.get(`/api/reports/project?year=${reportYear}&month=${reportMonth}&projectId=${reportProjectId}`);
    if (res.code !== 200) return;
    renderTable(
        ['模块名称', '预估工时(小时)', '实际工时(小时)', '完成率'],
        res.data.details || [],
        ['moduleName', 'estimatedHours', 'actualHours', 'ratio'],
        (d) => d.ratio !== undefined ? (Math.round(d.ratio * 100) + '%') : '—'
    );
}

// ====== 部门统计 ======

async function loadDepartmentReport() {
    const res = await API.get(`/api/reports/department?year=${reportYear}&month=${reportMonth}`);
    if (res.code !== 200) return;

    const data = res.data;
    renderSummary([
        { label: '总工时', value: (data.totalHours ?? 0) + 'h' },
        { label: '平均工时', value: (data.avgHours ?? 0) + 'h' },
        { label: '填报人数', value: data.userCount ?? 0 }
    ]);
    renderTable(
        ['项目名称', '工时(小时)', '占比'],
        data.details || [],
        ['projectName', 'hours', 'ratio'],
        (d) => d.ratio !== undefined ? (Math.round(d.ratio * 100) + '%') : '—'
    );
}

// ====== 通用渲染 ======

function renderSummary(items) {
    const el = document.getElementById('reportSummary');
    el.innerHTML = items.map(item => `
        <div class="report-summary-item">
            <div class="label">${item.label}</div>
            <div class="value">${item.value}</div>
        </div>
    `).join('');
}

function renderTable(headers, data, keys, formatFn) {
    const thead = document.getElementById('reportTable').querySelector('thead');
    const tbody = document.getElementById('reportTable').querySelector('tbody');

    thead.innerHTML = `<tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr>`;

    if (!data || data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="' + headers.length + '" style="padding:32px;color:var(--color-ink-mute)">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = data.map((row, idx) => {
        const isTotal = keys[0] && row[keys[0]] === '合计';
        return `<tr class="${isTotal ? 'total-row' : ''}">${keys.map((key, i) => {
            let val = row[key];
            if (key === 'ratio' && formatFn) {
                val = formatFn(row);
            } else if (key === 'hours' || key === 'actualHours' || key === 'estimatedHours') {
                val = val != null ? val + '' : '0';
            }
            return `<td>${val ?? '—'}</td>`;
        }).join('')}</tr>`;
    }).join('');
}

// ====== Excel 导出 ======

async function exportExcel() {
    const type = currentReportTab;
    const year = reportYear;
    const month = reportMonth;
    let url = `/api/reports/export?type=${type}&year=${year}&month=${month}`;
    if (type === 'project' && reportProjectId) {
        url += `&projectId=${reportProjectId}`;
    }

    try {
        // 使用 rawResponse 获取二进制数据
        const res = await API.request(url, { rawResponse: true });
        if (!res.ok) {
            alert('导出失败');
            return;
        }
        const blob = await res.blob();
        const downloadUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = `${type}_${year}_${month}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(downloadUrl);
    } catch (e) {
        console.error('Export error:', e);
        alert('导出失败');
    }
}

// Register to global
window.renderPage = renderPage;
