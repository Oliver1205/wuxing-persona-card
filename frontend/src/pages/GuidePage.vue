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
          <p class="lead">出生年月 + 5 题，生成五行人格倾向</p>
        </div>

        <div class="hero-metrics" aria-label="测试特点">
          <span>90 秒完成</span>
          <span>出生年月 + 5 题</span>
          <span>结果可分享</span>
        </div>

        <div class="hero-middle">
          <div id="preview" class="hero-preview" aria-label="五行人格卡样例">
            <div class="preview-card">
              <div class="element-thread" aria-hidden="true">
                <span
                  v-for="item in elementVisuals"
                  :key="`${item.code}-thread`"
                  :style="{ '--element-color': item.color }"
                ></span>
              </div>
              <div class="element-preview-grid">
                <div
                  v-for="item in elementVisuals"
                  :key="item.code"
                  class="element-column"
                  :style="{ '--element-color': item.color }"
                >
                  <ElementMark :code="item.code" :name="item.name" compact size="legend" />
                  <div class="element-keywords" :aria-label="`${item.name}关键词`">
                    <span v-for="keyword in item.keywords" :key="keyword">{{ keyword }}</span>
                  </div>
                  <i aria-hidden="true"></i>
                </div>
              </div>
            </div>
          </div>

          <div class="home-flow-bridge" aria-label="开卡流程">
            <div class="bridge-heading" aria-hidden="true">
              <span></span>
              <b>生成路径</b>
              <span></span>
            </div>
            <p>年月定底色，五题校准倾向</p>
            <div class="flow-line" aria-hidden="true">
              <span
                v-for="item in elementVisuals"
                :key="`${item.code}-dot`"
                :style="{ '--element-color': item.color }"
              ></span>
            </div>
            <div class="flow-steps">
              <span><b>定年月</b><small>取底色</small></span>
              <span><b>答 5 题</b><small>看偏好</small></span>
              <span><b>生成人格卡</b><small>可分享</small></span>
            </div>
          </div>
        </div>

        <div class="actions primary-action-row">
          <RouterLink data-testid="start-test-link" class="button-link primary-cta" to="/test" @click="start">开始测试</RouterLink>
          <div class="action-divider" aria-hidden="true">
            <span></span>
            <i></i>
            <span></span>
          </div>
        </div>
      </div>

      <section class="home-secondary stack" aria-label="短链匹配与项目说明">
        <div class="secondary-copy">
          <p class="secondary-kicker">已有朋友的五行卡</p>
          <h2>输入短码做双人匹配</h2>
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
        <p class="notice home-notice">
          本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
        </p>
      </section>
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
  overflow-x: hidden;
  display: flex;
  align-items: stretch;
  justify-content: center;
  min-height: 100vh;
  padding: clamp(24px, 4svh, 42px) 16px 92px;
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
  gap: 22px;
  align-items: start;
  width: min(100%, 720px);
}

.hero-copy {
  position: relative;
  display: grid;
  grid-template-areas:
    "title"
    "metrics"
    "middle"
    "action";
  grid-template-rows: auto auto auto auto;
  align-content: start;
  gap: 14px;
  justify-items: center;
  min-height: min(760px, calc(100svh - 96px));
  padding-top: clamp(58px, 9svh, 96px);
  text-align: center;
  animation: heroEnter 520ms ease both;
}

.vertical-motto {
  position: absolute;
  top: 6px;
  left: 4px;
  color: rgba(37, 48, 45, 0.72);
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 0;
}

.hero-title-group {
  grid-area: title;
  display: grid;
  justify-items: center;
  gap: 12px;
}

.hero-title-group h1 {
  color: var(--color-ink);
  font-family: var(--font-serif);
  font-size: 64px;
  font-weight: 600;
  letter-spacing: 0;
  text-indent: 0;
  line-height: 1.02;
}

.title-rule {
  display: grid;
  grid-template-columns: minmax(64px, 1fr) auto minmax(64px, 1fr);
  gap: 14px;
  align-items: center;
  width: min(100%, 360px);
}

.title-rule span {
  height: 1px;
  background: rgba(37, 48, 45, 0.34);
}

.title-rule i {
  width: 8px;
  height: 8px;
  background: var(--color-warm);
  transform: rotate(45deg);
}

.lead {
  margin: 0;
  max-width: 100%;
  color: #33413d;
  font-size: 18px;
  letter-spacing: 0;
  text-indent: 0;
  line-height: 1.5;
  white-space: nowrap;
}

.hero-metrics {
  grid-area: metrics;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.hero-metrics span {
  border: 1px solid rgba(201, 111, 61, 0.18);
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(255, 252, 245, 0.68);
  color: #4f4035;
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.36);
}

.primary-cta {
  justify-self: center;
  width: 100%;
  max-width: 380px;
  min-height: 56px;
  border: 1px solid rgba(158, 79, 46, 0.42);
  border-radius: 8px;
  background: linear-gradient(135deg, var(--color-warm), var(--color-warm-deep));
  color: #fff;
  font-family: var(--font-ui);
  font-size: 21px;
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
  width: 8px;
  height: 8px;
  margin: 0 14px;
  background: rgba(255, 232, 192, 0.86);
  transform: rotate(45deg);
}

.actions {
  justify-content: center;
  width: 100%;
  margin-top: 0;
}

.primary-action-row {
  grid-area: action;
  align-self: end;
  display: grid;
  gap: 0;
  justify-items: center;
  margin-top: clamp(8px, 1.4svh, 14px);
  padding-top: 0;
  padding-bottom: clamp(18px, 2.8svh, 28px);
}

.action-divider {
  display: grid;
  grid-template-columns: minmax(86px, 1fr) auto minmax(86px, 1fr);
  gap: 12px;
  align-items: center;
  width: min(82vw, 340px);
  margin: 13px auto 0;
  opacity: 0.74;
}

.action-divider span {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 48, 45, 0.26), transparent);
}

.action-divider i {
  width: 5px;
  height: 5px;
  background: var(--color-warm);
  transform: rotate(45deg);
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
  max-width: 520px;
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
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 8px;
  width: min(100%, 360px);
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
  min-width: 0;
  padding-inline: 16px;
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

.hero-middle {
  grid-area: middle;
  position: relative;
  display: grid;
  align-content: start;
  justify-items: center;
  gap: clamp(12px, 2svh, 18px);
  width: 100%;
  min-height: 0;
  padding: clamp(12px, 2svh, 22px) 0 0;
}

.hero-preview {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  align-self: stretch;
  width: 100%;
  margin-top: 0;
}

.preview-card {
  position: relative;
  display: grid;
  align-content: start;
  gap: 18px;
  width: min(100%, 560px);
  min-height: 198px;
  overflow: hidden;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 16px 14px 14px;
  background:
    linear-gradient(180deg, rgba(255, 252, 245, 0.94), rgba(249, 241, 227, 0.84)),
    linear-gradient(90deg, rgba(201, 111, 61, 0.08), rgba(47, 98, 85, 0.08));
  box-shadow: 0 14px 30px rgba(49, 44, 35, 0.1);
  backdrop-filter: blur(8px);
  animation: previewRise 620ms 120ms ease both;
}

.preview-card::before {
  content: "";
  position: absolute;
  pointer-events: none;
}

.preview-card::before {
  inset: 10px;
  border: 1px solid rgba(37, 48, 45, 0.055);
  border-radius: 6px;
}

.preview-card > * {
  position: relative;
  z-index: 1;
}

.element-thread {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  padding: 0 6px;
}

.element-thread span {
  height: 3px;
  border-radius: 999px;
  background: var(--element-color);
  opacity: 0.54;
}

.element-preview-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0;
  align-items: stretch;
}

.element-column {
  position: relative;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 8px;
  min-width: 0;
  padding: 4px 6px;
}

.element-column + .element-column {
  border-left: 1px solid rgba(37, 48, 45, 0.12);
}

.element-keywords {
  display: grid;
  gap: 5px;
  color: var(--color-ink);
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.12;
  white-space: nowrap;
}

.element-column i {
  width: 7px;
  height: 7px;
  margin-top: 0;
  border-radius: 50%;
  background: var(--element-color);
}

.home-flow-bridge {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 8px;
  justify-items: center;
  width: min(100%, 430px);
  min-height: 124px;
  overflow: hidden;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 12px 14px 11px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.86), rgba(246, 238, 224, 0.72)),
    linear-gradient(90deg, rgba(201, 111, 61, 0.08), rgba(47, 98, 85, 0.07));
  box-shadow: 0 10px 24px rgba(49, 44, 35, 0.08);
}

.home-flow-bridge::before {
  content: "";
  position: absolute;
  inset: 8px;
  border: 1px solid rgba(37, 48, 45, 0.045);
  border-radius: 6px;
  pointer-events: none;
}

.bridge-heading {
  display: grid;
  grid-template-columns: minmax(32px, 1fr) auto minmax(32px, 1fr);
  gap: 10px;
  align-items: center;
  width: min(100%, 270px);
}

.bridge-heading span {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 48, 45, 0.22), transparent);
}

.bridge-heading b {
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  white-space: nowrap;
}

.home-flow-bridge p {
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  color: #41524d;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-line {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
  width: min(76%, 280px);
 }

.flow-line::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 48, 45, 0.24), transparent);
  transform: translateY(-50%);
}

.flow-line span {
  z-index: 1;
  justify-self: center;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--element-color);
  box-shadow: 0 0 0 4px rgba(255, 250, 240, 0.86);
}

.flow-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.flow-steps span {
  display: grid;
  gap: 2px;
  justify-items: center;
  min-width: 0;
  border: 1px solid rgba(37, 48, 45, 0.1);
  border-radius: 6px;
  padding: 7px 5px;
  background: rgba(255, 252, 245, 0.42);
}

.flow-steps b,
.flow-steps small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-steps b {
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 850;
}

.flow-steps small {
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 750;
}

.home-secondary {
  justify-items: center;
  gap: 12px;
  width: min(100%, 520px);
  margin: 0 auto;
  border-top: 1px solid rgba(37, 48, 45, 0.12);
  padding: 18px 0 0;
  text-align: center;
}

.secondary-copy {
  display: grid;
  justify-items: center;
  gap: 3px;
  width: min(100%, 360px);
}

.secondary-copy h2 {
  margin: 0;
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: 20px;
  line-height: 1.25;
  white-space: nowrap;
}

.secondary-kicker {
  margin: 0;
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 850;
}

.home-notice {
  max-width: 520px;
  margin: 18px 0 0;
  text-align: left;
}

@media (max-width: 820px) {
  .guide-page {
    padding: 24px 14px 82px;
  }

  .hero-shell {
    gap: 18px;
  }

  .hero-copy {
    min-height: min(740px, calc(100svh - 88px));
    padding-top: 58px;
  }

  .hero-title-group h1 {
    font-size: 52px;
  }

  .match-invite {
    grid-template-columns: 1fr;
  }

  .match-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .manual-match-entry {
    grid-template-columns: minmax(0, 1fr) 94px;
    width: min(100%, 350px);
  }

  .preview-card {
    width: min(100%, 620px);
    min-height: 238px;
    padding: 14px 10px 12px;
  }

  .element-keywords {
    gap: 4px;
    font-size: 17px;
  }

  .vertical-motto {
    left: 0;
    font-size: 14px;
  }
}

@media (max-width: 430px) {
  .guide-page {
    padding-top: 20px;
  }

  .hero-copy {
    grid-template-rows: auto auto auto auto;
    gap: 12px;
    min-height: min(704px, calc(100svh - 116px));
    padding-top: 52px;
  }

  .hero-middle {
    gap: 12px;
  }

  .hero-title-group h1 {
    font-size: 42px;
  }

  .title-rule {
    width: min(100%, 300px);
  }

  .lead {
    max-width: 100%;
    font-size: 15px;
    letter-spacing: 0;
    text-indent: 0;
  }

  .hero-metrics {
    gap: 9px;
  }

  .hero-metrics span {
    padding: 6px 10px;
    font-size: 12px;
  }

  .primary-cta {
    min-height: 52px;
    font-size: 19px;
  }

  .primary-cta::before,
  .primary-cta::after {
    margin: 0 10px;
  }

  .preview-card {
    min-height: 212px;
    padding: 14px 8px 10px;
  }

  .element-column {
    gap: 7px;
  }

  .element-keywords {
    font-size: 16px;
  }

  .home-secondary {
    padding-top: 14px;
  }

  .secondary-copy h2 {
    font-size: 18px;
  }

}

@media (max-width: 430px) and (max-height: 740px) {
  .hero-copy {
    gap: 10px;
    padding-top: 42px;
  }

  .hero-middle {
    gap: 8px;
    padding-top: 8px;
  }

  .vertical-motto {
    font-size: 13px;
  }

  .hero-title-group h1 {
    font-size: 38px;
  }

  .title-rule {
    width: min(100%, 270px);
  }

  .lead {
    max-width: 100%;
    font-size: 13px;
    line-height: 1.42;
  }

  .hero-metrics span {
    padding: 5px 9px;
    font-size: 11px;
  }

  .preview-card {
    min-height: 174px;
    gap: 12px;
    padding: 10px 8px 9px;
  }

  .element-thread {
    gap: 7px;
  }

  .element-column {
    gap: 5px;
    padding-inline: 4px;
  }

  .element-keywords {
    font-size: 12px;
  }

  .home-flow-bridge {
    min-height: 92px;
    gap: 7px;
    padding: 8px 10px;
  }

  .bridge-heading {
    width: min(100%, 230px);
  }

  .bridge-heading b,
  .home-flow-bridge p {
    font-size: 11px;
  }

  .flow-steps {
    gap: 6px;
  }

  .flow-steps span {
    padding: 5px 2px;
  }

  .flow-steps b {
    font-size: 12px;
  }

  .flow-steps small {
    font-size: 11px;
  }

  .primary-cta {
    min-height: 50px;
    font-size: 18px;
  }

  .primary-action-row {
    padding-bottom: 20px;
  }

  .action-divider {
    width: min(76vw, 280px);
    margin-top: 9px;
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
