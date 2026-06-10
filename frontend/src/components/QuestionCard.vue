<script setup lang="ts">
import type { Question } from '../api/types';

defineProps<{
  question: Question;
  modelValue?: string;
  questionIndex?: number;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<template>
  <section class="question-card">
    <div class="question-head">
      <span class="question-number">{{ questionIndex ?? question.questionCode }}</span>
      <h3>{{ question.title }}</h3>
    </div>
    <div class="options">
      <button
        v-for="(option, optionIndex) in question.options"
        :key="option.optionCode"
        type="button"
        class="option"
        :class="{ active: modelValue === option.optionCode }"
        @click="emit('update:modelValue', option.optionCode)"
      >
        <span class="option-mark">{{ String.fromCharCode(65 + optionIndex) }}</span>
        <span>{{ option.optionText }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.question-card {
  display: grid;
  gap: 16px;
}

.question-head {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  align-items: start;
}

.question-number {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #24302f;
  color: #fff;
  font-weight: 900;
}

h3 {
  margin: 0;
  color: #263735;
  font-size: 18px;
  line-height: 1.45;
  letter-spacing: 0;
}

.options {
  display: grid;
  gap: 10px;
}

.option {
  justify-content: flex-start;
  min-height: 58px;
  border: 1px solid rgba(36, 48, 47, 0.14);
  background: rgba(255, 255, 255, 0.92);
  color: #263735;
  text-align: left;
  font-weight: 650;
}

.option.active {
  border-color: #2f6f5e;
  background: linear-gradient(135deg, #e8f3ef, #f7efe0);
  color: #173d34;
  box-shadow: 0 10px 28px rgba(47, 111, 94, 0.14);
}

.option-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  margin-right: 10px;
  border-radius: 50%;
  background: #f1eadc;
  color: #6d4f29;
  font-weight: 800;
}

.option.active .option-mark {
  background: #2f6f5e;
  color: #fff;
}
</style>
