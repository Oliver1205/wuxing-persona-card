<script setup lang="ts">
import { computed } from 'vue';
import { elementVisualByCode } from '../utils/elementVisuals';

const props = withDefaults(defineProps<{
  code: string;
  name?: string;
  compact?: boolean;
  size?: 'hero' | 'card' | 'legend';
}>(), {
  name: '',
  compact: false,
  size: 'card',
});

const visual = computed(() => elementVisualByCode(props.code, props.name));
const elementName = computed(() => props.name || visual.value.name);
</script>

<template>
  <span
    class="element-mark"
    :class="[`size-${size}`, { compact }]"
    :style="{ '--element-color': visual.color, '--element-soft': visual.soft }"
    :aria-label="elementName"
  >
    <span>{{ elementName }}</span>
  </span>
</template>

<style scoped>
.element-mark {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  color: var(--element-color);
}

.element-mark.compact {
  min-width: 30px;
}

.element-mark.size-legend {
  min-width: 26px;
}

.element-mark.size-hero {
  min-width: 48px;
}

.element-mark > span {
  position: relative;
  color: currentColor;
  font-family: "Songti SC", "STSong", "Noto Serif SC", var(--font-display);
  font-size: 28px;
  font-weight: 600;
  line-height: 1;
}

.element-mark.compact > span {
  font-size: 22px;
}

.element-mark.size-legend > span {
  font-size: 18px;
}

.element-mark.size-hero > span {
  font-size: 34px;
}
</style>
