<template>
  <footer class="site-regulatory-footer" aria-label="网站备案与合规信息">
    <p>
      五行人格卡为娱乐性人格解读产品，结果不构成现实决策建议。
    </p>
    <p v-if="icpRecordNo" class="site-regulatory-links">
      <a :href="icpLink" target="_blank" rel="noreferrer noopener">
        {{ icpRecordNo }}
      </a>
    </p>
  </footer>
</template>

<script setup lang="ts">
const icpRecordNo = String(import.meta.env.VITE_ICP_RECORD_NO ?? '').trim();
const configuredIcpLink = String(import.meta.env.VITE_ICP_LINK ?? '').trim();
const icpLink = configuredIcpLink || 'https://beian.miit.gov.cn/';
</script>
