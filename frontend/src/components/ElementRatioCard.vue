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
  color: var(--color-ink);
  font-weight: 800;
}

.bar {
  display: flex;
  width: 100%;
  height: 16px;
  overflow: hidden;
  border-radius: 8px;
  background: #eadbc5;
  box-shadow: inset 0 0 0 1px rgba(37, 48, 45, 0.08);
}

.secondary {
  opacity: 0.9;
}

@media (max-width: 640px) {
  .ratio-card {
    gap: 7px;
  }

  .ratio-head {
    font-size: 12px;
  }

  .bar {
    height: 10px;
  }
}
</style>
