package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.vo.ResultDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private UserResultMapper userResultMapper;

    @Mock
    private ElementCalculateService elementCalculateService;

    @Mock
    private StarOfficerService starOfficerService;

    @Mock
    private ResultTextService resultTextService;

    @Mock
    private ShortLinkService shortLinkService;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private VisitEventService visitEventService;

    @Mock
    private HttpServletRequest servletRequest;

    private ResultService resultService;

    @BeforeEach
    void setUp() {
        resultService = new ResultService(
                userResultMapper,
                new ObjectMapper(),
                elementCalculateService,
                starOfficerService,
                resultTextService,
                shortLinkService,
                redisCacheService,
                visitEventService);
    }

    @Test
    void createShouldRetryWhenGeneratedResultIdCollides() {
        CreateResultRequest request = buildRequest();
        ElementScoreResult scoreResult = buildScoreResult();
        StarOfficer starOfficer = new StarOfficer("zheng-guan", "正官", ElementType.WOOD, List.of("秩序"));
        ResultText resultText = buildResultText();
        ShortLinkEntity shortLink = buildShortLink();

        when(elementCalculateService.calculate(request)).thenReturn(scoreResult);
        when(starOfficerService.byMonth(6)).thenReturn(starOfficer);
        when(resultTextService.build(scoreResult, starOfficer, request)).thenReturn(resultText);
        when(userResultMapper.insert(any(UserResultEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate result_id"))
                .thenReturn(1);
        when(shortLinkService.createForResult(any())).thenReturn(shortLink);

        ResultDetailVO detail = resultService.create(request, "client-a", servletRequest);

        ArgumentCaptor<UserResultEntity> entityCaptor = ArgumentCaptor.forClass(UserResultEntity.class);
        verify(userResultMapper, times(2)).insert(entityCaptor.capture());
        List<UserResultEntity> insertedEntities = entityCaptor.getAllValues();
        String retryResultId = insertedEntities.get(1).getResultId();

        assertTrue(retryResultId.matches("R\\d{23}"));
        assertEquals(retryResultId, detail.getResultId());
        verify(shortLinkService).createForResult(retryResultId);
        verify(redisCacheService).setResult(eq(retryResultId), any(ResultDetailVO.class));
        verify(visitEventService).record(EventType.TEST_SUBMIT, "/test", retryResultId, "abc123", "client-a", servletRequest);
        verify(visitEventService).record(EventType.RESULT_CREATED, "/result/" + retryResultId,
                retryResultId, "abc123", "client-a", servletRequest);
        verify(visitEventService).record(EventType.SHORT_LINK_CREATED, null,
                retryResultId, "abc123", "client-a", servletRequest);
    }

    private CreateResultRequest buildRequest() {
        CreateResultRequest request = new CreateResultRequest();
        request.setBirthYear(2000);
        request.setBirthMonth(6);
        request.setBirthDay(18);
        request.setBirthTimeRange("09:00-11:00");
        request.setAnswers(List.of(
                answer("q1", "a"),
                answer("q2", "b"),
                answer("q3", "c"),
                answer("q4", "d"),
                answer("q5", "e")));
        return request;
    }

    private AnswerRequest answer(String questionCode, String optionCode) {
        AnswerRequest answer = new AnswerRequest();
        answer.setQuestionCode(questionCode);
        answer.setOptionCode(optionCode);
        return answer;
    }

    private ElementScoreResult buildScoreResult() {
        ElementScoreResult scoreResult = new ElementScoreResult();
        scoreResult.setPrimaryElement(ElementType.WOOD);
        scoreResult.setSecondaryElement(ElementType.FIRE);
        scoreResult.setPrimaryPercent(62);
        scoreResult.setSecondaryPercent(38);
        scoreResult.setAllScores(Map.of("WOOD", 62, "FIRE", 38, "EARTH", 20, "METAL", 18, "WATER", 16));
        return scoreResult;
    }

    private ResultText buildResultText() {
        ResultText resultText = new ResultText();
        resultText.setKeywords(List.of("成长", "表达"));
        resultText.setLayoutExplanation("木火相生");
        resultText.setStrengthText("适合持续推进");
        resultText.setRelationshipText("表达清晰");
        return resultText;
    }

    private ShortLinkEntity buildShortLink() {
        ShortLinkEntity shortLink = new ShortLinkEntity();
        shortLink.setShortCode("abc123");
        shortLink.setShortUrl("http://example.test/s/abc123");
        return shortLink;
    }
}
