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

onMounted(load);

async function load() {
  error.value = '';
  try {
    visits.value = await fetchShortLinkVisits(token.value, String(route.params.shortCode), 1, 20, dateFilter());
  } catch (err) {
    error.value = err instanceof Error ? err.message : '访问详情加载失败';
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
</script>

<template>
  <main class="page">
    <section class="shell panel stack">
      <h2>短链访问详情</h2>
      <p class="muted">短码：{{ route.params.shortCode }}</p>
      <div class="filter-bar">
        <label>
          开始日期
          <input v-model="startDate" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="endDate" type="date" />
        </label>
        <button type="button" @click="load">应用筛选</button>
        <button class="secondary" type="button" @click="clearDateFilter">清空</button>
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>来源</th>
              <th>事件</th>
              <th>Client Hash</th>
              <th>IP Hash</th>
              <th>User-Agent Hash</th>
              <th>Referer</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in visits?.records" :key="`${item.createdAt}-${item.clientIdHash}`">
              <td>{{ item.createdAt }}</td>
              <td>{{ item.statSource }}</td>
              <td>{{ item.eventType }}</td>
              <td>{{ item.clientIdHash }}</td>
              <td>{{ item.ipHash }}</td>
              <td>{{ item.userAgentHash }}</td>
              <td>{{ item.referer || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <RouterLink class="button-link" to="/admin">返回后台</RouterLink>
    </section>
  </main>
</template>
