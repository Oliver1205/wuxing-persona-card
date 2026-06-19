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
const sharedEntry = computed(() => Boolean(
  route.query.sc
  || route.query.channel === 'shared-result'
  || route.query.channel === 'share',
));

const personaLine = computed(() => {
  if (!result.value) {
    return '';
  }
  return `你可能是那种既有${result.value.primaryElementName}的主心骨，也保留${result.value.secondaryElementName}的弹性空间的人。`;
});

const resonanceSignals = computed(() => {
  if (!result.value) {
    return [];
  }
  const [primaryTrait = '有主见', orderTrait = '有节奏', judgmentTrait = '会判断', secondaryTrait = '能调和', starTrait = '有辨识度'] = result.value.keywords;
  return [
    {
      label: '做决定时',
      text: `你更容易先抓住${primaryTrait}和${judgmentTrait}，不太喜欢长期停在含糊状态。`,
    },
    {
      label: '推进事情时',
      text: `你会用${orderTrait}维持自己的步调，也会留下${secondaryTrait}的回旋空间。`,
    },
    {
      label: '和人相处时',
      text: `${result.value.starOfficerName}让你带着${starTrait}，既有个人风格，也愿意照顾关系里的感受。`,
    },
  ];
});

const analysisBlocks = computed(() => {
  if (!result.value) {
    return [];
  }
  return [
    {
      kicker: '判定依据',
      title: '为什么判定为这个命盘',
      text: result.value.layoutExplanation,
    },
    {
      kicker: '元素强弱',
      title: '元素逐项解读',
      text: result.value.strengthText,
    },
    {
      kicker: '互动总览',
      title: '元素互动与总览',
      text: result.value.relationshipText,
    },
  ];
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
    <section class="shell stack result-shell">
      <section v-if="loading" class="result-state-card" role="status" aria-live="polite">
        <p class="eyebrow">正在展开你的五行人格卡</p>
        <div class="loading-mark" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <h2>结果马上就好</h2>
        <p class="muted">正在读取人格身份、五行比例和专属分享链接。</p>
        <div class="skeleton-lines" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </section>
      <section v-else-if="error" class="result-state-card error-state" role="alert" aria-live="polite">
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
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-banner', matchCode: result.shortCode } }"
            @click="sharedLandingStart"
          >
            我也测一张
          </RouterLink>
        </section>

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

        <section class="panel stack resonance-panel">
          <div>
            <p class="eyebrow">为什么像你</p>
            <h2>朋友最容易认出的三个表现</h2>
          </div>
          <div class="resonance-grid">
            <article v-for="signal in resonanceSignals" :key="signal.label">
              <span>{{ signal.label }}</span>
              <p>{{ signal.text }}</p>
            </article>
          </div>
        </section>

        <section class="panel interpretation-panel" aria-label="命盘解释">
          <div class="interpretation-head">
            <p class="eyebrow">命盘解释</p>
            <h2>这张卡为什么这样判断</h2>
          </div>
          <div class="interpretation-grid">
            <article v-for="block in analysisBlocks" :key="block.kicker">
              <span>{{ block.kicker }}</span>
              <h3>{{ block.title }}</h3>
              <p>{{ block.text }}</p>
            </article>
          </div>
        </section>

        <div class="panel">
          <ElementSpectrum :scores="result.allElementScores" />
        </div>

        <ShareLinkBox
          v-if="!sharedEntry"
          :result-id="result.resultId"
          :short-code="result.shortCode"
          :short-url="result.shortUrl"
          show-save-image
          @copied="copied"
          @save-image="downloadShareImage"
        />

        <section v-else class="panel shared-bottom-cta" aria-label="分享结果底部行动">
          <div>
            <span>想看看你们合不合拍？</span>
            <strong>测完自己的卡，继续和这张短码做双人匹配。</strong>
          </div>
          <RouterLink
            class="button-link primary-cta"
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-footer', matchCode: result.shortCode } }"
            @click="sharedLandingStart"
          >
            生成我的人格卡
          </RouterLink>
        </section>

        <RouterLink v-if="!sharedEntry" class="button-link secondary result-retake-link" to="/test" @click="retake">重新测试</RouterLink>
        <p v-if="shareImageStatus" class="muted" role="status" aria-live="polite">{{ shareImageStatus }}</p>
      </template>
    </section>
  </main>
</template>

<style scoped>
.result-page {
  position: relative;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 78% 8%, rgba(198, 227, 226, 0.44), transparent 24%),
    linear-gradient(180deg, #f8f3e9 0%, #fbf7ef 50%, #edf3ee 100%);
}

.result-page::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
}

.result-page::before {
  height: 170px;
  background: rgba(177, 211, 209, 0.64);
  clip-path: ellipse(76% 47% at 78% 100%);
}

.result-shell {
  position: relative;
  z-index: 1;
}

.shared-entry-banner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(47, 111, 94, 0.18);
  border-radius: 8px;
  padding: 16px;
  background: rgba(237, 247, 242, 0.9);
  color: #24302f;
  box-shadow: 0 12px 28px rgba(31, 48, 43, 0.07);
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
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(250, 246, 237, 0.9)),
    linear-gradient(90deg, rgba(177, 211, 209, 0.2), transparent);
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
  background: #123253;
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

.result-action-strip,
.shared-bottom-cta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  border-color: rgba(47, 111, 94, 0.14);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(237, 247, 242, 0.88)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.08), rgba(215, 155, 67, 0.08));
}

.result-action-strip span,
.shared-bottom-cta span {
  display: block;
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 950;
}

.result-action-strip strong,
.shared-bottom-cta strong {
  display: block;
  margin-top: 5px;
  color: #24302f;
  font-size: 20px;
  line-height: 1.35;
}

.result-action-strip p {
  max-width: 620px;
  margin: 6px 0 0;
  color: #596764;
  font-size: 14px;
  font-weight: 760;
  line-height: 1.6;
}

.result-action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.result-action-buttons .primary-action,
.shared-bottom-cta .primary-cta {
  min-width: 144px;
  min-height: 44px;
  border: 1px solid rgba(47, 111, 94, 0.18);
  border-radius: 8px;
  background: #2f6f5e;
  color: #fff;
  font-size: 14px;
  font-weight: 950;
  box-shadow: 0 12px 24px rgba(47, 111, 94, 0.16);
}

.result-action-buttons .compact-action {
  min-width: 96px;
  min-height: 44px;
  padding: 0 14px;
  font-size: 14px;
}

.identity-statement {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 10px;
  border-radius: 8px;
  padding: 24px;
  background:
    linear-gradient(135deg, #123253, #173d55),
    #123253;
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

.resonance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.resonance-grid article {
  display: grid;
  gap: 10px;
  min-height: 138px;
  border: 1px solid rgba(47, 112, 94, 0.14);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.72);
}

.resonance-grid span {
  color: #2f6f5e;
  font-size: 13px;
  font-weight: 900;
}

.resonance-grid p {
  margin: 0;
  color: #253634;
  font-size: 15px;
  font-weight: 750;
  line-height: 1.7;
}

.interpretation-panel {
  display: grid;
  gap: 16px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.84), rgba(250, 246, 237, 0.78)),
    rgba(255, 255, 255, 0.78);
}

.interpretation-head {
  display: grid;
  gap: 4px;
}

.interpretation-head .eyebrow,
.interpretation-head h2 {
  margin: 0;
}

.interpretation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.interpretation-grid article {
  display: grid;
  align-content: start;
  gap: 9px;
  border: 1px solid rgba(47, 111, 94, 0.13);
  border-radius: 8px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.72);
}

.interpretation-grid span {
  color: #7b5d35;
  font-size: 12px;
  font-weight: 900;
}

.interpretation-grid h3 {
  margin: 0;
  color: #23302e;
  font-size: 17px;
  line-height: 1.35;
}

.interpretation-grid p {
  margin: 0;
  color: #40514e;
  font-size: 14px;
  line-height: 1.78;
}

.result-retake-link {
  justify-self: center;
  min-width: min(100%, 220px);
}

@media (max-width: 760px) {
  .resonance-grid,
  .shared-entry-banner,
  .result-action-strip,
  .shared-bottom-cta {
    grid-template-columns: 1fr;
  }

  .identity-statement h2 {
    font-size: 22px;
  }

  .identity-statement {
    padding: 20px;
  }

  .interpretation-grid {
    grid-template-columns: 1fr;
  }

  .resonance-grid article {
    min-height: auto;
    padding: 14px;
  }

  .resonance-grid p {
    font-size: 14px;
  }

  .result-action-strip strong,
  .shared-bottom-cta strong {
    font-size: 18px;
  }

  .result-action-buttons {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    justify-content: stretch;
  }

  .result-action-buttons .primary-action {
    grid-column: 1 / -1;
  }

  .result-action-buttons .compact-action,
  .shared-bottom-cta .primary-cta {
    width: 100%;
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
