<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchQuestions } from '../api/questions';
import { createResult } from '../api/results';
import type { Question } from '../api/types';
import QuestionCard from '../components/QuestionCard.vue';
import { track } from '../utils/tracker';

const router = useRouter();
const questions = ref<Question[]>([]);
const loading = ref(true);
const submitting = ref(false);
const error = ref('');
const formStarted = ref(false);

const form = reactive<{
  birthYear: number | null;
  birthMonth: number | null;
  birthDay: number | null;
  birthTimeRange: string | null;
  answers: Record<string, string>;
}>({
  birthYear: null,
  birthMonth: null,
  birthDay: null,
  birthTimeRange: null,
  answers: {},
});

const currentYear = new Date().getFullYear();
const years = computed(() => Array.from({ length: currentYear - 1900 + 1 }, (_, index) => currentYear - index));
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

onMounted(async () => {
  try {
    questions.value = await fetchQuestions();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题目加载失败';
  } finally {
    loading.value = false;
  }
});

async function submit() {
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
  markFormStart();
  form.answers[questionCode] = optionCode;
  track('QUESTION_ANSWER_SELECT', '/test');
}
</script>

<template>
  <main class="page test-page">
    <section class="shell stack test-shell">
      <div class="test-header">
        <p class="eyebrow">五行人格测试</p>
        <h1>用 5 道题找到你的主副五行</h1>
        <p class="muted">题目没有标准答案，按第一反应选择即可。出生日期和时段可以不透露。</p>
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

      <div class="panel stack birth-panel">
        <div>
          <h2>出生信息</h2>
          <p class="muted">用于生成娱乐化五行倾向，不保存明文 IP，也不要求昵称和性别。</p>
        </div>

        <div class="form-grid">
          <label>
            出生年份
            <select v-model.number="form.birthYear" @change="markFormStart">
              <option :value="null">请选择</option>
              <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
            </select>
          </label>

          <label>
            出生月份
            <select v-model.number="form.birthMonth" @change="markFormStart">
              <option :value="null">请选择</option>
              <option v-for="month in 12" :key="month" :value="month">{{ month }} 月</option>
            </select>
          </label>

          <label>
            出生日期
            <select v-model.number="form.birthDay" @change="markFormStart">
              <option :value="null">不透露</option>
              <option v-for="day in 31" :key="day" :value="day">{{ day }} 日</option>
            </select>
          </label>

          <label>
            出生时段
            <select v-model="form.birthTimeRange" @change="markFormStart">
              <option :value="null">不透露</option>
              <option value="MORNING">上午</option>
              <option value="NOON">中午</option>
              <option value="AFTERNOON">下午</option>
              <option value="EVENING">傍晚</option>
              <option value="NIGHT">夜晚</option>
              <option value="UNKNOWN">不确定</option>
            </select>
          </label>
        </div>
      </div>

      <div v-if="loading" class="panel">题目加载中...</div>
      <QuestionCard
        v-for="(question, index) in questions"
        v-else
        :key="question.questionCode"
        class="panel"
        :model-value="form.answers[question.questionCode]"
        :question="question"
        :question-index="index + 1"
        @update:model-value="selectAnswer(question.questionCode, $event)"
      />

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="sticky-action">
        <div>
          <strong>{{ answeredCount }}/{{ questions.length }} 题已完成</strong>
          <span>{{ birthInfoComplete ? '出生年月已填写' : '请先填写出生年月' }}</span>
        </div>
        <button type="button" :disabled="submitting" @click="submit">
          {{ submitting ? '生成中...' : '生成人格卡' }}
        </button>
        <RouterLink class="button-link secondary" to="/">返回首页</RouterLink>
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
    linear-gradient(180deg, #f8f5eb 0%, #edf4ef 46%, #f6f3ec 100%);
}

.test-shell {
  padding-bottom: 96px;
}

.test-header {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.72);
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

.birth-panel {
  background: rgba(255, 255, 255, 0.86);
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

.sticky-action div {
  display: grid;
  gap: 3px;
}

.sticky-action span {
  color: #697674;
  font-size: 13px;
}

@media (max-width: 760px) {
  .sticky-action {
    grid-template-columns: 1fr;
  }

  .sticky-action button,
  .sticky-action a {
    width: 100%;
  }

  .test-header h1 {
    font-size: 30px;
  }
}
</style>
