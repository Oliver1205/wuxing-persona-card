<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  exportAdminShortLinks,
  fetchAdminOverview,
  fetchAdminShortLinks,
  fetchExternalShortLinkRuntime,
} from '../api/admin';
import type { AdminDateFilter } from '../api/admin';
import type { AdminOverview, ExternalShortLinkRuntime, PageResult, ShortLinkListItem } from '../api/types';
import StatCard from '../components/StatCard.vue';

const token = ref(localStorage.getItem('wuxing_admin_token') || '');
const startDate = ref('');
const endDate = ref('');
const keyword = ref('');
const statSource = ref<'local' | 'external' | ''>('');
const overview = ref<AdminOverview | null>(null);
const shortLinks = ref<PageResult<ShortLinkListItem> | null>(null);
const runtime = ref<ExternalShortLinkRuntime | null>(null);
const error = ref('');
const loading = ref(false);
const exporting = ref(false);

onMounted(() => {
  if (token.value) {
    load();
  }
});

async function load() {
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
  startDate.value = '';
  endDate.value = '';
  keyword.value = '';
  statSource.value = '';
  load();
}

async function checkExternalRuntime() {
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

async function exportCsv() {
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
          <input v-model="token" type="password" placeholder="输入管理 token" />
          <button type="button" @click="load">{{ loading ? '加载中...' : '进入后台' }}</button>
        </div>
        <div class="filter-bar">
          <label>
            开始日期
            <input v-model="startDate" type="date" />
          </label>
          <label>
            结束日期
            <input v-model="endDate" type="date" />
          </label>
          <label>
            短码 / 结果
            <input v-model="keyword" type="search" placeholder="输入短码或 resultId" />
          </label>
          <label>
            来源
            <select v-model="statSource">
              <option value="">全部</option>
              <option value="local">本地</option>
              <option value="external">外部</option>
            </select>
          </label>
          <button type="button" @click="load">应用筛选</button>
          <button class="secondary" type="button" @click="clearDateFilter">清空</button>
          <button class="secondary" type="button" :disabled="exporting || !shortLinks" @click="exportCsv">
            {{ exporting ? '导出中...' : '导出 CSV' }}
          </button>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
      </div>

      <template v-if="overview">
        <div v-if="runtime" class="panel stack">
          <div class="section-head">
            <h2>外部短链状态</h2>
            <button class="secondary" type="button" :disabled="loading" @click="checkExternalRuntime">检查</button>
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
        </div>

        <div class="stats-grid">
          <StatCard label="总 PV" :value="overview.totalPv" />
          <StatCard label="总 UV" :value="overview.totalUv" />
          <StatCard label="总 UIP" :value="overview.totalUip" />
          <StatCard label="首页访问" :value="overview.homeViews" />
          <StatCard label="开始点击" :value="overview.startClicks" />
          <StatCard label="提交次数" :value="overview.testSubmits" />
          <StatCard label="结果生成" :value="overview.resultCreated" />
          <StatCard label="短链生成" :value="overview.shortLinkCreated" />
          <StatCard label="短链访问" :value="overview.shortLinkVisits" />
          <StatCard label="完成率" :value="`${overview.completionRate}%`" />
        </div>

        <div class="panel stack">
          <h2>日趋势</h2>
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
            <div class="table-wrap">
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
          </div>

          <div class="panel stack">
            <h2>最近短链</h2>
            <div class="table-wrap">
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
          </div>
        </div>

        <div class="panel stack">
          <h2>短链列表</h2>
          <p class="muted">当前筛选共 {{ shortLinks?.total ?? 0 }} 条，来源筛选最多扫描最近 500 条短链。</p>
          <div class="table-wrap">
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
        </div>
      </template>
    </section>
  </main>
</template>
