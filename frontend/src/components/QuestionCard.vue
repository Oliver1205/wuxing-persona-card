<script setup lang="ts">
import type { Question } from '../api/types';

defineProps<{
  question: Question;
  modelValue?: string;
  questionIndex?: number;
  totalQuestions?: number;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<template>
  <section class="question-card">
    <div class="question-head">
      <span class="question-number">{{ questionIndex ?? question.questionCode }}</span>
      <div class="question-title-block">
        <p v-if="totalQuestions" class="question-progress">第 {{ questionIndex }} / {{ totalQuestions }} 题</p>
        <h3>{{ question.title }}</h3>
        <p class="question-hint">不用猜结果，选更像你的那个。</p>
      </div>
    </div>
    <div class="options">
      <button
        v-for="(option, optionIndex) in question.options"
        :key="option.optionCode"
        type="button"
        class="option"
        :data-testid="`question-${question.questionCode}-option-${option.optionCode}`"
        :class="{ active: modelValue === option.optionCode }"
        :aria-pressed="modelValue === option.optionCode"
        :disabled="disabled"
        @click="emit('update:modelValue', option.optionCode)"
      >
        <span class="option-mark">{{ String.fromCharCode(65 + optionIndex) }}</span>
        <span class="option-body">
          <span class="option-title">{{ option.optionText }}</span>
        </span>
        <span class="option-state" aria-hidden="true"></span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.question-card {
  display: grid;
  gap: 14px;
  background:
    linear-gradient(180deg, rgba(255, 252, 245, 0.95), rgba(248, 240, 226, 0.9));
}

.question-head {
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 12px;
  align-items: start;
}

.question-number {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--color-warm), var(--color-primary));
  color: #fff;
  font-weight: 850;
  box-shadow: 0 8px 18px rgba(157, 86, 49, 0.16);
}

.question-title-block {
  display: grid;
  gap: 5px;
}

.question-progress {
  margin: 0;
  color: var(--color-warm-deep);
  font-size: 12px;
  font-weight: 800;
}

h3 {
  margin: 0;
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 850;
  line-height: 1.42;
  letter-spacing: 0;
}

.question-hint {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 650;
}

.options {
  display: grid;
  gap: 9px;
}

.option {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  justify-content: flex-start;
  min-height: 56px;
  border: 1px solid rgba(37, 48, 45, 0.12);
  border-radius: 8px;
  padding: 11px 12px 11px 14px;
  background: rgba(255, 252, 245, 0.92);
  color: #31403b;
  font-size: 15px;
  line-height: 1.45;
  text-align: left;
  font-weight: 620;
  box-shadow: 0 8px 18px rgba(49, 44, 35, 0.04);
  transition:
    transform 150ms ease,
    border-color 150ms ease,
    background 150ms ease,
    box-shadow 150ms ease;
}

.option:hover {
  transform: translateY(-1px);
  border-color: rgba(201, 111, 61, 0.28);
}

.option:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
}

.option:disabled:hover {
  border-color: rgba(37, 48, 45, 0.14);
}

.option.active {
  border-color: rgba(201, 111, 61, 0.48);
  background: linear-gradient(135deg, rgba(255, 241, 229, 0.96), rgba(239, 248, 244, 0.88));
  color: #513424;
  box-shadow: 0 12px 24px rgba(157, 86, 49, 0.1);
  transform: translateY(-1px);
}

.option-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: #f3e4cf;
  color: #7c5735;
  font-weight: 760;
}

.option-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.option-title {
  display: block;
  line-height: 1.45;
  word-break: break-word;
}

.option-state {
  display: grid;
  place-items: center;
  justify-self: end;
  align-self: center;
  width: 24px;
  height: 24px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  opacity: 0;
  transition:
    opacity 150ms ease,
    border-color 150ms ease,
    background 150ms ease;
}

.option-state::before {
  content: "";
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(106, 119, 116, 0.36);
}

.option.active .option-mark {
  background: var(--color-warm);
  color: #fff;
}

.option.active .option-state {
  border-color: rgba(201, 111, 61, 0.22);
  background: rgba(255, 252, 245, 0.8);
  opacity: 1;
}

.option.active .option-state::before {
  content: "✓";
  width: auto;
  height: auto;
  color: var(--color-warm-deep);
  background: transparent;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
}

@media (max-width: 520px) {
  .question-card {
    gap: 12px;
  }

  .question-head {
    grid-template-columns: 34px 1fr;
    gap: 10px;
  }

  .question-number {
    width: 34px;
    height: 34px;
    font-size: 14px;
  }

  h3 {
    font-size: 16px;
    line-height: 1.38;
  }

  .question-hint {
    display: none;
  }

  .option {
    grid-template-columns: 26px minmax(0, 1fr) 28px;
    gap: 8px;
    min-height: 52px;
    padding: 10px 10px 10px 12px;
  }

  .option-mark {
    width: 26px;
    height: 26px;
    font-size: 13px;
  }

  .option-title {
    font-size: 14px;
    line-height: 1.38;
  }

  .option-state {
    width: 24px;
    height: 24px;
    border-color: rgba(36, 48, 47, 0.14);
  }

  .option.active {
    background: rgba(239, 248, 244, 0.96);
  }

  .option.active .option-title {
    color: #174943;
    font-weight: 760;
  }

  .option.active .option-state {
    border-color: rgba(47, 111, 94, 0.28);
    background: #fff;
  }

}
</style>
