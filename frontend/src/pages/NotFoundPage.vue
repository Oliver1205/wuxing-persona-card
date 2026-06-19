<script setup lang="ts">
import { ref } from 'vue';

const copyMessage = ref('');

async function copyCurrentAddress() {
  copyMessage.value = '';
  try {
    await navigator.clipboard.writeText(window.location.href);
    copyMessage.value = '当前地址已复制';
  } catch {
    copyMessage.value = '当前浏览器不支持自动复制，请长按地址栏复制';
  }
}
</script>

<template>
  <main class="page not-found-page">
    <section class="shell panel stack not-found-card" role="status" aria-labelledby="not-found-title">
      <p class="brand-line">五行人格卡</p>
      <p class="eyebrow">链接没有找到</p>
      <h2 id="not-found-title">页面不存在</h2>
      <p class="muted">这个地址可能是短链已失效、分享链接被截断，或路径输入有误。可以重新打开朋友发来的完整链接，或回到首页输入短码。</p>
      <div class="actions not-found-actions">
        <RouterLink class="button-link" to="/">返回首页</RouterLink>
        <button class="secondary" type="button" @click="copyCurrentAddress">复制当前地址</button>
        <RouterLink class="button-link secondary" to="/test">重新测一张</RouterLink>
      </div>
      <p v-if="copyMessage" class="not-found-message" role="status" aria-live="polite">{{ copyMessage }}</p>
    </section>
  </main>
</template>

<style scoped>
.not-found-page {
  display: grid;
  align-items: center;
  background:
    linear-gradient(135deg, rgba(47, 111, 94, 0.08), transparent 36%),
    linear-gradient(180deg, #f8f5eb 0%, #eef5f0 100%);
}

.not-found-card {
  width: min(100%, 620px);
  text-align: left;
}

.brand-line {
  color: #8c6533;
  font-size: 13px;
  font-weight: 900;
}

.not-found-card h2 {
  margin-bottom: 6px;
  font-size: 32px;
}

.not-found-card p {
  margin: 0;
}

.not-found-actions {
  margin-top: 4px;
}

.not-found-message {
  color: #2f6f5e;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 520px) {
  .not-found-card {
    padding: 20px;
  }

  .not-found-card h2 {
    font-size: 26px;
  }

  .not-found-actions {
    display: grid;
  }
}
</style>
