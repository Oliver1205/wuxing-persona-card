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
const sharedEntry = computed(() => Boolean(route.query.sc || route.query.channel === 'shared-result'));

const identityTitle = computed(() => {
  if (!result.value) {
    return '';
  }
  return `${result.value.primaryElementName}${result.value.secondaryElementName} · ${result.value.starOfficerName}`;
});

const archetypeName = computed(() => {
  if (!result.value) {
    return '';
  }
  return `${result.value.primaryElementName}${result.value.secondaryElementName}型${result.value.keywords[0] ?? '探索者'}`;
});

const personaLine = computed(() => {
  if (!result.value) {
    return '';
  }
  return `你可能是那种既有${result.value.primaryElementName}的主心骨，也保留${result.value.secondaryElementName}的弹性空间的人。`;
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
      <section v-if="loading" class="result-state-card" aria-live="polite">
        <p class="eyebrow">正在展开你的五行人格卡</p>
        <div class="loading-mark" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <h2>结果马上就好</h2>
        <p class="muted">正在读取人格身份、五行比例和专属短链。</p>
        <div class="skeleton-lines" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </section>
      <section v-else-if="error" class="result-state-card error-state">
        <p class="eyebrow">这张人格卡暂时打不开</p>
        <h2>可能是链接失效，或结果还没有生成完成</h2>
        <p class="muted">{{ error }}</p>
        <div class="actions">
          <RouterLink class="button-link" to="/test">重新测一张</RouterLink>
          <RouterLink class="button-link secondary" to="/">返回首页</RouterLink>
        </div>
      </section>
      <template v-else-if="result">
        <section v-if="sharedEntry" class="shared-entry-banner" aria-label="分享来源提示">
          <div>
            <span>朋友分享给你的五行人格卡</span>
            <strong>先看看这张卡像不像 TA，也可以顺手测一张自己的。</strong>
          </div>
          <RouterLink
            class="button-link primary-cta"
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-banner' } }"
            @click="sharedLandingStart"
          >
            我也测一张
          </RouterLink>
        </section>

        <div class="result-hero">
          <p class="eyebrow">你的五行人格身份</p>
          <h1>{{ identityTitle }}</h1>
          <p>{{ result.keywords.join(' · ') }}</p>
          <div class="result-identity-grid" aria-label="结果摘要">
            <span>{{ archetypeName }}</span>
            <span>{{ result.primaryPercent }}% {{ result.primaryElementName }}</span>
            <span>{{ result.secondaryPercent }}% {{ result.secondaryElementName }}</span>
          </div>
        </div>

        <div class="panel">
          <PersonaCard :result="result" />
        </div>

        <section class="identity-statement">
          <p class="eyebrow">一句话人格感</p>
          <h2>{{ personaLine }}</h2>
          <p>
            {{ result.starOfficerName }} 让你的表达更有辨识度：{{ result.keywords.slice(0, 3).join('、') }}。
          </p>
        </section>

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
          <RouterLink
            class="button-link"
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-cta' } }"
            @click="sharedLandingStart"
          >
            我也要测
          </RouterLink>
          <RouterLink class="button-link secondary" to="/test" @click="retake">重新测试</RouterLink>
        </div>
        <div class="share-guidance">
          <span>保存图适合发朋友圈</span>
          <span>短链适合发私聊</span>
          <span>朋友打开就是这张卡</span>
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
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.86), rgba(236, 244, 239, 0.92)),
    radial-gradient(circle at 80% 20%, rgba(215, 155, 67, 0.2), transparent 34%);
}

.shared-entry-banner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(47, 111, 94, 0.18);
  border-radius: 8px;
  padding: 16px;
  background: #edf7f2;
  color: #24302f;
}

.shared-entry-banner span,
.shared-entry-banner strong {
  display: block;
}

.shared-entry-banner span {
  color: #2f6f5e;
  font-size: 13px;
  font-weight: 900;
}

.shared-entry-banner strong {
  margin-top: 4px;
  font-size: 16px;
}

.result-state-card {
  display: grid;
  gap: 14px;
  overflow: hidden;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(237, 247, 242, 0.94)),
    radial-gradient(circle at 88% 18%, rgba(215, 155, 67, 0.16), transparent 34%);
  box-shadow: 0 12px 36px rgba(31, 48, 43, 0.08);
}

.result-state-card h2 {
  max-width: 560px;
  margin: 0;
  font-size: 30px;
}

.result-state-card p {
  margin: 0;
}

.loading-mark {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 28px;
}

.loading-mark span {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #2f6f5e;
  animation: resultPulse 1.15s ease-in-out infinite;
}

.loading-mark span:nth-child(2) {
  animation-delay: 0.16s;
}

.loading-mark span:nth-child(3) {
  animation-delay: 0.32s;
}

.skeleton-lines {
  display: grid;
  gap: 10px;
  margin-top: 6px;
}

.skeleton-lines span {
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(47, 111, 94, 0.12), rgba(215, 155, 67, 0.18), rgba(47, 111, 94, 0.12));
}

.skeleton-lines span:nth-child(1) {
  width: min(100%, 520px);
}

.skeleton-lines span:nth-child(2) {
  width: min(86%, 430px);
}

.skeleton-lines span:nth-child(3) {
  width: min(64%, 320px);
}

.error-state {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 240, 237, 0.92)),
    radial-gradient(circle at 88% 18%, rgba(157, 57, 41, 0.12), transparent 34%);
}

.error-state .eyebrow {
  color: #9d3929;
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

.result-identity-grid {
  display: grid;
  grid-template-columns: 1.2fr repeat(2, minmax(0, 0.8fr));
  gap: 10px;
  margin-top: 10px;
}

.result-identity-grid span {
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 11px 12px;
  background: rgba(255, 255, 255, 0.72);
  color: #253634;
  font-size: 14px;
  font-weight: 850;
}

.identity-statement {
  display: grid;
  gap: 10px;
  border-radius: 8px;
  padding: 24px;
  background: #24302f;
  color: #fff;
}

.identity-statement .eyebrow {
  color: #e7c783;
}

.identity-statement h2 {
  max-width: 780px;
  margin: 0;
  color: #fff;
  font-size: 28px;
}

.identity-statement p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
}

.share-guidance {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.share-guidance span {
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.68);
  color: #40514e;
  font-size: 13px;
  font-weight: 800;
  text-align: center;
}

@media (max-width: 760px) {
  .result-hero h1 {
    font-size: 30px;
  }

  .result-identity-grid,
  .shared-entry-banner,
  .share-guidance {
    grid-template-columns: 1fr;
  }

  .identity-statement h2 {
    font-size: 22px;
  }
}

@keyframes resultPulse {
  0%,
  100% {
    opacity: 0.32;
    transform: translateY(0);
  }

  50% {
    opacity: 1;
    transform: translateY(-4px);
  }
}
</style>
