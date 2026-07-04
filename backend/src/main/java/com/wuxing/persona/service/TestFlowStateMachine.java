package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.common.TestFlowPolicy;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TestFlowStateMachine {

    private TestFlowStateMachine() {
    }

    public static TestFlowState evaluate(CreateResultRequest request) {
        return evaluate(request, TestFlowPolicy.REQUIRED_QUESTION_COUNT);
    }

    public static TestFlowState evaluate(CreateResultRequest request, int requiredQuestionCount) {
        List<String> answeredQuestionCodes = normalizeAnsweredQuestionCodes(request, requiredQuestionCount);
        List<String> missingQuestionCodes = missingQuestionCodes(answeredQuestionCodes, requiredQuestionCount);
        if (request == null || request.getBirthYear() == null || request.getBirthMonth() == null) {
            return new TestFlowState(TestFlowStage.BIRTH_REQUIRED,
                    answeredQuestionCodes.size(), requiredQuestionCount, missingQuestionCodes);
        }
        if (!missingQuestionCodes.isEmpty()) {
            return new TestFlowState(TestFlowStage.ANSWERING,
                    answeredQuestionCodes.size(), requiredQuestionCount, missingQuestionCodes);
        }
        return new TestFlowState(TestFlowStage.READY_TO_SUBMIT,
                answeredQuestionCodes.size(), requiredQuestionCount, List.of());
    }

    public static TestFlowState requireReadyToSubmit(CreateResultRequest request) {
        if (hasDuplicateQuestionCodes(request)) {
            throw new BusinessException("answers must contain 5 unique questions");
        }
        TestFlowState state = evaluate(request);
        if (!state.readyToSubmit()) {
            throw new BusinessException(notReadyMessage(state));
        }
        return state;
    }

    private static List<String> normalizeAnsweredQuestionCodes(CreateResultRequest request, int requiredQuestionCount) {
        if (request == null || request.getAnswers() == null) {
            return List.of();
        }
        Set<String> uniqueCodes = new HashSet<>();
        for (AnswerRequest answer : request.getAnswers()) {
            if (answer == null
                    || answer.getQuestionCode() == null
                    || answer.getQuestionCode().isBlank()
                    || answer.getOptionCode() == null
                    || answer.getOptionCode().isBlank()) {
                continue;
            }
            String normalizedQuestionCode = answer.getQuestionCode().trim().toUpperCase(Locale.ROOT);
            if (isRequiredQuestionCode(normalizedQuestionCode, requiredQuestionCount)) {
                uniqueCodes.add(normalizedQuestionCode);
            }
        }
        return uniqueCodes.stream().sorted().toList();
    }

    private static List<String> missingQuestionCodes(List<String> answeredQuestionCodes, int requiredQuestionCount) {
        List<String> missing = new ArrayList<>();
        for (int index = 1; index <= requiredQuestionCount; index++) {
            String questionCode = questionCode(index);
            if (!answeredQuestionCodes.contains(questionCode)) {
                missing.add(questionCode);
            }
        }
        return missing;
    }

    private static String questionCode(int index) {
        return "Q" + index;
    }

    private static boolean isRequiredQuestionCode(String questionCode, int requiredQuestionCount) {
        for (int index = 1; index <= requiredQuestionCount; index++) {
            if (questionCode(index).equals(questionCode)) {
                return true;
            }
        }
        return false;
    }

    private static String notReadyMessage(TestFlowState state) {
        if (state.stage() == TestFlowStage.BIRTH_REQUIRED) {
            return "test flow requires birth year and month before submit";
        }
        return "test flow is not ready to submit: missing " + String.join(",", state.missingQuestionCodes());
    }

    private static boolean hasDuplicateQuestionCodes(CreateResultRequest request) {
        if (request == null || request.getAnswers() == null) {
            return false;
        }
        Set<String> uniqueCodes = new HashSet<>();
        int normalizedCount = 0;
        for (AnswerRequest answer : request.getAnswers()) {
            if (answer == null || answer.getQuestionCode() == null || answer.getQuestionCode().isBlank()) {
                continue;
            }
            normalizedCount++;
            uniqueCodes.add(answer.getQuestionCode().trim().toUpperCase(Locale.ROOT));
        }
        return normalizedCount != uniqueCodes.size();
    }
}
