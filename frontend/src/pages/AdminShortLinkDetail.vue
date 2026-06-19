<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchShortLinkVisits } from '../api/admin';
import type { AdminShortLinkVisitFilter } from '../api/admin';
import type { PageResult, ShortLinkVisit } from '../api/types';

const route = useRoute();
const token = ref(localStorage.getItem('wuxing_admin_token') || '');
const startDate = ref(String(route.query.startDate || ''));
const endDate = ref(String(route.query.endDate || ''));
const includeSynthetic = ref(route.query.includeSynthetic === 'true');
const keyword = String(route.query.keyword || '');
const statSource = normalizeStatSource(route.query.statSource);
const visits = ref<PageResult<ShortLinkVisit> | null>(null);
const error = ref('');
const loading = ref(false);
const visitPage = ref(numberQuery(route.query.visitPage, 1));
const visitPageSize = ref(numberQuery(route.query.visitPageSize, 20));
const pageSizeOptions = [20, 50, 100];
const visitTotalPages = computed(() => Math.max(1, Math.ceil((visits.value?.total ?? 0) / visitPageSize.value)));
const visitStartIndex = computed(() => {
  if (!visits.value?.total) {
    return 0;
  }
  return (visitPage.value - 1) * visitPageSize.value + 1;
});
const visitEndIndex = computed(() => {
  const total = visits.value?.total ?? 0;
  return Math.min(total, visitPage.value * visitPageSize.value);
});
const returnQuery = computed(() => {
  const query: Record<string, string> = {};
  if (startDate.value) {
    query.startDate = startDate.value;
  }
  if (endDate.value) {
    query.endDate = endDate.value;
  }
  if (includeSynthetic.value) {
    query.includeSynthetic = 'true';
  }
  if (keyword) {
    query.keyword = keyword;
  }
  if (statSource) {
    query.statSource = statSource;
  }
  return query;
});

onMounted(load);

async function load() {
  if (loading.value) {
    return;
  }
  error.value = '';
  loading.value = true;
  try {
    visits.value = await fetchShortLinkVisits(token.value, String(route.params.shortCode), visitPage.value, visitPageSize.value, dateFilter());
  } catch (err) {
    const message = err instanceof Error ? err.message : '访问详情加载失败';
    if (message.includes('admin token is invalid')) {
      token.value = '';
      localStorage.removeItem('wuxing_admin_token');
      visits.value = null;
      error.value = '管理 token 无效，请返回后台重新登录。';
    } else {
      error.value = message;
    }
  } finally {
    loading.value = false;
  }
}

function clearDateFilter() {
  startDate.value = '';
  endDate.value = '';
  includeSynthetic.value = false;
  visitPage.value = 1;
  load();
}

function applyFilter() {
  visitPage.value = 1;
  load();
}

function changeVisitPageSize() {
  visitPage.value = 1;
  load();
}

function goVisitPage(page: number) {
  if (loading.value) {
    return;
  }
  const nextPage = Math.max(1, Math.min(visitTotalPages.value, page));
  if (nextPage === visitPage.value) {
    return;
  }
  visitPage.value = nextPage;
  load();
}

function dateFilter(): AdminShortLinkVisitFilter {
  return {
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    includeSynthetic: includeSynthetic.value,
    statSource,
  };
}

function statSourceLabel(value: ShortLinkVisit['statSource']) {
  return value === 'external' ? '外部平台' : '本地统计';
}

function statSourceContextLabel(value: string) {
  return value === 'external' ? '外部平台' : value === 'local' ? '本地统计' : value;
}

function normalizeStatSource(value: unknown): 'local' | 'external' | '' {
  const rawValue = Array.isArray(value) ? value[0] : value;
  if (rawValue === 'local' || rawValue === 'external') {
    return rawValue;
  }
  return '';
}

function eventTypeLabel(value: string) {
  if (value === 'SHORT_LINK_VISIT') {
    return '访问短链';
  }
  if (value === 'EXTERNAL_SHORT_LINK_VISIT') {
    return '外部短链访问';
  }
  return value;
}

function shortHash(value?: string | null) {
  if (!value) {
    return '-';
  }
  return value.length > 12 ? `${value.slice(0, 12)}...` : value;
}

function attributionText(item: ShortLinkVisit, value?: string | null) {
  if (value) {
    return value;
  }
  return item.statSource === 'external' ? '外部平台未返回' : '-';
}

function formatVisitTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function visitRecordKey(item: ShortLinkVisit, index: number, suffix = '') {
  const fingerprint = item.clientIdHash || item.ipHash || item.userAgentHash || item.eventType;
  return `${item.createdAt}-${fingerprint}-${index}${suffix}`;
}

function numberQuery(value: unknown, fallback: number) {
  const raw = Array.isArray(value) ? value[0] : value;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}
</script>

<template>
  <main class="page short-link-detail-page">
    <section class="shell panel stack detail-shell">
      <div class="detail-hero">
        <div>
          <p class="eyebrow">短链排查</p>
          <h2>短链访问详情</h2>
          <p class="muted">按访问记录核对渠道、活动、设备和匿名技术指纹。</p>
        </div>
        <RouterLink class="button-link secondary compact-return" :to="{ path: '/admin', query: returnQuery }">返回后台</RouterLink>
      </div>
      <div class="scope-line" aria-label="当前查询口径">
        <span class="short-code-token">短码 {{ route.params.shortCode }}</span>
        <span>{{ startDate || '起始不限' }} 至 {{ endDate || '今日' }}</span>
        <span>{{ includeSynthetic ? '包含测试流量' : '默认排除 perf-test' }}</span>
        <span v-if="keyword">关键词 {{ keyword }}</span>
        <span v-if="statSource">明细来源 {{ statSourceContextLabel(statSource) }}</span>
        <span v-if="statSource">测试流量开关只影响 perf-test 排除</span>
      </div>
      <div class="filter-bar detail-filter-bar">
        <label>
          开始日期
          <span class="date-input-shell" :class="{ empty: !startDate }" data-placeholder="选择开始日期">
            <input v-model="startDate" type="date" aria-label="开始日期，格式 YYYY-MM-DD" :disabled="loading" />
          </span>
        </label>
        <label>
          结束日期
          <span class="date-input-shell" :class="{ empty: !endDate }" data-placeholder="选择结束日期">
            <input v-model="endDate" type="date" aria-label="结束日期，格式 YYYY-MM-DD" :disabled="loading" />
          </span>
        </label>
        <label class="toggle-row">
          <input v-model="includeSynthetic" type="checkbox" :disabled="loading" />
          <span>包含测试流量</span>
        </label>
        <button type="button" :disabled="loading" @click="applyFilter">
          {{ loading ? '加载中...' : '应用筛选' }}
        </button>
        <button class="secondary" type="button" :disabled="loading" @click="clearDateFilter">清空</button>
      </div>
      <div v-if="visits" class="detail-pager" aria-label="访问明细分页">
        <span>
          当前 {{ visitStartIndex }}-{{ visitEndIndex }} / {{ visits.total }} 条，
          第 {{ visitPage }} / {{ visitTotalPages }} 页
        </span>
        <label>
          每页
          <select v-model.number="visitPageSize" :disabled="loading" @change="changeVisitPageSize">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
          </select>
        </label>
        <button class="secondary edge-page-action" type="button" :disabled="loading || visitPage <= 1" @click="goVisitPage(1)">首页</button>
        <button class="secondary step-page-action" type="button" :disabled="loading || visitPage <= 1" @click="goVisitPage(visitPage - 1)">上一页</button>
        <button class="secondary step-page-action" type="button" :disabled="loading || visitPage >= visitTotalPages" @click="goVisitPage(visitPage + 1)">下一页</button>
        <button class="secondary edge-page-action" type="button" :disabled="loading || visitPage >= visitTotalPages" @click="goVisitPage(visitTotalPages)">末页</button>
      </div>
      <p v-if="loading" class="muted admin-busy" role="status" aria-live="polite">正在加载访问明细，请稍候。</p>
      <p v-if="error" class="error-text" role="alert" aria-live="polite">{{ error }}</p>
      <div v-else-if="visits?.records.length" class="table-wrap" :aria-busy="loading">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>来源</th>
              <th>访问动作</th>
              <th>渠道</th>
              <th>活动</th>
              <th>设备</th>
              <th>Referer</th>
              <th>技术明细</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in visits?.records" :key="visitRecordKey(item, index)">
              <td>{{ formatVisitTime(item.createdAt) }}</td>
              <td>{{ statSourceLabel(item.statSource) }}</td>
              <td>{{ eventTypeLabel(item.eventType) }}</td>
              <td>{{ attributionText(item, item.channel) }}</td>
              <td>{{ attributionText(item, item.campaign) }}</td>
              <td>{{ item.deviceType || '-' }}</td>
              <td>{{ item.referer || '-' }}</td>
              <td>
                <details class="inline-debug">
                  <summary>查看</summary>
                  <dl>
                    <div>
                      <dt>埋点代码</dt>
                      <dd><code>{{ item.eventType }}</code></dd>
                    </div>
                    <div>
                      <dt>匿名访客</dt>
                      <dd><code :title="item.clientIdHash || undefined">{{ shortHash(item.clientIdHash) }}</code></dd>
                    </div>
                    <div>
                      <dt>匿名 IP</dt>
                      <dd><code :title="item.ipHash || undefined">{{ shortHash(item.ipHash) }}</code></dd>
                    </div>
                    <div>
                      <dt>设备指纹</dt>
                      <dd><code :title="item.userAgentHash || undefined">{{ shortHash(item.userAgentHash) }}</code></dd>
                    </div>
                  </dl>
                </details>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="visits?.records.length" class="visit-card-list" :aria-busy="loading">
        <article
          v-for="(item, index) in visits.records"
          :key="visitRecordKey(item, index, '-card')"
          class="visit-card"
          data-testid="shortlink-visit-card"
        >
          <header>
            <span>{{ eventTypeLabel(item.eventType) }}</span>
            <strong>{{ formatVisitTime(item.createdAt) }}</strong>
          </header>
          <div class="visit-card-grid">
            <span>
              <small>来源</small>
              {{ statSourceLabel(item.statSource) }}
            </span>
            <span>
              <small>渠道</small>
              {{ attributionText(item, item.channel) }}
            </span>
            <span>
              <small>活动</small>
              {{ attributionText(item, item.campaign) }}
            </span>
            <span>
              <small>设备</small>
              {{ item.deviceType || '-' }}
            </span>
          </div>
          <p v-if="item.referer" class="visit-referer">Referer：{{ item.referer }}</p>
          <details class="inline-debug mobile-debug">
            <summary>技术明细</summary>
            <dl>
              <div>
                <dt>埋点代码</dt>
                <dd><code>{{ item.eventType }}</code></dd>
              </div>
              <div>
                <dt>匿名访客</dt>
                <dd><code :title="item.clientIdHash || undefined">{{ shortHash(item.clientIdHash) }}</code></dd>
              </div>
              <div>
                <dt>匿名 IP</dt>
                <dd><code :title="item.ipHash || undefined">{{ shortHash(item.ipHash) }}</code></dd>
              </div>
              <div>
                <dt>设备指纹</dt>
                <dd><code :title="item.userAgentHash || undefined">{{ shortHash(item.userAgentHash) }}</code></dd>
              </div>
            </dl>
          </details>
        </article>
      </div>
      <p v-else-if="visits && !loading" class="muted empty-state" role="status" aria-live="polite">
        当前筛选范围内暂无访问记录，可以调整日期后再查看。
      </p>
      <div class="detail-actions">
        <RouterLink class="button-link" :to="{ path: '/admin', query: returnQuery }">带筛选返回后台</RouterLink>
        <RouterLink class="button-link secondary" to="/admin">返回默认后台</RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
.short-link-detail-page {
  background:
    linear-gradient(135deg, rgba(47, 111, 94, 0.07), transparent 32%),
    linear-gradient(180deg, #f7f3eb 0%, #eef4ef 100%);
}

.detail-shell {
  width: min(100%, 1080px);
  padding: 26px;
}

.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid rgba(36, 48, 47, 0.08);
  padding-bottom: 18px;
}

.detail-hero h2 {
  margin-bottom: 8px;
  font-size: 30px;
}

.detail-hero p {
  margin: 0;
}

.compact-return {
  flex: 0 0 auto;
  min-height: 44px;
  padding: 0 14px;
}

.scope-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scope-line span {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 30px;
  border: 1px solid rgba(47, 111, 94, 0.12);
  border-radius: 999px;
  padding: 6px 11px;
  background: rgba(237, 247, 242, 0.72);
  color: #40514e;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.25;
  overflow-wrap: anywhere;
  white-space: normal;
}

.scope-line .short-code-token {
  border-color: rgba(215, 155, 67, 0.28);
  background: #fff7e8;
  color: #6a4a22;
}

.detail-filter-bar {
  grid-template-columns: repeat(2, minmax(160px, 1fr)) minmax(150px, auto) repeat(2, auto);
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.68);
}

.detail-filter-bar label {
  font-size: 12px;
  font-weight: 850;
}

.detail-filter-bar .toggle-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.82);
  white-space: nowrap;
}

.detail-filter-bar .toggle-row input {
  width: 16px;
  height: 16px;
}

.detail-pager,
.detail-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.detail-pager {
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.78);
}

.detail-pager span,
.detail-pager label {
  color: #596764;
  font-size: 12px;
  font-weight: 900;
}

.detail-pager label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-pager select {
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 0 8px;
  background: #fff;
  color: #24302f;
  font-weight: 800;
}

.table-wrap {
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
}

.table-wrap table {
  min-width: 980px;
}

.table-wrap th {
  background: rgba(250, 252, 249, 0.92);
}

.visit-card-list {
  display: none;
}

.visit-card {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.86);
}

.visit-card header {
  display: grid;
  gap: 4px;
}

.visit-card header span {
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 950;
}

.visit-card header strong {
  color: #24302f;
  font-size: 16px;
  line-height: 1.3;
}

.visit-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.visit-card-grid span {
  display: grid;
  gap: 3px;
  min-height: 58px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 10px;
  background: rgba(250, 252, 249, 0.72);
  color: #24302f;
  font-size: 13px;
  font-weight: 850;
  overflow-wrap: anywhere;
}

.visit-card-grid small {
  color: #6b7875;
  font-size: 11px;
  font-weight: 850;
}

.visit-referer {
  margin: 0;
  color: #596764;
  font-size: 12px;
  font-weight: 760;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.mobile-debug dl {
  margin-top: 10px;
}

.detail-actions {
  border-top: 1px solid rgba(36, 48, 47, 0.08);
  padding-top: 14px;
}

@media (max-width: 760px) {
  .short-link-detail-page {
    padding: 14px;
  }

  .detail-shell {
    padding: 18px;
  }

  .detail-hero {
    display: grid;
  }

  .detail-hero h2 {
    font-size: 24px;
  }

  .compact-return {
    width: 100%;
  }

  .detail-filter-bar {
    grid-template-columns: 1fr;
  }

  .detail-filter-bar button {
    width: 100%;
  }

  .detail-pager {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: stretch;
  }

  .detail-pager > span,
  .detail-pager label {
    grid-column: 1 / -1;
    width: 100%;
  }

  .detail-pager .edge-page-action {
    display: none;
  }

  .detail-pager .step-page-action {
    width: 100%;
  }

  .table-wrap {
    display: none;
  }

  .visit-card-list {
    display: grid;
    gap: 10px;
  }
}
</style>
