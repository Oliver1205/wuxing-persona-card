package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.enums.ElementType;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultTextServiceTest {

    private final PersonaArchetypeRegistry registry = new PersonaArchetypeRegistry();
    private final ResultTextService service = new ResultTextService(registry);

    private static final List<String> FORBIDDEN_VISIBLE_TEXT = List.of(
            "personaTypeId",
            "命中类型",
            "底色清晰型",
            "双气互照型",
            "主从关系：",
            "星宿部分",
            "日主说明框架",
            "第二属性",
            "后台字段",
            "占比",
            "/5",
            "primaryPercent",
            "secondaryPercent",
            "score gap",
            "match route",
            "shortUrl",
            "算法判断",
            "MBTI",
            "WATER",
            "WOOD",
            "FIRE",
            "METAL",
            "EARTH",
            "dominant",
            "balanced");

    private static final List<String> FORBIDDEN_FATALISTIC_TEXT = List.of(
            "你一定会",
            "你注定",
            "你必然",
            "你命中带",
            "你迟早会",
            "此命主",
            "大富大贵",
            "必有灾",
            "破财",
            "婚姻失败",
            "身体疾病",
            "死亡",
            "灾祸",
            "保持自信",
            "发挥优势",
            "顺其自然",
            "做最好的自己",
            "相信未来",
            "坚持就好",
            "你很特别",
            "你很优秀",
            "灵气很重",
            "能量很高",
            "磁场很强",
            "福报很厚",
            "贵人很多",
            "天选之人");

    @Test
    void buildShouldSeparateResultPageSectionsByV2Responsibilities() {
        ResultText result = service.build(
                scoreResult(ElementType.WATER, ElementType.EARTH, ElementType.FIRE),
                starOfficer(),
                request(2005, 12, 5, "NOON", "WATER", "WATER", "WOOD", "FIRE", "EARTH"));

        assertEquals("WATER-EARTH-FIRE-dominant", result.getPersonaTypeId());
        assertValidPersonaLabel(result.getPersonaLabel());
        assertEquals("太阴化衡", result.getPersonaLabel());
        assertEquals("太阴化衡", result.getStarToneName());
        assertEquals("星曜取象", result.getStarToneLabel());
        assertEquals("太阴化衡：水感入心，土气成形", result.getStructureTitle());
        assertEquals("观察力 · 责任感 · 点睛感", result.getIdentityLine());
        assertTrue(result.getHeroSummary().contains("你先把世界接进心里"));
        assertTrue(result.getStarToneExplanation().contains("「太阴」"));
        assertTrue(result.getStarToneExplanation().contains("「化衡」"));
        assertFalse(result.getStarToneExplanation().contains("紫微主星"));
        assertFalse(result.getStarToneExplanation().contains("化禄"));

        assertFalse(result.getLayoutExplanation().contains("这张卡由两类信息合成"));
        assertTrue(result.getLayoutExplanation().contains("你填写的2005年12月5日和出生时段"));
        assertTrue(result.getLayoutExplanation().contains("传统文化里的气质锚点"));
        assertTrue(result.getLayoutExplanation().contains("五题选择记录"));
        assertTrue(result.getLayoutExplanation().contains("不构成现实决策建议"));
        assertFalse(result.getLayoutExplanation().contains("日主核心"));

        assertTrue(result.getDayMasterText().contains("对应的是「癸亥日」"));
        assertTrue(result.getDayMasterText().contains("天干「癸」就是日主"));
        assertTrue(result.getDayMasterText().contains("日主核心是癸水"));
        assertTrue(result.getDayMasterText().contains("雨露和泉脉"));
        assertTrue(result.getDayMasterText().contains("日柱由天干和地支组成"));
        assertFalse(result.getDayMasterText().contains("星官"));

        assertTrue(result.getPrimarySecondaryText().contains("水让你的第一反应先接收信息"));
        assertTrue(result.getPrimarySecondaryText().contains("土是第二层力量"));
        assertTrue(result.getPrimarySecondaryText().contains("土则像河岸与砾坡"));
        assertFalse(result.getPrimarySecondaryText().contains("日柱"));

        assertTrue(result.getAccentText().contains("火是点睛元素"));
        assertTrue(result.getAccentText().contains("伏火"));
        assertFalse(result.getAccentText().contains("第三"));

        assertTrue(result.getStarOfficerText().contains("你的星官是「虚宿」"));
        assertTrue(result.getStarOfficerText().contains("北方玄武七宿第四宿"));
        assertTrue(result.getStarOfficerText().contains("传统意象"));
        assertFalse(result.getStarOfficerText().contains("不负责判断命运"));
        assertFalse(result.getStarOfficerText().contains("日主"));

        assertTrue(result.getHeavenText().contains("你的内心"));
        assertTrue(result.getHumanText().contains("刚认识你的人"));
        assertTrue(result.getHumanText().contains("熟悉之后"));
        assertTrue(result.getHumanText().contains("在合作里"));
        assertTrue(result.getStrengthText().contains("最容易卡住"));
        assertTrue(result.getStrengthText().contains("这不是缺陷"));
        assertEquals(4, result.getGrowthAdvice().size());
        assertTrue(result.getGrowthAdvice().stream().allMatch(advice -> advice.getText().contains("。")));
        assertNoForbiddenVisibleText(result.getPersonaTypeId(), result);
    }

    @Test
    void buildShouldUseFallbackWhenDayMasterCannotBeCalculated() {
        ResultText result = service.build(
                scoreResult(ElementType.WATER, ElementType.WOOD, ElementType.FIRE),
                starOfficer(),
                request(2001, 2, null, null, "WATER", "WATER", "WOOD", "WOOD", "FIRE"));

        assertTrue(result.getDayMasterText().contains("当前出生信息不足以准确换算日主"));
        assertTrue(result.getDayMasterText().contains("先以问卷五行倾向为主生成"));
        assertTrue(result.getLayoutExplanation().contains("五题选择"));
        assertFalse(result.getDayMasterText().contains("硬编"));
        assertNoForbiddenVisibleText(result.getPersonaTypeId(), result);
    }

    @Test
    void buildShouldPassFiveV2SampleCombinations() {
        List<Sample> samples = List.of(
                new Sample(ElementType.WATER, ElementType.EARTH, ElementType.FIRE),
                new Sample(ElementType.FIRE, ElementType.METAL, ElementType.WATER),
                new Sample(ElementType.WOOD, ElementType.EARTH, ElementType.FIRE),
                new Sample(ElementType.METAL, ElementType.WATER, ElementType.WOOD),
                new Sample(ElementType.EARTH, ElementType.WOOD, ElementType.METAL));

        for (Sample sample : samples) {
            ResultText result = service.build(
                    scoreResult(sample.primary(), sample.secondary(), sample.accent()),
                    starOfficer(),
                    request(2005, 12, 5, "NOON",
                            sample.primary().name(),
                            sample.primary().name(),
                            sample.secondary().name(),
                            sample.accent().name(),
                            sample.secondary().name()));

            assertEquals(sample.primary() + "-" + sample.secondary() + "-" + sample.accent() + "-dominant",
                    result.getPersonaTypeId());
            assertUserFacingV2Copy(result, sample);
            assertNoForbiddenVisibleText(result.getPersonaTypeId(), result);
        }
    }

    private void assertUserFacingV2Copy(ResultText result, Sample sample) {
        assertTrue(result.getLayoutExplanation().contains("你"));
        assertTrue(result.getDayMasterText().contains("你"));
        assertTrue(result.getPrimarySecondaryText().contains("你"));
        assertTrue(result.getAccentText().contains("你"));
        assertTrue(result.getStarOfficerText().contains("你"));
        assertTrue(result.getHeavenText().contains("你"));
        assertTrue(result.getHumanText().contains("你"));
        assertTrue(result.getStrengthText().contains("你"));
        assertTrue(result.getPrimarySecondaryText().contains(sample.primary().getDisplayName()));
        assertTrue(result.getPrimarySecondaryText().contains(sample.secondary().getDisplayName()));
        assertTrue(result.getAccentText().contains(sample.accent().getDisplayName()));
        assertTrue(result.getAccentText().contains("点睛元素"));
        assertTrue(result.getStrengthText().contains("现实出口"));
        assertTrue(result.getGrowthAdvice().stream().allMatch(advice ->
                advice.getTitle() != null && !advice.getTitle().isBlank()
                        && advice.getText() != null && advice.getText().length() >= 30));
    }

    private ElementScoreResult scoreResult(ElementType primary, ElementType secondary, ElementType accent) {
        LinkedHashMap<String, Integer> allScores = new LinkedHashMap<>();
        for (ElementType elementType : ElementType.values()) {
            int score = 18;
            if (elementType == primary) {
                score = 80;
            } else if (elementType == secondary) {
                score = 54;
            } else if (elementType == accent) {
                score = 46;
            }
            allScores.put(elementType.name(), score);
        }
        ElementScoreResult result = new ElementScoreResult();
        result.setPrimaryElement(primary);
        result.setSecondaryElement(secondary);
        result.setPrimaryPercent(66);
        result.setSecondaryPercent(34);
        result.setAllScores(allScores);
        return result;
    }

    private StarOfficer starOfficer() {
        return new StarOfficer("XU_XIU", "虚宿", ElementType.WATER, List.of("深思", "留白", "包容"));
    }

    private void assertValidPersonaLabel(String label) {
        assertTrue(StarToneRegistry.isValidStarToneName(label), "invalid star tone label: " + label);
    }

    private void assertNoForbiddenVisibleText(String personaTypeId, ResultText result) {
        String visibleText = String.join("\n",
                result.getPersonaLabel(),
                result.getStarToneName(),
                result.getStarToneLabel(),
                result.getStructureTitle(),
                result.getHeroSummary(),
                result.getIdentityLine(),
                result.getStarToneExplanation(),
                result.getStarOfficerText(),
                result.getDayMasterText(),
                result.getPrimarySecondaryText(),
                result.getAccentText(),
                result.getHeavenText(),
                result.getHumanText(),
                result.getLayoutExplanation(),
                result.getStrengthText(),
                result.getRelationshipText(),
                result.getGrowthAdvice().stream()
                        .map(advice -> advice.getTitle() + advice.getText())
                        .reduce("", String::concat));
        for (String forbidden : FORBIDDEN_VISIBLE_TEXT) {
            assertFalse(visibleText.contains(forbidden),
                    personaTypeId + " visible text leaked forbidden text: " + forbidden + " in " + visibleText);
        }
        for (String forbidden : FORBIDDEN_FATALISTIC_TEXT) {
            assertFalse(visibleText.contains(forbidden),
                    personaTypeId + " visible text leaked fatalistic text: " + forbidden + " in " + visibleText);
        }
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

    private record Sample(ElementType primary, ElementType secondary, ElementType accent) {
    }
}
