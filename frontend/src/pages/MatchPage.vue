<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchMatch } from '../api/matches';
import type { MatchResult, ResultDetail } from '../api/types';
import ElementMark from '../components/ElementMark.vue';
import { track } from '../utils/tracker';

const route = useRoute();
const router = useRouter();
const match = ref<MatchResult | null>(null);
const loading = ref(true);
const error = ref('');
const nextShortCode = ref('');
const nextShortCodeMessage = ref('');
const shortCodePattern = /^[0-9a-zA-Z]{6,7}$/;

const scoreStyle = computed(() => ({
  width: `${match.value?.compatibilityScore ?? 0}%`,
}));
const matchHeadline = computed(() => normalizeHeadline(match.value?.headline ?? ''));

const people = computed(() => {
  if (!match.value) {
    return [];
  }
  return [
    { label: '我', result: match.value.currentResult },
    { label: 'TA', result: match.value.partnerResult },
  ];
});

const elementRoleMap: Record<string, string> = {
  METAL: '边界、判断和节奏收束',
  WOOD: '成长、推进和长期规划',
  WATER: '感受、缓冲和信息流动',
  FIRE: '表达、热度和行动启动',
  EARTH: '承接、稳定和关系安全感',
};

const relationReference = computed(() => {
  if (!match.value) {
    return [];
  }
  const current = match.value.currentResult;
  const partner = match.value.partnerResult;
  return [
    {
      label: '你的节奏',
      text: `${identityName(current)}更容易贡献${elementRole(current)}。`,
    },
    {
      label: 'TA 的节奏',
      text: `${identityName(partner)}更容易带来${elementRole(partner)}。`,
    },
    {
      label: '相处重点',
      text: `${match.value.relationLabel}的关键，是先确认目标和边界，再把各自擅长的节奏接起来。`,
    },
  ];
});

onMounted(async () => {
  const partnerShortCode = routeParam(route.params.partnerShortCode);
  const currentShortCode = routeParam(route.params.currentShortCode);
  try {
    match.value = await fetchMatch(partnerShortCode, currentShortCode);
    track('MATCH_RESULT_VIEW', `/match/${partnerShortCode}/${currentShortCode}`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '匹配结果加载失败';
  } finally {
    loading.value = false;
  }
});

function routeParam(value: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

function identityName(result: ResultDetail) {
  return `${result.primaryElementName}${result.secondaryElementName}型${result.keywords[0] ?? '探索者'}`;
}

function elementRole(result: ResultDetail) {
  return elementRoleMap[result.primaryElement] ?? result.keywords.slice(0, 2).join('和');
}

function normalizeHeadline(value: string) {
  return value
    .replace(/[\u00a0\u3000]/g, ' ')
    .replace(/\s*与\s*/g, '与')
    .replace(/([\u3400-\u9fff])\s+([\u3400-\u9fff])/g, '$1$2')
    .replace(/\s{2,}/g, ' ')
    .trim();
}

function normalizeShortCode(value: string) {
  const trimmed = value.trim();
  return shortCodePattern.test(trimmed) ? trimmed : '';
}

function openNextMatch() {
  const code = normalizeShortCode(nextShortCode.value);
  nextShortCodeMessage.value = '';
  if (!code) {
    nextShortCodeMessage.value = '请输入 6 到 7 位短码';
    return;
  }
  const currentShortCode = routeParam(route.params.currentShortCode);
  if (code.toLowerCase() === currentShortCode.toLowerCase()) {
    nextShortCodeMessage.value = '请输入朋友的新短码';
    return;
  }
  void router.push(`/match/${encodeURIComponent(code)}/${encodeURIComponent(currentShortCode)}`);
}
</script>

<template>
  <main class="page match-page">
    <section class="shell stack">
      <section v-if="loading" class="match-state-card" role="status" aria-live="polite">
        <p class="eyebrow">正在合盘你们的五行节奏</p>
        <h1>匹配结果马上就好</h1>
        <p class="muted">正在读取两张人格卡，计算五行分布和关系节奏。</p>
      </section>

      <section v-else-if="error" class="match-state-card error-state" role="alert" aria-live="polite">
        <p class="eyebrow">这份匹配暂时打不开</p>
        <h1>短码可能失效，或其中一张人格卡不存在</h1>
        <p class="muted">{{ error }}</p>
        <div class="actions">
          <RouterLink class="button-link" to="/">返回首页</RouterLink>
          <RouterLink class="button-link secondary" to="/test">重新测一张</RouterLink>
        </div>
      </section>

      <template v-else-if="match">
        <section class="match-hero">
          <div>
            <p class="eyebrow">双人五行匹配</p>
            <h1>{{ matchHeadline }}</h1>
            <p>{{ match.summary }}</p>
          </div>
          <div class="score-panel" aria-label="匹配分数">
            <span>{{ match.relationLabel }}</span>
            <strong>{{ match.compatibilityScore }}<small>%</small></strong>
            <div class="score-track"><i :style="scoreStyle"></i></div>
          </div>
        </section>

        <section class="person-compare-grid" aria-label="双方人格卡摘要">
          <article
            v-for="person in people"
            :key="person.label"
            class="person-card"
            :data-element="person.result.primaryElement"
          >
            <span class="person-label">{{ person.label }}</span>
            <div class="person-mark-row" aria-hidden="true">
              <ElementMark :code="person.result.primaryElement" :name="person.result.primaryElementName" />
              <ElementMark :code="person.result.secondaryElement" :name="person.result.secondaryElementName" compact />
            </div>
            <h2>{{ identityName(person.result) }}</h2>
            <p>{{ person.result.keywords.slice(0, 3).join(' · ') }}</p>
            <div class="person-ratio">
              <strong>{{ person.result.primaryPercent }}% {{ person.result.primaryElementName }}</strong>
              <em></em>
              <strong>{{ person.result.secondaryPercent }}% {{ person.result.secondaryElementName }}</strong>
            </div>
          </article>
        </section>

        <section class="match-reference" aria-label="双人关系参照">
          <div class="match-reference-head">
            <span>关系参照</span>
            <strong>把五行差异翻译成相处节奏</strong>
          </div>
          <div class="reference-grid">
            <article v-for="item in relationReference" :key="item.label">
              <span>{{ item.label }}</span>
              <p>{{ item.text }}</p>
            </article>
          </div>
        </section>

        <section class="panel stack match-insights">
          <div>
            <p class="eyebrow">{{ match.relationLabel }}</p>
            <h2>你们容易形成的相处优势</h2>
          </div>
          <div class="insight-grid">
            <article v-for="item in match.strengths" :key="item">
              <span>优势</span>
              <p>{{ item }}</p>
            </article>
          </div>
        </section>

        <section class="panel stack match-insights suggestion-panel">
          <div>
            <p class="eyebrow">沟通建议</p>
            <h2>把互补变成协作的小提醒</h2>
          </div>
          <div class="suggestion-list">
            <article v-for="(item, index) in match.suggestions" :key="item" class="suggestion-step">
              <span>0{{ index + 1 }}</span>
              <p>{{ item }}</p>
            </article>
          </div>
        </section>

        <section class="match-next-step">
          <div>
            <strong>想换一个短码继续匹配？</strong>
            <span>输入朋友的新短码，直接用你的这张卡重新计算关系节奏。</span>
          </div>
          <form class="match-next-form" aria-label="继续匹配新的短码" @submit.prevent="openNextMatch">
            <input
              v-model="nextShortCode"
              data-testid="match-next-code"
              inputmode="text"
              maxlength="7"
              placeholder="输入短码"
              aria-label="新的匹配短码"
              :aria-invalid="Boolean(nextShortCodeMessage)"
            >
            <button data-testid="match-next-submit" type="submit">继续匹配</button>
            <RouterLink class="button-link secondary" to="/">回首页</RouterLink>
          </form>
          <p v-if="nextShortCodeMessage" class="match-next-message" role="status" aria-live="polite">{{ nextShortCodeMessage }}</p>
        </section>
      </template>
    </section>
  </main>
</template>

<style scoped>
.match-page {
  background:
    linear-gradient(180deg, #f8f5eb 0%, #eef5f0 45%, #f7efe9 100%);
}

.match-state-card,
.match-hero {
  display: grid;
  gap: 16px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(236, 244, 239, 0.94)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.08), rgba(196, 122, 80, 0.08));
  box-shadow: 0 18px 48px rgba(31, 48, 43, 0.1);
}

.match-hero {
  grid-template-columns: minmax(0, 1fr) 190px;
  align-items: center;
}

.match-hero h1,
.match-state-card h1 {
  max-width: 760px;
  margin: 0;
  font-size: 40px;
  text-wrap: balance;
}

.match-hero p,
.match-state-card p {
  margin: 0;
  color: #40514e;
  font-size: 17px;
}

.score-panel {
  display: grid;
  gap: 8px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.74);
}

.score-panel span {
  color: #2f6f5e;
  font-size: 13px;
  font-weight: 950;
}

.score-panel strong {
  display: flex;
  align-items: baseline;
  gap: 3px;
  color: #24302f;
  font-size: 48px;
  line-height: 1;
}

.score-panel small {
  color: #596764;
  font-size: 20px;
  font-weight: 900;
}

.score-track {
  overflow: hidden;
  height: 10px;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.1);
}

.score-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2f6f5e, #d79b43, #b85b48);
}

.person-compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.match-reference {
  display: grid;
  gap: 10px;
  margin-top: -2px;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.62);
}

.match-reference-head {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: baseline;
}

.match-reference-head span {
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.match-reference-head strong {
  color: #263735;
  font-size: 15px;
  font-weight: 900;
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.reference-grid article {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.74);
}

.reference-grid span {
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 950;
}

.reference-grid p {
  margin: 0;
  color: #40514e;
  font-size: 13px;
  font-weight: 780;
  line-height: 1.58;
}

.person-card {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 18px;
  background: #fff;
}

.person-label {
  width: fit-content;
  border-radius: 999px;
  padding: 6px 10px;
  background: #24302f;
  color: #fff;
  font-size: 12px;
  font-weight: 950;
}

.person-mark-row {
  display: flex;
  align-items: end;
  gap: 4px;
}

.person-card h2 {
  margin: 0;
  font-size: 26px;
}

.person-card p {
  margin: 0;
  color: #596764;
  font-weight: 800;
}

.person-ratio {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  color: #263735;
  font-size: 13px;
}

.person-ratio em {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f6f5e, #486f92);
}

.person-card[data-element="METAL"] {
  background: linear-gradient(135deg, #fff, #eef1ef);
}

.person-card[data-element="WOOD"] {
  background: linear-gradient(135deg, #fff, #eef7ec);
}

.person-card[data-element="WATER"] {
  background: linear-gradient(135deg, #fff, #edf4fa);
}

.person-card[data-element="FIRE"] {
  background: linear-gradient(135deg, #fff, #fff0e8);
}

.person-card[data-element="EARTH"] {
  background: linear-gradient(135deg, #fff, #f6efe2);
}

.match-insights h2 {
  margin: 0;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.insight-grid article {
  display: grid;
  gap: 8px;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.7);
}

.insight-grid span {
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.insight-grid p {
  margin: 0;
  color: #40514e;
  font-weight: 800;
  line-height: 1.7;
}

.suggestion-panel {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(250, 252, 249, 0.9)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.05), rgba(215, 155, 67, 0.05));
}

.suggestion-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.suggestion-step {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.68);
}

.suggestion-step span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: rgba(47, 111, 94, 0.1);
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 950;
}

.suggestion-step p {
  margin: 0;
  color: #40514e;
  font-weight: 820;
  line-height: 1.62;
}

.match-next-step {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(330px, auto);
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

.match-next-step strong,
.match-next-step span {
  display: block;
}

.match-next-step span {
  margin-top: 4px;
  color: #697674;
  font-size: 13px;
}

.match-next-form {
  display: grid;
  grid-template-columns: minmax(110px, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}

.match-next-form input {
  width: 100%;
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.14);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.94);
  color: #24302f;
  font: inherit;
  font-size: 14px;
  font-weight: 850;
  text-transform: uppercase;
}

.match-next-form input[aria-invalid="true"] {
  border-color: rgba(184, 91, 72, 0.7);
}

.match-next-form button,
.match-next-form .button-link {
  min-width: 92px;
  padding-inline: 12px;
  white-space: nowrap;
}

.match-next-message {
  grid-column: 1 / -1;
  margin: -4px 0 0;
  color: #b85b48;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 760px) {
  .match-hero,
  .person-compare-grid,
  .match-next-step {
    grid-template-columns: 1fr;
  }

  .match-hero h1,
  .match-state-card h1 {
    font-size: 28px;
    line-height: 1.18;
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }

  .reference-grid {
    grid-template-columns: 1fr;
  }

  .suggestion-list {
    grid-template-columns: 1fr;
  }

  .suggestion-step {
    padding: 12px;
  }

  .match-next-form {
    grid-template-columns: 1fr;
  }

  .person-ratio {
    grid-template-columns: auto minmax(42px, 1fr) auto;
    gap: 8px;
    font-size: 12px;
  }
}
</style>
