<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchResult } from '../api/results';
import type { ResultDetail } from '../api/types';
import PersonaCard from '../components/PersonaCard.vue';
import ShareLinkBox from '../components/ShareLinkBox.vue';
import { downloadResultShareCard } from '../utils/shareCard';
import { track } from '../utils/tracker';

const route = useRoute();
const result = ref<ResultDetail | null>(null);
const loading = ref(true);
const error = ref('');
const shareImageStatus = ref('');
const sharedEntry = computed(() => Boolean(
  route.query.sc
  || route.query.channel === 'shared-result'
  || route.query.channel === 'share',
));

interface TextBlock {
  eyebrow: string;
  title: string;
  paragraphs: string[];
  termHint?: string;
  quote?: QuoteBlock;
  tone?: 'default' | 'stuck';
}

interface QuoteBlock {
  quote: string;
  source: string;
  explain: string;
}

const fallbackAdvice = [
  { title: '先确定主线', text: '当方向太多时，先选一条最重要的线推进。' },
  { title: '固定复盘节奏', text: '用固定复盘把经验沉淀下来，能减少反复摇摆。' },
  { title: '保留行动版本', text: '先做出一个小版本，再根据反馈调整。' },
  { title: '把感受说清楚', text: '表达真实需求，会让关系和合作更稳定。' },
];

const termGlossary = {
  dayMaster: '日主：出生日天干，在传统命理里常作为“我”的核心符号。',
  primaryElement: '主元素：五题选择里最先冒出来的反应倾向。',
  secondaryElement: '副元素：修正主元素的第二层力量。',
  accentElement: '点睛元素：不主导人格，但会让人格多出反差、入口和余味。',
  starOfficer: '星官：传统星宿文化里的意象锚点，用来增加画面和记忆，仅作文化参考。',
  xuXiu: '虚宿：北方玄武七宿之一，意象偏幽静、藏蓄、留白。',
};

const stemProfiles: Record<string, { label: string; action: string; hint: string; quote?: QuoteBlock }> = {
  甲: {
    label: '甲木',
    action: '先立骨架，再向上生长',
    hint: '甲木：十天干之一，属阳木，常取大树、梁柱、向上支撑之象。',
  },
  乙: {
    label: '乙木',
    action: '先贴近环境，再柔韧生长',
    hint: '乙木：十天干之一，属阴木，常取藤蔓、花草、细枝舒展之象。',
  },
  丙: {
    label: '丙火',
    action: '先被点亮，再照向外界',
    hint: '丙火：十天干之一，属阳火，常取太阳、明光、外放照临之象。',
    quote: {
      quote: '离，丽也',
      source: '《说卦传》',
      explain: '离火有附着、照明、显现之意，用在这里是说目标被看见后，行动才会真正亮起来。',
    },
  },
  丁: {
    label: '丁火',
    action: '先守住微光，再照见重点',
    hint: '丁火：十天干之一，属阴火，常取灯火、烛焰、精神亮点之象。',
    quote: {
      quote: '离，丽也',
      source: '《说卦传》',
      explain: '离火重在显现与照明。放到性格上，它不是喧闹，而是在关键处把重要目标照出来。',
    },
  },
  戊: {
    label: '戊土',
    action: '先稳住边界，再承接重量',
    hint: '戊土：十天干之一，属阳土，常取山岳、台基、厚重承载之象。',
    quote: {
      quote: '地势坤，君子以厚德载物',
      source: '《周易·坤卦》',
      explain: '土的力量在承载与安顿。这里取其“能接住重量”的意思，不作命运判断。',
    },
  },
  己: {
    label: '己土',
    action: '先整理细节，再慢慢滋养',
    hint: '己土：十天干之一，属阴土，常取田地、沃土、整理滋养之象。',
    quote: {
      quote: '地势坤，君子以厚德载物',
      source: '《周易·坤卦》',
      explain: '这句话强调承载和包容。放在这里，是说土能把复杂感受安放到现实秩序里。',
    },
  },
  庚: {
    label: '庚金',
    action: '先辨标准，再利落出手',
    hint: '庚金：十天干之一，属阳金，常取矿石、刀剑、原则与执行之象。',
  },
  辛: {
    label: '辛金',
    action: '分寸在前，轻重自清',
    hint: '辛金：十天干之一，属阴金，常取珠玉、镜面、精细辨别之象。',
  },
  壬: {
    label: '壬水',
    action: '观大势，顺流转向',
    hint: '壬水：十天干之一，属阳水，常取江河、流动、格局与探索之象。',
    quote: {
      quote: '上善若水',
      source: '《道德经》',
      explain: '水的价值不在硬冲，而在能容、能入、能转。这里用来说明你会先理解局势再行动。',
    },
  },
  癸: {
    label: '癸水',
    action: '先感知，再判断',
    hint: '癸水：十天干之一，属阴水，常取雨露、泉脉、细流之象。',
    quote: {
      quote: '癸水至弱，达于天津',
      source: '《滴天髓》',
      explain: '这里的“弱”不是软弱，而是柔细、流通、能深入。放到现实里，就是更擅长在复杂信息里找到暗线。',
    },
  },
};

const elementTitles: Record<string, { relation: string; section: string; inner: string; outer: string; stuck: string }> = {
  水: {
    relation: '水先入心',
    section: '水有岸，灯才会亮',
    inner: '心中有潮，先让局势变清',
    outer: '外在有岸，慢热但可靠',
    stuck: '想得太深，出口太少',
  },
  土: {
    relation: '土后成形',
    section: '土能承重，光才站稳',
    inner: '心里先安顿，行动再落地',
    outer: '外在能承接，节奏有重量',
    stuck: '太想稳住，启动偏慢',
  },
  火: {
    relation: '火把目标照亮',
    section: '火有方向，才有行动',
    inner: '心里有光，目标会先亮',
    outer: '外在有热度，行动更明显',
    stuck: '热度太急，容易烧散',
  },
  木: {
    relation: '木把路径长出来',
    section: '枝有方向，路会长出',
    inner: '心里先找路，再让想法生长',
    outer: '外在有生长感，愿意推进',
    stuck: '路径太多，收束太少',
  },
  金: {
    relation: '金替边界定音',
    section: '镜能照形，判断才清',
    inner: '心里先分辨，再决定轻重',
    outer: '外在有分寸，标准更清楚',
    stuck: '标准太高，行动被拦',
  },
};

const accentTitles: Record<string, string> = {
  水: '暗处有回声，判断会更深',
  土: '脚下有台面，想法能站稳',
  火: '暗处有灯，目标会亮',
  木: '石缝有青枝，路径会长出',
  金: '袖口有清铃，边界会响',
};

const starTitleRegistry: Record<string, string> = {
  JIAO_XIU: '角宿：春木初探',
  FANG_XIU: '房宿：有处安放',
  JI_XIU: '箕宿：风里有序',
  JING_XIU: '井宿：水源有井',
  XING_XIU: '星宿：光点可辨',
  ZHANG_XIU: '张宿：气势铺开',
  KUI_XIU: '奎宿：文理有纹',
  LOU_XIU: '娄宿：收拢成仓',
  MAO_XIU: '昴宿：众星成辨',
  NIU_XIU: '牛宿：慢处蓄力',
  XU_XIU: '虚宿：深处有留白',
  WEI_XIU: '危宿：临界见边',
};

const quoteRegistry: { element: Record<string, QuoteBlock | undefined> } = {
  element: {
    水: {
      quote: '上善若水',
      source: '《道德经》',
      explain: '水的价值不在硬冲，而在能容、能入、能转。放到这里，是先接住信息，再找到行动的方向。',
    },
    土: {
      quote: '地势坤，君子以厚德载物',
      source: '《周易·坤卦》',
      explain: '土的力量在承载、稳定和包容。放到这里，是把感受落成责任、节奏和可交付的结果。',
    },
    火: {
      quote: '离，丽也',
      source: '《说卦传》',
      explain: '离火有照明、显现之意。放到这里，是让目标被看见，让行动有一个亮起来的入口。',
    },
  },
};

function sanitizeResultText(current: ResultDetail, text?: string) {
  const raw = current.personaLabel?.trim();
  const displayLabel = current.starToneName || current.personaLabel || '';
  if (!text || !raw || raw === displayLabel) {
    return text || '';
  }
  return text.split(raw).join(displayLabel);
}

const dayMasterParagraphs = computed(() => (
  result.value ? splitResultParagraphs(result.value, result.value.dayMasterText) : []
));

const primarySecondaryParagraphs = computed(() => (
  result.value ? splitResultParagraphs(result.value, result.value.primarySecondaryText) : []
));

const accentParagraphs = computed(() => (
  result.value ? splitResultParagraphs(result.value, result.value.accentText) : []
));

const starParagraphs = computed(() => (
  result.value ? splitResultParagraphs(result.value, result.value.starOfficerText) : []
));

const coreBlocks = computed<TextBlock[]>(() => {
  if (!result.value) {
    return [];
  }
  const current = result.value;
  const dayMaster = dayMasterProfile(current);
  const secondaryTitle = elementTitles[current.secondaryElementName]?.relation ?? `${current.secondaryElementName}来成形`;
  const accentTitle = accentTitles[current.accentElementName] ?? `${current.accentElementName || '点睛'}让气质多一层余味`;
  return [
    {
      eyebrow: '日主',
      title: dayMaster ? `${dayMaster.label}：${dayMaster.action}` : '日主：底色先定',
      paragraphs: dayMasterParagraphs.value,
      termHint: `${termGlossary.dayMaster}${dayMaster?.hint ?? ''}`.trim(),
      quote: dayMaster?.quote,
    },
    {
      eyebrow: '主从',
      title: `${elementTitles[current.primaryElementName]?.relation ?? `${current.primaryElementName}先发声`}，${secondaryTitle}`,
      paragraphs: primarySecondaryParagraphs.value,
      termHint: `${termGlossary.primaryElement}${termGlossary.secondaryElement}`,
      quote: quoteRegistry.element[current.secondaryElementName] ?? quoteRegistry.element[current.primaryElementName],
    },
    {
      eyebrow: '点睛',
      title: accentTitle,
      paragraphs: accentParagraphs.value,
      termHint: termGlossary.accentElement,
      quote: current.accentElementName === current.secondaryElementName ? undefined : quoteRegistry.element[current.accentElementName],
    },
  ];
});

const expressionBlocks = computed<TextBlock[]>(() => {
  if (!result.value) {
    return [];
  }
  const current = result.value;
  return [
    {
      eyebrow: '内',
      title: elementTitles[current.primaryElementName]?.inner ?? '内在先接住真实反应',
      paragraphs: splitResultParagraphs(current, current.heavenText || '你的内在底色会影响你如何理解自己、安放情绪和判断方向。'),
    },
    {
      eyebrow: '外',
      title: elementTitles[current.primaryElementName]?.outer ?? '外在呈现出可感的气质',
      paragraphs: splitResultParagraphs(current, current.humanText || '你面对外部世界时，会在关系、目标和行动之间寻找自己的节奏。'),
    },
  ];
});

const stuckParagraphs = computed(() => (
  result.value ? splitResultParagraphs(result.value, result.value.strengthText) : []
));

function splitResultParagraphs(current: ResultDetail, text?: string) {
  const paragraphs = (text || '')
    .split(/\n\s*\n/)
    .map((paragraph) => sanitizeResultText(current, paragraph.trim()))
    .filter(Boolean)
    .flatMap((paragraph) => splitLongParagraph(paragraph));
  return paragraphs.length ? paragraphs : ['这一部分暂时没有足够信息展开，会在重新生成结果后补充。'];
}

function splitLongParagraph(paragraph: string) {
  if (paragraph.length <= 118) {
    return [paragraph];
  }
  const sentences = paragraph.match(/[^。！？；]+[。！？；]?/g) ?? [paragraph];
  const chunks: string[] = [];
  let current = '';
  for (const sentence of sentences) {
    const next = current ? current + sentence : sentence;
    if (current && next.length > 112) {
      chunks.push(current);
      current = sentence;
    } else {
      current = next;
    }
  }
  if (current) {
    chunks.push(current);
  }
  return chunks;
}

function dayMasterProfile(current: ResultDetail) {
  const explicit = current.dayMasterText.match(/日主核心是([甲乙丙丁戊己庚辛壬癸][金木水火土])/);
  const stem = explicit?.[1]?.slice(0, 1) ?? current.dayMasterText.match(/天干「([甲乙丙丁戊己庚辛壬癸])」/)?.[1];
  return stem ? stemProfiles[stem] : undefined;
}

const coreSectionTitle = computed(() => {
  if (!result.value) {
    return '星曜取象：结构正在生成';
  }
  return result.value.structureTitle || `${result.value.starToneName || result.value.personaLabel}：${result.value.primaryElementName}气为主，${result.value.secondaryElementName}气成形`;
});

const starTitle = computed(() => {
  if (!result.value) {
    return '星官：传统意象锚点';
  }
  return starTitleRegistry[result.value.starOfficerCode] ?? `${result.value.starOfficerName}：传统意象锚点`;
});

const starTermHint = computed(() => {
  if (!result.value) {
    return termGlossary.starOfficer;
  }
  if (result.value.starOfficerName === '虚宿') {
    return `${termGlossary.starOfficer}${termGlossary.xuXiu}`;
  }
  return `${result.value.starOfficerName}：传统星宿文化里的意象锚点，用来增加画面和记忆，仅作文化参考。`;
});

const expressionTitle = computed(() => {
  if (!result.value) {
    return '心里有潮，外在有岸';
  }
  if (result.value.primaryElementName === '水' && result.value.secondaryElementName === '土') {
    return '心里有潮，外在有岸';
  }
  return '内在有路，外在有形';
});

const stuckTitle = computed(() => {
  if (!result.value) {
    return '想得太深，出口太少';
  }
  return elementTitles[result.value.primaryElementName]?.stuck ?? '反应太满，出口要清';
});

const growthAdvice = computed(() => {
  const advice = result.value?.growthAdvice ?? [];
  return advice.length ? advice : fallbackAdvice;
});

onMounted(async () => {
  resetResultScroll();
  try {
    result.value = await fetchResult(String(route.params.resultId));
  } catch (err) {
    error.value = err instanceof Error ? err.message : '结果加载失败';
  } finally {
    loading.value = false;
    await resetResultScrollAfterRender();
  }
});

function resetResultScroll() {
  window.scrollTo({ left: 0, top: 0 });
}

async function resetResultScrollAfterRender() {
  await nextTick();
  await new Promise<void>((resolve) => {
    window.requestAnimationFrame(() => {
      resetResultScroll();
      resolve();
    });
  });
}

function copied() {
  if (result.value) {
    track('SHORT_LINK_COPY', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  }
}

function downloadShareImage() {
  if (!result.value) {
    return;
  }
  shareImageStatus.value = '';
  try {
    downloadResultShareCard(result.value);
    shareImageStatus.value = '分享图已生成';
    track('SAVE_SHARE_IMAGE_SUCCESS', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  } catch (err) {
    shareImageStatus.value = err instanceof Error ? err.message : '分享图生成失败';
  }
}

function retake() {
  if (result.value) {
    track('RETAKE_TEST_CLICK', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  }
}

function sharedLandingStart() {
  if (result.value) {
    track('SHARED_RESULT_CTA_CLICK', `/result/${result.value.resultId}`, result.value.resultId, result.value.shortCode);
  }
}
</script>

<template>
  <main class="page result-page">
    <section class="shell stack result-shell">
      <section v-if="loading" class="result-state-card" role="status" aria-live="polite">
        <p class="eyebrow">正在展开你的五行人格卡</p>
        <div class="loading-mark" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <h2>结果马上就好</h2>
        <p class="muted">正在读取人格身份、五行比例和专属分享链接。</p>
        <div class="skeleton-lines" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </section>
      <section v-else-if="error" class="result-state-card error-state" role="alert" aria-live="polite">
        <p class="eyebrow">这张人格卡暂时打不开</p>
        <h2>可能是链接失效，或结果还没有生成完成</h2>
        <p class="muted">{{ error }}</p>
        <div class="actions">
          <RouterLink class="button-link" to="/test">重新测一张</RouterLink>
          <RouterLink class="button-link secondary" to="/">返回首页</RouterLink>
        </div>
      </section>
      <template v-else-if="result">
        <section v-if="sharedEntry" class="shared-entry-banner" aria-label="分享来源提示">
          <div>
            <span>朋友分享给你的五行人格卡</span>
            <strong>看看这张卡像不像 TA，也可以顺手测一张自己的。</strong>
          </div>
          <RouterLink
            class="button-link primary-cta"
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-banner', matchCode: result.shortCode } }"
            @click="sharedLandingStart"
          >
            我也测一张
          </RouterLink>
        </section>

        <div class="panel result-hero-panel">
          <PersonaCard :result="result" />
        </div>

        <section class="panel interpretation-panel structured-analysis-panel" aria-label="核心结构">
          <div class="interpretation-head">
            <p class="eyebrow">核心结构</p>
            <h2>{{ coreSectionTitle }}</h2>
            <p v-if="result.starToneExplanation" class="interpretation-summary">{{ result.starToneExplanation }}</p>
          </div>
          <div class="analysis-flow">
            <article v-for="(section, index) in coreBlocks" :key="section.eyebrow" class="analysis-card">
              <span class="analysis-order">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="analysis-card-main">
                <p class="analysis-kicker">{{ section.eyebrow }}</p>
                <h3>{{ section.title }}</h3>
                <p v-if="section.termHint" class="term-hint">{{ section.termHint }}</p>
                <div class="analysis-body">
                  <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
                </div>
                <blockquote v-if="section.quote" class="quote-block">
                  <p>「{{ section.quote.quote }}」</p>
                  <footer>{{ section.quote.source }}</footer>
                  <span>{{ section.quote.explain }}</span>
                </blockquote>
              </div>
            </article>
          </div>
        </section>

        <section class="identity-statement star-statement" :data-element="result.primaryElement">
          <p class="eyebrow">星官</p>
          <h2>{{ starTitle }}</h2>
          <p class="term-hint star-term-hint">{{ starTermHint }}</p>
          <div class="identity-body">
            <p v-for="paragraph in starParagraphs" :key="paragraph" class="identity-copy">{{ paragraph }}</p>
          </div>
        </section>

        <section class="panel interpretation-panel expression-panel" aria-label="内外表现">
          <div class="interpretation-head">
            <p class="eyebrow">内外</p>
            <h2>{{ expressionTitle }}</h2>
          </div>
          <div class="section-split-grid">
            <article v-for="section in expressionBlocks" :key="section.eyebrow" class="analysis-card expression-card">
              <span class="analysis-order">{{ section.eyebrow }}</span>
              <div class="analysis-card-main">
                <h3>{{ section.title }}</h3>
                <div class="analysis-body">
                  <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="panel growth-panel" aria-label="成长建议">
          <div class="growth-head">
            <p class="eyebrow">成长</p>
            <h2>把敏锐变成稳定输出</h2>
          </div>
          <article class="analysis-card stuck-card">
            <span class="analysis-order">卡</span>
            <div class="analysis-card-main">
              <h3>{{ stuckTitle }}</h3>
              <div class="analysis-body">
                <p v-for="paragraph in stuckParagraphs" :key="paragraph">{{ paragraph }}</p>
              </div>
            </div>
          </article>
          <div class="advice-grid">
            <article v-for="(advice, index) in growthAdvice" :key="advice.title">
              <span>{{ index + 1 }}</span>
              <div>
                <h3>{{ advice.title }}</h3>
                <p>{{ advice.text }}</p>
              </div>
            </article>
          </div>
        </section>

        <ShareLinkBox
          v-if="!sharedEntry"
          :result-id="result.resultId"
          :short-code="result.shortCode"
          :short-url="result.shortUrl"
          show-save-image
          @copied="copied"
          @save-image="downloadShareImage"
          show-retake
          @retake="retake"
        />

        <section v-else class="panel shared-bottom-cta" aria-label="分享结果底部行动">
          <div>
            <span>想看看你们合不合拍？</span>
            <strong>测完自己的卡，继续和这张短码做双人匹配。</strong>
          </div>
          <RouterLink
            class="button-link primary-cta"
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-footer', matchCode: result.shortCode } }"
            @click="sharedLandingStart"
          >
            生成我的人格卡
          </RouterLink>
        </section>

        <p v-if="shareImageStatus" class="muted" role="status" aria-live="polite">{{ shareImageStatus }}</p>

      </template>
    </section>
  </main>
</template>

<style scoped>
.result-page {
  position: relative;
  overflow-x: hidden;
  background:
    linear-gradient(180deg, rgba(255, 251, 243, 0.98) 0%, rgba(247, 239, 226, 0.96) 52%, rgba(239, 228, 207, 0.98) 100%),
    var(--color-paper);
}

.result-page::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
}

.result-page::before {
  height: 170px;
  background: rgba(47, 98, 85, 0.1);
  clip-path: ellipse(76% 47% at 78% 100%);
}

.result-shell {
  position: relative;
  z-index: 1;
}

.shared-entry-banner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(47, 98, 85, 0.18);
  border-radius: 8px;
  padding: 16px;
  background: rgba(245, 238, 225, 0.9);
  color: var(--color-ink);
  box-shadow: var(--shadow-paper);
}

.shared-entry-banner span,
.shared-entry-banner strong {
  display: block;
}

.shared-entry-banner span {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 900;
}

.shared-entry-banner strong {
  margin-top: 4px;
  font-size: 16px;
}

.result-state-card {
  display: grid;
  gap: 14px;
  overflow: hidden;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.94), rgba(248, 240, 226, 0.9)),
    linear-gradient(90deg, rgba(201, 111, 61, 0.12), transparent);
  box-shadow: var(--shadow-paper);
}

.result-state-card h2 {
  max-width: 560px;
  margin: 0;
  font-size: 30px;
}

.result-state-card p {
  margin: 0;
}

.loading-mark {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 28px;
}

.loading-mark span {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--color-warm);
  animation: resultPulse 1.15s ease-in-out infinite;
}

.loading-mark span:nth-child(2) {
  animation-delay: 0.16s;
}

.loading-mark span:nth-child(3) {
  animation-delay: 0.32s;
}

.skeleton-lines {
  display: grid;
  gap: 10px;
  margin-top: 6px;
}

.skeleton-lines span {
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(47, 98, 85, 0.12), rgba(201, 111, 61, 0.18), rgba(47, 98, 85, 0.12));
}

.skeleton-lines span:nth-child(1) {
  width: min(100%, 520px);
}

.skeleton-lines span:nth-child(2) {
  width: min(86%, 430px);
}

.skeleton-lines span:nth-child(3) {
  width: min(64%, 320px);
}

.error-state {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 240, 237, 0.92)),
    radial-gradient(circle at 88% 18%, rgba(157, 57, 41, 0.12), transparent 34%);
}

.error-state .eyebrow {
  color: #9d3929;
}

.result-action-strip,
.shared-bottom-cta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  border-color: rgba(47, 98, 85, 0.14);
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.94), rgba(241, 232, 215, 0.88)),
    linear-gradient(90deg, rgba(47, 98, 85, 0.08), rgba(201, 111, 61, 0.08));
}

.result-action-strip span,
.shared-bottom-cta span {
  display: block;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 950;
}

.result-action-strip strong,
.shared-bottom-cta strong {
  display: block;
  margin-top: 5px;
  color: var(--color-ink);
  font-size: 20px;
  line-height: 1.35;
}

.result-action-strip p {
  max-width: 620px;
  margin: 6px 0 0;
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 760;
  line-height: 1.6;
}

.result-action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.result-action-buttons .primary-action,
.shared-bottom-cta .primary-cta {
  min-width: 144px;
  min-height: 44px;
  border: 1px solid rgba(158, 79, 46, 0.24);
  border-radius: 8px;
  background: linear-gradient(135deg, var(--color-warm), var(--color-warm-deep));
  color: #fff;
  font-size: 14px;
  font-weight: 950;
  box-shadow: 0 12px 24px rgba(157, 86, 49, 0.16);
}

.result-action-buttons .compact-action {
  min-width: 96px;
  min-height: 44px;
  padding: 0 14px;
  font-size: 14px;
}

.identity-statement {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 9px;
  border-radius: 8px;
  padding: 22px 24px;
  border: 1px solid rgba(230, 202, 153, 0.32);
  background:
    radial-gradient(circle at 84% -44px, rgba(255, 238, 202, 0.15), transparent 38%),
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--identity-primary-color, var(--color-primary)) 78%, #18231f) 0%,
      color-mix(in srgb, var(--identity-primary-color, var(--color-primary)) 55%, #101a18) 58%,
      color-mix(in srgb, var(--identity-primary-color, var(--color-primary)) 36%, #07110f) 100%
    );
  color: #fff;
  box-shadow:
    inset 0 0 0 1px rgba(255, 248, 232, 0.14),
    inset 0 -24px 46px rgba(0, 0, 0, 0.13),
    0 18px 34px color-mix(in srgb, var(--identity-primary-color, var(--color-primary)) 18%, transparent);
}

.identity-statement::before {
  content: "";
  position: absolute;
  inset: 8px;
  z-index: 0;
  pointer-events: none;
  border: 1px solid rgba(255, 235, 188, 0.18);
  border-radius: 6px;
  background:
    linear-gradient(90deg, transparent 0 8%, rgba(255, 232, 184, 0.36) 8% 27%, transparent 27% 73%, rgba(255, 232, 184, 0.36) 73% 92%, transparent 92%) top / 100% 1px,
    linear-gradient(90deg, transparent 0 18%, rgba(255, 248, 232, 0.16) 18% 82%, transparent 82%) bottom / 100% 1px;
  background-repeat: no-repeat;
}

.identity-statement::after {
  content: "";
  position: absolute;
  right: 20px;
  bottom: 18px;
  z-index: 0;
  width: 58px;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 218, 171, 0.18), rgba(255, 218, 171, 0.72));
}

.identity-statement > * {
  position: relative;
  z-index: 1;
}

.identity-statement .eyebrow {
  color: #ffd6a7;
}

.identity-statement h2 {
  max-width: 780px;
  margin: 0;
  color: #fff;
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 650;
  line-height: 1.25;
}

.identity-statement p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
}

.identity-statement .identity-copy {
  max-width: min(100%, 30em);
  line-height: 1.68;
}

.identity-body {
  display: grid;
  gap: 10px;
  justify-items: center;
}

.identity-body .identity-copy {
  margin: 0;
}

.resonance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.resonance-intro {
  max-width: 760px;
  margin: 6px 0 0;
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 760;
  line-height: 1.68;
}

.resonance-grid article {
  display: grid;
  gap: 10px;
  min-height: 138px;
  border: 1px solid rgba(47, 98, 85, 0.14);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 252, 245, 0.72);
}

.resonance-grid span {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 900;
}

.resonance-grid strong {
  display: block;
  margin-top: -4px;
  color: var(--color-ink);
  font-size: 18px;
  font-weight: 900;
  line-height: 1.25;
}

.resonance-grid p {
  margin: 0;
  color: var(--color-ink);
  font-size: 15px;
  font-weight: 750;
  line-height: 1.7;
}

.interpretation-panel {
  display: grid;
  gap: 16px;
  overflow: hidden;
  border-color: rgba(47, 98, 85, 0.12);
  background:
    radial-gradient(circle at 92% 8%, rgba(47, 98, 85, 0.07), transparent 26%),
    linear-gradient(135deg, rgba(255, 252, 245, 0.88), rgba(248, 240, 226, 0.78)),
    rgba(255, 252, 245, 0.78);
}

.interpretation-head {
  display: grid;
  gap: 5px;
  border-bottom: 1px solid rgba(47, 98, 85, 0.1);
  padding-bottom: 12px;
}

.interpretation-head .eyebrow,
.interpretation-head h2 {
  margin: 0;
}

.interpretation-head p:not(.eyebrow) {
  max-width: 660px;
  margin: 0;
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 760;
  line-height: 1.7;
}

.structured-analysis-panel {
  gap: 18px;
}

.analysis-flow {
  display: grid;
  gap: 14px;
}

.section-split-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.expression-card {
  min-height: 100%;
  align-content: start;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.84), rgba(242, 232, 214, 0.68));
}

.expression-panel {
  background:
    radial-gradient(circle at 0% 0%, rgba(47, 98, 85, 0.08), transparent 30%),
    linear-gradient(135deg, rgba(255, 252, 245, 0.88), rgba(244, 236, 222, 0.82));
}

.star-statement {
  border-color: rgba(230, 202, 153, 0.34);
}

.star-statement h2 {
  font-size: 30px;
}

.analysis-card {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-content: start;
  gap: 12px;
  border: 1px solid rgba(47, 98, 85, 0.13);
  border-radius: 8px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.84), rgba(247, 238, 222, 0.72)),
    rgba(255, 252, 245, 0.72);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.66),
    0 8px 16px rgba(58, 48, 34, 0.035);
}

.analysis-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 0 999px 999px 0;
  background: rgba(201, 111, 61, 0.38);
}

.analysis-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: rgba(201, 111, 61, 0.08);
  color: var(--color-warm-deep);
  font-size: 11px;
  font-weight: 950;
}

.analysis-card-main {
  display: grid;
  gap: 8px;
}

.analysis-kicker {
  margin: 0;
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 900;
}

.analysis-card h3 {
  margin: 0;
  color: var(--color-ink);
  font-size: 18px;
  line-height: 1.35;
}

.analysis-body {
  display: grid;
  gap: 10px;
}

.analysis-body p {
  margin: 0;
  color: #43524d;
  font-size: 14px;
  line-height: 1.74;
}

.term-hint {
  margin: 0;
  border-left: 2px solid rgba(201, 111, 61, 0.26);
  border-radius: 0 8px 8px 0;
  padding: 7px 10px;
  background: rgba(246, 233, 214, 0.46);
  color: #68736d;
  font-size: 12px;
  font-weight: 720;
  line-height: 1.62;
}

.quote-block {
  display: grid;
  gap: 5px;
  margin: 2px 0 0;
  border-left: 3px solid rgba(47, 98, 85, 0.3);
  border-radius: 0 8px 8px 0;
  padding: 10px 12px;
  background:
    linear-gradient(135deg, rgba(239, 231, 214, 0.56), rgba(255, 252, 245, 0.62)),
    rgba(255, 252, 245, 0.66);
}

.quote-block p,
.quote-block footer,
.quote-block span {
  margin: 0;
}

.quote-block p {
  color: var(--color-ink);
  font-family: var(--font-serif);
  font-size: 17px;
  line-height: 1.42;
}

.quote-block footer {
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 900;
}

.quote-block span {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 720;
  line-height: 1.62;
}

.star-term-hint {
  justify-self: center;
  max-width: min(100%, 32em);
  border-color: rgba(255, 218, 171, 0.32);
  background: rgba(255, 248, 232, 0.08);
  color: rgba(255, 248, 232, 0.7);
}

.growth-panel {
  display: grid;
  gap: 16px;
  overflow: hidden;
  border-color: rgba(201, 111, 61, 0.14);
  background:
    radial-gradient(circle at 100% 0%, rgba(201, 111, 61, 0.09), transparent 28%),
    linear-gradient(135deg, rgba(255, 252, 245, 0.9), rgba(247, 237, 221, 0.82)),
    rgba(255, 252, 245, 0.78);
}

.stuck-card {
  border-color: rgba(201, 111, 61, 0.16);
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.86), rgba(245, 232, 213, 0.78)),
    rgba(255, 252, 245, 0.72);
}

.growth-head {
  display: grid;
  gap: 5px;
  border-bottom: 1px solid rgba(201, 111, 61, 0.11);
  padding-bottom: 12px;
}

.growth-head .eyebrow,
.growth-head h2,
.growth-head p {
  margin: 0;
}

.growth-head p {
  max-width: 660px;
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 760;
  line-height: 1.7;
}

.advice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.advice-grid article {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  min-height: 116px;
  border: 1px solid rgba(201, 111, 61, 0.14);
  border-radius: 8px;
  padding: 14px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.86), rgba(246, 236, 218, 0.72));
}

.advice-grid article::after {
  content: "";
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 9px;
  height: 1px;
  background: linear-gradient(90deg, rgba(201, 111, 61, 0.2), transparent);
}

.advice-grid span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background:
    linear-gradient(135deg, var(--color-primary), #9b613a);
  color: #fffaf0;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 7px 13px rgba(47, 98, 85, 0.12);
}

.advice-grid h3 {
  margin: 0 0 6px;
  color: var(--color-ink);
  font-size: 16px;
}

.advice-grid p {
  margin: 0;
  color: #43524d;
  font-size: 14px;
  font-weight: 720;
  line-height: 1.72;
}

@media (max-width: 760px) {
  .result-page {
    padding: 16px 14px;
  }

  .result-shell {
    gap: 11px;
  }

  .resonance-grid,
  .section-split-grid,
  .shared-entry-banner,
  .result-action-strip,
  .shared-bottom-cta {
    grid-template-columns: 1fr;
  }

  .shared-entry-banner {
    gap: 10px;
    padding: 12px;
  }

  .shared-entry-banner strong {
    font-size: 14px;
    line-height: 1.45;
  }

  .result-hero-panel,
  .spectrum-panel,
  .structured-analysis-panel,
  .growth-panel,
  .resonance-panel,
  .shared-bottom-cta {
    padding: 14px;
  }

  .identity-statement h2 {
    font-size: 28px;
    line-height: 1.24;
  }

  .identity-statement {
    gap: 7px;
    padding: 16px;
  }

  .identity-statement p {
    font-size: 13px;
    line-height: 1.55;
  }

  .identity-statement .identity-copy {
    justify-self: center;
    max-width: min(100%, 22em);
    line-height: 1.62;
  }

  .interpretation-panel,
  .growth-panel {
    gap: 12px;
  }

  .interpretation-head p:not(.eyebrow),
  .growth-head p {
    font-size: 13px;
    line-height: 1.5;
  }

  .interpretation-grid {
    grid-template-columns: 1fr;
  }

  .analysis-card {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 8px;
    padding: 12px 11px;
  }

  .analysis-order {
    width: 30px;
    height: 30px;
    font-size: 11px;
  }

  .analysis-card-main {
    gap: 6px;
  }

  .analysis-card h3 {
    font-size: 16px;
    line-height: 1.32;
  }

  .analysis-body {
    gap: 8px;
  }

  .analysis-body p {
    font-size: 13px;
    line-height: 1.68;
  }

  .term-hint {
    padding: 6px 9px;
    font-size: 11px;
    line-height: 1.55;
  }

  .quote-block {
    padding: 8px 10px;
  }

  .quote-block p {
    font-size: 15px;
  }

  .quote-block span {
    font-size: 12px;
    line-height: 1.55;
  }

  .resonance-panel {
    gap: 10px;
  }

  .resonance-intro {
    margin-top: 5px;
    font-size: 12px;
    line-height: 1.5;
  }

  .resonance-grid article {
    min-height: auto;
    gap: 6px;
    padding: 11px 12px;
  }

  .resonance-grid span {
    font-size: 12px;
  }

  .resonance-grid strong {
    font-size: 15px;
  }

  .resonance-grid p {
    font-size: 13px;
    line-height: 1.52;
  }

  .advice-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .advice-grid article {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 8px;
    min-height: 0;
    padding: 11px;
  }

  .advice-grid span {
    width: 24px;
    height: 24px;
    font-size: 11px;
  }

  .advice-grid h3 {
    margin-bottom: 4px;
    font-size: 14px;
    line-height: 1.25;
  }

  .advice-grid p {
    font-size: 12px;
    line-height: 1.5;
  }

  .result-action-strip strong,
  .shared-bottom-cta strong {
    font-size: 16px;
    line-height: 1.35;
  }

  .result-action-buttons {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    justify-content: stretch;
  }

  .result-action-buttons .primary-action {
    grid-column: 1 / -1;
  }

  .result-action-buttons .compact-action,
  .shared-bottom-cta .primary-cta {
    width: 100%;
  }

}

@keyframes resultPulse {
  0%,
  100% {
    opacity: 0.32;
    transform: translateY(0);
  }

  50% {
    opacity: 1;
    transform: translateY(-4px);
  }
}
</style>
