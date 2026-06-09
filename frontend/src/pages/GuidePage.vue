<template>
  <main class="page guide-page">
    <section class="shell stack">
      <p class="eyebrow">传统文化趣味人格测试</p>
      <h1>五行人格卡</h1>
      <p class="lead">
        输入出生年月和 5 道价值取向题，生成你的五行人格卡。
      </p>
      <div class="actions">
        <RouterLink class="button-link" to="/test" @click="start">开始测试</RouterLink>
      </div>
      <p class="notice">
        本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
      </p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { track } from '../utils/tracker';

onMounted(() => track('PAGE_VIEW_HOME', '/'));

function start() {
  track('START_TEST_CLICK', '/');
}
</script>

<style scoped>
.guide-page {
  display: grid;
  align-items: center;
  background:
    radial-gradient(circle at top left, rgba(47, 111, 94, 0.14), transparent 32%),
    linear-gradient(180deg, #faf8f2 0%, #edf4ef 100%);
}

.lead {
  max-width: 520px;
  margin: 8px 0 0;
  color: #40514e;
  font-size: 18px;
}
</style>
