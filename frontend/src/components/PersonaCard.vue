<script setup lang="ts">
import { computed } from 'vue';
import type { ResultDetail } from '../api/types';
import { elementVisualByCode } from '../utils/elementVisuals';
import { normalizePersonaLabel } from '../utils/personaLabel';

const props = defineProps<{
  result: ResultDetail;
}>();

const primaryVisual = computed(() => elementVisualByCode(props.result.primaryElement, props.result.primaryElementName));
const secondaryVisual = computed(() => elementVisualByCode(props.result.secondaryElement, props.result.secondaryElementName));
const accentVisual = computed(() => elementVisualByCode(props.result.accentElement, props.result.accentElementName));
const personaLabel = computed(() => normalizePersonaLabel(props.result));
const identityLine = computed(() => props.result.identityLine || props.result.keywords.slice(0, 3).join(' · '));
const identitySummary = computed(() => props.result.heroSummary || '星曜取象会把日主、主副元素、点睛元素和星官意象收束成一句可记住的性格说明。');
</script>

<template>
  <section class="persona-card">
    <div
      class="card-visual"
      :data-element="result.primaryElement"
      :style="{
        '--primary-deco-color': primaryVisual.color,
        '--primary-deco-soft': primaryVisual.soft,
        '--secondary-deco-color': secondaryVisual.color,
        '--secondary-deco-soft': secondaryVisual.soft,
      }"
    >
      <span class="element-pattern water-pattern" aria-hidden="true"></span>
      <span class="element-pattern earth-pattern" aria-hidden="true"></span>
      <div class="persona-title-stack">
        <p class="eyebrow">{{ result.starToneLabel || '星曜取象' }} · {{ result.starOfficerName }}</p>
        <h1>{{ personaLabel }}</h1>
        <p class="identity-line">{{ identityLine }}</p>
        <p class="identity-summary">{{ identitySummary }}</p>
      </div>
    </div>

    <div class="element-roles" aria-label="五行角色">
      <article
        class="element-role primary-role"
        :style="{ '--role-color': primaryVisual.color, '--role-soft': primaryVisual.soft }"
      >
        <span>主元素</span>
        <strong>{{ result.primaryElementName }}</strong>
        <em>第一反应</em>
      </article>
      <article
        class="element-role"
        :style="{ '--role-color': secondaryVisual.color, '--role-soft': secondaryVisual.soft }"
      >
        <span>副元素</span>
        <strong>{{ result.secondaryElementName }}</strong>
        <em>校准方式</em>
      </article>
      <article
        v-if="result.accentElementName"
        class="element-role"
        :style="{ '--role-color': accentVisual.color, '--role-soft': accentVisual.soft }"
      >
        <span>点睛</span>
        <strong>{{ result.accentElementName }}</strong>
        <em>记忆点</em>
      </article>
    </div>

    <div class="keywords">
      <span v-for="keyword in result.keywords" :key="keyword">{{ keyword }}</span>
    </div>
  </section>
</template>

<style scoped>
.persona-card {
  display: grid;
  gap: 14px;
}

.card-visual {
  position: relative;
  overflow: hidden;
  display: grid;
  place-items: center;
  min-height: 112px;
  border-radius: 8px;
  padding: 16px 18px;
  border: 1px solid rgba(230, 202, 153, 0.38);
  background:
    radial-gradient(circle at 50% -42px, rgba(255, 244, 214, 0.15), transparent 42%),
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--primary-deco-color) 80%, #162522) 0%,
      color-mix(in srgb, var(--primary-deco-color) 58%, #101a18) 52%,
      color-mix(in srgb, var(--primary-deco-color) 42%, #08110f) 100%
    );
  color: #fff8e8;
  box-shadow:
    inset 0 0 0 1px rgba(255, 248, 232, 0.2),
    inset 0 -22px 42px rgba(0, 0, 0, 0.12),
    0 14px 28px rgba(49, 44, 35, 0.08);
}

.card-visual::before {
  content: "";
  position: absolute;
  inset: 7px;
  z-index: 0;
  pointer-events: none;
  border: 1px solid rgba(255, 235, 188, 0.34);
  border-radius: 6px;
  background:
    linear-gradient(90deg, transparent 0 10%, rgba(255, 232, 184, 0.68) 10% 29%, transparent 29% 71%, rgba(255, 232, 184, 0.68) 71% 90%, transparent 90%) top / 100% 1px,
    linear-gradient(90deg, transparent 0 17%, rgba(255, 232, 184, 0.36) 17% 38%, transparent 38% 62%, rgba(255, 232, 184, 0.36) 62% 83%, transparent 83%) 0 7px / 100% 1px;
  background-repeat: no-repeat;
  box-shadow:
    inset 0 0 0 1px rgba(255, 248, 232, 0.08),
    inset 0 0 24px rgba(255, 232, 184, 0.045);
}

.element-pattern {
  position: absolute;
  z-index: 0;
  pointer-events: none;
  background-repeat: no-repeat;
}

.water-pattern {
  left: 15px;
  top: 15px;
  width: 88px;
  height: 58px;
  opacity: 0.66;
  background:
    linear-gradient(rgba(255, 235, 188, 0.72) 0 0) 0 0 / 64px 1px,
    linear-gradient(rgba(255, 235, 188, 0.72) 0 0) 0 0 / 1px 38px,
    linear-gradient(rgba(255, 235, 188, 0.45) 0 0) 12px 12px / 50px 1px,
    linear-gradient(rgba(255, 235, 188, 0.45) 0 0) 12px 12px / 1px 30px,
    linear-gradient(rgba(255, 235, 188, 0.32) 0 0) 24px 24px / 34px 1px,
    linear-gradient(rgba(255, 235, 188, 0.32) 0 0) 24px 24px / 1px 18px;
  background-repeat: no-repeat;
}

.earth-pattern {
  right: 15px;
  top: 15px;
  width: 88px;
  height: 58px;
  opacity: 0.72;
  background:
    linear-gradient(color-mix(in srgb, var(--secondary-deco-color) 32%, #ffecbf) 0 0) 0 0 / 64px 1px,
    linear-gradient(color-mix(in srgb, var(--secondary-deco-color) 32%, #ffecbf) 0 0) 0 0 / 1px 38px,
    linear-gradient(rgba(255, 235, 188, 0.48) 0 0) 12px 12px / 50px 1px,
    linear-gradient(rgba(255, 235, 188, 0.48) 0 0) 12px 12px / 1px 30px,
    linear-gradient(rgba(255, 235, 188, 0.34) 0 0) 24px 24px / 34px 1px,
    linear-gradient(rgba(255, 235, 188, 0.34) 0 0) 24px 24px / 1px 18px;
  background-repeat: no-repeat;
}

.persona-title-stack {
  position: relative;
  z-index: 1;
  min-width: 0;
  text-align: center;
  justify-self: center;
}

h1 {
  margin: 0;
  color: #fff9ec;
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 650;
  line-height: 1.22;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.16);
}

.identity-line {
  margin: 6px 0 0;
  color: rgba(255, 248, 232, 0.76);
  font-weight: 800;
}

.identity-summary {
  max-width: 520px;
  margin: 9px auto 0;
  color: rgba(255, 248, 232, 0.82);
  font-size: 13px;
  font-weight: 760;
  line-height: 1.62;
}

.card-visual .eyebrow {
  color: rgba(255, 218, 163, 0.9);
}

.keywords {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
}

.element-roles {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.element-role {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 3px;
  justify-items: center;
  min-height: 74px;
  min-width: 0;
  border: 1px solid color-mix(in srgb, var(--role-color) 30%, rgba(37, 48, 45, 0.08));
  border-radius: 8px;
  padding: 10px 8px 9px;
  background:
    linear-gradient(180deg, rgba(255, 252, 245, 0.9), rgba(255, 252, 245, 0.72)),
    linear-gradient(135deg, var(--role-soft), rgba(255, 252, 245, 0.4));
  color: var(--color-ink);
  text-align: center;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 8px 16px rgba(58, 48, 34, 0.04);
}

.element-role::before {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  top: 8px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--role-color) 74%, #fff4dc), transparent);
  opacity: 0.82;
}

.element-role span {
  margin-top: 4px;
  color: #796d5f;
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
}

.element-role strong {
  color: var(--role-color);
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 650;
  line-height: 1;
}

.element-role em {
  color: #52625c;
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
  line-height: 1.15;
}

.keywords span {
  min-width: 0;
  border: 1px solid rgba(138, 102, 62, 0.13);
  border-radius: 999px;
  padding: 7px 8px;
  background:
    linear-gradient(180deg, rgba(255, 252, 245, 0.82), rgba(243, 228, 207, 0.78));
  color: #77593d;
  font-size: 12px;
  font-weight: 850;
  line-height: 1.15;
  text-align: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.66);
}

@media (max-width: 640px) {
  .persona-card {
    gap: 9px;
  }

  .card-visual {
    min-height: 0;
    padding: 15px 18px 14px;
  }

  .water-pattern {
    left: 12px;
    top: 12px;
    transform: scale(0.82);
    transform-origin: top left;
  }

  .earth-pattern {
    right: 12px;
    top: 12px;
    transform: scale(0.82);
    transform-origin: top right;
  }

  .card-visual .eyebrow {
    margin-bottom: 4px;
    font-size: 11px;
    line-height: 1.25;
  }

  h1 {
    font-size: 19px;
    line-height: 1.2;
  }

  .identity-line {
    margin-top: 5px;
    font-size: 12px;
    line-height: 1.35;
  }

  .identity-summary {
    max-width: 25em;
    margin-top: 7px;
    font-size: 12px;
    line-height: 1.55;
  }

  .keywords {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 5px;
  }

  .element-roles {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 5px;
  }

  .element-role {
    min-height: 61px;
    padding: 9px 5px 7px;
  }

  .element-role::before {
    left: 9px;
    right: 9px;
    top: 7px;
  }

  .element-role span {
    font-size: 11px;
  }

  .element-role strong {
    font-size: 23px;
  }

  .element-role em {
    font-size: 11px;
  }

  .keywords span {
    padding: 6px 3px;
    font-size: 11px;
  }
}

@media (max-width: 380px) {
  h1 {
    font-size: 18px;
  }

  .keywords {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
