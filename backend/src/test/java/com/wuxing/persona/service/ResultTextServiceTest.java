package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.enums.ElementType;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultTextServiceTest {

    private final ResultTextService service = new ResultTextService();

    @Test
    void buildShouldExplainWaterWoodWithFireAccentPositively() {
        ElementScoreResult scoreResult = scoreResult(ElementType.WATER, ElementType.WOOD, 70, 55, 35, 20, 20);
        StarOfficer starOfficer = new StarOfficer("BRIGHT", "明灯星", ElementType.FIRE, List.of("灵感", "热心"));

        ResultText result = service.build(scoreResult, starOfficer, request(2001, 2, null, null,
                "FIRE", "FIRE", "FIRE", "WATER", "WOOD"));

        assertTrue(result.getLayoutExplanation().contains("2001年取干支纪年为辛巳年"));
        assertTrue(result.getLayoutExplanation().contains("纳音白蜡金属金"));
        assertTrue(result.getLayoutExplanation().contains("2月按节令近似为立春雨水"));
        assertTrue(result.getLayoutExplanation().contains("寅月木气初生，水来滋养"));
        assertTrue(result.getLayoutExplanation().contains("5题里火向最多（3/5）"));
        assertTrue(result.getLayoutExplanation().contains("主水、次木"));
        assertTrue(result.getStrengthText().contains("主水代表"));
        assertTrue(result.getStrengthText().contains("火作为点睛"));
        assertTrue(result.getRelationshipText().contains("水会把能量自然推向木"));
        assertTrue(result.getKeywords().contains("热情"));
        assertEquals(5, result.getKeywords().size());
    }

    @Test
    void buildShouldTreatWeakThirdElementAsBackgroundInsteadOfDefect() {
        ElementScoreResult scoreResult = scoreResult(ElementType.WATER, ElementType.WOOD, 86, 66, 20, 20, 20);
        StarOfficer starOfficer = new StarOfficer("BRIGHT", "明灯星", ElementType.FIRE, List.of("灵感", "热心"));

        ResultText result = service.build(scoreResult, starOfficer, request(2001, 2, null, null,
                "WATER", "WATER", "WOOD", "WOOD", "METAL"));

        assertTrue(result.getRelationshipText().contains("其他元素作为背景"));
        assertTrue(result.getStrengthText().contains("主水代表"));
    }

    @Test
    void buildShouldIncludeOptionalDayAndTimeRationaleWhenProvided() {
        ElementScoreResult scoreResult = scoreResult(ElementType.FIRE, ElementType.EARTH, 20, 20, 66, 22, 52);
        StarOfficer starOfficer = new StarOfficer("EMBER", "炎庭星官", ElementType.FIRE, List.of("明亮", "热心"));

        ResultText result = service.build(scoreResult, starOfficer, request(2003, 12, 18, "NOON",
                "FIRE", "FIRE", "EARTH", "FIRE", "METAL"));

        assertTrue(result.getLayoutExplanation().contains("2003年取干支纪年为癸未年"));
        assertTrue(result.getLayoutExplanation().contains("纳音杨柳木属木"));
        assertTrue(result.getLayoutExplanation().contains("12月按节令近似为大雪冬至"));
        assertTrue(result.getLayoutExplanation().contains("子水当令，寒土收束"));
        assertTrue(result.getLayoutExplanation().contains("18日取象为火"));
        assertTrue(result.getLayoutExplanation().contains("正午按时辰取象为火"));
        assertTrue(result.getLayoutExplanation().contains("5题里火向最多（3/5）"));
        assertTrue(result.getStrengthText().contains("主火代表活力斗志、积极乐观"));
        assertTrue(result.getRelationshipText().contains("火会把能量自然推向土"));
    }

    private ElementScoreResult scoreResult(ElementType primary,
                                           ElementType secondary,
                                           int water,
                                           int wood,
                                           int fire,
                                           int metal,
                                           int earth) {
        LinkedHashMap<String, Integer> allScores = new LinkedHashMap<>();
        allScores.put("METAL", metal);
        allScores.put("WOOD", wood);
        allScores.put("WATER", water);
        allScores.put("FIRE", fire);
        allScores.put("EARTH", earth);

        ElementScoreResult result = new ElementScoreResult();
        result.setPrimaryElement(primary);
        result.setSecondaryElement(secondary);
        result.setPrimaryPercent(56);
        result.setSecondaryPercent(44);
        result.setAllScores(allScores);
        return result;
    }

    private CreateResultRequest request(int year,
                                        int month,
                                        Integer day,
                                        String timeRange,
                                        String... optionCodes) {
        CreateResultRequest request = new CreateResultRequest();
        request.setBirthYear(year);
        request.setBirthMonth(month);
        request.setBirthDay(day);
        request.setBirthTimeRange(timeRange);
        request.setAnswers(List.of(
                answer("Q1", optionCodes[0]),
                answer("Q2", optionCodes[1]),
                answer("Q3", optionCodes[2]),
                answer("Q4", optionCodes[3]),
                answer("Q5", optionCodes[4])));
        return request;
    }

    private AnswerRequest answer(String questionCode, String optionCode) {
        AnswerRequest answer = new AnswerRequest();
        answer.setQuestionCode(questionCode);
        answer.setOptionCode(optionCode);
        return answer;
    }
}
