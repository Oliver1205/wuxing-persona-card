<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchResult } from '../api/results';
import type { ResultDetail } from '../api/types';
import PersonaCard from '../components/PersonaCard.vue';
import ShareLinkBox from '../components/ShareLinkBox.vue';
import { track } from '../utils/tracker';

const route = useRoute();
const result = ref<ResultDetail | null>(null);
const loading = ref(true);
const error = ref('');

onMounted(async () => {
  try {
    result.value = await fetchResult(String(route.params.resultId));
  } catch (err) {
    error.value = err instanceof Error ? err.message : '结果加载失败';
  } finally {
    loading.value = false;
  }
});

function copied() {
  if (result.value) {
    track('SHORT_LINK_COPY', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  }
}
</script>

<template>
  <main class="page">
    <section class="shell stack">
      <div v-if="loading" class="panel">结果加载中...</div>
      <div v-else-if="error" class="panel stack">
        <h2>结果没有找到</h2>
        <p class="muted">{{ error }}</p>
        <RouterLink class="button-link" to="/test">重新测试</RouterLink>
      </div>
      <template v-else-if="result">
        <div class="panel">
          <PersonaCard :result="result" />
        </div>

        <div class="panel stack">
          <h2>五行布局解释</h2>
          <p>{{ result.layoutExplanation }}</p>
          <h2>性格亮点</h2>
          <p>{{ result.strengthText }}</p>
          <h2>相处优势</h2>
          <p>{{ result.relationshipText }}</p>
        </div>

        <ShareLinkBox :short-url="result.shortUrl" @copied="copied" />

        <div class="actions">
          <RouterLink class="button-link" to="/test">重新测试</RouterLink>
        </div>
      </template>
    </section>
  </main>
</template>
