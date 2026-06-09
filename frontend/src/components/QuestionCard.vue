<script setup lang="ts">
import type { Question } from '../api/types';

defineProps<{
  question: Question;
  modelValue?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<template>
  <section class="question-card">
    <h3>{{ question.questionCode }}. {{ question.title }}</h3>
    <div class="options">
      <button
        v-for="option in question.options"
        :key="option.optionCode"
        type="button"
        class="option"
        :class="{ active: modelValue === option.optionCode }"
        @click="emit('update:modelValue', option.optionCode)"
      >
        <span class="element">{{ option.elementName }}</span>
        <span>{{ option.optionText }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.question-card {
  display: grid;
  gap: 12px;
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
  min-height: 52px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  background: #fff;
  color: #263735;
  text-align: left;
  font-weight: 500;
}

.option.active {
  border-color: #2f6f5e;
  background: #e8f3ef;
  color: #173d34;
}

.element {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-right: 10px;
  border-radius: 50%;
  background: #f1eadc;
  color: #7a5a2e;
  font-weight: 800;
}
</style>
