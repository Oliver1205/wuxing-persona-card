<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { withShareAttribution } from '../utils/attribution';
import { track } from '../utils/tracker';

const props = defineProps<{
  shortUrl: string;
  resultId?: string;
  shortCode?: string;
  showSaveImage?: boolean;
}>();

const emit = defineEmits<{
  copied: [];
  'save-image': [];
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
const displayShareUrl = computed(() => compactShareUrl(props.shortUrl));
const codeCopyLabel = computed(() => {
  if (codeCopying.value) {
    return '复制中';
  }
  return codeCopied.value ? '短码已复制' : '复制短码';
});
const copyLabel = computed(() => {
  if (copying.value) {
    return '复制中';
  }
  return copied.value ? '链接已复制' : '复制链接';
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

function compactShareUrl(url: string) {
  try {
    const target = new URL(url, window.location.origin);
    const cleanPath = `${target.pathname}${target.hash}`;
    if (target.origin === window.location.origin) {
      return cleanPath || target.pathname;
    }
    return `${target.host}${cleanPath}`;
  } catch {
    return url.split('?')[0] || url;
  }
}
</script>

<template>
  <section id="share-box" class="share-box" :class="{ 'copy-only': !showSaveImage }">
    <div class="share-heading">
      <p class="label">保存与分享</p>
      <strong>分享图、短码和链接</strong>
      <span>保存图片适合发聊天窗口；短码用于双人匹配，链接用于打开结果。</span>
    </div>
    <div class="share-main">
      <div v-if="shortCode" class="short-code-block">
        <p class="label">双人匹配短码</p>
        <p ref="codeEl" class="short-code" tabindex="0" @click="selectShortCode">{{ shortCode }}</p>
      </div>
      <div>
        <p class="label">打开结果短链</p>
        <p ref="urlEl" class="url" tabindex="0" @click="selectShareUrl">{{ displayShareUrl }}</p>
      </div>
    </div>
    <div class="share-actions">
      <button v-if="showSaveImage" data-testid="save-share-image" class="primary-share-action" type="button" @click="emit('save-image')">保存分享图</button>
      <button data-testid="native-share" class="secondary main-secondary-action" type="button" :disabled="sharing" @click="nativeShare">
        {{ sharing ? '分享中' : '系统分享' }}
      </button>
      <details class="copy-tools" :open="!showSaveImage">
        <summary data-testid="copy-tools-toggle">复制备用信息</summary>
        <div class="copy-actions">
          <button v-if="shortCode" data-testid="copy-match-code" class="secondary subtle-action" type="button" :disabled="codeCopying" @click="copyShortCode">{{ codeCopyLabel }}</button>
          <button data-testid="copy-share-link" class="secondary subtle-action" type="button" :disabled="copying" @click="copy">{{ copyLabel }}</button>
        </div>
      </details>
    </div>
    <p v-if="message" class="tip" role="status" aria-live="polite">{{ message }}</p>
  </section>
</template>

<style scoped>
.share-box {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(180px, 0.56fr) minmax(0, 1fr);
  gap: 14px 18px;
  align-items: center;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(250, 246, 237, 0.86)),
    linear-gradient(90deg, rgba(177, 211, 209, 0.18), transparent);
  box-shadow: 0 16px 36px rgba(31, 48, 43, 0.08);
}

.share-heading {
  display: grid;
  gap: 6px;
  align-content: center;
}

.share-heading strong {
  color: #202725;
  font-family: "Songti SC", "STSong", "Noto Serif SC", var(--font-display);
  font-size: 22px;
  font-weight: 650;
  line-height: 1.25;
}

.share-heading span {
  max-width: 260px;
  color: #596764;
  font-size: 13px;
  font-weight: 760;
  line-height: 1.55;
}

.share-actions {
  position: relative;
  z-index: 1;
  display: grid;
  grid-column: 2;
  grid-template-columns: repeat(2, minmax(0, auto));
  gap: 8px;
  justify-content: start;
  align-items: start;
}

.share-box.copy-only .share-actions {
  grid-template-columns: minmax(0, auto);
}

.share-actions button,
.copy-tools summary {
  min-width: 104px;
  min-height: 44px;
  padding: 0 13px;
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;
}

.copy-tools {
  grid-column: 1 / -1;
}

.copy-tools summary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  border: 1px solid rgba(47, 111, 94, 0.18);
  border-radius: 6px;
  color: #2f6f5e;
  cursor: pointer;
  font-weight: 760;
  list-style: none;
}

.copy-tools summary::-webkit-details-marker {
  display: none;
}

.copy-tools summary::after {
  content: "展开";
  margin-left: 8px;
  color: #7d8b86;
  font-size: 12px;
  font-weight: 700;
}

.copy-tools[open] summary::after {
  content: "收起";
}

.copy-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 8px;
}

.share-actions .primary-share-action {
  background: #2f6f5e;
  color: #fff;
  box-shadow: 0 10px 22px rgba(47, 111, 94, 0.18);
}

.share-actions .subtle-action {
  border-color: rgba(47, 111, 94, 0.18);
  color: #2f6f5e;
}

.share-main {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(120px, auto) minmax(0, 1fr);
  gap: 10px 14px;
  align-items: start;
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
  border: 1px solid rgba(191, 137, 24, 0.28);
  padding: 7px 12px;
  background: #fff8e8;
  color: #123253;
  font-size: 22px;
  font-weight: 950;
  letter-spacing: 0;
}

.short-code:focus,
.url:focus {
  box-shadow: 0 0 0 3px rgba(47, 111, 94, 0.14);
}

.url {
  display: -webkit-box;
  overflow: hidden;
  color: #40514e;
  font-size: 13px;
  font-weight: 760;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.tip {
  grid-column: 1 / -1;
  margin: 0;
  color: #2f6f5e;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 760px) {
  .share-box {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .share-heading span {
    max-width: none;
  }

  .share-main {
    grid-template-columns: 1fr;
  }

  .share-actions {
    grid-column: auto;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .share-actions button,
  .copy-tools summary {
    width: 100%;
    min-width: 0;
    min-height: 44px;
    padding: 0 12px;
    font-size: 14px;
  }

  .share-actions .primary-share-action {
    grid-column: 1 / -1;
  }

  .share-box.copy-only .share-actions button[data-testid="native-share"],
  .copy-tools {
    grid-column: 1 / -1;
  }

  .copy-tools summary {
    display: flex;
  }

  .copy-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .short-code {
    font-size: 20px;
  }
}
</style>
