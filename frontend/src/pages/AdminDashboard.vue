<template>
  <main class="admin-dashboard" data-testid="admin-shell">
    <header class="admin-hero">
      <div>
        <p class="eyebrow">五行人格卡 · 数据中台</p>
        <h1>行为采集与实时监控</h1>
        <p class="hero-copy">
          从匿名访问、在线心跳、结果生成到后台聚合，展示一个轻量产品的数据闭环。
        </p>
      </div>
      <div class="hero-status" :class="{ 'is-offline': !overview }">
        <span class="pulse-dot"></span>
        <strong>{{ overview ? '已连接' : '等待登录' }}</strong>
        <small>{{ lastLoadedAt || '输入管理 Token 后进入后台' }}</small>
      </div>
    </header>

    <section v-if="!overview" class="login-panel" aria-label="管理端登录">
      <div>
        <p class="section-kicker">Admin Token</p>
        <h2>进入统计后台</h2>
        <p>登录成功后将直接进入正式中台，Token 不再在页面顶部长期展示。</p>
      </div>
      <form class="login-form" @submit.prevent="submitToken">
        <label for="admin-token">管理端 Token</label>
        <input
          id="admin-token"
          v-model.trim="tokenInput"
          type="password"
          autocomplete="current-password"
          placeholder="请输入 ADMIN_TOKEN"
        />
        <button type="submit" data-testid="admin-login-button" :disabled="loading || !tokenInput">
          {{ loading ? '连接中...' : '进入后台' }}
        </button>
        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      </form>
    </section>

    <div v-else class="admin-layout desktop-admin-layout">
      <aside class="admin-side-nav" data-testid="admin-side-nav" aria-label="数据中台导航">
        <div class="nav-brand">
          <span class="nav-mark">数</span>
          <div>
            <strong>Wuxing Ops</strong>
            <small>真实数据 · 监控视角</small>
          </div>
        </div>
        <nav>
          <button
            v-for="page in dashboardPages"
            :key="page.key"
            type="button"
            :class="{ active: activePage === page.key }"
            @click="activePage = page.key"
          >
            <span>{{ page.icon }}</span>
            <strong>{{ page.label }}</strong>
            <small>{{ page.description }}</small>
          </button>
        </nav>
        <div class="nav-footer">
          <span>Token 已保存到本机浏览器</span>
          <button type="button" @click="logout">退出</button>
        </div>
      </aside>

      <section class="admin-workbench">
        <div class="toolbar">
          <div>
            <p class="section-kicker">当前视图</p>
            <h2>{{ currentPage?.label }}</h2>
          </div>
          <div class="toolbar-actions">
            <div class="date-controls" aria-label="统计范围">
              <button type="button" @click="setQuickRange(0)">今日</button>
              <button type="button" @click="setQuickRange(6)">近 7 天</button>
              <button type="button" @click="setQuickRange(13)">近 14 天</button>
              <input v-model="filters.startDate" type="date" aria-label="开始日期" />
              <input v-model="filters.endDate" type="date" aria-label="结束日期" />
            </div>
            <label class="switch">
              <input v-model="filters.includeSynthetic" type="checkbox" />
              <span>包含压测流量</span>
            </label>
            <button type="button" class="ghost-action" :disabled="loading" @click="refreshAll(true)">
              {{ loading ? '刷新中' : '强制刷新' }}
            </button>
          </div>
        </div>

        <p v-if="errorMessage" class="inline-error" role="alert">{{ errorMessage }}</p>

        <section v-if="activePage === 'realtime'" class="page-stack" data-testid="admin-realtime-console">
          <div class="kpi-grid">
            <article class="kpi-card primary">
              <span>当前在线用户</span>
              <strong>{{ realtime?.currentOnlineVisitors ?? 0 }}</strong>
              <small>在线窗口 {{ realtime?.onlineWindowSeconds ?? 120 }} 秒</small>
            </article>
            <article class="kpi-card">
              <span>今日 PV</span>
              <strong>{{ formatNumber(realtime?.todayPv ?? 0) }}</strong>
              <small>UV {{ formatNumber(realtime?.todayUv ?? 0) }}</small>
            </article>
            <article class="kpi-card">
              <span>今日生成结果</span>
              <strong>{{ formatNumber(realtime?.todayResults ?? 0) }}</strong>
              <small>总结果 {{ formatNumber(overview.resultCreated) }}</small>
            </article>
            <article class="kpi-card" :class="healthTone">
              <span>系统健康状态</span>
              <strong>{{ healthLabel }}</strong>
              <small>{{ visitRuntime?.healthMessage || '等待运行态回传' }}</small>
            </article>
          </div>

          <div class="content-grid two">
            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Live Curve</p>
                  <h3>在线用户趋势</h3>
                </div>
                <select v-model="timeseriesRange" @change="refreshTimeseries">
                  <option value="1h">最近 1 小时</option>
                  <option value="6h">最近 6 小时</option>
                  <option value="24h">最近 24 小时</option>
                </select>
              </div>
              <svg class="line-chart" viewBox="0 0 640 260" role="img" aria-label="在线用户趋势图">
                <g class="grid-lines">
                  <line x1="32" y1="40" x2="608" y2="40" />
                  <line x1="32" y1="110" x2="608" y2="110" />
                  <line x1="32" y1="180" x2="608" y2="180" />
                  <line x1="32" y1="236" x2="608" y2="236" />
                </g>
                <polyline class="line-online" :points="linePoints('onlineVisitors')" />
                <polyline class="line-pv" :points="linePoints('pv')" />
              </svg>
              <div class="legend-row">
                <span><i class="legend online"></i>在线用户</span>
                <span><i class="legend pv"></i>PV</span>
                <span>刷新于 {{ realtime ? formatTime(realtime.refreshedAt) : '-' }}</span>
              </div>
            </article>

            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">System Snapshot</p>
                  <h3>运行态摘要</h3>
                </div>
              </div>
              <div class="runtime-list">
                <div>
                  <span>异步队列</span>
                  <strong>{{ visitRuntime?.queueSize ?? 0 }} / {{ visitRuntime?.queueCapacity ?? 0 }}</strong>
                </div>
                <div>
                  <span>已落库事件</span>
                  <strong>{{ formatNumber(visitRuntime?.totalFlushedEvents ?? 0) }}</strong>
                </div>
                <div>
                  <span>批量写失败</span>
                  <strong>{{ formatNumber(visitRuntime?.batchWriteFailures ?? 0) }}</strong>
                </div>
                <div>
                  <span>采集模式</span>
                  <strong>{{ visitRuntime?.asyncMode || '-' }}</strong>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activePage === 'traffic'" class="page-stack" data-testid="admin-traffic-console">
          <div class="kpi-grid">
            <article class="kpi-card primary">
              <span>累计 PV</span>
              <strong>{{ formatNumber(overview.totalPv) }}</strong>
              <small>UV {{ formatNumber(overview.totalUv) }} · UIP {{ formatNumber(overview.totalUip) }}</small>
            </article>
            <article class="kpi-card">
              <span>首页访问</span>
              <strong>{{ formatNumber(overview.homeViews) }}</strong>
              <small>开始测试 {{ formatNumber(overview.startClicks) }}</small>
            </article>
            <article class="kpi-card">
              <span>平均完成耗时</span>
              <strong>{{ durationText(overview.averageCompletionSeconds) }}</strong>
              <small>由匿名 session 事件差计算</small>
            </article>
            <article class="kpi-card">
              <span>完成率</span>
              <strong>{{ overview.completionRate.toFixed(1) }}%</strong>
              <small>结果数 / 开始测试</small>
            </article>
          </div>

          <div class="content-grid two">
            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Daily Trend</p>
                  <h3>访问与结果趋势</h3>
                </div>
                <span>{{ metricSourceLabel }}</span>
              </div>
              <div class="bar-chart">
                <div
                  v-for="day in overview.dailyTrends"
                  :key="day.date"
                  class="bar-column"
                >
                  <div class="bar-stack">
                    <i class="bar pv-bar" :style="{ height: barHeight(day.pv, maxDailyPv) }"></i>
                    <i class="bar result-bar" :style="{ height: barHeight(day.resultCreated, maxDailyResults) }"></i>
                  </div>
                  <span>{{ compactDate(day.date) }}</span>
                </div>
              </div>
              <div class="legend-row">
                <span><i class="legend pv"></i>PV</span>
                <span><i class="legend result"></i>生成结果</span>
              </div>
            </article>

            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Traffic Source</p>
                  <h3>来源与活动</h3>
                </div>
              </div>
              <div class="rank-columns">
                <rank-list title="渠道 Top" :items="overview.topChannels" empty-text="暂无渠道标记" />
                <rank-list title="活动 Top" :items="overview.topCampaigns" empty-text="暂无活动标记" />
              </div>
            </article>
          </div>
        </section>

        <section v-if="activePage === 'distribution'" class="page-stack" data-testid="admin-distribution-console">
          <div class="content-grid two">
            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Persona Rank</p>
                  <h3>热门人格</h3>
                </div>
                <span>按真实结果聚合</span>
              </div>
              <rank-list
                title="120 人格分流 Top"
                :items="overview.popularPersonas"
                empty-text="暂无人格结果"
                prominent
              />
            </article>

            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Star & Element</p>
                  <h3>星官与五行组合</h3>
                </div>
              </div>
              <div class="rank-columns">
                <rank-list title="星官 Top" :items="overview.popularStarOfficers" empty-text="暂无星官数据" />
                <rank-list title="五行组合 Top" :items="overview.popularElementCombos" empty-text="暂无五行组合" />
              </div>
            </article>
          </div>

          <article class="panel">
            <div class="panel-heading">
              <div>
                <p class="section-kicker">Distribution</p>
                <h3>人格分布明细</h3>
              </div>
              <span>{{ overview.personaDistribution.length }} 类已有命中</span>
            </div>
            <div class="distribution-list">
              <div
                v-for="item in overview.personaDistribution"
                :key="item.name"
                class="distribution-row"
              >
                <span>{{ item.name }}</span>
                <div>
                  <i :style="{ width: ratioWidth(item.count, maxPersonaCount) }"></i>
                </div>
                <strong>{{ formatNumber(item.count) }}</strong>
              </div>
              <p v-if="!overview.personaDistribution.length" class="empty-state">暂无人格分布数据</p>
            </div>
          </article>

          <article class="panel">
            <div class="panel-heading">
              <div>
                <p class="section-kicker">Recent Results</p>
                <h3>最近生成</h3>
              </div>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>结果 ID</th>
                    <th>五行组合</th>
                    <th>星官</th>
                    <th>生成时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in overview.recentResults" :key="item.resultId">
                    <td>{{ item.resultId }}</td>
                    <td>{{ item.elementCombo }}</td>
                    <td>{{ item.starOfficerName }}</td>
                    <td>{{ formatTime(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
              <p v-if="!overview.recentResults.length" class="empty-state">暂无结果记录</p>
            </div>
          </article>
        </section>

        <section v-if="activePage === 'pipeline'" class="page-stack" data-testid="admin-pipeline-console">
          <article class="panel">
            <div class="panel-heading">
              <div>
                <p class="section-kicker">Pipeline</p>
                <h3>行为采集链路</h3>
              </div>
              <span>匿名化采集 · 异步写入 · 聚合展示</span>
            </div>
            <div class="pipeline">
              <div v-for="step in pipelineSteps" :key="step.title" class="pipeline-step">
                <span>{{ step.index }}</span>
                <strong>{{ step.title }}</strong>
                <p>{{ step.text }}</p>
              </div>
            </div>
          </article>

          <div class="content-grid two">
            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Async Writer</p>
                  <h3>异步写入状态</h3>
                </div>
              </div>
              <div class="runtime-list dense">
                <div>
                  <span>队列容量</span>
                  <strong>{{ visitRuntime?.queueCapacity ?? 0 }}</strong>
                </div>
                <div>
                  <span>单轮 Drain</span>
                  <strong>{{ visitRuntime?.drainLimit ?? 0 }}</strong>
                </div>
                <div>
                  <span>丢弃事件</span>
                  <strong>{{ visitRuntime?.droppedAsyncEvents ?? 0 }}</strong>
                </div>
                <div>
                  <span>Worker</span>
                  <strong>{{ visitRuntime?.workerAlive ? 'Alive' : 'Check' }}</strong>
                </div>
                <div>
                  <span>RocketMQ</span>
                  <strong>{{ visitRuntime?.rocketMqAvailable ? 'Ready' : 'Local fallback' }}</strong>
                </div>
                <div>
                  <span>上次落库</span>
                  <strong>{{ visitRuntime?.lastFlushAt ? formatTime(visitRuntime.lastFlushAt) : '-' }}</strong>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Recent Events</p>
                  <h3>最近事件</h3>
                </div>
                <span>近 {{ recentEventRange }}</span>
              </div>
              <div class="event-feed">
                <div v-for="event in recentEvents" :key="`${event.eventType}-${event.occurredAt}`">
                  <strong>{{ event.eventType }}</strong>
                  <span>{{ event.pagePath || '-' }}</span>
                  <small>{{ formatTime(event.occurredAt) }}</small>
                </div>
                <p v-if="!recentEvents.length" class="empty-state">暂无事件</p>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activePage === 'health'" class="page-stack" data-testid="admin-health-console">
          <div class="content-grid two">
            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Quality Gate</p>
                  <h3>系统健康</h3>
                </div>
                <span :class="['status-pill', healthTone]">{{ healthLabel }}</span>
              </div>
              <div class="health-copy">
                <p>{{ visitRuntime?.healthMessage || '运行态接口尚未返回健康说明。' }}</p>
                <p>{{ overview.syntheticIsolationNote }}</p>
              </div>
              <div class="health-actions">
                <button type="button" :disabled="loading" @click="runAggregation">执行聚合</button>
                <button
                  type="button"
                  class="ghost-action"
                  data-testid="admin-export-csv"
                  :disabled="loading"
                  @click="downloadShortLinks"
                >
                  导出短链 CSV
                </button>
                <button type="button" class="ghost-action" :disabled="probingExternal" @click="probeExternalShortLink">
                  {{ probingExternal ? '探测中' : '探测短链服务' }}
                </button>
              </div>
              <p v-if="aggregationMessage" class="success-note">{{ aggregationMessage }}</p>
            </article>

            <article class="panel">
              <div class="panel-heading">
                <div>
                  <p class="section-kicker">Short Link Runtime</p>
                  <h3>短链服务运行态</h3>
                </div>
                <span>{{ externalRuntime?.mode || '-' }}</span>
              </div>
              <div class="runtime-list dense">
                <div>
                  <span>外部模式</span>
                  <strong>{{ externalRuntime?.externalMode ? '开启' : '关闭' }}</strong>
                </div>
                <div>
                  <span>统计接口</span>
                  <strong>{{ externalRuntime?.statsEnabled ? '开启' : '关闭' }}</strong>
                </div>
                <div>
                  <span>探测状态</span>
                  <strong>{{ externalRuntime?.reachable === true ? '可达' : externalRuntime?.reachable === false ? '不可达' : '未探测' }}</strong>
                </div>
                <div>
                  <span>回退策略</span>
                  <strong>{{ externalRuntime?.fallbackToInternal ? '内部短链' : '无回退' }}</strong>
                </div>
              </div>
              <p class="muted">{{ externalRuntime?.message }}</p>
            </article>
          </div>

          <article class="panel">
            <div class="panel-heading">
              <div>
                <p class="section-kicker">Recent Short Links</p>
                <h3>短链样本</h3>
              </div>
              <span>用于排查访问统计口径</span>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>短码</th>
                    <th>五行组合</th>
                    <th>PV / UV / UIP</th>
                    <th>统计源</th>
                    <th>最后访问</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in shortLinks" :key="item.shortCode">
                    <td>
                      <RouterLink :to="`/admin/short-links/${item.shortCode}`">{{ item.shortCode }}</RouterLink>
                    </td>
                    <td>{{ item.elementCombo }}</td>
                    <td>{{ item.pv }} / {{ item.uv }} / {{ item.uip }}</td>
                    <td>{{ item.statSource }} · {{ item.metricSource }}</td>
                    <td>{{ item.lastVisitAt ? formatTime(item.lastVisitAt) : '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <p v-if="!shortLinks.length" class="empty-state">暂无短链样本</p>
            </div>
          </article>
        </section>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue';
import {
  aggregateAdminAnalytics,
  exportAdminShortLinks,
  fetchAdminOverview,
  fetchAdminShortLinks,
  fetchExternalShortLinkRuntime,
  fetchVisitEventRuntime,
  type AdminOverviewFilter,
} from '../api/admin';
import {
  fetchMetricTimeseries,
  fetchRealtimeMetrics,
  fetchRecentMetricEvents,
} from '../api/analytics';
import type {
  AdminOverview,
  ExternalShortLinkRuntime,
  MetricTimeseries,
  NameCount,
  RecentMetricEvent,
  RealtimeMetrics,
  ShortLinkListItem,
  VisitEventRuntime,
} from '../api/types';

type DashboardPageKey = 'realtime' | 'traffic' | 'distribution' | 'pipeline' | 'health';

const ADMIN_TOKEN_KEY = 'wuxing_admin_token';

const dashboardPages: Array<{
  key: DashboardPageKey;
  label: string;
  description: string;
  icon: string;
}> = [
  { key: 'realtime', label: '实时概览', description: '在线 / PV / 健康', icon: '01' },
  { key: 'traffic', label: '流量趋势', description: '访问与完成效率', icon: '02' },
  { key: 'distribution', label: '人格分布', description: '120 类结果分流', icon: '03' },
  { key: 'pipeline', label: '采集链路', description: '事件写入与队列', icon: '04' },
  { key: 'health', label: '系统健康', description: '运行态与运维动作', icon: '05' },
];

const RankList = defineComponent({
  name: 'RankList',
  props: {
    title: { type: String, required: true },
    items: { type: Array as () => NameCount[], required: true },
    emptyText: { type: String, default: '暂无数据' },
    prominent: { type: Boolean, default: false },
  },
  setup(props) {
    const maxCount = computed(() => Math.max(1, ...props.items.map((item) => item.count)));
    return () => h('div', { class: ['rank-list', props.prominent ? 'prominent' : ''] }, [
      h('h4', props.title),
      props.items.length
        ? props.items.map((item, index) => h('div', { class: 'rank-row', key: item.name }, [
          h('span', { class: 'rank-index' }, String(index + 1).padStart(2, '0')),
          h('div', { class: 'rank-main' }, [
            h('strong', item.name),
            h('i', { style: { width: `${Math.max(8, Math.round((item.count / maxCount.value) * 100))}%` } }),
          ]),
          h('em', formatNumber(item.count)),
        ]))
        : h('p', { class: 'empty-state' }, props.emptyText),
    ]);
  },
});

const activePage = ref<DashboardPageKey>('realtime');
const tokenInput = ref(localStorage.getItem(ADMIN_TOKEN_KEY) || '');
const adminToken = ref(localStorage.getItem(ADMIN_TOKEN_KEY) || '');
const loading = ref(false);
const probingExternal = ref(false);
const errorMessage = ref('');
const aggregationMessage = ref('');
const lastLoadedAt = ref('');
const timeseriesRange = ref('1h');
const recentEventRange = ref('24h');

const today = new Date();
const filters = ref<AdminOverviewFilter>({
  startDate: isoDate(addDays(today, -6)),
  endDate: isoDate(today),
  includeSynthetic: false,
});

const overview = ref<AdminOverview | null>(null);
const realtime = ref<RealtimeMetrics | null>(null);
const timeseries = ref<MetricTimeseries | null>(null);
const recentEvents = ref<RecentMetricEvent[]>([]);
const visitRuntime = ref<VisitEventRuntime | null>(null);
const externalRuntime = ref<ExternalShortLinkRuntime | null>(null);
const shortLinks = ref<ShortLinkListItem[]>([]);

const currentPage = computed(() => dashboardPages.find((page) => page.key === activePage.value));
const healthLabel = computed(() => {
  if (!visitRuntime.value) {
    return '待检测';
  }
  return visitRuntime.value.healthStatus === 'ok'
    ? '健康'
    : visitRuntime.value.healthStatus === 'watch'
      ? '观察'
      : '告警';
});
const healthTone = computed(() => {
  if (!visitRuntime.value) {
    return 'watch';
  }
  return visitRuntime.value.healthStatus;
});
const metricSourceLabel = computed(() => {
  if (!overview.value) {
    return '-';
  }
  const sourceMap: Record<string, string> = {
    live_event: '实时事件',
    daily_metric: '日聚合',
    mixed: '聚合 + 实时',
  };
  return sourceMap[overview.value.metricSource] || overview.value.metricSource;
});
const maxDailyPv = computed(() => Math.max(1, ...(overview.value?.dailyTrends ?? []).map((day) => day.pv)));
const maxDailyResults = computed(() => Math.max(1, ...(overview.value?.dailyTrends ?? []).map((day) => day.resultCreated)));
const maxPersonaCount = computed(() => Math.max(1, ...(overview.value?.personaDistribution ?? []).map((item) => item.count)));

const pipelineSteps = [
  {
    index: '01',
    title: '前端埋点',
    text: '页面访问、测试动作、结果页浏览通过匿名 clientId 和 sessionId 采集，不上传真实身份信息。',
  },
  {
    index: '02',
    title: '异步写入',
    text: 'VisitEventService 先入本地队列，再批量落库；RocketMQ 可作为扩展通道，失败时回退本地写入。',
  },
  {
    index: '03',
    title: 'MySQL 聚合',
    text: 'visit_event、user_result、short_link 提供准确统计口径，支持日期范围和压测流量隔离。',
  },
  {
    index: '04',
    title: 'Redis 加速',
    text: '结果缓存、后台概览缓存和人格榜单 ZSET 承担高频读取与实时排行能力。',
  },
  {
    index: '05',
    title: '后台展示',
    text: 'Admin API 汇总实时在线、趋势、人格分布和运行态，形成面试可讲的数据闭环。',
  },
];

onMounted(() => {
  if (adminToken.value) {
    void refreshAll(false);
  }
});

watch(filters, () => {
  if (overview.value && adminToken.value) {
    void refreshAll(false);
  }
}, { deep: true });

function submitToken() {
  adminToken.value = tokenInput.value;
  localStorage.setItem(ADMIN_TOKEN_KEY, tokenInput.value);
  void refreshAll(true);
}

async function refreshAll(forceRefresh: boolean) {
  if (!adminToken.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  aggregationMessage.value = '';
  try {
    const [nextOverview, nextRealtime, nextTimeseries, nextEvents, nextVisitRuntime] = await Promise.all([
      fetchAdminOverview(adminToken.value, { ...filters.value, forceRefresh }),
      fetchRealtimeMetrics(adminToken.value),
      fetchMetricTimeseries(adminToken.value, timeseriesRange.value),
      fetchRecentMetricEvents(adminToken.value, recentEventRange.value),
      fetchVisitEventRuntime(adminToken.value),
    ]);
    overview.value = nextOverview;
    realtime.value = nextRealtime;
    timeseries.value = nextTimeseries;
    recentEvents.value = nextEvents;
    visitRuntime.value = nextVisitRuntime;
    lastLoadedAt.value = formatTime(new Date().toISOString());
    await Promise.all([
      loadExternalRuntime(false),
      loadShortLinks(),
    ]);
  } catch (error) {
    handleAdminError(error);
  } finally {
    loading.value = false;
  }
}

async function refreshTimeseries() {
  if (!adminToken.value) {
    return;
  }
  try {
    timeseries.value = await fetchMetricTimeseries(adminToken.value, timeseriesRange.value);
  } catch (error) {
    handleAdminError(error);
  }
}

async function loadExternalRuntime(probe: boolean) {
  try {
    externalRuntime.value = await fetchExternalShortLinkRuntime(adminToken.value, probe);
  } catch (error) {
    console.warn('external shortlink runtime unavailable', error);
  }
}

async function loadShortLinks() {
  try {
    const page = await fetchAdminShortLinks(adminToken.value, 1, 8, filters.value);
    shortLinks.value = page.records;
  } catch (error) {
    console.warn('short link sample unavailable', error);
  }
}

async function runAggregation() {
  if (!adminToken.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  aggregationMessage.value = '';
  try {
    const result = await aggregateAdminAnalytics(adminToken.value, filters.value);
    aggregationMessage.value = `已聚合 ${result.daysAggregated} 天，短链统计 ${result.shortLinkRowsAggregated} 行。`;
    await refreshAll(true);
  } catch (error) {
    handleAdminError(error);
  } finally {
    loading.value = false;
  }
}

async function downloadShortLinks() {
  if (!adminToken.value) {
    return;
  }
  try {
    const blob = await exportAdminShortLinks(adminToken.value, filters.value);
    const href = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = href;
    link.download = `wuxing-short-links-${filters.value.startDate || 'all'}-${filters.value.endDate || 'all'}.csv`;
    link.click();
    URL.revokeObjectURL(href);
  } catch (error) {
    handleAdminError(error);
  }
}

async function probeExternalShortLink() {
  probingExternal.value = true;
  try {
    await loadExternalRuntime(true);
  } finally {
    probingExternal.value = false;
  }
}

function logout() {
  localStorage.removeItem(ADMIN_TOKEN_KEY);
  adminToken.value = '';
  tokenInput.value = '';
  overview.value = null;
  realtime.value = null;
  timeseries.value = null;
  recentEvents.value = [];
  visitRuntime.value = null;
  externalRuntime.value = null;
  shortLinks.value = [];
  errorMessage.value = '';
}

function handleAdminError(error: unknown) {
  const message = error instanceof Error ? error.message : '后台加载失败';
  errorMessage.value = message;
  if (message.includes('401') || message.includes('Unauthorized') || message.includes('token')) {
    logout();
    errorMessage.value = '管理 Token 校验失败，请重新输入。';
  }
}

function setQuickRange(daysBeforeToday: number) {
  const end = new Date();
  filters.value = {
    ...filters.value,
    startDate: isoDate(addDays(end, -daysBeforeToday)),
    endDate: isoDate(end),
  };
}

function linePoints(metricKey: 'onlineVisitors' | 'pv') {
  const points = timeseries.value?.points ?? [];
  if (points.length === 0) {
    return '';
  }
  const width = 576;
  const left = 32;
  const top = 36;
  const height = 196;
  const values = points.map((point) => point[metricKey] ?? 0);
  const max = Math.max(1, ...values);
  return values.map((value, index) => {
    const x = left + (points.length === 1 ? width : (index / (points.length - 1)) * width);
    const y = top + height - (value / max) * height;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
}

function ratioWidth(value: number, max: number) {
  return `${Math.max(4, Math.round((value / Math.max(1, max)) * 100))}%`;
}

function barHeight(value: number, max: number) {
  return `${Math.max(6, Math.round((value / Math.max(1, max)) * 100))}%`;
}

function durationText(seconds: number) {
  if (!seconds) {
    return '-';
  }
  if (seconds < 60) {
    return `${seconds.toFixed(1)} 秒`;
  }
  const minutes = Math.floor(seconds / 60);
  const rest = Math.round(seconds % 60);
  return `${minutes} 分 ${rest} 秒`;
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value || 0);
}

function formatTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function compactDate(value: string) {
  return value.slice(5).replace('-', '/');
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function isoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}
</script>

<style scoped>
.admin-dashboard {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgba(242, 247, 252, 0.94), rgba(247, 250, 253, 0.96)),
    radial-gradient(circle at 12% 8%, rgba(29, 78, 216, 0.08), transparent 30%),
    #f3f7fb;
  color: #172033;
  padding: 24px;
}

.admin-hero {
  max-width: 1480px;
  margin: 0 auto 18px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 30px;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff, #f4f8fc);
  box-shadow: 0 14px 36px rgba(41, 57, 80, 0.08);
}

.eyebrow,
.section-kicker {
  margin: 0 0 8px;
  color: #2f6db5;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.admin-hero h1,
.toolbar h2,
.login-panel h2 {
  margin: 0;
  color: #111827;
  letter-spacing: 0;
}

.admin-hero h1 {
  font-size: 40px;
}

.hero-copy {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 15px;
}

.hero-status {
  display: grid;
  gap: 5px;
  min-width: 180px;
  padding: 14px 16px;
  border: 1px solid #b9d4ee;
  border-radius: 8px;
  background: #eef6ff;
}

.hero-status strong {
  font-size: 18px;
}

.hero-status small,
.nav-brand small,
.nav-footer span,
.kpi-card small,
.panel-heading span,
.muted {
  color: #64748b;
}

.hero-status.is-offline {
  border-color: #e7d7c4;
  background: #fff8ed;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #16a34a;
  box-shadow: 0 0 0 6px rgba(22, 163, 74, 0.12);
}

.is-offline .pulse-dot {
  background: #d97706;
  box-shadow: 0 0 0 6px rgba(217, 119, 6, 0.12);
}

.login-panel {
  max-width: 860px;
  margin: 42px auto;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 28px;
  padding: 30px;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 20px 45px rgba(41, 57, 80, 0.08);
}

.login-panel p {
  color: #64748b;
  line-height: 1.75;
}

.login-form {
  display: grid;
  gap: 12px;
}

.login-form label {
  font-weight: 800;
}

.login-form input,
.date-controls input,
.panel select {
  min-height: 44px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  padding: 0 12px;
  color: #172033;
  font: inherit;
}

button {
  min-height: 44px;
  border: 0;
  border-radius: 8px;
  background: #1d4f91;
  color: #fff;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.admin-layout {
  max-width: 1480px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  gap: 18px;
}

.admin-side-nav {
  position: sticky;
  top: 18px;
  align-self: start;
  display: grid;
  gap: 16px;
  padding: 16px;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 30px rgba(41, 57, 80, 0.08);
}

.nav-brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.nav-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #1d4f91;
  color: #fff;
  font-weight: 900;
}

.admin-side-nav nav {
  display: grid;
  gap: 8px;
}

.admin-side-nav nav button {
  min-height: 72px;
  display: grid;
  grid-template-columns: 34px 1fr;
  grid-template-areas:
    "icon title"
    "icon desc";
  gap: 2px 10px;
  align-items: center;
  text-align: left;
  padding: 11px 12px;
  border: 1px solid transparent;
  background: transparent;
  color: #334155;
}

.admin-side-nav nav button span {
  grid-area: icon;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #edf4fb;
  color: #2f6db5;
  font-size: 12px;
}

.admin-side-nav nav button strong {
  grid-area: title;
}

.admin-side-nav nav button small {
  grid-area: desc;
  color: #64748b;
}

.admin-side-nav nav button.active {
  border-color: #bdd4ef;
  background: #eef6ff;
  color: #123b6d;
}

.nav-footer {
  display: grid;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.nav-footer button,
.ghost-action {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #1d4f91;
}

.admin-workbench {
  min-width: 0;
  display: grid;
  gap: 18px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(41, 57, 80, 0.06);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.date-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.date-controls button {
  min-height: 38px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  padding: 0 12px;
}

.date-controls input {
  min-height: 38px;
}

.switch {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  font-size: 14px;
}

.page-stack {
  display: grid;
  gap: 18px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card,
.panel {
  border: 1px solid #d8e2ec;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(41, 57, 80, 0.06);
}

.kpi-card {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.kpi-card span {
  color: #64748b;
  font-weight: 700;
}

.kpi-card strong {
  color: #101827;
  font-size: 32px;
  line-height: 1;
}

.kpi-card.primary {
  border-color: #a9c8ea;
  background: linear-gradient(135deg, #eaf4ff, #ffffff);
}

.kpi-card.ok {
  border-color: #a7d8b8;
}

.kpi-card.watch {
  border-color: #f1d59b;
}

.kpi-card.danger {
  border-color: #f4b4b4;
}

.content-grid {
  display: grid;
  gap: 18px;
}

.content-grid.two {
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
}

.panel {
  min-width: 0;
  padding: 20px;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-heading h3 {
  margin: 0;
  color: #111827;
  font-size: 22px;
}

.line-chart {
  width: 100%;
  height: 270px;
  overflow: visible;
}

.grid-lines line {
  stroke: #e2e8f0;
  stroke-width: 1;
}

.line-online,
.line-pv {
  fill: none;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.line-online {
  stroke: #1d4f91;
}

.line-pv {
  stroke: #2a9d8f;
  opacity: 0.78;
}

.legend-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  color: #64748b;
  font-size: 14px;
}

.legend {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  margin-right: 6px;
}

.legend.online,
.legend.pv {
  background: #1d4f91;
}

.legend.pv {
  background: #2a9d8f;
}

.legend.result {
  background: #d97706;
}

.runtime-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.runtime-list div {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.runtime-list.dense {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.runtime-list span {
  color: #64748b;
  font-size: 13px;
}

.runtime-list strong {
  color: #111827;
}

.bar-chart {
  height: 260px;
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(36px, 1fr);
  align-items: end;
  gap: 12px;
  padding: 16px 0 6px;
  border-bottom: 1px solid #e2e8f0;
}

.bar-column {
  height: 100%;
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 8px;
  text-align: center;
  color: #64748b;
  font-size: 12px;
}

.bar-stack {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: end;
  gap: 5px;
}

.bar {
  display: block;
  width: 12px;
  min-height: 6px;
  border-radius: 6px 6px 0 0;
}

.pv-bar {
  background: linear-gradient(180deg, #1d4f91, #6ea8dc);
}

.result-bar {
  background: linear-gradient(180deg, #d97706, #f2c879);
}

.rank-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.rank-list {
  display: grid;
  gap: 10px;
}

.rank-list h4 {
  margin: 0;
  color: #334155;
}

.rank-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fbfdff;
}

.rank-list.prominent .rank-row {
  min-height: 58px;
}

.rank-index {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #edf4fb;
  color: #2f6db5;
  font-size: 12px;
  font-weight: 900;
}

.rank-main {
  min-width: 0;
  display: grid;
  gap: 7px;
}

.rank-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-main i {
  display: block;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1d4f91, #2a9d8f);
}

.rank-row em {
  color: #334155;
  font-style: normal;
  font-weight: 800;
}

.distribution-list {
  display: grid;
  gap: 8px;
}

.distribution-row {
  display: grid;
  grid-template-columns: minmax(120px, 220px) minmax(0, 1fr) 54px;
  gap: 12px;
  align-items: center;
  min-height: 36px;
}

.distribution-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #334155;
  font-weight: 700;
}

.distribution-row div {
  height: 8px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.distribution-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1d4f91, #7ab6e8);
}

.distribution-row strong {
  text-align: right;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 10px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  vertical-align: top;
  white-space: nowrap;
}

th {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

td a {
  color: #1d4f91;
  font-weight: 800;
  text-decoration: none;
}

.pipeline {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.pipeline-step {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid #dbe6f2;
  border-radius: 8px;
  background: #f8fbff;
}

.pipeline-step span {
  width: 34px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #1d4f91;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.pipeline-step strong {
  color: #111827;
}

.pipeline-step p,
.health-copy p {
  margin: 0;
  color: #64748b;
  line-height: 1.65;
}

.event-feed {
  display: grid;
  gap: 8px;
  max-height: 366px;
  overflow: auto;
}

.event-feed div {
  display: grid;
  grid-template-columns: minmax(130px, 0.8fr) minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fbfdff;
}

.event-feed span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #64748b;
}

.event-feed small {
  color: #94a3b8;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: #edf4fb;
  color: #1d4f91;
  font-weight: 800;
}

.status-pill.ok {
  background: #e9f8ef;
  color: #15803d;
}

.status-pill.watch {
  background: #fff7e7;
  color: #b45309;
}

.status-pill.danger {
  background: #fff1f2;
  color: #be123c;
}

.health-copy {
  display: grid;
  gap: 12px;
}

.health-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.success-note,
.form-error,
.inline-error {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
}

.success-note {
  margin-top: 14px;
  background: #ecfdf5;
  color: #047857;
}

.form-error,
.inline-error {
  background: #fff1f2;
  color: #be123c;
}

.empty-state {
  margin: 0;
  color: #94a3b8;
}

@media (max-width: 1180px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-side-nav {
    position: static;
  }

  .admin-side-nav nav {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .admin-side-nav nav button {
    grid-template-columns: 1fr;
    grid-template-areas:
      "icon"
      "title"
      "desc";
  }

  .kpi-grid,
  .content-grid.two,
  .pipeline {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .admin-dashboard {
    padding: 12px;
  }

  .admin-hero,
  .toolbar,
  .login-panel {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .admin-side-nav nav,
  .kpi-grid,
  .content-grid.two,
  .rank-columns,
  .runtime-list,
  .runtime-list.dense,
  .pipeline {
    grid-template-columns: 1fr;
  }

  .distribution-row {
    grid-template-columns: 110px minmax(0, 1fr) 42px;
  }
}
</style>
