<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
}>();
</script>

<template>
  <article class="stat-card">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
  </article>
</template>

<style scoped>
.stat-card {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

span {
  color: #697674;
  font-size: 13px;
}

strong {
  color: #263735;
  font-size: 24px;
}
</style>
