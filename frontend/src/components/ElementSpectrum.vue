<script setup lang="ts">
import { computed } from 'vue';
import ElementMark from './ElementMark.vue';
import { elementVisualByCode } from '../utils/elementVisuals';

const props = defineProps<{
  scores: Record<string, number>;
}>();

const rows = computed(() => {
  const total = Object.values(props.scores).reduce((sum, score) => sum + score, 0);
  return Object.entries(props.scores)
    .map(([code, score]) => {
      const visual = elementVisualByCode(code);
      return {
        code,
        name: visual.name,
        keywords: visual.keywords,
        color: visual.color,
        soft: visual.soft,
        score,
        percent: total > 0 ? Math.round((score / total) * 100) : 0,
      };
    })
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
      <div
        v-for="item in rows"
        :key="item.code"
        class="spectrum-row"
        :style="{ '--element-color': item.color, '--element-soft': item.soft }"
      >
        <ElementMark :code="item.code" :name="item.name" compact size="legend" />
        <div class="row-label">
          <strong>{{ item.keywords.join(' / ') }}</strong>
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
  border-radius: 8px;
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

.spectrum-head h2 {
  color: #202725;
  font-family: "Songti SC", "STSong", "Noto Serif SC", var(--font-display);
  font-weight: 650;
}

.spectrum-list {
  display: grid;
  gap: 12px;
}

.spectrum-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 7px;
  align-items: center;
  border: 1px solid color-mix(in srgb, var(--element-color), transparent 82%);
  border-radius: 8px;
  padding: 9px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.76), var(--element-soft));
}

.row-label {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #263735;
}

.row-label,
.row-track {
  grid-column: 2;
}

.row-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.08);
}

.row-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--element-color);
}

@media (max-width: 460px) {
  .spectrum-row {
    grid-template-columns: 1fr;
  }

  .row-label,
  .row-track {
    grid-column: auto;
  }
}
</style>
