<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchAdminOverview, fetchAdminShortLinks } from '../api/admin';
import type { AdminOverview, PageResult, ShortLinkListItem } from '../api/types';
import StatCard from '../components/StatCard.vue';

const token = ref(localStorage.getItem('wuxing_admin_token') || '');
const overview = ref<AdminOverview | null>(null);
const shortLinks = ref<PageResult<ShortLinkListItem> | null>(null);
const error = ref('');
const loading = ref(false);

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
    overview.value = await fetchAdminOverview(token.value);
    shortLinks.value = await fetchAdminShortLinks(token.value, 1, 20);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '后台数据加载失败';
  } finally {
    loading.value = false;
  }
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
        <p v-if="error" class="error-text">{{ error }}</p>
      </div>

      <template v-if="overview">
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
          <h2>热门五行组合</h2>
          <div class="rank-row" v-for="item in overview.popularElementCombos" :key="item.name">
            <span>{{ item.name }}</span>
            <strong>{{ item.count }}</strong>
          </div>
        </div>

        <div class="panel stack">
          <h2>短链列表</h2>
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
                  <td>
                    <RouterLink :to="`/admin/short-links/${item.shortCode}`">查看</RouterLink>
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
