<template>
  <main class="page guide-page">
    <div class="guide-art" aria-hidden="true">
      <span class="mountain mountain-front"></span>
      <span class="mountain mountain-back"></span>
      <span class="deep-water"></span>
    </div>
    <section class="shell hero-shell" aria-label="五行人格卡首页">
      <div class="hero-copy stack">
        <div class="vertical-motto" aria-hidden="true">
          <span>简于形 · 明于心</span>
        </div>

        <div class="hero-title-group">
          <h1>五行人格卡</h1>
          <div class="title-rule" aria-hidden="true">
            <span></span>
            <i></i>
            <span></span>
          </div>
          <p class="lead">从出生年月和 5 个选择，读出你的五行人格倾向。</p>
        </div>

        <div class="hero-metrics" aria-label="测试特点">
          <span>90 秒完成</span>
          <span>出生年月 + 5 题</span>
          <span>结果可分享</span>
        </div>

        <div class="actions">
          <RouterLink data-testid="start-test-link" class="button-link primary-cta" to="/test" @click="start">开始测试</RouterLink>
          <a class="button-link secondary" href="#preview">看五行样例</a>
        </div>
        <p
          id="manual-match-message"
          class="clipboard-message"
          :class="{ empty: !clipboardMessage }"
          role="status"
          aria-live="polite"
        >
          {{ clipboardMessage }}
        </p>
        <form class="manual-match-entry" aria-label="手动输入匹配短码" :aria-busy="manualChecking" @submit.prevent="lookupManualShortCode">
          <input
            v-model="manualShortCode"
            data-testid="manual-match-code"
            maxlength="120"
            inputmode="text"
            autocomplete="off"
            placeholder="短码或分享链接"
            aria-label="已有匹配短码或分享链接"
            aria-describedby="manual-match-message"
            :aria-invalid="manualShortCode.length > 0 && !manualShortCodeValid"
            @input="resetManualMatchCandidate"
          >
          <button data-testid="manual-match-submit" type="submit" :disabled="manualChecking">
            {{ manualChecking ? '识别中' : '匹配' }}
          </button>
        </form>
        <div class="manual-match-tools">
          <button
            type="button"
            class="secondary clipboard-check-button"
            :disabled="clipboardChecking"
            @click="detectClipboardShortCode(true)"
          >
            {{ clipboardChecking ? '检测中' : '检测剪贴板' }}
          </button>
        </div>
        <section v-if="matchCandidate" class="match-invite" aria-label="双人匹配邀请" aria-live="polite">
          <div>
            <p class="match-kicker">检测到匹配短码</p>
            <h2>要和这张人格卡做双人匹配吗？</h2>
            <p>
              短码 {{ matchCandidate.shortCode }} · {{ matchCandidate.displayName }}
            </p>
          </div>
          <div class="match-actions">
            <button data-testid="match-accept-button" type="button" @click="startMatch">开始双人匹配</button>
            <button type="button" class="secondary" @click="dismissMatch">暂时不用</button>
          </div>
        </section>
        <p class="notice">
          本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
        </p>
      </div>

      <div id="preview" class="hero-preview" aria-label="五行人格卡样例">
        <div class="preview-card">
          <div
            v-for="item in elementVisuals"
            :key="item.code"
            class="element-column"
            :style="{ '--element-color': item.color }"
          >
            <ElementMark :code="item.code" :name="item.name" />
            <div class="element-keywords" :aria-label="`${item.name}关键词`">
              <span v-for="keyword in item.keywords" :key="keyword">{{ keyword }}</span>
            </div>
            <i aria-hidden="true"></i>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchMatchCandidate } from '../api/matches';
import type { MatchCandidate } from '../api/types';
import ElementMark from '../components/ElementMark.vue';
import { getAttribution } from '../utils/attribution';
import { elementVisuals } from '../utils/elementVisuals';
import { track } from '../utils/tracker';

const router = useRouter();
const matchCandidate = ref<MatchCandidate | null>(null);
const clipboardChecking = ref(false);
const manualChecking = ref(false);
const matchDismissed = ref(false);
const clipboardMessage = ref('');
const manualShortCode = ref('');
const manualLookupSeq = ref(0);
const shortCodePattern = /^[0-9a-zA-Z]{6,7}$/;
const manualShortCodeValid = computed(() => Boolean(normalizeClipboardShortCode(manualShortCode.value)));

onMounted(() => {
  track('PAGE_VIEW_HOME', '/');
});

function start() {
  track('START_TEST_CLICK', '/');
}

async function detectClipboardShortCode(manual = false) {
  let lookupSeq: number | undefined;
  if (manual) {
    clipboardMessage.value = '';
    matchCandidate.value = null;
    matchDismissed.value = false;
    manualLookupSeq.value += 1;
    lookupSeq = manualLookupSeq.value;
  }
  if (!manual && new URLSearchParams(window.location.search).has('skipClipboardAuto')) {
    return;
  }
  if (!manual && navigator.webdriver) {
    return;
  }
  if (clipboardChecking.value || !navigator.clipboard?.readText) {
    if (manual) {
      clipboardMessage.value = '当前浏览器暂时不能读取剪贴板';
    }
    return;
  }
  clipboardChecking.value = true;
  try {
    const text = await navigator.clipboard.readText();
    const shortCode = normalizeClipboardShortCode(text);
    if (!shortCode) {
      if (manual) {
        clipboardMessage.value = '剪贴板里没有可识别的短码或分享链接';
      }
      return;
    }
    if (manual) {
      manualShortCode.value = shortCode;
    }
    await loadMatchCandidate(shortCode, 'MATCH_CLIPBOARD_DETECTED', lookupSeq);
  } catch {
    matchCandidate.value = null;
    if (manual) {
      clipboardMessage.value = '没有识别到可用短码';
    }
  } finally {
    clipboardChecking.value = false;
  }
}

function normalizeClipboardShortCode(value: string) {
  const trimmed = value.trim();
  if (shortCodePattern.test(trimmed)) {
    return trimmed;
  }
  const pathMatch = trimmed.match(/(?:^|[/?#&\s])s\/([0-9a-zA-Z]{6,7})(?=$|[/?#&\s])/);
  if (pathMatch) {
    return pathMatch[1];
  }
  try {
    const url = new URL(trimmed);
    const match = url.pathname.match(/^\/s\/([0-9a-zA-Z]{6,7})\/?$/);
    return match ? match[1] : null;
  } catch {
    return null;
  }
}

async function lookupManualShortCode() {
  if (manualChecking.value) {
    return;
  }
  clipboardMessage.value = '';
  matchCandidate.value = null;
  matchDismissed.value = false;
  manualLookupSeq.value += 1;
  const shortCode = normalizeClipboardShortCode(manualShortCode.value);
  if (!shortCode) {
    clipboardMessage.value = '请输入 6 到 7 位短码，或粘贴 /s/ 开头的分享链接';
    return;
  }
  const lookupSeq = manualLookupSeq.value;
  manualChecking.value = true;
  try {
    await loadMatchCandidate(shortCode, 'MATCH_SHORT_CODE_ENTERED', lookupSeq);
  } catch {
    if (lookupSeq === manualLookupSeq.value) {
      clipboardMessage.value = '没有识别到可用短码';
    }
  } finally {
    if (lookupSeq === manualLookupSeq.value) {
      manualChecking.value = false;
    }
  }
}

function resetManualMatchCandidate() {
  matchCandidate.value = null;
  matchDismissed.value = false;
  clipboardMessage.value = '';
  manualChecking.value = false;
  manualLookupSeq.value += 1;
}

async function loadMatchCandidate(shortCode: string, eventType: string, lookupSeq?: number) {
  const candidate = await fetchMatchCandidate(shortCode);
  if (lookupSeq !== undefined) {
    const currentShortCode = normalizeClipboardShortCode(manualShortCode.value);
    if (lookupSeq !== manualLookupSeq.value || currentShortCode !== shortCode) {
      return;
    }
  }
  if (matchDismissed.value) {
    return;
  }
  matchCandidate.value = candidate;
  manualShortCode.value = candidate.shortCode;
  clipboardMessage.value = '';
  track(eventType, '/', candidate.resultId, candidate.shortCode);
}

function startMatch() {
  if (!matchCandidate.value) {
    return;
  }
  const candidate = matchCandidate.value;
  const attribution = getAttribution();
  const preserveSynthetic = attribution.channel === 'perf-test';
  track('MATCH_MODE_ACCEPT', '/', candidate.resultId, candidate.shortCode);
  void router.push({
    path: '/test',
    query: {
      matchCode: candidate.shortCode,
      channel: preserveSynthetic ? 'perf-test' : 'match',
      campaign: preserveSynthetic ? (attribution.campaign ?? 'match-e2e') : 'clipboard-short-code',
    },
  });
}

function dismissMatch() {
  if (matchCandidate.value) {
    track('MATCH_MODE_DISMISS', '/', matchCandidate.value.resultId, matchCandidate.value.shortCode);
  }
  matchDismissed.value = true;
  matchCandidate.value = null;
}
</script>

<style scoped>
.guide-page {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: stretch;
  justify-content: center;
  min-height: 100vh;
  padding: 48px 18px 96px;
  background:
    linear-gradient(180deg, rgba(255, 251, 243, 0.96) 0%, rgba(247, 239, 226, 0.96) 58%, rgba(242, 231, 212, 0.98) 100%),
    var(--color-paper);
}

.guide-page::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.2;
  background-image:
    linear-gradient(rgba(114, 89, 54, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(114, 89, 54, 0.06) 1px, transparent 1px);
  background-size: 42px 42px, 42px 42px;
  mix-blend-mode: multiply;
}

.guide-art,
.guide-art span {
  position: absolute;
  pointer-events: none;
}

.guide-art {
  inset: 0;
  z-index: 0;
}

.mountain {
  left: 0;
  right: 0;
  bottom: 0;
  height: 185px;
  transform-origin: bottom center;
}

.mountain-back {
  bottom: 28px;
  background: rgba(229, 205, 168, 0.6);
  clip-path: ellipse(72% 42% at 80% 100%);
}

.mountain-front {
  bottom: 28px;
  background: rgba(213, 151, 93, 0.18);
  clip-path: ellipse(74% 45% at 18% 100%);
}

.deep-water {
  left: 0;
  right: 0;
  bottom: 0;
  height: 42px;
  background: rgba(47, 98, 85, 0.12);
}

.hero-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr;
  gap: 34px;
  align-items: start;
  width: min(100%, 760px);
  min-height: calc(100vh - 170px);
}

.hero-copy {
  position: relative;
  justify-items: center;
  padding-top: 126px;
  text-align: center;
  animation: heroEnter 520ms ease both;
}

.vertical-motto {
  position: absolute;
  top: 10px;
  left: 8px;
  color: rgba(37, 48, 45, 0.72);
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0;
}

.hero-title-group {
  display: grid;
  justify-items: center;
  gap: 16px;
}

.hero-title-group h1 {
  color: var(--color-ink);
  font-family: var(--font-serif);
  font-size: 78px;
  font-weight: 600;
  letter-spacing: 0;
  text-indent: 0;
  line-height: 1.04;
}

.title-rule {
  display: grid;
  grid-template-columns: minmax(86px, 1fr) auto minmax(86px, 1fr);
  gap: 18px;
  align-items: center;
  width: min(100%, 440px);
}

.title-rule span {
  height: 1px;
  background: rgba(37, 48, 45, 0.34);
}

.title-rule i {
  width: 9px;
  height: 9px;
  background: var(--color-warm);
  transform: rotate(45deg);
}

.lead {
  margin: 0;
  max-width: 620px;
  color: #33413d;
  font-size: 22px;
  letter-spacing: 0;
  text-indent: 0;
  line-height: 1.45;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 14px;
}

.hero-metrics span {
  border: 1px solid rgba(201, 111, 61, 0.18);
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(255, 252, 245, 0.68);
  color: #4f4035;
  font-size: 15px;
  font-weight: 650;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.36);
}

.primary-cta {
  min-width: min(76vw, 500px);
  min-height: 72px;
  border: 1px solid rgba(158, 79, 46, 0.42);
  border-radius: 8px;
  background: linear-gradient(135deg, var(--color-warm), var(--color-warm-deep));
  color: #fff;
  font-family: var(--font-ui);
  font-size: 28px;
  font-weight: 850;
  letter-spacing: 0;
  text-indent: 0;
  box-shadow: var(--shadow-lift);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease;
}

.primary-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 42px rgba(157, 86, 49, 0.22);
}

.primary-cta::before,
.primary-cta::after {
  content: "";
  width: 11px;
  height: 11px;
  margin: 0 24px;
  background: rgba(255, 232, 192, 0.86);
  transform: rotate(45deg);
}

.actions {
  justify-content: center;
  margin-top: 16px;
}

.actions .secondary {
  border: 0;
  border-bottom: 1px solid rgba(201, 111, 61, 0.46);
  border-radius: 0;
  background: transparent;
  color: #4e4038;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0;
}

.clipboard-check-button {
  min-width: 0;
  min-height: 44px;
  border: 0;
  border-bottom: 1px solid rgba(37, 48, 45, 0.18);
  border-radius: 0;
  padding: 0 2px;
  background: transparent;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

.clipboard-message {
  max-width: 620px;
  margin: -2px 0 0;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
}

.clipboard-message.empty {
  position: absolute;
  overflow: hidden;
  width: 1px;
  height: 1px;
  margin: 0;
  clip-path: inset(50%);
  white-space: nowrap;
}

.manual-match-entry {
  display: grid;
  grid-template-columns: minmax(0, 180px) auto;
  gap: 8px;
  max-width: 350px;
  justify-self: center;
}

.manual-match-tools {
  display: flex;
  justify-content: center;
  margin-top: -10px;
}

.match-invite + .notice {
  margin-top: 0;
}

.manual-match-entry input {
  min-height: 44px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(255, 252, 245, 0.9);
  color: var(--color-ink);
  font: inherit;
  font-weight: 850;
}

.manual-match-entry input[aria-invalid="true"] {
  border-color: rgba(201, 95, 60, 0.44);
  box-shadow: 0 0 0 3px rgba(201, 95, 60, 0.09);
}

.manual-match-entry button {
  min-width: 82px;
}

.match-invite {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  max-width: 620px;
  text-align: left;
  border: 1px solid rgba(47, 98, 85, 0.18);
  border-radius: 8px;
  padding: 14px;
  background:
    linear-gradient(135deg, rgba(246, 240, 228, 0.96), rgba(255, 250, 242, 0.94));
  box-shadow: var(--shadow-paper);
}

.match-invite h2,
.match-invite p {
  margin: 0;
}

.match-invite h2 {
  margin-top: 4px;
  font-size: 20px;
}

.match-invite p:not(.match-kicker) {
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 800;
}

.match-kicker {
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 950;
}

.match-actions {
  display: flex;
  gap: 8px;
}

.match-actions button {
  min-height: 44px;
  white-space: nowrap;
}

.hero-preview {
  display: grid;
  place-items: center;
  margin-top: -2px;
}

.preview-card {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0;
  width: min(100%, 650px);
  min-height: 280px;
  overflow: hidden;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 34px 22px 30px;
  background:
    linear-gradient(180deg, rgba(255, 252, 245, 0.94), rgba(249, 241, 227, 0.84)),
    linear-gradient(90deg, rgba(201, 111, 61, 0.08), rgba(47, 98, 85, 0.08));
  box-shadow: 0 18px 42px rgba(49, 44, 35, 0.12);
  backdrop-filter: blur(8px);
  animation: previewRise 620ms 120ms ease both;
}

.element-column {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 16px;
  min-width: 0;
}

.element-column + .element-column {
  border-left: 1px solid rgba(37, 48, 45, 0.12);
}

.element-keywords {
  display: grid;
  gap: 8px;
  color: var(--color-ink);
  font-family: var(--font-serif);
  font-size: 21px;
  line-height: 1.18;
}

.element-column i {
  width: 9px;
  height: 9px;
  margin-top: 4px;
  border-radius: 50%;
  background: var(--element-color);
}

@media (max-width: 820px) {
  .guide-page {
    padding: 34px 14px 84px;
  }

  .hero-shell {
    gap: 30px;
  }

  .hero-copy {
    padding-top: 82px;
  }

  .match-invite {
    grid-template-columns: 1fr;
  }

  .match-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .manual-match-entry {
    grid-template-columns: 1fr auto;
    max-width: none;
  }

  .preview-card {
    width: min(100%, 690px);
    min-height: 246px;
    padding: 28px 12px 24px;
  }

  .element-keywords {
    gap: 7px;
    font-size: 18px;
  }

  .vertical-motto {
    left: 2px;
    font-size: 15px;
  }
}

@media (max-width: 430px) {
  .hero-title-group h1 {
    font-size: 48px;
  }

  .lead {
    font-size: 16px;
    letter-spacing: 0;
    text-indent: 0;
  }

  .hero-metrics {
    gap: 9px;
  }

  .hero-metrics span {
    padding: 8px 12px;
    font-size: 14px;
  }

  .primary-cta {
    min-height: 62px;
    font-size: 25px;
  }

  .primary-cta::before,
  .primary-cta::after {
    margin: 0 14px;
  }

  .preview-card {
    padding-inline: 8px;
  }

  .element-column {
    gap: 12px;
  }

  .element-keywords {
    font-size: 15px;
  }

}

@keyframes heroEnter {
  from {
    transform: translateY(14px);
  }

  to {
    transform: translateY(0);
  }
}

@keyframes previewRise {
  from {
    transform: translateY(18px);
  }

  to {
    transform: translateY(0);
  }
}
</style>
