package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.enums.ElementType;
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
    void calculateShouldRejectMissingQuestion() {
        CreateResultRequest request = requestWithAnswers("WOOD");
        request.getAnswers().remove(0);

        assertThrows(BusinessException.class, () -> service.calculate(request));
    }

    @Test
    void calculateShouldRejectInvalidBirthTimeRange() {
        CreateResultRequest request = requestWithAnswers("WOOD");
        request.setBirthTimeRange("MIDNIGHT");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.calculate(request));

        assertEquals("birthTimeRange must be a valid value", exception.getMessage());
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
