<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  shortUrl: string;
}>();

const emit = defineEmits<{
  copied: [];
}>();

const copied = ref(false);

async function copy() {
  try {
    await navigator.clipboard.writeText(props.shortUrl);
    copied.value = true;
    emit('copied');
  } catch {
    const selection = window.getSelection();
    selection?.removeAllRanges();
    copied.value = false;
  }
}
</script>

<template>
  <section class="share-box">
    <div>
      <p class="label">专属短链接</p>
      <p class="url">{{ shortUrl }}</p>
    </div>
    <button type="button" @click="copy">复制</button>
    <p v-if="copied" class="tip">短链接已复制，发给朋友看看吧</p>
  </section>
</template>

<style scoped>
.share-box {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

.label {
  margin: 0 0 4px;
  color: #6c7976;
  font-size: 13px;
}

.url {
  margin: 0;
  overflow-wrap: anywhere;
  color: #263735;
  font-weight: 700;
}

.tip {
  grid-column: 1 / -1;
  margin: 0;
  color: #2f6f5e;
  font-size: 14px;
  font-weight: 700;
}
</style>
