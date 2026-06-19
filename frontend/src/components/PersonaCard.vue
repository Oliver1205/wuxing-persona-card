<script setup lang="ts">
import type { ResultDetail } from '../api/types';
import ElementRatioCard from './ElementRatioCard.vue';
import ElementMark from './ElementMark.vue';

defineProps<{
  result: ResultDetail;
}>();
</script>

<template>
  <section class="persona-card">
    <div class="card-visual" :data-element="result.primaryElement">
      <div class="persona-mark-pair" aria-hidden="true">
        <ElementMark :code="result.primaryElement" :name="result.primaryElementName" />
        <ElementMark :code="result.secondaryElement" :name="result.secondaryElementName" compact />
      </div>
      <div>
        <p class="eyebrow">你的五行人格身份 · {{ result.starOfficerName }}</p>
        <h1>{{ result.primaryElementName }}{{ result.secondaryElementName }} · {{ result.starOfficerName }}</h1>
        <p class="identity-line">{{ result.keywords.slice(0, 3).join(' · ') }}</p>
      </div>
    </div>

    <ElementRatioCard
      :primary-name="result.primaryElementName"
      :primary-element="result.primaryElement"
      :primary-percent="result.primaryPercent"
      :secondary-name="result.secondaryElementName"
      :secondary-element="result.secondaryElement"
      :secondary-percent="result.secondaryPercent"
    />

    <div class="keywords">
      <span v-for="keyword in result.keywords" :key="keyword">{{ keyword }}</span>
    </div>

    <p class="notice">
      本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
    </p>
  </section>
</template>

<style scoped>
.persona-card {
  display: grid;
  gap: 18px;
}

.card-visual {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  min-height: 132px;
  border-radius: 8px;
  padding: 18px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 243, 233, 0.84)),
    linear-gradient(90deg, rgba(177, 211, 209, 0.18), transparent);
  color: #24302f;
}

.persona-mark-pair {
  display: grid;
  grid-template-columns: auto auto;
  gap: 6px;
  align-items: end;
}

h1 {
  margin: 0;
  color: #202725;
  font-family: "Songti SC", "STSong", "Noto Serif SC", var(--font-display);
  font-size: 28px;
  font-weight: 650;
  line-height: 1.22;
}

.identity-line {
  margin: 8px 0 0;
  color: rgba(36, 48, 47, 0.76);
  font-weight: 800;
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keywords span {
  border-radius: 999px;
  padding: 7px 10px;
  background: #f1eadc;
  color: #6d4f29;
  font-weight: 700;
}

@media (max-width: 640px) {
  .card-visual {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 10px;
    min-height: 0;
    align-items: start;
    padding: 14px;
  }

  .persona-mark-pair {
    justify-content: start;
  }

  .persona-mark-pair :deep(.element-mark) {
    min-width: 32px;
  }

  .persona-mark-pair :deep(.element-mark.compact) {
    min-width: 26px;
  }

  .persona-mark-pair :deep(.element-mark > span) {
    font-size: 24px;
  }

  .persona-mark-pair :deep(.element-mark.compact > span) {
    font-size: 20px;
  }

  .card-visual .eyebrow {
    font-size: 12px;
    line-height: 1.35;
  }

  h1 {
    font-size: 22px;
    line-height: 1.26;
  }

  .identity-line {
    font-size: 14px;
  }
}
</style>
