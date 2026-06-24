<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchResult } from '../api/results';
import type { ResultDetail } from '../api/types';
import ElementSpectrum from '../components/ElementSpectrum.vue';
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

interface GrowthAdvice {
  title: string;
  text: string;
}

interface ElementProfile {
  label: string;
  image: string;
  core: string;
  action: string;
  pressure: string;
  role: string;
  advice: GrowthAdvice[];
}

interface AnalysisSection {
  order: string;
  kicker: string;
  title: string;
  paragraphs: string[];
}

const elementProfiles: Record<string, ElementProfile> = {
  METAL: {
    label: '清醒型的理性行动者',
    image: '金像经过淬炼的器物和清晰的边界，代表判断、秩序、执行和标准感。',
    core: '你通常不是靠情绪推进的人，而是会先分辨重点、确认规则，再决定怎么行动。',
    action: '当目标和边界清楚时，你会展现出很强的完成欲，也更愿意把事情做到有标准、有结果。',
    pressure: '压力大时，你容易把标准拉得太高，反复校准细节，反而让自己迟迟不满意。',
    role: '金会增强你的归纳、判断和边界感，让你更容易看见规则、重点和逻辑漏洞。',
    advice: [
      { title: '把标准变成步骤', text: '不要只在脑子里判断好坏，把标准拆成可执行清单，你会推进得更轻松。' },
      { title: '允许先完成一版', text: '先交付一个可修改版本，再继续打磨，会比一直等到完美更适合你。' },
      { title: '把判断说出来', text: '你看到的问题和边界感很有价值，表达出来才能变成团队或关系里的帮助。' },
      { title: '给自己留一点弹性', text: '不是每件事都需要满分，保留回旋空间会让你的清醒更有温度。' },
    ],
  },
  WOOD: {
    label: '生长型的长期规划者',
    image: '木像持续向上的枝条，代表生长、规划、表达和创造。',
    core: '你更容易从未来可能性里获得动力，喜欢看到事情一点点展开、变清晰、变成自己的路径。',
    action: '当你相信一件事值得长期投入时，会愿意积累、复盘、调整，并把想法慢慢养成成果。',
    pressure: '压力大时，你容易被太多方向拉扯，想同时生长很多枝条，导致节奏变散。',
    role: '木会带来表达、生长和长期规划，让你的想法不只是停留在脑内，而是逐渐长成作品和能力。',
    advice: [
      { title: '给目标设阶段', text: '把长期目标拆成三到五个阶段，你会更容易看到自己正在成长。' },
      { title: '减少同时开太多线', text: '选择一条主线先养起来，比同时追很多方向更能积累势能。' },
      { title: '多做公开表达', text: '写总结、讲给别人听、做作品集，会让你的规划力被看见。' },
      { title: '保留稳定节奏', text: '灵感之外还需要固定训练，节奏会让你的成长更扎实。' },
    ],
  },
  WATER: {
    label: '深水型的理性行动者',
    image: '水像雨露、细流和雾气，代表细腻、敏感、观察、学习和适应。',
    core: '你通常不是一上来就强硬推进的人，而是会先观察局面、捕捉细节、理解逻辑，再决定怎么行动。',
    action: '你对信息、氛围和人的状态比较敏感，理解力强、学习吸收快，也擅长处理复杂线索。',
    pressure: '压力大时，你容易想得太多，把自己困在反复推演里。',
    role: '水会增强你的感知、学习和适应能力，让你更容易理解复杂信息，也更能捕捉环境变化。',
    advice: [
      { title: '把想明白变成先做一版', text: '你不需要等到完全确定才开始，小步试错会比一直推演更适合你。' },
      { title: '用固定节奏增强稳定感', text: '固定复盘、固定训练、固定输出，会让你的敏锐变成长期成果。' },
      { title: '多做表达输出', text: '写总结、讲给别人听、做文档，能把复杂思考整理成可复用能力。' },
      { title: '压力大时先结构化', text: '先写下事实、原因、下一步，你会更快从内耗回到行动。' },
    ],
  },
  FIRE: {
    label: '点火型的表达行动者',
    image: '火像一束被点亮的光，代表热情、行动、表达、目标感和感染力。',
    core: '你更容易被清晰目标、现场反馈和强烈兴趣点燃，一旦进入状态，会有明显的投入感。',
    action: '你适合把想法推到台前，用表达、行动和反馈来确认方向。',
    pressure: '压力大时，你可能会急着证明结果，或者在情绪被点燃后快速消耗能量。',
    role: '火会给你目标感和行动欲，让想法不只停留在判断里，而是被点燃、被表达、被推进。',
    advice: [
      { title: '先定一个清晰目标', text: '目标越明确，你越容易进入行动状态，也越不容易被杂事消耗。' },
      { title: '把热情接到节奏上', text: '热情负责启动，节奏负责完成，两者结合会让你更稳定。' },
      { title: '表达前先收束重点', text: '先写下要传达的三件事，你的感染力会更聚焦。' },
      { title: '给自己留恢复时间', text: '高投入之后需要主动降温，才能避免短时间内过度透支。' },
    ],
  },
  EARTH: {
    label: '厚土型的稳定承载者',
    image: '土像稳稳托住局面的地面，代表稳定、承载、责任、秩序和安全感。',
    core: '你更容易关注事情能不能落地、关系能不能稳定、结果能不能长期维持。',
    action: '你不是只看一时兴奋的人，更适合在稳定节奏里慢慢做出可靠成果。',
    pressure: '压力大时，你可能会过度承担，或者为了稳定而把变化推迟太久。',
    role: '土会给你责任感、秩序感和承载力，让你的想法更容易被固定成计划和结果。',
    advice: [
      { title: '把责任分清楚', text: '你可以承担，但不需要替所有人兜底，边界清楚会让稳定更健康。' },
      { title: '给变化留一个入口', text: '在计划里预留调整空间，会让你的稳不变成僵。' },
      { title: '用复盘沉淀经验', text: '你适合把每次完成的事复盘成方法，下次会越来越稳。' },
      { title: '及时表达自己的需要', text: '不要只照顾整体，也要让别人知道你需要什么支持。' },
    ],
  },
};

const fallbackProfile: ElementProfile = {
  label: '平衡型的自我观察者',
  image: '你的结构里有多种元素共同作用，代表观察、平衡、适应和持续调整。',
  core: '你更适合在真实情境里理解自己，而不是被单一标签完全定义。',
  action: '当目标清楚、节奏稳定时，你会更容易把想法变成行动。',
  pressure: '压力大时，先把问题拆开，会比直接给自己下结论更有效。',
  role: '这一元素会给你的性格增加一层调和力，让整体表达更有弹性。',
  advice: [
    { title: '先确定主线', text: '当方向太多时，先选一条最重要的线推进。' },
    { title: '固定复盘节奏', text: '用固定复盘把经验沉淀下来，能减少反复摇摆。' },
    { title: '保留行动版本', text: '先做出一个小版本，再根据反馈调整。' },
    { title: '把感受说清楚', text: '表达真实需求，会让关系和合作更稳定。' },
  ],
};

function profileOf(elementCode: string) {
  return elementProfiles[elementCode] ?? fallbackProfile;
}

const primaryProfile = computed(() => (result.value ? profileOf(result.value.primaryElement) : fallbackProfile));
const secondaryProfile = computed(() => (result.value ? profileOf(result.value.secondaryElement) : fallbackProfile));

const personaLine = computed(() => {
  if (!result.value) {
    return '';
  }
  return `你像一张以${result.value.primaryElementName}为底色、由${result.value.secondaryElementName}来校准节奏的人格卡，习惯先抓住自己的主线，再把想法慢慢落成行动。`;
});

const behaviorSignals = computed(() => {
  if (!result.value) {
    return [];
  }
  const [primaryTrait = '有主见', orderTrait = '有节奏', judgmentTrait = '会判断', secondaryTrait = '能调和', starTrait = '有辨识度'] = result.value.keywords;
  return [
    {
      label: '做决定时',
      text: `你更容易先抓住${primaryTrait}和${judgmentTrait}，不太喜欢长期停在含糊状态。`,
    },
    {
      label: '推进事情时',
      text: `你会用${orderTrait}维持自己的步调，也会留下${secondaryTrait}的回旋空间。`,
    },
    {
      label: '和人相处时',
      text: `${result.value.starOfficerName}让你带着${starTrait}，既有个人风格，也愿意照顾关系里的感受。`,
    },
  ];
});

const analysisSections = computed<AnalysisSection[]>(() => {
  if (!result.value) {
    return [];
  }
  return [
    {
      order: '01',
      kicker: '日主核心',
      title: `你的核心底色偏「${result.value.primaryElementName}」`,
      paragraphs: [
        primaryProfile.value.image,
        primaryProfile.value.core,
        `${primaryProfile.value.action} ${primaryProfile.value.pressure}`,
      ],
    },
    {
      order: '02',
      kicker: '五行互动',
      title: `${result.value.primaryElementName}是主线，${result.value.secondaryElementName}负责补充和校准`,
      paragraphs: [
        `你的结构里，${result.value.primaryElementName}的存在感最明显，所以它决定了你面对事情时的第一反应。`,
        secondaryProfile.value.role,
        result.value.relationshipText,
      ],
    },
    {
      order: '03',
      kicker: '人格推测',
      title: `你更像一个「${primaryProfile.value.label}」`,
      paragraphs: [
        `你擅长使用${result.value.keywords.slice(0, 3).join('、')}这些能力来理解问题，也会在熟悉的节奏里慢慢展现稳定的投入感。`,
        '你不一定总是第一时间外放，但当目标足够清晰、环境足够可信时，会更愿意把自己的判断、行动和责任感拿出来。',
        '压力场景里，你需要避免只在脑子里反复推演。把问题写下来、拆成步骤，会让你更快回到行动状态。',
      ],
    },
  ];
});

const growthAdvice = computed(() => primaryProfile.value.advice);

onMounted(async () => {
  try {
    result.value = await fetchResult(String(route.params.resultId));
  } catch (err) {
    error.value = err instanceof Error ? err.message : '结果加载失败';
  } finally {
    loading.value = false;
  }
});

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
            <strong>先看看这张卡像不像 TA，也可以顺手测一张自己的。</strong>
          </div>
          <RouterLink
            class="button-link primary-cta"
            :to="{ path: '/test', query: { channel: 'shared-result', campaign: 'result-banner', matchCode: result.shortCode } }"
            @click="sharedLandingStart"
          >
            我也测一张
          </RouterLink>
        </section>

        <div class="panel">
          <PersonaCard :result="result" />
        </div>

        <section class="result-data-strip" aria-label="人格核心数据">
          <article>
            <span>主五行</span>
            <strong>{{ result.primaryElementName }} {{ result.primaryPercent }}%</strong>
          </article>
          <article>
            <span>副五行</span>
            <strong>{{ result.secondaryElementName }} {{ result.secondaryPercent }}%</strong>
          </article>
          <article>
            <span>人格短码</span>
            <strong>{{ result.shortCode }}</strong>
          </article>
        </section>

        <section class="identity-statement">
          <p class="eyebrow">一句话人格标签</p>
          <h2>{{ primaryProfile.label }}</h2>
          <p>{{ personaLine }}</p>
        </section>

        <section class="panel interpretation-panel structured-analysis-panel" aria-label="人格分析">
          <div class="interpretation-head">
            <p class="eyebrow">人格分析</p>
            <h2>从日主核心到五行互动</h2>
            <p>这部分不讲命运好坏，只把你的主五行、副五行和选择倾向翻译成性格语言。</p>
          </div>
          <div class="analysis-flow">
            <article v-for="section in analysisSections" :key="section.kicker" class="analysis-card">
              <span class="analysis-order">{{ section.order }}</span>
              <div>
                <p class="analysis-kicker">{{ section.kicker }}</p>
                <h3>{{ section.title }}</h3>
                <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
              </div>
            </article>
          </div>
        </section>

        <section class="panel stack resonance-panel">
          <div>
            <p class="eyebrow">人格推测</p>
            <h2>朋友最容易认出的三个表现</h2>
          </div>
          <div class="resonance-grid">
            <article v-for="signal in behaviorSignals" :key="signal.label">
              <span>{{ signal.label }}</span>
              <p>{{ signal.text }}</p>
            </article>
          </div>
        </section>

        <section class="panel growth-panel" aria-label="成长建议">
          <div class="growth-head">
            <p class="eyebrow">成长建议</p>
            <h2>把人格结构用起来</h2>
            <p>建议尽量具体，不做空泛判断，只帮助你把优势变成可持续的行动方式。</p>
          </div>
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

        <div class="panel">
          <ElementSpectrum :scores="result.allElementScores" />
        </div>

        <ShareLinkBox
          v-if="!sharedEntry"
          :result-id="result.resultId"
          :short-code="result.shortCode"
          :short-url="result.shortUrl"
          show-save-image
          @copied="copied"
          @save-image="downloadShareImage"
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

        <RouterLink v-if="!sharedEntry" class="button-link secondary result-retake-link" to="/test" @click="retake">重新测试</RouterLink>
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

.result-data-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.result-data-strip article {
  display: grid;
  gap: 6px;
  min-height: 86px;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 14px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.92), rgba(248, 240, 226, 0.82));
  box-shadow: 0 10px 24px rgba(49, 44, 35, 0.06);
}

.result-data-strip span {
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 900;
}

.result-data-strip strong {
  color: var(--color-ink);
  font-size: 22px;
  font-weight: 900;
  line-height: 1.16;
  overflow-wrap: anywhere;
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
  gap: 10px;
  border-radius: 8px;
  padding: 24px;
  border: 1px solid rgba(47, 98, 85, 0.18);
  background:
    linear-gradient(135deg, rgba(47, 98, 85, 0.96), rgba(31, 71, 62, 0.98)),
    var(--color-primary-deep);
  color: #fff;
  box-shadow: 0 18px 34px rgba(31, 71, 62, 0.15);
}

.identity-statement::after {
  content: "";
  position: absolute;
  right: 20px;
  bottom: 18px;
  width: 58px;
  height: 1px;
  background: rgba(255, 218, 171, 0.5);
}

.identity-statement .eyebrow {
  color: #ffd6a7;
}

.identity-statement h2 {
  max-width: 780px;
  margin: 0;
  color: #fff;
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 650;
  line-height: 1.38;
}

.identity-statement p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
}

.resonance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
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
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.84), rgba(248, 240, 226, 0.8)),
    rgba(255, 252, 245, 0.78);
}

.interpretation-head {
  display: grid;
  gap: 4px;
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
  gap: 12px;
}

.analysis-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-content: start;
  gap: 14px;
  border: 1px solid rgba(47, 98, 85, 0.13);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 252, 245, 0.72);
}

.analysis-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: rgba(201, 111, 61, 0.12);
  color: var(--color-warm-deep);
  font-size: 13px;
  font-weight: 950;
}

.analysis-card > div {
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
  font-size: 19px;
  line-height: 1.35;
}

.analysis-card p:not(.analysis-kicker) {
  margin: 0;
  color: #43524d;
  font-size: 14px;
  line-height: 1.78;
}

.growth-panel {
  display: grid;
  gap: 16px;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.88), rgba(247, 237, 221, 0.82)),
    rgba(255, 252, 245, 0.78);
}

.growth-head {
  display: grid;
  gap: 4px;
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
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  min-height: 118px;
  border: 1px solid rgba(201, 111, 61, 0.14);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 252, 245, 0.72);
}

.advice-grid span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 900;
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

.result-retake-link {
  justify-self: center;
  min-width: min(100%, 220px);
}

@media (max-width: 760px) {
  .resonance-grid,
  .result-data-strip,
  .shared-entry-banner,
  .result-action-strip,
  .shared-bottom-cta {
    grid-template-columns: 1fr;
  }

  .identity-statement h2 {
    font-size: 22px;
  }

  .identity-statement {
    padding: 20px;
  }

  .analysis-card,
  .advice-grid,
  .interpretation-grid {
    grid-template-columns: 1fr;
  }

  .analysis-card {
    gap: 10px;
  }

  .resonance-grid article {
    min-height: auto;
    padding: 14px;
  }

  .resonance-grid p {
    font-size: 14px;
  }

  .result-action-strip strong,
  .shared-bottom-cta strong {
    font-size: 18px;
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
