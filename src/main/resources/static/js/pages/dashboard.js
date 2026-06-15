// js/pages/dashboard.js — 首页大屏

// ====== Render ======
function renderPage() {
    const content = document.getElementById('pageContent');
    content.innerHTML = `
        <div class="page-header">
            <h1>首页大屏</h1>
        </div>
        <!-- 指标卡行 -->
        <div class="dashboard-cards" id="dashboardCards">
            <div class="stat-card">
                <div class="stat-label">今日提交人数</div>
                <div class="stat-value" id="statSubmitCount">—</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">今日总工时</div>
                <div class="stat-value" id="statTotalHours">—</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">待审批数</div>
                <div class="stat-value" id="statPendingCount">—</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">总用户数</div>
                <div class="stat-value" id="statTotalUsers">—</div>
            </div>
        </div>
        <!-- 图表行 -->
        <div class="dashboard-charts">
            <div class="chart-card">
                <div class="chart-card-header">本月完成率</div>
                <div id="chartMonthlyRate" style="height:280px;"></div>
            </div>
            <div class="chart-card">
                <div class="chart-card-header">项目工时分布</div>
                <div id="chartProjectDist" style="height:280px;"></div>
            </div>
        </div>
        <div class="dashboard-charts" style="margin-top:16px;">
            <div class="chart-card" style="flex:1;">
                <div class="chart-card-header">整体概览</div>
                <div id="chartOverview" style="height:200px;display:flex;align-items:center;justify-content:space-around;padding:16px 0;">
                    <div style="text-align:center;">
                        <div style="font-size:13px;color:var(--color-ink-mute);margin-bottom:4px;">总工时</div>
                        <div style="font-size:28px;font-weight:600;" id="overviewTotalHours">—</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:13px;color:var(--color-ink-mute);margin-bottom:4px;">平均工时</div>
                        <div style="font-size:28px;font-weight:600;" id="overviewAvgHours">—</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:13px;color:var(--color-ink-mute);margin-bottom:4px;">用户数</div>
                        <div style="font-size:28px;font-weight:600;" id="overviewTotalUsers">—</div>
                    </div>
                </div>
            </div>
        </div>
        <div id="dashboardStyles"></div>
    `;

    // Inject dashboard-specific styles
    if (!document.getElementById('dashCss')) {
        const style = document.createElement('style');
        style.id = 'dashCss';
        style.textContent = `
            .dashboard-cards {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 16px;
                margin-bottom: 16px;
            }
            .dashboard-cards .stat-card {
                text-align: center;
            }
            .dashboard-charts {
                display: flex;
                gap: 16px;
            }
            .chart-card {
                flex: 1;
                background: var(--color-canvas);
                border-radius: var(--rounded-lg);
                box-shadow: var(--shadow-card);
                border: 1px solid var(--color-hairline);
                padding: 16px;
            }
            .chart-card-header {
                font-size: 15px;
                font-weight: 500;
                color: var(--color-ink);
                margin-bottom: 8px;
            }
            @media (max-width: 768px) {
                .dashboard-cards { grid-template-columns: repeat(2, 1fr); }
                .dashboard-charts { flex-direction: column; }
            }
        `;
        document.head.appendChild(style);
    }

    // Load all data
    loadDashboard();
}

// ====== Data Loading ======
async function loadDashboard() {
    try {
        const [todayRes, pendingRes, monthlyRes, distRes, overviewRes] = await Promise.all([
            API.get('/api/dashboard/today'),
            API.get('/api/dashboard/pending'),
            API.get('/api/dashboard/monthly-rate'),
            API.get('/api/dashboard/project-distribution'),
            API.get('/api/dashboard/overview')
        ]);

        // 指标卡
        if (todayRes.code === 200) {
            document.getElementById('statSubmitCount').textContent = todayRes.data.submitCount ?? '—';
            document.getElementById('statTotalHours').textContent = (todayRes.data.totalHours ?? 0) + 'h';
        }
        if (pendingRes.code === 200) {
            document.getElementById('statPendingCount').textContent = pendingRes.data.count ?? '—';
        }
        if (overviewRes.code === 200) {
            document.getElementById('statTotalUsers').textContent = overviewRes.data.totalUsers ?? '—';
            document.getElementById('overviewTotalHours').textContent = (overviewRes.data.totalHours ?? 0) + 'h';
            document.getElementById('overviewAvgHours').textContent = (overviewRes.data.avgHours ?? 0) + 'h';
            document.getElementById('overviewTotalUsers').textContent = overviewRes.data.totalUsers ?? '—';
        }

        // 图表1：本月完成率 — 环形图
        if (monthlyRes.code === 200) {
            renderMonthlyRate(monthlyRes.data);
        }

        // 图表2：项目工时分布 — 饼图
        if (distRes.code === 200) {
            renderProjectDistribution(distRes.data || []);
        }
    } catch (e) {
        console.error('Dashboard load error:', e);
    }
}

// ====== 图表渲染 ======

function renderMonthlyRate(data) {
    const dom = document.getElementById('chartMonthlyRate');
    if (!dom) return;

    const totalHours = data.totalHours || 0;
    const rate = data.rate || 0;
    const ratePercent = Math.round(rate * 100);

    const chart = echarts.init(dom);
    chart.setOption({
        tooltip: {
            formatter: function() {
                return `总工时：${totalHours}h<br>完成率：${ratePercent}%`;
            }
        },
        series: [{
            type: 'gauge',
            center: ['50%', '55%'],
            radius: '90%',
            startAngle: 220,
            endAngle: -40,
            min: 0,
            max: 100,
            splitNumber: 5,
            progress: {
                show: true,
                width: 18,
                roundCap: true
            },
            axisLine: {
                lineStyle: {
                    width: 18,
                    color: [
                        [1, '#e8ecf1']
                    ]
                }
            },
            axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: { show: false },
            detail: {
                offsetCenter: [0, '50%'],
                valueAnimation: true,
                formatter: function() {
                    return ratePercent + '%';
                },
                fontSize: 32,
                fontWeight: 600,
                color: '#0d253d'
            },
            data: [{
                value: ratePercent,
                name: '完成率'
            }]
        }]
    });

    // Resize handler
    window.addEventListener('resize', () => chart.resize());
}

function renderProjectDistribution(list) {
    const dom = document.getElementById('chartProjectDist');
    if (!dom) return;

    if (!list || list.length === 0) {
        dom.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:var(--color-ink-mute);font-size:14px;">暂无数据</div>';
        return;
    }

    const names = list.map(d => d.projectName || '未命名');
    const values = list.map(d => parseFloat(d.hours) || 0);
    const total = values.reduce((a, b) => a + b, 0);

    const chart = echarts.init(dom);
    chart.setOption({
        tooltip: {
            trigger: 'item',
            formatter: function(params) {
                const pct = total > 0 ? ((params.value / total) * 100).toFixed(1) : 0;
                return `${params.name}: ${params.value}h (${pct}%)`;
            }
        },
        series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['50%', '55%'],
            avoidLabelOverlap: true,
            itemStyle: {
                borderRadius: 4,
                borderColor: '#fff',
                borderWidth: 2
            },
            label: {
                show: true,
                formatter: '{b}: {c}h',
                fontSize: 12,
                color: '#64748b'
            },
            labelLine: {
                show: true,
                lineStyle: { color: '#e3e8ee' }
            },
            emphasis: {
                label: { fontSize: 14, fontWeight: 'bold' }
            },
            data: names.map((name, i) => ({
                name: name,
                value: values[i]
            }))
        }]
    });

    window.addEventListener('resize', () => chart.resize());
}

// Register to global
window.renderPage = renderPage;
