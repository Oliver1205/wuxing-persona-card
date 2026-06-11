<script setup lang="ts">
import type { ResultDetail } from '../api/types';
import ElementRatioCard from './ElementRatioCard.vue';

defineProps<{
  result: ResultDetail;
}>();
</script>

<template>
  <section class="persona-card">
    <div class="card-visual" :data-element="result.primaryElement">
      <div class="symbol">{{ result.primaryElementName }}</div>
      <div>
        <p class="eyebrow">{{ result.starOfficerName }}</p>
        <h2>{{ result.primaryElementName }}{{ result.secondaryElementName }}人格卡</h2>
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
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 16px;
  align-items: center;
  min-height: 150px;
  border-radius: 8px;
  padding: 18px;
  background: linear-gradient(135deg, #f8f5eb, #e6f2ee);
  color: #24302f;
}

.symbol {
  display: grid;
  place-items: center;
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: #2f6f5e;
  color: #fff;
  font-size: 34px;
  font-weight: 900;
}

h2 {
  margin: 0;
  font-size: 25px;
}

.identity-line {
  margin: 8px 0 0;
  color: rgba(36, 48, 47, 0.76);
  font-weight: 800;
}

.card-visual[data-element="METAL"] {
  background: linear-gradient(135deg, #f7f4e8, #dfe5e2);
}

.card-visual[data-element="WOOD"] {
  background: linear-gradient(135deg, #eef6e8, #dcebd9);
}

.card-visual[data-element="WATER"] {
  background: linear-gradient(135deg, #e9f2f8, #dce8f1);
}

.card-visual[data-element="FIRE"] {
  background: linear-gradient(135deg, #fff1e8, #f5d9cc);
}

.card-visual[data-element="EARTH"] {
  background: linear-gradient(135deg, #f7efe1, #eadfc7);
}

.card-visual[data-element="METAL"] .symbol {
  background: #5c6670;
}

.card-visual[data-element="WOOD"] .symbol {
  background: #5e8d63;
}

.card-visual[data-element="WATER"] .symbol {
  background: #486f92;
}

.card-visual[data-element="FIRE"] .symbol {
  background: #b66045;
}

.card-visual[data-element="EARTH"] .symbol {
  background: #9d7a42;
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
</style>
