<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchShortLinkVisits } from '../api/admin';
import type { AdminDateFilter } from '../api/admin';
import type { PageResult, ShortLinkVisit } from '../api/types';

const route = useRoute();
const token = ref(localStorage.getItem('wuxing_admin_token') || '');
const startDate = ref(String(route.query.startDate || ''));
const endDate = ref(String(route.query.endDate || ''));
const visits = ref<PageResult<ShortLinkVisit> | null>(null);
const error = ref('');
const loading = ref(false);

onMounted(load);

async function load() {
  if (loading.value) {
    return;
  }
  error.value = '';
  loading.value = true;
  try {
    visits.value = await fetchShortLinkVisits(token.value, String(route.params.shortCode), 1, 20, dateFilter());
  } catch (err) {
    error.value = err instanceof Error ? err.message : '访问详情加载失败';
  } finally {
    loading.value = false;
  }
}

function clearDateFilter() {
  startDate.value = '';
  endDate.value = '';
  load();
}

function dateFilter(): AdminDateFilter {
  return {
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
  };
}

function statSourceLabel(value: ShortLinkVisit['statSource']) {
  return value === 'external' ? '外部平台' : '本地统计';
}

function eventTypeLabel(value: string) {
  if (value === 'SHORT_LINK_VISIT') {
    return '访问短链';
  }
  return value;
}

function shortHash(value: string) {
  return value.length > 12 ? `${value.slice(0, 12)}...` : value;
}
</script>

<template>
  <main class="page">
    <section class="shell panel stack">
      <h2>短链访问详情</h2>
      <p class="muted">短码：{{ route.params.shortCode }}</p>
      <div class="filter-bar">
        <label>
          开始日期
          <input v-model="startDate" type="date" :disabled="loading" />
        </label>
        <label>
          结束日期
          <input v-model="endDate" type="date" :disabled="loading" />
        </label>
        <button type="button" :disabled="loading" @click="load">
          {{ loading ? '加载中...' : '应用筛选' }}
        </button>
        <button class="secondary" type="button" :disabled="loading" @click="clearDateFilter">清空</button>
      </div>
      <p v-if="loading" class="muted admin-busy">正在加载访问明细，请稍候。</p>
      <p v-if="error" class="error-text">{{ error }}</p>
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
            <tr v-for="item in visits?.records" :key="`${item.createdAt}-${item.clientIdHash}`">
              <td>{{ item.createdAt }}</td>
              <td>{{ statSourceLabel(item.statSource) }}</td>
              <td>{{ eventTypeLabel(item.eventType) }}</td>
              <td>{{ item.channel || '-' }}</td>
              <td>{{ item.campaign || '-' }}</td>
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
                      <dd><code :title="item.clientIdHash">{{ shortHash(item.clientIdHash) }}</code></dd>
                    </div>
                    <div>
                      <dt>匿名 IP</dt>
                      <dd><code :title="item.ipHash">{{ shortHash(item.ipHash) }}</code></dd>
                    </div>
                    <div>
                      <dt>设备指纹</dt>
                      <dd><code :title="item.userAgentHash">{{ shortHash(item.userAgentHash) }}</code></dd>
                    </div>
                  </dl>
                </details>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="visits && !loading" class="muted empty-state">
        当前筛选范围内暂无访问记录，可以调整日期后再查看。
      </p>
      <RouterLink class="button-link" to="/admin">返回后台</RouterLink>
    </section>
  </main>
</template>
