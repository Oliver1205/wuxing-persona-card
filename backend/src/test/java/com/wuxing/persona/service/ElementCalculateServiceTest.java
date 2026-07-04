package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.enums.ElementType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElementCalculateServiceTest {

    private final ElementCalculateService service = new ElementCalculateService();

    @Test
    void calculateShouldReturnPrimaryAndSecondaryElements() {
        CreateResultRequest request = requestWithAnswers("METAL");
        request.setBirthYear(2002);
        request.setBirthMonth(8);

        ElementScoreResult result = service.calculate(request);

        assertEquals(ElementType.METAL, result.getPrimaryElement());
        assertEquals(ElementType.EARTH, result.getSecondaryElement());
        assertEquals(78, result.getPrimaryPercent());
        assertEquals(22, result.getSecondaryPercent());
        assertEquals(105, result.getAllScores().get("METAL"));
    }

    @Test
    void calculateShouldNormalizeQuestionCodesBeforeFinalValidation() {
        CreateResultRequest request = requestWithAnswers("water");
        request.getAnswers().get(0).setQuestionCode(" q1 ");
        request.getAnswers().get(1).setQuestionCode("q2");

        ElementScoreResult result = service.calculate(request);

        assertEquals(ElementType.WATER, result.getPrimaryElement());
    }

    @Test
    void calculateShouldRejectMissingQuestion() {
        CreateResultRequest request = requestWithAnswers("WOOD");
        request.getAnswers().remove(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

        assertEquals("test flow is not ready to submit: missing Q1", exception.getMessage());
    }

    @Test
    void calculateShouldRejectExtraUnknownQuestionEvenIfRequiredQuestionsAreComplete() {
        CreateResultRequest request = requestWithAnswers("WOOD");
        AnswerRequest extra = new AnswerRequest();
        extra.setQuestionCode("QX");
        extra.setOptionCode("WATER");
        request.getAnswers().add(extra);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

        assertEquals("answers must contain 5 unique questions", exception.getMessage());
    }

    @Test
    void calculateShouldRejectInvalidBirthTimeRange() {
        CreateResultRequest request = requestWithAnswers("WOOD");
        request.setBirthTimeRange("MIDNIGHT");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

        assertEquals("birthTimeRange must be a valid value", exception.getMessage());
    }

    @Test
    void calculateShouldRejectImpossibleBirthDate() {
        CreateResultRequest request = requestWithAnswers("WOOD");
        request.setBirthYear(2001);
        request.setBirthMonth(2);
        request.setBirthDay(29);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

        assertEquals("birthDate must be a real calendar date", exception.getMessage());
    }

    @Test
    void calculateShouldRejectFutureBirthPeriod() {
        LocalDate today = LocalDate.now();
        CreateResultRequest request = requestWithAnswers("WOOD");
        request.setBirthDay(null);
        if (today.getMonthValue() < 12) {
            request.setBirthYear(today.getYear());
            request.setBirthMonth(today.getMonthValue() + 1);

            BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

            assertEquals("birthMonth must not be in the future", exception.getMessage());
            return;
        }

        request.setBirthYear(today.getYear() + 1);
        request.setBirthMonth(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

        if (today.getYear() >= 2026) {
            assertEquals("birthYear must not be greater than 2026", exception.getMessage());
        } else {
            assertEquals("birthYear must not be greater than current year", exception.getMessage());
        }
    }

    @Test
    void calculateShouldRejectBirthYearOutsideSupportedRange() {
        CreateResultRequest tooEarly = requestWithAnswers("WOOD");
        tooEarly.setBirthYear(1949);

        BusinessException earlyException = assertThrows(BusinessException.class, () -> service.calculate(tooEarly));

        assertEquals("birthYear must not be earlier than 1950", earlyException.getMessage());

        CreateResultRequest tooLate = requestWithAnswers("WOOD");
        tooLate.setBirthYear(2027);

        BusinessException lateException = assertThrows(BusinessException.class, () -> service.calculate(tooLate));

        assertEquals("birthYear must not be greater than 2026", lateException.getMessage());
    }

    private CreateResultRequest requestWithAnswers(String optionCode) {
        CreateResultRequest request = new CreateResultRequest();
        request.setBirthYear(2002);
        request.setBirthMonth(8);
        request.setBirthDay(null);
        request.setBirthTimeRange(null);
        List<AnswerRequest> answers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            AnswerRequest answer = new AnswerRequest();
            answer.setQuestionCode("Q" + i);
            answer.setOptionCode(optionCode);
            answers.add(answer);
        }
        request.setAnswers(answers);
        return request;
    }
}
