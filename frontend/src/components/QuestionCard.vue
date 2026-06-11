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
        :aria-pressed="modelValue === option.optionCode"
        @click="emit('update:modelValue', option.optionCode)"
      >
        <span class="option-mark">{{ String.fromCharCode(65 + optionIndex) }}</span>
        <span class="option-body">
          <span class="option-title">{{ option.optionText }}</span>
          <span class="option-meta">{{ option.elementName }}系倾向</span>
        </span>
        <span class="option-state">{{ modelValue === option.optionCode ? '已选' : '选择' }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.question-card {
  display: grid;
  gap: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(252, 248, 241, 0.86));
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
  box-shadow: 0 10px 22px rgba(36, 48, 47, 0.16);
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
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  justify-content: flex-start;
  min-height: 58px;
  border: 1px solid rgba(36, 48, 47, 0.14);
  background: rgba(255, 255, 255, 0.92);
  color: #263735;
  text-align: left;
  font-weight: 650;
  box-shadow: 0 8px 18px rgba(31, 48, 43, 0.05);
  transition:
    transform 150ms ease,
    border-color 150ms ease,
    background 150ms ease,
    box-shadow 150ms ease;
}

.option:hover {
  transform: translateY(-1px);
  border-color: rgba(47, 111, 94, 0.26);
}

.option.active {
  border-color: #2f6f5e;
  background:
    linear-gradient(135deg, rgba(232, 243, 239, 0.98), rgba(252, 239, 216, 0.94)),
    #fff;
  color: #173d34;
  box-shadow: 0 14px 30px rgba(47, 111, 94, 0.16);
  transform: translateY(-1px);
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

.option-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.option-title {
  line-height: 1.45;
  word-break: break-word;
}

.option-meta {
  color: #7a8582;
  font-size: 12px;
  font-weight: 850;
}

.option-state {
  align-self: center;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 999px;
  padding: 4px 8px;
  background: rgba(246, 243, 236, 0.76);
  color: #6a7774;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.option.active .option-mark {
  background: #2f6f5e;
  color: #fff;
}

.option.active .option-meta,
.option.active .option-state {
  color: #2f6f5e;
}

.option.active .option-state {
  border-color: rgba(47, 111, 94, 0.22);
  background: rgba(255, 255, 255, 0.78);
}

@media (max-width: 520px) {
  .option {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .option-state {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
