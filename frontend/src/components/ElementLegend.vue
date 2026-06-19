<script setup lang="ts">
import ElementMark from './ElementMark.vue';
import { elementVisuals } from '../utils/elementVisuals';

withDefaults(defineProps<{
  compact?: boolean;
  showTitle?: boolean;
}>(), {
  compact: false,
  showTitle: true,
});
</script>

<template>
  <section class="element-legend" :class="{ compact }" aria-label="五行图例">
    <div v-if="showTitle" class="legend-head">
      <span>五行图例</span>
      <strong>每个选项都会落到一个元素倾向</strong>
    </div>
    <div class="legend-grid">
      <article
        v-for="item in elementVisuals"
        :key="item.code"
        class="legend-item"
        :style="{ '--element-color': item.color, '--element-soft': item.soft }"
      >
        <ElementMark :code="item.code" :name="item.name" compact size="legend" />
        <div class="legend-copy">
          <span>{{ item.keywords.join(' / ') }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.element-legend {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 8px;
  padding: 14px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.84), rgba(249, 245, 236, 0.78));
}

.legend-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.legend-head span {
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.legend-head strong {
  color: #40514e;
  font-size: 13px;
  font-weight: 850;
  text-align: right;
}

.legend-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.legend-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 58px;
  border: 1px solid color-mix(in srgb, var(--element-color), transparent 76%);
  border-radius: 8px;
  padding: 8px;
  background: var(--element-soft);
}

.legend-copy span {
  display: block;
}

.legend-copy span {
  color: #596764;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
}

.compact {
  gap: 8px;
  padding: 10px;
}

.compact .legend-head strong {
  display: none;
}

.compact .legend-grid {
  grid-template-columns: repeat(5, minmax(44px, 1fr));
  gap: 6px;
}

.compact .legend-item {
  grid-template-columns: 1fr;
  justify-items: center;
  min-height: 48px;
  padding: 7px 4px;
  text-align: center;
}

@media (max-width: 720px) {
  .legend-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .compact {
    border-color: rgba(36, 48, 47, 0.08);
    background: rgba(255, 255, 255, 0.58);
  }

  .compact .legend-head {
    display: none;
  }

  .compact .legend-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .compact .legend-item {
    min-height: 40px;
    border-color: transparent;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.66);
  }

  .compact .legend-copy {
    display: none;
  }

  .compact .legend-item:last-child {
    grid-column: auto;
  }
}

@media (max-width: 420px) {
  .legend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .legend-head {
    display: grid;
  }

  .legend-head strong {
    text-align: left;
  }
}
</style>
