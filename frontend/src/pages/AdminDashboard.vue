<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  aggregateAdminAnalytics,
  exportAdminShortLinks,
  fetchAdminOverview,
  fetchAdminShortLinks,
  fetchExternalShortLinkRuntime,
} from '../api/admin';
import type { AdminDateFilter } from '../api/admin';
import type {
  AdminOverview,
  AnalyticsAggregation,
  ExternalShortLinkRuntime,
  PageResult,
  ShortLinkListItem,
} from '../api/types';
import StatCard from '../components/StatCard.vue';

const token = ref(localStorage.getItem('wuxing_admin_token') || '');
const startDate = ref('');
const endDate = ref('');
const keyword = ref('');
const statSource = ref<'local' | 'external' | ''>('');
const overview = ref<AdminOverview | null>(null);
const shortLinks = ref<PageResult<ShortLinkListItem> | null>(null);
const runtime = ref<ExternalShortLinkRuntime | null>(null);
const aggregation = ref<AnalyticsAggregation | null>(null);
const error = ref('');
const loading = ref(false);
const exporting = ref(false);
const aggregating = ref(false);
const busy = computed(() => loading.value || exporting.value || aggregating.value);

onMounted(() => {
  if (token.value) {
    load();
  }
});

async function load() {
  if (busy.value) {
    return;
  }
  error.value = '';
  loading.value = true;
  try {
    localStorage.setItem('wuxing_admin_token', token.value);
    overview.value = await fetchAdminOverview(token.value, dateFilter());
    shortLinks.value = await fetchAdminShortLinks(token.value, 1, 20, shortLinkFilter());
    runtime.value = await fetchExternalShortLinkRuntime(token.value, false);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '后台数据加载失败';
  } finally {
    loading.value = false;
  }
}

function clearDateFilter() {
  if (busy.value) {
    return;
  }
  startDate.value = '';
  endDate.value = '';
  keyword.value = '';
  statSource.value = '';
  load();
}

async function checkExternalRuntime() {
  if (busy.value) {
    return;
  }
  error.value = '';
  loading.value = true;
  try {
    runtime.value = await fetchExternalShortLinkRuntime(token.value, true);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '外部短链状态检查失败';
  } finally {
    loading.value = false;
  }
}

async function refreshAggregation() {
  if (busy.value) {
    return;
  }
  error.value = '';
  aggregating.value = true;
  try {
    aggregation.value = await aggregateAdminAnalytics(token.value, dateFilter());
    overview.value = await fetchAdminOverview(token.value, dateFilter());
  } catch (err) {
    error.value = err instanceof Error ? err.message : '增长聚合刷新失败';
  } finally {
    aggregating.value = false;
  }
}

async function exportCsv() {
  if (busy.value || !shortLinks.value) {
    return;
  }
  error.value = '';
  exporting.value = true;
  try {
    const blob = await exportAdminShortLinks(token.value, shortLinkFilter());
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `wuxing-short-links-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '短链导出失败';
  } finally {
    exporting.value = false;
  }
}

function dateFilter(): AdminDateFilter {
  return {
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
  };
}

function shortLinkFilter() {
  return {
    ...dateFilter(),
    keyword: keyword.value.trim() || undefined,
    statSource: statSource.value || undefined,
  };
}

function detailQuery() {
  const query: Record<string, string> = {};
  if (startDate.value) {
    query.startDate = startDate.value;
  }
  if (endDate.value) {
    query.endDate = endDate.value;
  }
  return query;
}

function statSourceLabel(source: ShortLinkListItem['statSource']) {
  return source === 'external' ? '外部' : '本地';
}

function runtimeReachableLabel(value: boolean | null) {
  if (value === true) {
    return '可达';
  }
  if (value === false) {
    return '不可达';
  }
  return '未探测';
}

function metricSourceLabel(value: AdminOverview['metricSource']) {
  if (value === 'daily_metric') {
    return '日聚合';
  }
  if (value === 'mixed') {
    return '聚合 + 实时';
  }
  return '实时事件';
}

function formatDateTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <main class="page">
    <section class="shell stack admin-page">
      <div class="panel stack">
        <h2>数据中台</h2>
        <div class="admin-token">
          <input v-model="token" type="password" placeholder="输入管理 token" :disabled="busy" />
          <button type="button" :disabled="busy" @click="load">{{ loading ? '加载中...' : '进入后台' }}</button>
        </div>
        <div class="filter-bar" :aria-busy="busy">
          <label>
            开始日期
            <input v-model="startDate" type="date" :disabled="busy" />
          </label>
          <label>
            结束日期
            <input v-model="endDate" type="date" :disabled="busy" />
          </label>
          <label>
            短码 / 结果
            <input v-model="keyword" type="search" placeholder="输入短码或 resultId" :disabled="busy" />
          </label>
          <label>
            来源
            <select v-model="statSource" :disabled="busy">
              <option value="">全部</option>
              <option value="local">本地</option>
              <option value="external">外部</option>
            </select>
          </label>
          <button type="button" :disabled="busy" @click="load">应用筛选</button>
          <button class="secondary" type="button" :disabled="busy" @click="clearDateFilter">清空</button>
          <button class="secondary" type="button" :disabled="busy || !shortLinks" @click="exportCsv">
            {{ exporting ? '导出中...' : '导出 CSV' }}
          </button>
        </div>
        <p v-if="busy" class="muted admin-busy">正在处理当前请求，请稍候。</p>
        <p v-if="error" class="error-text">{{ error }}</p>
      </div>

      <template v-if="overview">
        <div class="stats-grid" aria-label="关键指标口径">
          <StatCard label="总 PV" :value="overview.totalPv" note="当前筛选范围内的事件总次数" />
          <StatCard label="总 UV" :value="overview.totalUv" note="按匿名 clientId hash 去重" />
          <StatCard label="总 UIP" :value="overview.totalUip" note="按脱敏 IP hash 去重" />
          <StatCard label="首页访问" :value="overview.homeViews" note="用户进入产品首屏的次数" />
          <StatCard label="开始点击" :value="overview.startClicks" note="点击开始测试的行为次数" />
          <StatCard label="提交次数" :value="overview.testSubmits" note="测试页发起提交的尝试次数" />
          <StatCard label="结果生成" :value="overview.resultCreated" note="后端成功生成结果的次数" />
          <StatCard label="短链生成" :value="overview.shortLinkCreated" note="结果绑定专属短码的次数" />
          <StatCard label="短链访问" :value="overview.shortLinkVisits" note="朋友或本人访问 /s/{code} 的次数" />
          <StatCard label="完成率" :value="`${overview.completionRate}%`" note="结果生成数 / 开始点击数" />
        </div>

        <details v-if="runtime" class="panel stack debug-panel">
          <summary>
            <span>外部短链调试信息</span>
            <small>
              {{ runtime.mode }} · {{ runtimeReachableLabel(runtime.reachable) }} ·
              {{ runtime.fallbackToInternal ? '可自动降级' : '不降级' }}
            </small>
          </summary>
          <div class="section-head">
            <p class="muted">用于排查外部短链服务连通性，日常运营优先看上方核心指标。</p>
            <button class="secondary" type="button" :disabled="busy" @click="checkExternalRuntime">
              {{ loading ? '检查中...' : '检查' }}
            </button>
          </div>
          <div class="runtime-grid">
            <span>模式：{{ runtime.mode }}</span>
            <span>统计：{{ runtime.statsEnabled ? '开启' : '关闭' }}</span>
            <span>降级：{{ runtime.fallbackToInternal ? '开启' : '关闭' }}</span>
            <span>可达性：{{ runtimeReachableLabel(runtime.reachable) }}</span>
            <span>域名：{{ runtime.domain }}</span>
            <span>分组：{{ runtime.groupId }}</span>
            <span>状态码：{{ runtime.httpStatus ?? '-' }}</span>
            <span>信息：{{ runtime.message }}</span>
          </div>
        </details>

        <div class="panel stack">
          <h2>增长漏斗</h2>
          <p class="muted">按当前日期筛选统计，转化率为相邻步骤转化。</p>
          <div class="table-wrap">
            <table class="compact-table">
              <thead>
                <tr>
                  <th>步骤</th>
                  <th>次数</th>
                  <th>转化率</th>
                  <th>埋点代码</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in overview.funnelSteps" :key="item.eventType">
                  <td>{{ item.label }}</td>
                  <td>{{ item.count }}</td>
                  <td>{{ item.conversionRate }}%</td>
                  <td><code class="debug-code">{{ item.eventType }}</code></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel stack">
          <div class="section-head">
            <div>
              <h2>日趋势</h2>
              <p class="muted">
                来源：{{ metricSourceLabel(overview.metricSource) }}
                <span v-if="overview.aggregatedThroughDate">，已聚合至 {{ overview.aggregatedThroughDate }}</span>
              </p>
            </div>
            <button class="secondary" type="button" :disabled="busy" @click="refreshAggregation">
              {{ aggregating ? '聚合中...' : '刷新聚合' }}
            </button>
          </div>
          <p v-if="aggregation" class="muted">
            最近聚合：{{ aggregation.startDate }} 至 {{ aggregation.endDate }}，
            {{ aggregation.daysAggregated }} 天，短链明细 {{ aggregation.shortLinkRowsAggregated }} 行。
          </p>
          <div class="table-wrap">
            <table class="compact-table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>PV</th>
                  <th>结果</th>
                  <th>短链生成</th>
                  <th>短链访问</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in overview.dailyTrends" :key="item.date">
                  <td>{{ item.date }}</td>
                  <td>{{ item.pv }}</td>
                  <td>{{ item.resultCreated }}</td>
                  <td>{{ item.shortLinkCreated }}</td>
                  <td>{{ item.shortLinkVisits }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="insight-grid">
          <div class="panel stack">
            <h2>Top Channel</h2>
            <div v-if="overview.topChannels.length">
              <div class="rank-row" v-for="item in overview.topChannels" :key="item.name">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无渠道数据</p>
          </div>

          <div class="panel stack">
            <h2>Top Campaign</h2>
            <div v-if="overview.topCampaigns.length">
              <div class="rank-row" v-for="item in overview.topCampaigns" :key="item.name">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无活动数据</p>
          </div>

          <div class="panel stack">
            <h2>热门五行组合</h2>
            <div v-if="overview.popularElementCombos.length">
              <div class="rank-row" v-for="item in overview.popularElementCombos" :key="item.name">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无数据</p>
          </div>

          <div class="panel stack">
            <h2>热门星官</h2>
            <div v-if="overview.popularStarOfficers.length">
              <div class="rank-row" v-for="item in overview.popularStarOfficers" :key="item.name">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无数据</p>
          </div>

          <div class="panel stack">
            <h2>最近结果</h2>
            <div v-if="overview.recentResults.length" class="table-wrap">
              <table class="compact-table">
                <thead>
                  <tr>
                    <th>结果</th>
                    <th>组合</th>
                    <th>星官</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in overview.recentResults" :key="item.resultId">
                    <td>{{ item.resultId }}</td>
                    <td>{{ item.elementCombo }}</td>
                    <td>{{ item.starOfficerName }}</td>
                    <td>{{ formatDateTime(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="muted empty-state">当前筛选范围内暂无结果生成记录。</p>
          </div>

          <div class="panel stack">
            <h2>最近短链</h2>
            <div v-if="overview.recentShortLinks.length" class="table-wrap">
              <table class="compact-table">
                <thead>
                  <tr>
                    <th>短码</th>
                    <th>结果</th>
                    <th>PV</th>
                    <th>来源</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in overview.recentShortLinks" :key="item.shortCode">
                    <td>{{ item.shortCode }}</td>
                    <td>{{ item.resultId }}</td>
                    <td>{{ item.pv }}</td>
                    <td>{{ statSourceLabel(item.statSource) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="muted empty-state">当前筛选范围内暂无短链生成记录。</p>
          </div>
        </div>

        <div class="panel stack">
          <h2>短链列表</h2>
          <p class="muted">当前筛选共 {{ shortLinks?.total ?? 0 }} 条，来源筛选最多扫描最近 500 条短链。</p>
          <div v-if="shortLinks?.records.length" class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>短码</th>
                  <th>结果</th>
                  <th>组合</th>
                  <th>星官</th>
                  <th>PV</th>
                  <th>UV</th>
                  <th>UIP</th>
                  <th>来源</th>
                  <th>详情</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in shortLinks?.records" :key="item.shortCode">
                  <td>{{ item.shortCode }}</td>
                  <td>{{ item.resultId }}</td>
                  <td>{{ item.elementCombo }}</td>
                  <td>{{ item.starOfficerName }}</td>
                  <td>{{ item.pv }}</td>
                  <td>{{ item.uv }}</td>
                  <td>{{ item.uip }}</td>
                  <td>{{ statSourceLabel(item.statSource) }}</td>
                  <td>
                    <RouterLink :to="{ path: `/admin/short-links/${item.shortCode}`, query: detailQuery() }">查看</RouterLink>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="muted empty-state">没有匹配当前筛选条件的短链。</p>
        </div>
      </template>
    </section>
  </main>
</template>
