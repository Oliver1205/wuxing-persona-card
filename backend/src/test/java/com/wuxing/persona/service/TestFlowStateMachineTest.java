package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestFlowStateMachineTest {

    @Test
    void evaluateShouldRequireBirthInformationBeforeSubmit() {
        CreateResultRequest request = requestWithAnswers(5);
        request.setBirthYear(null);

        TestFlowState state = TestFlowStateMachine.evaluate(request);

        assertEquals(TestFlowStage.BIRTH_REQUIRED, state.stage());
        assertEquals(5, state.answeredCount());
        assertTrue(state.missingQuestionCodes().isEmpty());
        assertFalse(state.readyToSubmit());
    }

    @Test
    void evaluateShouldReportMissingQuestionCodes() {
        CreateResultRequest request = requestWithAnswers(3);

        TestFlowState state = TestFlowStateMachine.evaluate(request);

        assertEquals(TestFlowStage.ANSWERING, state.stage());
        assertEquals(3, state.answeredCount());
        assertEquals(List.of("Q4", "Q5"), state.missingQuestionCodes());
        assertFalse(state.readyToSubmit());
    }

    @Test
    void evaluateShouldIgnoreUnknownQuestionCodesWhenCountingProgress() {
        CreateResultRequest request = requestWithAnswers(4);
        AnswerRequest unknown = new AnswerRequest();
        unknown.setQuestionCode("QX");
        unknown.setOptionCode("WATER");
        request.getAnswers().add(unknown);

        TestFlowState state = TestFlowStateMachine.evaluate(request);

        assertEquals(TestFlowStage.ANSWERING, state.stage());
        assertEquals(4, state.answeredCount());
        assertEquals(List.of("Q5"), state.missingQuestionCodes());
        assertFalse(state.readyToSubmit());
    }

    @Test
    void evaluateShouldRequireNonBlankOptionForAnsweredQuestion() {
        CreateResultRequest request = requestWithAnswers(5);
        request.getAnswers().get(0).setOptionCode(" ");

        TestFlowState state = TestFlowStateMachine.evaluate(request);

        assertEquals(TestFlowStage.ANSWERING, state.stage());
        assertEquals(4, state.answeredCount());
        assertEquals(List.of("Q1"), state.missingQuestionCodes());
        assertFalse(state.readyToSubmit());
    }

    @Test
    void evaluateShouldBeReadyWhenBirthAndFiveQuestionsAreComplete() {
        CreateResultRequest request = requestWithAnswers(5);

        TestFlowState state = TestFlowStateMachine.evaluate(request);

        assertEquals(TestFlowStage.READY_TO_SUBMIT, state.stage());
        assertEquals(5, state.answeredCount());
        assertTrue(state.missingQuestionCodes().isEmpty());
        assertTrue(state.readyToSubmit());
    }

    @Test
    void requireReadyToSubmitShouldRejectIncompleteFlow() {
        CreateResultRequest request = requestWithAnswers(4);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> TestFlowStateMachine.requireReadyToSubmit(request));

        assertEquals("test flow is not ready to submit: missing Q5", exception.getMessage());
    }

    @Test
    void requireReadyToSubmitShouldKeepDuplicateQuestionContract() {
        CreateResultRequest request = requestWithAnswers(5);
        request.getAnswers().get(4).setQuestionCode("Q1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> TestFlowStateMachine.requireReadyToSubmit(request));

        assertEquals("answers must contain 5 unique questions", exception.getMessage());
    }

    @Test
    void requireReadyToSubmitShouldDetectDuplicateQuestionCodesAfterNormalization() {
        CreateResultRequest request = requestWithAnswers(5);
        request.getAnswers().get(4).setQuestionCode(" q1 ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> TestFlowStateMachine.requireReadyToSubmit(request));

        assertEquals("answers must contain 5 unique questions", exception.getMessage());
    }

    private CreateResultRequest requestWithAnswers(int count) {
        CreateResultRequest request = new CreateResultRequest();
        request.setBirthYear(2002);
        request.setBirthMonth(8);
        request.setBirthDay(null);
        request.setBirthTimeRange(null);
        List<AnswerRequest> answers = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            AnswerRequest answer = new AnswerRequest();
            answer.setQuestionCode("Q" + index);
            answer.setOptionCode("WATER");
            answers.add(answer);
        }
        request.setAnswers(answers);
        return request;
    }
}
