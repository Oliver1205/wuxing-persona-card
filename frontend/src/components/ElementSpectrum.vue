<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  scores: Record<string, number>;
}>();

const elementNames: Record<string, string> = {
  METAL: '金',
  WOOD: '木',
  WATER: '水',
  FIRE: '火',
  EARTH: '土',
};

const rows = computed(() => {
  const total = Object.values(props.scores).reduce((sum, score) => sum + score, 0);
  return Object.entries(props.scores)
    .map(([code, score]) => ({
      code,
      name: elementNames[code] ?? code,
      score,
      percent: total > 0 ? Math.round((score / total) * 100) : 0,
    }))
    .sort((left, right) => right.score - left.score);
});
</script>

<template>
  <section class="element-spectrum" aria-label="完整五行分布">
    <div class="spectrum-head">
      <h2>完整五行分布</h2>
      <p>主副元素用于展示，完整分数用于解释你的整体倾向。</p>
    </div>
    <div class="spectrum-list">
      <div v-for="item in rows" :key="item.code" class="spectrum-row" :data-element="item.code">
        <div class="row-label">
          <strong>{{ item.name }}</strong>
          <span>{{ item.percent }}%</span>
        </div>
        <div class="row-track">
          <span :style="{ width: `${item.percent}%` }"></span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.element-spectrum {
  display: grid;
  gap: 16px;
}

.spectrum-head {
  display: grid;
  gap: 4px;
}

.spectrum-head h2,
.spectrum-head p {
  margin: 0;
}

.spectrum-head p {
  color: #697674;
}

.spectrum-list {
  display: grid;
  gap: 12px;
}

.spectrum-row {
  display: grid;
  gap: 7px;
}

.row-label {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #263735;
}

.row-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.1);
}

.row-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2f6f5e;
}

.spectrum-row[data-element="METAL"] .row-track span {
  background: #5c6670;
}

.spectrum-row[data-element="WOOD"] .row-track span {
  background: #5e8d63;
}

.spectrum-row[data-element="WATER"] .row-track span {
  background: #486f92;
}

.spectrum-row[data-element="FIRE"] .row-track span {
  background: #b66045;
}

.spectrum-row[data-element="EARTH"] .row-track span {
  background: #9d7a42;
}
</style>
