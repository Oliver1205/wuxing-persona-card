<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
  note?: string;
}>();
</script>

<template>
  <article class="stat-card">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small v-if="note">{{ note }}</small>
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

small {
  min-height: 34px;
  color: #7a8783;
  font-size: 12px;
  line-height: 1.42;
}
</style>
