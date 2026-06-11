<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchQuestions } from '../api/questions';
import { createResult } from '../api/results';
import type { Question } from '../api/types';
import QuestionCard from '../components/QuestionCard.vue';
import { track } from '../utils/tracker';

const router = useRouter();
const AUTO_ADVANCE_DELAY_MS = 1100;
const questions = ref<Question[]>([]);
const activeStepIndex = ref(0);
const loading = ref(true);
const submitting = ref(false);
const error = ref('');
const formStarted = ref(false);
const autoAdvanceTimer = ref<number | null>(null);
const autoAdvanceQuestionCode = ref<string | null>(null);
const currentYear = new Date().getFullYear();
const minBirthYear = 1900;
const defaultBirthYear = Math.min(currentYear, 2002);

const form = reactive<{
  birthYear: number | null;
  birthMonth: number | null;
  birthDay: number | null;
  birthTimeRange: string | null;
  answers: Record<string, string>;
}>({
  birthYear: defaultBirthYear,
  birthMonth: null,
  birthDay: null,
  birthTimeRange: null,
  answers: {},
});

const yearDraft = ref(defaultBirthYear);
const yearPickerValue = computed(() => form.birthYear ?? yearDraft.value);
const quickYears = computed(() => [2008, 2005, 2002, 1999, 1996, 1993].filter((year) => year <= currentYear));
const yearScale = computed(() => [minBirthYear, 1970, 1990, 2000, 2010, currentYear]
  .filter((year, index, source) => year <= currentYear && source.indexOf(year) === index));
const monthHints = [
  '水气沉静',
  '木气初生',
  '春林舒展',
  '竹风有序',
  '火气点燃',
  '炎庭推进',
  '土气承接',
  '白露清醒',
  '金桂有界',
  '岩衡稳定',
  '澄夜洞察',
  '雪川包容',
];
const days = Array.from({ length: 31 }, (_, index) => index + 1);
const timeOptions: Array<{ value: string | null; label: string; hint: string }> = [
  { value: null, label: '不透露', hint: '保持神秘' },
  { value: 'MORNING', label: '上午', hint: '清新生长' },
  { value: 'NOON', label: '中午', hint: '明亮行动' },
  { value: 'AFTERNOON', label: '下午', hint: '稳住节奏' },
  { value: 'EVENING', label: '傍晚', hint: '清醒收束' },
  { value: 'NIGHT', label: '夜晚', hint: '安静洞察' },
  { value: 'UNKNOWN', label: '不确定', hint: '随缘即可' },
];
const answeredCount = computed(() => questions.value.filter((question) => form.answers[question.questionCode]).length);
const birthInfoComplete = computed(() => Boolean(form.birthYear && form.birthMonth));
const totalProgressUnits = computed(() => questions.value.length + 1);
const completedProgressUnits = computed(() => answeredCount.value + (birthInfoComplete.value ? 1 : 0));
const progressPercent = computed(() => {
  if (totalProgressUnits.value === 0) {
    return 0;
  }
  return Math.round((completedProgressUnits.value / totalProgressUnits.value) * 100);
});
const displayQuestions = computed<Question[]>(() => questions.value.map((question) => ({
  ...question,
  options: [...question.options].sort((left, right) => optionRank(question.questionCode, left.optionCode) - optionRank(question.questionCode, right.optionCode)),
})));
const isBirthStep = computed(() => activeStepIndex.value === 0);
const canGoPrevious = computed(() => activeStepIndex.value > 0 && !submitting.value);
const activeQuestionIndex = computed(() => Math.max(0, activeStepIndex.value - 1));
const activeQuestion = computed(() => displayQuestions.value[activeQuestionIndex.value]);
const activeQuestionAnswered = computed(() => {
  const question = activeQuestion.value;
  return Boolean(question && form.answers[question.questionCode]);
});
const isLastQuestion = computed(() => activeStepIndex.value === questions.value.length);
const autoAdvancePending = computed(() => Boolean(
  autoAdvanceTimer.value !== null
  && activeQuestion.value
  && autoAdvanceQuestionCode.value === activeQuestion.value.questionCode,
));
const primaryActionText = computed(() => {
  if (submitting.value) {
    return '生成中...';
  }
  if (isBirthStep.value) {
    return birthInfoComplete.value ? '进入第 1 题' : '选择月份后继续';
  }
  return isLastQuestion.value ? '生成我的人格卡' : '下一题';
});
const primaryActionDisabled = computed(() => {
  if (submitting.value || loading.value) {
    return true;
  }
  return isBirthStep.value && !birthInfoComplete.value;
});
const stepCaption = computed(() => {
  if (isBirthStep.value) {
    return '先完成基础信息';
  }
  return `第 ${activeQuestionIndex.value + 1} / ${questions.value.length} 题`;
});
const actionSummaryText = computed(() => {
  if (isBirthStep.value) {
    return birthInfoComplete.value ? '可以进入问答卡片' : '只需要选年份和月份';
  }
  if (!activeQuestionAnswered.value) {
    return '按第一反应选择一个答案';
  }
  if (isLastQuestion.value) {
    return '确认无误后生成卡片';
  }
  return autoAdvancePending.value ? '已选择，可点下一题，也可以再点选项修改' : '已选择，可以进入下一题';
});

onMounted(async () => {
  try {
    questions.value = await fetchQuestions();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题目加载失败';
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  clearAutoAdvance();
});

async function submit() {
  if (submitting.value) {
    return;
  }
  error.value = '';
  track('TEST_SUBMIT_ATTEMPT', '/test');
  if (!form.birthYear || !form.birthMonth) {
    error.value = '请填写出生年份和月份';
    return;
  }
  const answers = questions.value.map((question) => ({
    questionCode: question.questionCode,
    optionCode: form.answers[question.questionCode],
  }));
  if (answers.some((answer) => !answer.optionCode)) {
    error.value = '请完成 5 道价值取向题';
    return;
  }
  submitting.value = true;
  try {
    const result = await createResult({
      birthYear: form.birthYear,
      birthMonth: form.birthMonth,
      birthDay: form.birthDay,
      birthTimeRange: form.birthTimeRange,
      answers,
    });
    await router.push(`/result/${result.resultId}`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交失败，请稍后再试';
  } finally {
    submitting.value = false;
  }
}

function markFormStart() {
  if (formStarted.value) {
    return;
  }
  formStarted.value = true;
  track('TEST_FORM_START', '/test');
}

function selectAnswer(questionCode: string, optionCode: string) {
  if (submitting.value) {
    return;
  }
  markFormStart();
  clearAutoAdvance();
  form.answers[questionCode] = optionCode;
  track('QUESTION_ANSWER_SELECT', '/test');

  if (!isLastQuestion.value) {
    autoAdvanceQuestionCode.value = questionCode;
    autoAdvanceTimer.value = window.setTimeout(() => {
      if (form.answers[questionCode] === optionCode && activeQuestion.value?.questionCode === questionCode) {
        goNext();
      }
    }, AUTO_ADVANCE_DELAY_MS);
  }
}

function selectBirthYear(year: number) {
  const normalizedYear = clampBirthYear(year);
  yearDraft.value = normalizedYear;
  form.birthYear = normalizedYear;
  markFormStart();
}

function updateBirthYear(event: Event) {
  const target = event.target as HTMLInputElement;
  selectBirthYear(Number(target.value));
}

function updateBirthYearManual(event: Event) {
  const target = event.target as HTMLInputElement;
  const rawValue = target.value.trim();
  if (!rawValue) {
    form.birthYear = null;
    return;
  }
  const year = Number(rawValue);
  if (!Number.isFinite(year)) {
    return;
  }
  selectBirthYear(year);
  target.value = String(form.birthYear ?? '');
}

function adjustBirthYear(delta: number) {
  selectBirthYear((form.birthYear ?? yearDraft.value) + delta);
}

function clampBirthYear(year: number) {
  return Math.min(currentYear, Math.max(minBirthYear, Math.trunc(year)));
}

function selectBirthMonth(month: number) {
  form.birthMonth = month;
  markFormStart();
}

function selectBirthDay(day: number | null) {
  form.birthDay = day;
  markFormStart();
}

function selectBirthTime(value: string | null) {
  form.birthTimeRange = value;
  markFormStart();
}

function optionRank(questionCode: string, optionCode: string) {
  const seed = `${questionCode}:${optionCode}:wuxing-v2.6`;
  return Array.from(seed).reduce((sum, char) => (sum * 31 + char.charCodeAt(0)) % 1000003, 7);
}

function goPrevious() {
  if (submitting.value) {
    return;
  }
  error.value = '';
  clearAutoAdvance();
  activeStepIndex.value = Math.max(0, activeStepIndex.value - 1);
}

function goNext() {
  if (submitting.value) {
    return;
  }
  error.value = '';
  clearAutoAdvance();
  if (isBirthStep.value) {
    if (!birthInfoComplete.value) {
      error.value = '请先选择出生年份和月份';
      return;
    }
    activeStepIndex.value = Math.min(questions.value.length, activeStepIndex.value + 1);
    return;
  }

  if (!activeQuestionAnswered.value) {
    error.value = '先选一个最接近你的答案，再进入下一题';
    return;
  }

  if (isLastQuestion.value) {
    void submit();
    return;
  }

  activeStepIndex.value = Math.min(questions.value.length, activeStepIndex.value + 1);
}

function canOpenStep(index: number) {
  if (index <= activeStepIndex.value) {
    return true;
  }
  if (index === 1) {
    return birthInfoComplete.value;
  }
  const previousQuestion = questions.value[index - 2];
  return Boolean(previousQuestion && form.answers[previousQuestion.questionCode]);
}

function goToStep(index: number) {
  if (submitting.value) {
    return;
  }
  if (!canOpenStep(index)) {
    return;
  }
  error.value = '';
  clearAutoAdvance();
  activeStepIndex.value = index;
}

function clearAutoAdvance() {
  if (autoAdvanceTimer.value === null) {
    return;
  }
  window.clearTimeout(autoAdvanceTimer.value);
  autoAdvanceTimer.value = null;
  autoAdvanceQuestionCode.value = null;
}
</script>

<template>
  <main class="page test-page">
    <section class="shell stack test-shell">
      <div class="test-header">
        <p class="eyebrow">五行人格测试</p>
        <h1>用 5 道题找到你的主副五行</h1>
        <p class="muted">题目没有标准答案，按第一反应选择即可。无需登录和姓名，出生日期、时段可以不透露。</p>
        <div class="progress-card" aria-label="答题进度">
          <div class="progress-meta">
            <span>完成 {{ completedProgressUnits }} / {{ totalProgressUnits }}</span>
            <strong>{{ progressPercent }}%</strong>
          </div>
          <div class="progress-track">
            <span :style="{ width: `${progressPercent}%` }"></span>
          </div>
        </div>
      </div>

      <div class="step-strip" aria-label="测试步骤">
        <button type="button" class="step-pill" :class="{ active: activeStepIndex === 0, done: birthInfoComplete }" :disabled="submitting" @click="goToStep(0)">
          基础
        </button>
        <button
          v-for="(question, index) in questions"
          :key="question.questionCode"
          type="button"
          class="step-pill"
          :class="{ active: activeStepIndex === index + 1, done: Boolean(form.answers[question.questionCode]) }"
          :disabled="submitting || !canOpenStep(index + 1)"
          @click="goToStep(index + 1)"
        >
          {{ index + 1 }}
        </button>
      </div>

      <div class="flow-stage" :class="{ 'question-mode': !isBirthStep, 'birth-mode': isBirthStep }">
        <Transition name="card-slide" mode="out-in">
          <div v-if="isBirthStep" key="birth" class="panel stack birth-panel input-panel flow-card">
            <div class="birth-panel-head">
              <div>
                <p class="section-kicker">STEP 00</p>
                <h2>先选出生年月</h2>
                <p class="muted">这里只需要年份和月份，用来让解读更有个人感；日期、时段默认不透露也可以继续。</p>
              </div>
              <div class="birth-status" :class="{ active: birthInfoComplete }">
                <span>{{ birthInfoComplete ? '已完成' : '待选择' }}</span>
                <strong>{{ form.birthYear ?? '年份' }} / {{ form.birthMonth ? form.birthMonth + '月' : '月份' }}</strong>
              </div>
            </div>

            <div class="input-stack">
              <section class="field-block year-field" aria-labelledby="birth-year-label">
                <div class="field-head">
                  <span id="birth-year-label">出生年份</span>
                  <strong>{{ form.birthYear ? form.birthYear + ' 年' : '拖动或输入' }}</strong>
                </div>
                <div class="year-picker">
                  <div class="year-display">
                    <strong>{{ yearPickerValue }}</strong>
                    <span>{{ form.birthYear ? '默认可改，选错也能随时调整' : '滑动刻度、微调或直接输入' }}</span>
                  </div>
                  <div class="year-control-row" aria-label="出生年份精确调节">
                    <button type="button" class="year-step-button" data-testid="birth-year-minus" @click="adjustBirthYear(-1)">
                      -1
                    </button>
                    <input
                      class="year-manual-input"
                      data-testid="birth-year-input"
                      inputmode="numeric"
                      type="number"
                      :min="minBirthYear"
                      :max="currentYear"
                      :value="form.birthYear ?? ''"
                      placeholder="输入年份"
                      aria-label="手动输入出生年份"
                      @change="updateBirthYearManual"
                      @blur="updateBirthYearManual"
                    >
                    <button type="button" class="year-step-button" data-testid="birth-year-plus" @click="adjustBirthYear(1)">
                      +1
                    </button>
                  </div>
                  <input
                    class="year-range"
                    data-testid="birth-year-range"
                    type="range"
                    :min="minBirthYear"
                    :max="currentYear"
                    :value="yearPickerValue"
                    aria-label="出生年份"
                    @input="updateBirthYear"
                  >
                  <div class="year-scale" aria-hidden="true">
                    <span v-for="year in yearScale" :key="year">{{ year }}</span>
                  </div>
                  <div class="quick-row" aria-label="常用年份">
                    <button
                      v-for="year in quickYears"
                      :key="year"
                      type="button"
                      class="quick-chip"
                      :class="{ active: form.birthYear === year }"
                      :data-testid="'birth-year-quick-' + year"
                      @click="selectBirthYear(year)"
                    >
                      {{ year }} 年
                    </button>
                  </div>
                </div>
              </section>

              <section class="field-block" aria-labelledby="birth-month-label">
                <div class="field-head">
                  <span id="birth-month-label">出生月份</span>
                  <strong>{{ form.birthMonth ? form.birthMonth + ' 月' : '请选择' }}</strong>
                </div>
                <p class="rail-hint">横向滑动可以看到 12 个月。</p>
                <div class="choice-rail month-rail" role="list" aria-label="出生月份">
                  <button
                    v-for="month in 12"
                    :key="month"
                    type="button"
                    class="choice-chip month-chip"
                    :class="{ active: form.birthMonth === month }"
                    :data-testid="'birth-month-' + month"
                    @click="selectBirthMonth(month)"
                  >
                    <strong>{{ month }} 月</strong>
                    <span>{{ monthHints[month - 1] }}</span>
                  </button>
                </div>
              </section>

              <details class="optional-birth-details">
                <summary>
                  <span>补充日期和时段</span>
                  <strong>{{ form.birthDay ? form.birthDay + ' 日' : '日期不透露' }} / {{ timeOptions.find((item) => item.value === form.birthTimeRange)?.label ?? '时段不透露' }}</strong>
                </summary>
                <div class="optional-birth-content">
                  <section class="field-block" aria-labelledby="birth-day-label">
                    <div class="field-head">
                      <span id="birth-day-label">出生日期</span>
                      <strong>{{ form.birthDay ? form.birthDay + ' 日' : '可不透露' }}</strong>
                    </div>
                    <p class="rail-hint">不知道或不想填，可以保持“不透露”。</p>
                    <div class="choice-rail day-rail" role="list" aria-label="出生日期">
                      <button
                        type="button"
                        class="choice-chip day-chip optional"
                        :class="{ active: form.birthDay === null }"
                        data-testid="birth-day-none"
                        @click="selectBirthDay(null)"
                      >
                        <strong>不透露</strong>
                        <span>默认</span>
                      </button>
                      <button
                        v-for="day in days"
                        :key="day"
                        type="button"
                        class="choice-chip day-chip"
                        :class="{ active: form.birthDay === day }"
                        :data-testid="'birth-day-' + day"
                        @click="selectBirthDay(day)"
                      >
                        <strong>{{ day }}</strong>
                        <span>日</span>
                      </button>
                    </div>
                  </section>

                  <section class="field-block" aria-labelledby="birth-time-label">
                    <div class="field-head">
                      <span id="birth-time-label">出生时段</span>
                      <strong>{{ timeOptions.find((item) => item.value === form.birthTimeRange)?.label ?? '不透露' }}</strong>
                    </div>
                    <div class="time-grid" aria-label="出生时段">
                      <button
                        v-for="option in timeOptions"
                        :key="option.label"
                        type="button"
                        class="time-chip"
                        :class="{ active: form.birthTimeRange === option.value }"
                        :data-testid="'birth-time-' + (option.value ?? 'NONE')"
                        @click="selectBirthTime(option.value)"
                      >
                        <strong>{{ option.label }}</strong>
                        <span>{{ option.hint }}</span>
                      </button>
                    </div>
                  </section>
                </div>
              </details>
            </div>
          </div>

          <div v-else-if="loading" key="loading" class="panel flow-card">
            题目加载中...
          </div>

          <QuestionCard
            v-else-if="activeQuestion"
            :key="activeQuestion.questionCode"
            class="panel flow-card question-panel"
            :model-value="form.answers[activeQuestion.questionCode]"
            :question="activeQuestion"
            :question-index="activeQuestionIndex + 1"
            :disabled="submitting"
            :total-questions="questions.length"
            @update:model-value="selectAnswer(activeQuestion.questionCode, $event)"
          />
        </Transition>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="sticky-action" :class="{ 'birth-action': isBirthStep }">
        <div class="action-summary">
          <strong>{{ stepCaption }}</strong>
          <span>{{ actionSummaryText }}</span>
          <RouterLink class="home-inline-link" to="/">返回首页</RouterLink>
          <span v-if="submitting" class="submit-lock">正在生成，请不要关闭页面</span>
          <span class="auto-advance-track" :class="{ active: autoAdvancePending }" aria-hidden="true">
            <i></i>
          </span>
        </div>
        <button v-if="canGoPrevious" type="button" class="secondary nav-button" @click="goPrevious">
          上一张
        </button>
        <button type="button" class="primary-action-button" :disabled="primaryActionDisabled" @click="goNext">
          {{ primaryActionText }}
        </button>
      </div>

      <p class="notice">
        本结果为传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。
      </p>
    </section>
  </main>
</template>

<style scoped>
.test-page {
  background:
    linear-gradient(180deg, #fbf8f0 0%, #edf5f1 48%, #f7efe9 100%);
}

.test-shell {
  padding-bottom: 96px;
}

.test-header {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 12px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 22px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(238, 247, 242, 0.82)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.08), rgba(196, 122, 80, 0.08));
  box-shadow: 0 18px 48px rgba(31, 48, 43, 0.1);
}

.test-header h1 {
  max-width: 660px;
  font-size: 44px;
}

.progress-card {
  display: grid;
  gap: 8px;
  margin-top: 6px;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #40514e;
  font-size: 14px;
  font-weight: 800;
}

.progress-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.1);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2f6f5e, #d79b43);
  transition: width 180ms ease;
}

.step-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 2px 2px 6px;
  scrollbar-width: none;
}

.step-strip::-webkit-scrollbar {
  display: none;
}

.step-pill {
  flex: 0 0 auto;
  min-width: 48px;
  min-height: 40px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 999px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.72);
  color: #596764;
  font-size: 13px;
  font-weight: 950;
  box-shadow: none;
}

.step-pill.done {
  border-color: rgba(47, 111, 94, 0.28);
  color: #2f6f5e;
}

.step-pill.active {
  border-color: #24302f;
  background: #24302f;
  color: #fff;
  box-shadow: 0 12px 24px rgba(36, 48, 47, 0.16);
}

.step-pill:disabled {
  opacity: 0.42;
}

.flow-stage {
  position: relative;
  min-height: 580px;
  overflow: hidden;
}

.flow-card {
  width: 100%;
}

.question-mode {
  display: grid;
  align-items: start;
}

.card-slide-enter-active,
.card-slide-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.card-slide-enter-from {
  opacity: 0;
  transform: translateX(26px) scale(0.985);
}

.card-slide-leave-to {
  opacity: 0;
  transform: translateX(-26px) scale(0.985);
}

.birth-panel {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(252, 248, 240, 0.9));
}

.birth-panel-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
}

.section-kicker {
  margin: 0 0 8px;
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.birth-status {
  display: grid;
  gap: 5px;
  min-width: 138px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.72);
  color: #596764;
}

.birth-status span {
  font-size: 12px;
  font-weight: 900;
}

.birth-status strong {
  color: #24302f;
  font-size: 15px;
}

.birth-status.active {
  border-color: rgba(47, 111, 94, 0.24);
  background: #edf7f2;
  color: #2f6f5e;
}

.input-stack {
  display: grid;
  gap: 18px;
}

.field-block {
  display: grid;
  gap: 10px;
}

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #40514e;
  font-weight: 900;
}

.field-head strong {
  color: #24302f;
  font-size: 14px;
}

.rail-hint {
  margin: -2px 0 0;
  color: #7a8582;
  font-size: 12px;
  font-weight: 800;
}

.optional-birth-details {
  border: 1px dashed rgba(36, 48, 47, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.58);
}

.optional-birth-details summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 0 14px;
  color: #40514e;
  font-size: 14px;
  font-weight: 950;
  cursor: pointer;
  list-style: none;
}

.optional-birth-details summary::-webkit-details-marker {
  display: none;
}

.optional-birth-details summary::after {
  content: "+";
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(47, 111, 94, 0.1);
  color: #2f6f5e;
  font-size: 18px;
  font-weight: 950;
}

.optional-birth-details[open] summary::after {
  content: "-";
}

.optional-birth-details summary strong {
  min-width: 0;
  color: #6a7774;
  font-size: 13px;
  text-align: right;
}

.optional-birth-content {
  display: grid;
  gap: 18px;
  border-top: 1px solid rgba(36, 48, 47, 0.08);
  padding: 14px;
}

.year-picker {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(237, 247, 242, 0.96), rgba(255, 249, 238, 0.94));
}

.year-display {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.year-display strong {
  color: #1f3732;
  font-size: 42px;
  font-weight: 950;
  line-height: 1;
}

.year-display span {
  color: #697674;
  font-size: 13px;
  font-weight: 850;
}

.year-control-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.year-step-button {
  min-width: 50px;
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  background: #24302f;
  color: #fff;
  font-size: 15px;
  font-weight: 950;
}

.year-manual-input {
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #24302f;
  font: inherit;
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.year-range {
  width: 100%;
  min-height: 32px;
  height: 32px;
  border: 0;
  padding: 0;
  background: transparent;
  accent-color: #2f6f5e;
  cursor: pointer;
}

.year-range::-webkit-slider-runnable-track {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f6f5e, #d79b43, #b85b48);
}

.year-range::-webkit-slider-thumb {
  width: 26px;
  height: 26px;
  margin-top: -9px;
  border: 4px solid #fff;
  border-radius: 50%;
  background: #24302f;
  box-shadow: 0 8px 22px rgba(36, 48, 47, 0.25);
  appearance: none;
}

.year-range::-moz-range-track {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f6f5e, #d79b43, #b85b48);
}

.year-range::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border: 4px solid #fff;
  border-radius: 50%;
  background: #24302f;
  box-shadow: 0 8px 22px rgba(36, 48, 47, 0.25);
}

.year-scale {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #7a8582;
  font-size: 11px;
  font-weight: 800;
}

.quick-row,
.choice-rail {
  display: flex;
  gap: 9px;
  overflow-x: auto;
  padding: 2px 2px 8px;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.quick-row::-webkit-scrollbar,
.choice-rail::-webkit-scrollbar {
  display: none;
}

.quick-chip,
.choice-chip,
.time-chip {
  flex: 0 0 auto;
  min-height: auto;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #263735;
  box-shadow: 0 8px 18px rgba(31, 48, 43, 0.06);
  transition:
    transform 150ms ease,
    border-color 150ms ease,
    background 150ms ease,
    box-shadow 150ms ease;
}

.quick-chip {
  padding: 9px 12px;
  font-size: 13px;
  font-weight: 900;
  scroll-snap-align: start;
}

.choice-chip {
  display: grid;
  place-items: center;
  gap: 4px;
  width: 92px;
  min-height: 72px;
  padding: 10px 8px;
  text-align: center;
  scroll-snap-align: start;
}

.choice-chip strong {
  font-size: 16px;
  font-weight: 950;
}

.choice-chip span {
  color: #6a7774;
  font-size: 11px;
  font-weight: 800;
}

.day-chip {
  width: 58px;
  min-height: 58px;
}

.day-chip.optional {
  width: 88px;
}

.quick-chip:hover,
.choice-chip:hover,
.time-chip:hover {
  transform: translateY(-1px);
}

.quick-chip.active,
.choice-chip.active,
.time-chip.active {
  border-color: rgba(47, 111, 94, 0.5);
  background: linear-gradient(135deg, #24302f, #2f6f5e);
  color: #fff;
  box-shadow: 0 13px 26px rgba(47, 111, 94, 0.22);
  transform: translateY(-1px);
}

.choice-chip.active span,
.time-chip.active span {
  color: rgba(255, 255, 255, 0.78);
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 9px;
}

.time-chip {
  display: grid;
  gap: 5px;
  justify-items: start;
  min-height: 70px;
  padding: 11px;
  text-align: left;
}

.time-chip strong {
  font-size: 15px;
  font-weight: 950;
}

.time-chip span {
  color: #6a7774;
  font-size: 11px;
  font-weight: 800;
}

.sticky-action {
  position: sticky;
  bottom: 14px;
  z-index: 5;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(36, 48, 47, 0.14);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 42px rgba(31, 48, 43, 0.14);
}

.action-summary {
  display: grid;
  gap: 3px;
}

.home-inline-link {
  width: fit-content;
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 850;
  text-decoration: none;
}

.home-inline-link:hover {
  text-decoration: underline;
}

.sticky-action span {
  color: #697674;
  font-size: 13px;
}

.submit-lock {
  color: #2f6f5e;
  font-weight: 900;
}

.auto-advance-track {
  display: block;
  overflow: hidden;
  width: min(220px, 100%);
  height: 4px;
  border-radius: 999px;
  background: rgba(47, 111, 94, 0.08);
  opacity: 0;
}

.auto-advance-track i {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  background: #2f6f5e;
  transform: translateX(-100%);
}

.auto-advance-track.active {
  opacity: 1;
}

.auto-advance-track.active i {
  animation: autoAdvanceFill 1100ms linear forwards;
}

@keyframes autoAdvanceFill {
  to {
    transform: translateX(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auto-advance-track.active i {
    animation: none;
    transform: translateX(0);
  }
}

@media (max-width: 760px) {
  .test-shell {
    padding-bottom: 218px;
  }

  .birth-panel-head {
    grid-template-columns: 1fr;
  }

  .birth-status {
    min-width: 0;
  }

  .year-display strong {
    font-size: 36px;
  }

  .time-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-stage {
    min-height: 610px;
    padding-bottom: 178px;
  }

  .flow-stage.birth-mode {
    min-height: 0;
    padding-bottom: 0;
  }

  .step-pill {
    min-width: 44px;
  }

  .year-control-row {
    grid-template-columns: 48px minmax(0, 1fr) 48px;
  }

  .sticky-action {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    bottom: 10px;
    gap: 8px;
  }

  .sticky-action.birth-action {
    position: static;
    grid-template-columns: 1fr;
    margin-top: 2px;
  }

  .action-summary,
  .primary-action-button {
    grid-column: 1 / -1;
  }

  .primary-action-button {
    order: 1;
    min-height: 50px;
    font-size: 16px;
  }

  .action-summary {
    order: 0;
  }

  .nav-button {
    order: 2;
  }

  .sticky-action a {
    order: initial;
  }

  .sticky-action button {
    width: 100%;
  }

  .test-header h1 {
    font-size: 30px;
  }
}
</style>
