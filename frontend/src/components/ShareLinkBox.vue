<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { withShareAttribution } from '../utils/attribution';
import { track } from '../utils/tracker';

const props = defineProps<{
  shortUrl: string;
  resultId?: string;
  shortCode?: string;
}>();

const emit = defineEmits<{
  copied: [];
}>();

const codeEl = ref<HTMLElement | null>(null);
const urlEl = ref<HTMLElement | null>(null);
const copied = ref(false);
const codeCopied = ref(false);
const copying = ref(false);
const codeCopying = ref(false);
const sharing = ref(false);
const message = ref('');
const shareUrl = computed(() => withShareAttribution(props.shortUrl));
const codeCopyLabel = computed(() => {
  if (codeCopying.value) {
    return '复制中';
  }
  return codeCopied.value ? '短码已复制' : '复制匹配短码';
});
const copyLabel = computed(() => {
  if (copying.value) {
    return '复制中';
  }
  return copied.value ? '分享链接已复制' : '复制分享链接';
});

onMounted(() => {
  track('SHARE_PANEL_VIEW', undefined, props.resultId, props.shortCode);
});

async function copyShortCode() {
  if (codeCopying.value || !props.shortCode) {
    return;
  }
  codeCopying.value = true;
  message.value = '';
  try {
    await navigator.clipboard.writeText(props.shortCode);
    codeCopied.value = true;
    message.value = '匹配短码已复制。朋友打开首页时，系统会尝试识别这段短码。';
    emit('copied');
  } catch {
    codeCopied.value = false;
    selectShortCode();
    message.value = '当前浏览器不支持自动复制，请长按短码手动复制';
  } finally {
    codeCopying.value = false;
  }
}

async function copy() {
  if (copying.value) {
    return;
  }
  copying.value = true;
  message.value = '';
  try {
    await navigator.clipboard.writeText(shareUrl.value);
    copied.value = true;
    message.value = '分享链接已复制，发给朋友看看吧';
    emit('copied');
  } catch {
    copied.value = false;
    selectShareUrl();
    message.value = '当前浏览器不支持自动复制，请长按链接手动复制';
  } finally {
    copying.value = false;
  }
}

async function nativeShare() {
  if (sharing.value) {
    return;
  }
  message.value = '';
  if (!navigator.share) {
    message.value = '当前浏览器不支持系统分享，可以先复制分享链接';
    return;
  }
  sharing.value = true;
  try {
    await navigator.share({
      title: '我的五行人格卡',
      text: '我刚生成了一张五行人格卡，看看像不像我。',
      url: shareUrl.value,
    });
    track('NATIVE_SHARE_SUCCESS', undefined, props.resultId, props.shortCode);
  } catch {
    message.value = '分享已取消';
  } finally {
    sharing.value = false;
  }
}

function selectShareUrl() {
  const el = urlEl.value;
  if (!el) {
    return;
  }
  const selection = window.getSelection();
  const range = document.createRange();
  range.selectNodeContents(el);
  selection?.removeAllRanges();
  selection?.addRange(range);
}

function selectShortCode() {
  const el = codeEl.value;
  if (!el) {
    return;
  }
  const selection = window.getSelection();
  const range = document.createRange();
  range.selectNodeContents(el);
  selection?.removeAllRanges();
  selection?.addRange(range);
}
</script>

<template>
  <section class="share-box">
    <div class="share-main">
      <div v-if="shortCode" class="short-code-block">
        <p class="label">双人匹配短码</p>
        <p ref="codeEl" class="short-code" tabindex="0" @click="selectShortCode">{{ shortCode }}</p>
      </div>
      <div>
        <p class="label">专属分享链接</p>
        <p ref="urlEl" class="url" tabindex="0" @click="selectShareUrl">{{ shareUrl }}</p>
      </div>
      <div class="share-note">
        <span>短码用于双人匹配</span>
        <span>链接用于打开结果</span>
      </div>
    </div>
    <div class="share-actions">
      <button v-if="shortCode" type="button" :disabled="codeCopying" @click="copyShortCode">{{ codeCopyLabel }}</button>
      <button type="button" :disabled="copying" @click="copy">{{ copyLabel }}</button>
      <button class="secondary" type="button" :disabled="sharing" @click="nativeShare">
        {{ sharing ? '分享中' : '系统分享' }}
      </button>
    </div>
    <p v-if="message" class="tip">{{ message }}</p>
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

.share-actions {
  display: grid;
  gap: 8px;
}

.share-main {
  display: grid;
  gap: 10px;
}

.label {
  margin: 0 0 4px;
  color: #6c7976;
  font-size: 13px;
}

.short-code,
.url {
  margin: 0;
  overflow-wrap: anywhere;
  border-radius: 6px;
  color: #263735;
  font-weight: 700;
  outline: none;
}

.short-code {
  width: fit-content;
  border: 1px solid rgba(47, 111, 94, 0.18);
  padding: 7px 10px;
  background: #edf7f2;
  color: #1f3732;
  font-size: 22px;
}

.short-code:focus,
.url:focus {
  box-shadow: 0 0 0 3px rgba(47, 111, 94, 0.14);
}

.share-note {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.share-note span {
  border-radius: 999px;
  padding: 6px 9px;
  background: #f1eadc;
  color: #6d4f29;
  font-size: 12px;
  font-weight: 800;
}

.tip {
  grid-column: 1 / -1;
  margin: 0;
  color: #2f6f5e;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .share-box {
    grid-template-columns: 1fr;
  }

  .share-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
