<script setup lang="ts">
import { elementVisualByCode } from '../utils/elementVisuals';

const props = defineProps<{
  primaryName: string;
  primaryElement: string;
  primaryPercent: number;
  secondaryName: string;
  secondaryElement: string;
  secondaryPercent: number;
}>();

function barStyle(element: string, percent: number) {
  return {
    width: `${percent}%`,
    backgroundColor: elementVisualByCode(element).color,
  };
}
</script>

<template>
  <section class="ratio-card">
    <div class="ratio-head">
      <span>{{ primaryName }} {{ primaryPercent }}%</span>
      <span>{{ secondaryName }} {{ secondaryPercent }}%</span>
    </div>
    <div class="bar" aria-hidden="true">
      <div class="primary" :style="barStyle(props.primaryElement, primaryPercent)"></div>
      <div class="secondary" :style="barStyle(props.secondaryElement, secondaryPercent)"></div>
    </div>
  </section>
</template>

<style scoped>
.ratio-card {
  display: grid;
  gap: 10px;
}

.ratio-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #253634;
  font-weight: 800;
}

.bar {
  display: flex;
  width: 100%;
  height: 14px;
  overflow: hidden;
  border-radius: 8px;
  background: #e8e1d2;
}

.secondary {
  opacity: 0.9;
}
</style>
