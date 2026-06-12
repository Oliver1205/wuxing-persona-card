<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchMatch } from '../api/matches';
import type { MatchResult, ResultDetail } from '../api/types';
import { track } from '../utils/tracker';

const route = useRoute();
const match = ref<MatchResult | null>(null);
const loading = ref(true);
const error = ref('');

const scoreStyle = computed(() => ({
  width: `${match.value?.compatibilityScore ?? 0}%`,
}));

const people = computed(() => {
  if (!match.value) {
    return [];
  }
  return [
    { label: '我', result: match.value.currentResult },
    { label: 'TA', result: match.value.partnerResult },
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
</script>

<template>
  <main class="page match-page">
    <section class="shell stack">
      <section v-if="loading" class="match-state-card" aria-live="polite">
        <p class="eyebrow">正在合盘你们的五行节奏</p>
        <h1>匹配结果马上就好</h1>
        <p class="muted">正在读取两张人格卡，计算五行分布和关系节奏。</p>
      </section>

      <section v-else-if="error" class="match-state-card error-state">
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
            <h1>{{ match.headline }}</h1>
            <p>{{ match.summary }}</p>
          </div>
          <div class="score-panel" aria-label="匹配分数">
            <span>{{ match.relationLabel }}</span>
            <strong>{{ match.compatibilityScore }}</strong>
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
            <span>{{ person.label }}</span>
            <h2>{{ identityName(person.result) }}</h2>
            <p>{{ person.result.keywords.slice(0, 3).join(' · ') }}</p>
            <div class="person-ratio">
              <strong>{{ person.result.primaryPercent }}% {{ person.result.primaryElementName }}</strong>
              <em></em>
              <strong>{{ person.result.secondaryPercent }}% {{ person.result.secondaryElementName }}</strong>
            </div>
          </article>
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

        <section class="panel stack match-insights">
          <div>
            <p class="eyebrow">沟通建议</p>
            <h2>把互补变成协作的小提醒</h2>
          </div>
          <div class="insight-grid">
            <article v-for="item in match.suggestions" :key="item">
              <span>建议</span>
              <p>{{ item }}</p>
            </article>
          </div>
        </section>

        <section class="match-next-step">
          <div>
            <strong>想换一个短码继续匹配？</strong>
            <span>回到首页后，复制新的纯短码，再打开首页即可触发匹配邀请。</span>
          </div>
          <RouterLink class="button-link" to="/">回到首页</RouterLink>
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
  font-size: 42px;
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
  color: #24302f;
  font-size: 48px;
  line-height: 1;
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

.person-card {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 18px;
  background: #fff;
}

.person-card span {
  width: fit-content;
  border-radius: 999px;
  padding: 6px 10px;
  background: #24302f;
  color: #fff;
  font-size: 12px;
  font-weight: 950;
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

.match-next-step {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
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

@media (max-width: 760px) {
  .match-hero,
  .person-compare-grid,
  .match-next-step {
    grid-template-columns: 1fr;
  }

  .match-hero h1,
  .match-state-card h1 {
    font-size: 30px;
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }

  .person-ratio {
    grid-template-columns: 1fr;
  }
}
</style>
