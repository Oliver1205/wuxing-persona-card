<template>
  <main class="page guide-page">
    <section class="shell hero-shell">
      <div class="hero-copy stack">
        <p class="eyebrow">东方人格灵感测试</p>
        <h1>生成你的五行人格卡</h1>
        <p class="lead">
          90 秒完成出生年月和 5 道价值取向题，得到一个有身份名、关键词和分享链接的人格画像。
        </p>
        <div class="hero-metrics" aria-label="测试特点">
          <span>约 90 秒</span>
          <span>出生年月 + 5 题</span>
          <span>结果可分享</span>
        </div>
        <div class="persona-preview-line" aria-label="结果样式">
          <span>身份名</span>
          <span>五行比例</span>
          <span>人格关键词</span>
          <span>分享链接</span>
        </div>
        <div class="actions">
          <RouterLink class="button-link primary-cta" to="/test" @click="start">开始测试</RouterLink>
          <a class="button-link secondary" href="#preview">先看样例</a>
          <button
            type="button"
            class="secondary clipboard-check-button"
            :disabled="clipboardChecking"
            @click="detectClipboardShortCode(true)"
          >
            {{ clipboardChecking ? '检测中' : '检测剪贴板短码' }}
          </button>
        </div>
        <p v-if="clipboardMessage" class="clipboard-message">{{ clipboardMessage }}</p>
        <form class="manual-match-entry" aria-label="手动输入匹配短码" @submit.prevent="lookupManualShortCode">
          <input
            v-model="manualShortCode"
            data-testid="manual-match-code"
            maxlength="7"
            inputmode="text"
            autocomplete="off"
            placeholder="已有短码"
            aria-label="已有匹配短码"
          >
          <button type="submit" :disabled="manualChecking">
            {{ manualChecking ? '识别中' : '匹配' }}
          </button>
        </form>
        <section v-if="matchCandidate" class="match-invite" aria-label="双人匹配邀请">
          <div>
            <p class="match-kicker">检测到匹配短码</p>
            <h2>要和这张人格卡做双人匹配吗？</h2>
            <p>
              短码 {{ matchCandidate.shortCode }} · {{ matchCandidate.displayName }}
            </p>
          </div>
          <div class="match-actions">
            <button type="button" @click="startMatch">开始双人匹配</button>
            <button type="button" class="secondary" @click="dismissMatch">暂时不用</button>
          </div>
        </section>
        <p class="notice">
          本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
        </p>
      </div>

      <div id="preview" class="hero-preview" aria-label="五行人格卡样例">
        <div class="preview-card">
          <div class="preview-orbit" aria-hidden="true">
            <span>金</span>
            <span>木</span>
            <span>水</span>
            <span>火</span>
            <span>土</span>
          </div>
          <p class="preview-label">样例人格卡</p>
          <h2>金水观察者</h2>
          <p>清醒判断 · 细腻洞察 · 稳定边界</p>
          <div class="preview-tags" aria-label="样例关键词">
            <span>规则感</span>
            <span>观察力</span>
            <span>低消耗社交</span>
          </div>
          <div class="preview-ratio">
            <strong>68% 金</strong>
            <span></span>
            <strong>32% 水</strong>
          </div>
          <RouterLink class="preview-cta" to="/test" @click="start">生成我的人格卡</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchMatchCandidate } from '../api/matches';
import type { MatchCandidate } from '../api/types';
import { track } from '../utils/tracker';

const router = useRouter();
const matchCandidate = ref<MatchCandidate | null>(null);
const clipboardChecking = ref(false);
const manualChecking = ref(false);
const matchDismissed = ref(false);
const clipboardMessage = ref('');
const manualShortCode = ref('');
const shortCodePattern = /^[0-9a-zA-Z]{6,7}$/;

onMounted(() => {
  track('PAGE_VIEW_HOME', '/');
  void detectClipboardShortCode();
});

function start() {
  track('START_TEST_CLICK', '/');
}

async function detectClipboardShortCode(manual = false) {
  if (manual) {
    clipboardMessage.value = '';
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
  if (!manual && !(await canAutoReadClipboard())) {
    return;
  }
  clipboardChecking.value = true;
  try {
    const text = await navigator.clipboard.readText();
    const shortCode = normalizeClipboardShortCode(text);
    if (!shortCode) {
      if (manual) {
        clipboardMessage.value = '剪贴板里没有 6-7 位纯短码';
      }
      return;
    }
    await loadMatchCandidate(shortCode, 'MATCH_CLIPBOARD_DETECTED');
  } catch {
    matchCandidate.value = null;
    if (manual) {
      clipboardMessage.value = '没有识别到可用短码';
    }
  } finally {
    clipboardChecking.value = false;
  }
}

async function canAutoReadClipboard() {
  try {
    if (!navigator.permissions?.query) {
      return false;
    }
    const permission = await navigator.permissions.query({ name: 'clipboard-read' as PermissionName });
    return permission.state === 'granted';
  } catch {
    return false;
  }
}

function normalizeClipboardShortCode(value: string) {
  const trimmed = value.trim();
  return shortCodePattern.test(trimmed) ? trimmed : null;
}

async function lookupManualShortCode() {
  if (manualChecking.value) {
    return;
  }
  clipboardMessage.value = '';
  const shortCode = normalizeClipboardShortCode(manualShortCode.value);
  if (!shortCode) {
    clipboardMessage.value = '请输入 6-7 位纯短码';
    return;
  }
  manualChecking.value = true;
  try {
    await loadMatchCandidate(shortCode, 'MATCH_CLIPBOARD_DETECTED');
  } catch {
    clipboardMessage.value = '没有识别到可用短码';
  } finally {
    manualChecking.value = false;
  }
}

async function loadMatchCandidate(shortCode: string, eventType: string) {
  const candidate = await fetchMatchCandidate(shortCode);
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
  track('MATCH_MODE_ACCEPT', '/', candidate.resultId, candidate.shortCode);
  void router.push({
    path: '/test',
    query: {
      matchCode: candidate.shortCode,
      channel: 'match',
      campaign: 'clipboard-short-code',
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
  display: flex;
  align-items: center;
  background:
    linear-gradient(145deg, rgba(250, 248, 242, 0.94) 0%, rgba(237, 244, 239, 0.96) 48%, rgba(244, 234, 220, 0.9) 100%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180' viewBox='0 0 180 180'%3E%3Cg fill='none' stroke='%232f6f5e' stroke-opacity='.12'%3E%3Cpath d='M90 20c38.66 0 70 31.34 70 70s-31.34 70-70 70-70-31.34-70-70 31.34-70 70-70Z'/%3E%3Cpath d='M90 42c26.51 0 48 21.49 48 48s-21.49 48-48 48-48-21.49-48-48 21.49-48 48-48Z'/%3E%3Cpath d='M90 20v140M20 90h140M40.5 40.5l99 99M139.5 40.5l-99 99'/%3E%3C/g%3E%3C/svg%3E");
  background-size: auto, 180px 180px;
}

.hero-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(320px, 0.78fr);
  gap: 28px;
  align-items: center;
}

.lead {
  max-width: 620px;
  margin: 0;
  color: #40514e;
  font-size: 19px;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-metrics span {
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.72);
  color: #344542;
  font-size: 14px;
  font-weight: 800;
}

.persona-preview-line {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  max-width: 540px;
}

.persona-preview-line span {
  min-height: 42px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.66);
  color: #253634;
  font-size: 13px;
  font-weight: 850;
  text-align: center;
}

.primary-cta {
  min-width: 148px;
}

.clipboard-check-button {
  min-width: 148px;
}

.clipboard-message {
  max-width: 620px;
  margin: -2px 0 0;
  color: #697674;
  font-size: 13px;
  font-weight: 800;
}

.manual-match-entry {
  display: grid;
  grid-template-columns: minmax(0, 180px) auto;
  gap: 8px;
  max-width: 350px;
}

.manual-match-entry input {
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.14);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.88);
  color: #24302f;
  font: inherit;
  font-weight: 850;
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
  border: 1px solid rgba(47, 111, 94, 0.2);
  border-radius: 8px;
  padding: 14px;
  background:
    linear-gradient(135deg, rgba(237, 247, 242, 0.96), rgba(255, 249, 238, 0.94));
  box-shadow: 0 12px 28px rgba(31, 48, 43, 0.08);
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
  color: #50615f;
  font-size: 14px;
  font-weight: 800;
}

.match-kicker {
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 950;
}

.match-actions {
  display: flex;
  gap: 8px;
}

.match-actions button {
  min-height: 42px;
  white-space: nowrap;
}

.hero-preview {
  display: grid;
  place-items: center;
}

.preview-card {
  position: relative;
  width: min(100%, 380px);
  min-height: 470px;
  overflow: hidden;
  border: 1px solid rgba(36, 48, 47, 0.14);
  border-radius: 8px;
  padding: 26px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.95), rgba(232, 243, 239, 0.86)),
    linear-gradient(180deg, rgba(47, 111, 94, 0.1), transparent);
  box-shadow: 0 24px 70px rgba(31, 48, 43, 0.16);
}

.preview-orbit {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 7px;
  margin-bottom: 84px;
}

.preview-orbit span {
  display: grid;
  place-items: center;
  aspect-ratio: 1;
  border-radius: 50%;
  background: #24302f;
  color: #fff;
  font-weight: 900;
}

.preview-orbit span:nth-child(2) {
  background: #5e8d63;
}

.preview-orbit span:nth-child(3) {
  background: #486f92;
}

.preview-orbit span:nth-child(4) {
  background: #b66045;
}

.preview-orbit span:nth-child(5) {
  background: #9d7a42;
}

.preview-label {
  margin: 0 0 8px;
  color: #7b5d35;
  font-weight: 900;
}

.preview-card h2 {
  margin: 0 0 10px;
  font-size: 34px;
}

.preview-card p {
  color: #50615f;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
}

.preview-tags span {
  border-radius: 999px;
  padding: 8px 10px;
  background: #24302f;
  color: #fff;
  font-size: 13px;
  font-weight: 850;
}

.preview-ratio {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  margin-top: 48px;
  color: #263735;
}

.preview-ratio span {
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f6f5e 0 68%, #486f92 68% 100%);
}

.preview-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 48px;
  margin-top: 26px;
  border-radius: 8px;
  background: #2f6f5e;
  color: #fff;
  font-weight: 900;
  text-decoration: none;
  box-shadow: 0 12px 28px rgba(47, 111, 94, 0.22);
}

.preview-cta:hover {
  background: #25594c;
}

@media (max-width: 820px) {
  .guide-page {
    align-items: start;
  }

  .hero-shell {
    grid-template-columns: 1fr;
  }

  .persona-preview-line {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
    min-height: 360px;
  }

  .preview-orbit {
    margin-bottom: 42px;
  }
}
</style>
