<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchResult } from '../api/results';
import type { ResultDetail } from '../api/types';
import ElementSpectrum from '../components/ElementSpectrum.vue';
import PersonaCard from '../components/PersonaCard.vue';
import ShareLinkBox from '../components/ShareLinkBox.vue';
import { downloadResultShareCard } from '../utils/shareCard';
import { track } from '../utils/tracker';

const route = useRoute();
const result = ref<ResultDetail | null>(null);
const loading = ref(true);
const error = ref('');
const shareImageStatus = ref('');

const identityTitle = computed(() => {
  if (!result.value) {
    return '';
  }
  return `${result.value.primaryElementName}${result.value.secondaryElementName} · ${result.value.starOfficerName}`;
});

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

function downloadShareImage() {
  if (!result.value) {
    return;
  }
  shareImageStatus.value = '';
  try {
    downloadResultShareCard(result.value);
    shareImageStatus.value = '分享图已生成';
    track('SAVE_SHARE_IMAGE_SUCCESS', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  } catch (err) {
    shareImageStatus.value = err instanceof Error ? err.message : '分享图生成失败';
  }
}

function retake() {
  if (result.value) {
    track('RETAKE_TEST_CLICK', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  }
}

function sharedLandingStart() {
  if (result.value) {
    track('SHARED_RESULT_CTA_CLICK', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  }
}
</script>

<template>
  <main class="page result-page">
    <section class="shell stack">
      <div v-if="loading" class="panel">结果加载中...</div>
      <div v-else-if="error" class="panel stack">
        <h2>结果没有找到</h2>
        <p class="muted">{{ error }}</p>
        <RouterLink class="button-link" to="/test">重新测试</RouterLink>
      </div>
      <template v-else-if="result">
        <div class="result-hero">
          <p class="eyebrow">你的五行人格身份</p>
          <h1>{{ identityTitle }}</h1>
          <p>{{ result.keywords.join(' · ') }}</p>
        </div>

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

        <div class="panel">
          <ElementSpectrum :scores="result.allElementScores" />
        </div>

        <ShareLinkBox
          :result-id="result.resultId"
          :short-code="result.shortCode"
          :short-url="result.shortUrl"
          @copied="copied"
        />

        <div class="actions">
          <button type="button" @click="downloadShareImage">保存分享图</button>
          <RouterLink class="button-link" to="/test" @click="retake">重新测试</RouterLink>
          <RouterLink class="button-link secondary" to="/test" @click="sharedLandingStart">我也要测</RouterLink>
        </div>
        <p v-if="shareImageStatus" class="muted">{{ shareImageStatus }}</p>
      </template>
    </section>
  </main>
</template>

<style scoped>
.result-page {
  background:
    linear-gradient(180deg, #f8f5eb 0%, #edf4ef 42%, #f6f3ec 100%);
}

.result-hero {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.76);
}

.result-hero h1 {
  max-width: 720px;
  font-size: 46px;
}

.result-hero p {
  margin: 0;
  color: #40514e;
  font-size: 18px;
  font-weight: 800;
}

@media (max-width: 760px) {
  .result-hero h1 {
    font-size: 30px;
  }
}
</style>
