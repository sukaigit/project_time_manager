// 角色菜单配置
const MENU_CONFIG = {
    ADMIN: [
        { key: 'dashboard', label: '首页大屏', icon: '📊' },
        { key: 'users', label: '用户管理', icon: '👥' },
        { key: 'projects', label: '项目管理', icon: '📁' },
        { key: 'reports', label: '统计报表', icon: '📈' },
        { key: 'system', label: '系统管理', icon: '⚙️' }
    ],
    DEPT_MANAGER: [
        { key: 'dashboard', label: '首页大屏', icon: '📊' },
        { key: 'approvals', label: '工时审批', icon: '✅' },
        { key: 'reports', label: '统计报表', icon: '📈' }
    ],
    PM: [
        { key: 'dashboard', label: '首页大屏', icon: '📊' },
        { key: 'workhours', label: '我的工时', icon: '⏱️' },
        { key: 'approvals', label: '工时审批', icon: '✅' },
        { key: 'projects', label: '项目管理', icon: '📁' },
        { key: 'reports', label: '统计报表', icon: '📈' }
    ],
    USER: [
        { key: 'dashboard', label: '首页大屏', icon: '📊' },
        { key: 'workhours', label: '我的工时', icon: '⏱️' },
        { key: 'reports', label: '统计报表', icon: '📈' }
    ]
};

// 页面标题映射
const PAGE_TITLES = {
    dashboard: '首页大屏',
    users: '用户管理',
    projects: '项目管理',
    reports: '统计报表',
    system: '系统管理',
    approvals: '工时审批',
    workhours: '我的工时'
};

let currentPage = '';

// 获取当前用户角色
function getRole() {
    return localStorage.getItem('role') || '';
}

// 获取当前用户名称
function getUserName() {
    return localStorage.getItem('userName') || '';
}

// 退出登录
function logout() {
    localStorage.clear();
    window.location.href = '/login.html';
}

// 渲染侧边栏
function renderSidebar() {
    const role = getRole();
    const nav = document.getElementById('sidebarNav');
    const menuItems = MENU_CONFIG[role] || [];
    nav.innerHTML = menuItems.map(item => `
        <a href="javascript:void(0)" class="${currentPage === item.key ? 'active' : ''}" onclick="navigateTo('${item.key}')">
            ${item.icon} ${item.label}
        </a>
    `).join('');
}

// 导航到指定页面
function navigateTo(pageKey) {
    currentPage = pageKey;

    // 更新侧边栏高亮
    document.querySelectorAll('#sidebarNav a').forEach(a => a.classList.remove('active'));
    const links = document.querySelectorAll('#sidebarNav a');
    const menuItems = MENU_CONFIG[getRole()] || [];
    const idx = menuItems.findIndex(m => m.key === pageKey);
    if (links[idx]) links[idx].classList.add('active');

    // 更新页面标题
    document.getElementById('pageTitle').textContent = PAGE_TITLES[pageKey] || pageKey;

    // 动态加载页面脚本并渲染内容
    const content = document.getElementById('pageContent');
    content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">加载中...</div>';

    // 尝试加载对应的页面模块
    const script = document.createElement('script');
    script.src = `js/pages/${pageKey}.js`;
    script.onload = () => {
        if (typeof window.renderPage === 'function') {
            window.renderPage(pageKey);
        } else {
            content.innerHTML = `<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">页面 ${PAGE_TITLES[pageKey]} 待实现</div>`;
        }
    };
    script.onerror = () => {
        content.innerHTML = `<div style="text-align:center;padding:40px;color:var(--color-ink-mute)">页面 ${PAGE_TITLES[pageKey]} 待实现</div>`;
    };
    document.body.appendChild(script);
}

// 初始化
function init() {
    // 检查登录状态
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return;
    }

    // 显示用户名
    document.getElementById('userName').textContent = getUserName();

    // 渲染侧边栏
    renderSidebar();

    // 默认导航到首页
    navigateTo('dashboard');
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', init);
