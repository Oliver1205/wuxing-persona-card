<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchQuestions } from '../api/questions';
import { createResult } from '../api/results';
import type { Question } from '../api/types';
import QuestionCard from '../components/QuestionCard.vue';

const router = useRouter();
const questions = ref<Question[]>([]);
const loading = ref(true);
const submitting = ref(false);
const error = ref('');

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
</script>

<template>
  <main class="page">
    <section class="shell stack">
      <div class="panel stack">
        <h2>开始测试</h2>
        <p class="muted">填写出生年月，再完成 5 道价值取向题。</p>

        <div class="form-grid">
          <label>
            出生年份
            <select v-model.number="form.birthYear">
              <option :value="null">请选择</option>
              <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
            </select>
          </label>

          <label>
            出生月份
            <select v-model.number="form.birthMonth">
              <option :value="null">请选择</option>
              <option v-for="month in 12" :key="month" :value="month">{{ month }} 月</option>
            </select>
          </label>

          <label>
            出生日期
            <select v-model.number="form.birthDay">
              <option :value="null">不透露</option>
              <option v-for="day in 31" :key="day" :value="day">{{ day }} 日</option>
            </select>
          </label>

          <label>
            出生时段
            <select v-model="form.birthTimeRange">
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
        v-for="question in questions"
        v-else
        :key="question.questionCode"
        v-model="form.answers[question.questionCode]"
        class="panel"
        :question="question"
      />

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="actions">
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
