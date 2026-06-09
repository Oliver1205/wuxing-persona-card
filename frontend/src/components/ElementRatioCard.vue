<script setup lang="ts">
defineProps<{
  primaryName: string;
  primaryPercent: number;
  secondaryName: string;
  secondaryPercent: number;
}>();
</script>

<template>
  <section class="ratio-card">
    <div class="ratio-head">
      <span>{{ primaryName }} {{ primaryPercent }}%</span>
      <span>{{ secondaryName }} {{ secondaryPercent }}%</span>
    </div>
    <div class="bar" aria-hidden="true">
      <div class="primary" :style="{ width: `${primaryPercent}%` }"></div>
      <div class="secondary" :style="{ width: `${secondaryPercent}%` }"></div>
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

.primary {
  background: #2f6f5e;
}

.secondary {
  background: #d89a43;
}
</style>
