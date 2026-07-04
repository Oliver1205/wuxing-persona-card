<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { withShareAttribution } from '../utils/attribution';
import { track } from '../utils/tracker';

const props = defineProps<{
  shortUrl: string;
  resultId?: string;
  shortCode?: string;
  showSaveImage?: boolean;
  showRetake?: boolean;
}>();

const emit = defineEmits<{
  copied: [];
  'save-image': [];
  retake: [];
}>();

const codeEl = ref<HTMLElement | null>(null);
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
    message.value = '短码已复制，可以发给朋友做双人匹配。';
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
    message.value = '当前浏览器不支持自动复制，可以使用系统分享';
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
  <section id="share-box" class="share-box" :class="{ 'copy-only': !showSaveImage }">
    <div class="share-heading">
      <p class="label">保存与分享</p>
      <strong>把这张人格卡发给朋友</strong>
      <span>分享图适合直接转发，短码可用于双人匹配。</span>
    </div>

    <div v-if="shortCode" class="match-code-card">
      <div>
        <p class="label">匹配短码</p>
        <span>发给朋友，可一起查看匹配结果。</span>
      </div>
      <p ref="codeEl" class="short-code" tabindex="0" @click="selectShortCode">{{ shortCode }}</p>
    </div>

    <div class="share-actions">
      <button v-if="showSaveImage" data-testid="save-share-image" class="primary-share-action" type="button" @click="emit('save-image')">保存分享图</button>
      <div class="secondary-action-grid">
        <button data-testid="native-share" class="secondary share-secondary-action" type="button" :disabled="sharing" @click="nativeShare">
          {{ sharing ? '分享中' : '发给朋友' }}
        </button>
        <button v-if="shortCode" data-testid="copy-match-code" class="secondary share-secondary-action" type="button" :disabled="codeCopying" @click="copyShortCode">{{ codeCopyLabel }}</button>
        <button data-testid="copy-share-link" class="secondary share-secondary-action" type="button" :disabled="copying" @click="copy">{{ copyLabel }}</button>
      </div>
    </div>
    <p v-if="message" class="tip" role="status" aria-live="polite">{{ message }}</p>
    <RouterLink v-if="showRetake" class="retake-action" to="/test" @click="emit('retake')">重新测试</RouterLink>
  </section>
</template>

<style scoped>
.share-box {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 14px;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 18px;
  background:
    radial-gradient(circle at 88% -18%, rgba(201, 111, 61, 0.11), transparent 34%),
    linear-gradient(135deg, rgba(255, 252, 245, 0.94), rgba(248, 240, 226, 0.88)),
    linear-gradient(90deg, rgba(201, 111, 61, 0.07), rgba(47, 98, 85, 0.07));
  box-shadow: var(--shadow-paper);
}

.share-heading {
  display: grid;
  gap: 5px;
  text-align: center;
}

.share-heading strong {
  color: var(--color-ink);
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 650;
  line-height: 1.25;
}

.share-heading span {
  max-width: 32em;
  justify-self: center;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 760;
  line-height: 1.55;
}

.match-code-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(47, 98, 85, 0.14);
  border-radius: 8px;
  padding: 12px 13px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.78), rgba(242, 232, 214, 0.7));
}

.match-code-card span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 760;
  line-height: 1.45;
}

.share-actions {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 9px;
}

.secondary-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.share-actions button {
  min-height: 44px;
  padding: 0 13px;
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;
}

.share-actions .primary-share-action {
  width: 100%;
  min-height: 50px;
  border: 1px solid rgba(158, 79, 46, 0.24);
  border-radius: 8px;
  background: linear-gradient(135deg, var(--color-warm), var(--color-warm-deep));
  color: #fff;
  font-size: 15px;
  font-weight: 950;
  box-shadow: 0 12px 24px rgba(157, 86, 49, 0.2);
}

.share-secondary-action {
  border-color: rgba(47, 98, 85, 0.18);
  background: rgba(255, 252, 245, 0.78);
  color: var(--color-primary);
  font-weight: 900;
}

.label {
  margin: 0 0 4px;
  color: var(--color-warm-deep);
  font-size: 13px;
  font-weight: 900;
}

.short-code {
  margin: 0;
  overflow-wrap: anywhere;
  border-radius: 6px;
  color: var(--color-ink);
  font-weight: 700;
  outline: none;
}

.short-code {
  width: fit-content;
  border: 1px solid rgba(201, 111, 61, 0.28);
  padding: 8px 13px;
  background: #fff2e7;
  color: var(--color-warm-deep);
  font-size: 20px;
  font-weight: 950;
  letter-spacing: 0;
}

.short-code:focus {
  box-shadow: 0 0 0 3px rgba(47, 98, 85, 0.14);
}

.tip {
  margin: 0;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.retake-action {
  justify-self: center;
  min-height: 40px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(37, 48, 45, 0.11);
  border-radius: 8px;
  background: rgba(255, 252, 245, 0.62);
  color: #6d766f;
  font-size: 13px;
  font-weight: 850;
  text-decoration: none;
}

.retake-action:focus-visible {
  outline: 3px solid rgba(47, 98, 85, 0.2);
  outline-offset: 2px;
}

@media (max-width: 760px) {
  .share-box {
    gap: 10px;
    padding: 14px;
  }

  .share-heading {
    gap: 3px;
  }

  .share-heading strong {
    font-size: 20px;
    line-height: 1.2;
  }

  .share-heading span {
    max-width: none;
    font-size: 12px;
    line-height: 1.4;
  }

  .match-code-card {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 8px;
    padding: 10px;
  }

  .match-code-card span {
    font-size: 11px;
  }

  .secondary-action-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
  }

  .share-actions button {
    width: 100%;
    min-width: 0;
    min-height: 44px;
    padding: 0 12px;
    font-size: 13px;
  }

  .share-box.copy-only .secondary-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .label {
    margin-bottom: 3px;
    font-size: 11px;
  }

  .short-code {
    padding: 6px 8px;
    font-size: 17px;
  }

  .tip {
    font-size: 12px;
    line-height: 1.45;
  }
}

@media (max-width: 380px) {
  .match-code-card {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .secondary-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .secondary-action-grid button[data-testid="native-share"] {
    grid-column: 1 / -1;
  }
}
</style>
