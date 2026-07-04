package com.wuxing.persona.service;

import java.util.List;

public record TestFlowState(
        TestFlowStage stage,
        int answeredCount,
        int requiredQuestionCount,
        List<String> missingQuestionCodes
) {
    public TestFlowState {
        missingQuestionCodes = List.copyOf(missingQuestionCodes);
    }

    public boolean readyToSubmit() {
        return stage == TestFlowStage.READY_TO_SUBMIT;
    }
}
