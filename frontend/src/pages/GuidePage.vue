<template>
  <main class="page guide-page">
    <section class="shell hero-shell">
      <div class="hero-copy stack">
        <p class="eyebrow">东方人格灵感测试</p>
        <h1>生成你的五行人格卡</h1>
        <p class="lead">
          90 秒完成 5 道价值取向题，得到一个有身份名、关键词和分享短链的人格画像。
        </p>
        <div class="hero-metrics" aria-label="测试特点">
          <span>约 90 秒</span>
          <span>5 道题</span>
          <span>结果可分享</span>
        </div>
        <div class="persona-preview-line" aria-label="结果样式">
          <span>身份名</span>
          <span>五行比例</span>
          <span>人格关键词</span>
          <span>专属短链</span>
        </div>
        <div class="actions">
          <RouterLink class="button-link primary-cta" to="/test" @click="start">开始测试</RouterLink>
          <a class="button-link secondary" href="#preview">先看样例</a>
        </div>
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
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { track } from '../utils/tracker';

onMounted(() => track('PAGE_VIEW_HOME', '/'));

function start() {
  track('START_TEST_CLICK', '/');
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

  .preview-card {
    min-height: 360px;
  }

  .preview-orbit {
    margin-bottom: 42px;
  }
}
</style>
