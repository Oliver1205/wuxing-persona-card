export const MIN_BIRTH_YEAR = 1950;
export const MAX_BIRTH_YEAR = 2026;

export type TestFlowStage = 'birth' | 'question' | 'submitting' | 'blocked' | 'finished';

export interface TestFlowMachineInput {
  stepIndex: number;
  questionCount: number;
  birthInfoComplete: boolean;
  activeQuestionAnswered: boolean;
  submitting: boolean;
  loading: boolean;
  questionListUnavailable: boolean;
  matchMode: boolean;
}

export interface TestFlowMachineState {
  stage: TestFlowStage;
  stepIndex: number;
  activeQuestionIndex: number;
  isBirthStep: boolean;
  isLastQuestion: boolean;
  canGoPrevious: boolean;
  primaryActionDisabled: boolean;
  primaryActionText: string;
  previousActionText: string;
  stepCaption: string;
  actionSummaryText: string;
  topBackLabel: string;
}

export function deriveTestFlowMachineState(input: TestFlowMachineInput): TestFlowMachineState {
  const stepIndex = clampTestStepIndex(input.stepIndex, input.questionCount);
  const isBirthStep = stepIndex === 0;
  const activeQuestionIndex = Math.max(0, stepIndex - 1);
  const isLastQuestion = input.questionCount > 0 && stepIndex === input.questionCount;
  const stage = resolveStage(input, isBirthStep);
  const previousActionText = activeQuestionIndex === 0 ? '基础信息' : '上一题';

  return {
    stage,
    stepIndex,
    activeQuestionIndex,
    isBirthStep,
    isLastQuestion,
    canGoPrevious: stepIndex > 0 && !input.submitting,
    primaryActionDisabled: resolvePrimaryDisabled(input, isBirthStep),
    primaryActionText: resolvePrimaryText(input, isBirthStep, isLastQuestion),
    previousActionText,
    stepCaption: isBirthStep ? '基础信息' : `第 ${activeQuestionIndex + 1} / ${input.questionCount} 题`,
    actionSummaryText: resolveActionSummary(input, isBirthStep, isLastQuestion),
    topBackLabel: isBirthStep ? '返回首页' : previousActionText,
  };
}

export function canOpenTestStep(index: number, birthInfoComplete: boolean, answeredQuestionCodes: boolean[], currentStepIndex: number) {
  if (index <= currentStepIndex) {
    return true;
  }
  if (index === 1) {
    return birthInfoComplete;
  }
  return Boolean(answeredQuestionCodes[index - 2]);
}

export function clampTestStepIndex(index: number, questionCount: number) {
  const normalizedIndex = Number.isFinite(index) ? Math.trunc(index) : 0;
  const maxStep = questionCount > 0 ? questionCount : Math.max(0, normalizedIndex);
  return Math.max(0, Math.min(maxStep, normalizedIndex));
}

export function clampBirthYear(year: number) {
  return Math.min(MAX_BIRTH_YEAR, Math.max(MIN_BIRTH_YEAR, Math.trunc(year)));
}

function resolveStage(input: TestFlowMachineInput, isBirthStep: boolean): TestFlowStage {
  if (input.submitting) {
    return 'submitting';
  }
  if (input.questionListUnavailable) {
    return 'blocked';
  }
  return isBirthStep ? 'birth' : 'question';
}

function resolvePrimaryDisabled(input: TestFlowMachineInput, isBirthStep: boolean) {
  if (input.submitting || input.loading || input.questionListUnavailable) {
    return true;
  }
  return isBirthStep ? !input.birthInfoComplete : !input.activeQuestionAnswered;
}

function resolvePrimaryText(input: TestFlowMachineInput, isBirthStep: boolean, isLastQuestion: boolean) {
  if (input.submitting) {
    return input.matchMode ? '生成匹配中...' : '生成中...';
  }
  if (isBirthStep) {
    if (input.questionListUnavailable) {
      return '题目加载失败';
    }
    return input.birthInfoComplete ? '进入第 1 题' : '选择月份后继续';
  }
  if (isLastQuestion) {
    return input.matchMode ? '生成双人匹配' : '生成我的人格卡';
  }
  return '下一题';
}

function resolveActionSummary(input: TestFlowMachineInput, isBirthStep: boolean, isLastQuestion: boolean) {
  if (isBirthStep) {
    if (input.questionListUnavailable) {
      return '题目没有加载成功，请刷新重试';
    }
    if (!input.birthInfoComplete) {
      return '先选年份和月份';
    }
    return input.matchMode ? '完成后进入双人匹配' : '可以进入问答卡片';
  }
  if (!input.activeQuestionAnswered) {
    return '按第一反应选择一个答案';
  }
  if (isLastQuestion) {
    return input.matchMode ? '确认后生成匹配结果' : '确认后生成卡片';
  }
  return '已选择，可以改选或进入下一题';
}
