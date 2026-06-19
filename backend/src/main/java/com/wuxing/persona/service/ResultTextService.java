package com.wuxing.persona.service;

import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.enums.BirthTimeRange;
import com.wuxing.persona.enums.ElementType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ResultTextService {

    public ResultText build(ElementScoreResult scoreResult, StarOfficer starOfficer) {
        return build(scoreResult, starOfficer, null);
    }

    public ResultText build(ElementScoreResult scoreResult, StarOfficer starOfficer, CreateResultRequest request) {
        ElementType primary = scoreResult.getPrimaryElement();
        ElementType secondary = scoreResult.getSecondaryElement();
        RankedElement accent = findAccentElement(scoreResult, primary, secondary);
        ElementTone primaryTone = toneOf(primary);
        ElementTone accentTone = accent == null ? null : toneOf(accent.elementType());
        String relation = relationText(primary, secondary);
        String accentLine = accentTone == null
                ? "其他元素作为背景气质，提供留白和弹性，不被理解为缺失。"
                : String.format("%s排在第三，像一笔点睛的颜色，让你的%s多了%s。",
                        accent.elementType().getDisplayName(), primaryTone.coreGift(), accentTone.accentGift());
        List<String> keywords = buildKeywords(primary, secondary, accent, starOfficer);

        ResultText resultText = new ResultText();
        resultText.setKeywords(keywords);
        resultText.setLayoutExplanation(buildSourceExplanation(request, primary, secondary));
        resultText.setStrengthText(buildElementExplanation(primary, secondary, accent));
        resultText.setRelationshipText(buildOverviewExplanation(primary, secondary, accent, relation, accentLine, starOfficer));
        return resultText;
    }

    private String buildSourceExplanation(CreateResultRequest request, ElementType primary, ElementType secondary) {
        if (request == null) {
            return String.format("判定依据：系统综合出生信息和5题选择后，五行总分落在主%s、次%s。",
                    primary.getDisplayName(), secondary.getDisplayName());
        }
        WuxingCalendarTerms.YearTone yearTone = WuxingCalendarTerms.yearTone(request.getBirthYear());
        WuxingCalendarTerms.MonthTone monthTone = WuxingCalendarTerms.monthTone(request.getBirthMonth());
        List<String> parts = new ArrayList<>();
        parts.add(String.format("%d年取干支纪年为%s年，天干%s属%s、地支%s属%s，纳音%s属%s",
                request.getBirthYear(),
                yearTone.ganZhi(),
                yearTone.stem(),
                yearTone.stemElement().getDisplayName(),
                yearTone.branch(),
                yearTone.branchElement().getDisplayName(),
                yearTone.nayinName(),
                yearTone.nayinElement().getDisplayName()));
        parts.add(String.format("%d月按节令近似为%s，%s，主%s辅%s",
                request.getBirthMonth(),
                monthTone.solarTerms(),
                monthTone.reason(),
                monthTone.main().getDisplayName(),
                monthTone.secondary().getDisplayName()));
        if (request.getBirthDay() != null) {
            WuxingCalendarTerms.DayTone dayTone = WuxingCalendarTerms.dayTone(request.getBirthDay());
            parts.add(String.format("%d日取象为%s，%s",
                    request.getBirthDay(), dayTone.element().getDisplayName(), dayTone.reason()));
        }
        BirthTimeRange timeRange = parseBirthTimeRange(request.getBirthTimeRange());
        if (timeRange != null && timeRange != BirthTimeRange.UNKNOWN) {
            WuxingCalendarTerms.TimeTone timeTone = WuxingCalendarTerms.timeTone(timeRange);
            parts.add(String.format("%s按时辰取象为%s，%s",
                    timeTone.label(), timeTone.element().getDisplayName(), timeTone.reason()));
        }
        parts.add(answerRationale(request.getAnswers()));
        return String.format("判定依据：%s，所以总分形成主%s、次%s。",
                String.join("；", parts), primary.getDisplayName(), secondary.getDisplayName());
    }

    private String buildElementExplanation(ElementType primary, ElementType secondary, RankedElement accent) {
        List<String> parts = new ArrayList<>();
        parts.add(String.format("主%s代表%s", primary.getDisplayName(), toneOf(primary).directGift()));
        parts.add(String.format("次%s带来%s", secondary.getDisplayName(), toneOf(secondary).supportGift()));
        if (accent != null) {
            parts.add(String.format("%s作为点睛，提供%s", accent.elementType().getDisplayName(), toneOf(accent.elementType()).accentGift()));
        }
        return String.join("；", parts) + "。";
    }

    private String buildOverviewExplanation(ElementType primary,
                                            ElementType secondary,
                                            RankedElement accent,
                                            String relation,
                                            String accentLine,
                                            StarOfficer starOfficer) {
        String accentSummary = accent == null
                ? "其他元素作为背景，保留弹性。"
                : accentLine;
        return String.format("%s；%s整体看，这是一个既有%s，也有%s的命盘，%s让这种气质更有辨识度。",
                relation, accentSummary, toneOf(primary).shortGift(), toneOf(secondary).shortGift(), starOfficer.getName());
    }

    private List<String> buildKeywords(ElementType primary,
                                       ElementType secondary,
                                       RankedElement accent,
                                       StarOfficer starOfficer) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(primary.getKeywords().get(0));
        result.add(primary.getKeywords().get(1));
        result.add(secondary.getKeywords().get(0));
        if (accent != null) {
            result.add(accent.elementType().getKeywords().get(0));
        } else {
            result.add(primary.getKeywords().get(2));
        }
        result.add(starOfficer.getTraits().get(0));
        return new ArrayList<>(result).stream().limit(5).toList();
    }

    private RankedElement findAccentElement(ElementScoreResult scoreResult,
                                            ElementType primary,
                                            ElementType secondary) {
        Map<String, Integer> allScores = scoreResult.getAllScores();
        if (allScores == null || allScores.isEmpty()) {
            return null;
        }
        int total = allScores.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return null;
        }
        return allScores.entrySet().stream()
                .map(entry -> new RankedElement(ElementType.fromCode(entry.getKey()), entry.getValue()))
                .filter(entry -> entry.elementType() != primary && entry.elementType() != secondary)
                .sorted(Comparator.comparingInt(RankedElement::score).reversed()
                        .thenComparing(entry -> entry.elementType().ordinal()))
                .filter(entry -> entry.score() * 100.0 / total >= 13)
                .findFirst()
                .orElse(null);
    }

    private String relationText(ElementType primary, ElementType secondary) {
        if (generates(primary, secondary)) {
            return String.format("%s会把能量自然推向%s，像把想法变成行动路径",
                    primary.getDisplayName(), secondary.getDisplayName());
        }
        if (generates(secondary, primary)) {
            return String.format("%s在背后滋养%s，让你的主气质更稳定、更容易被看见",
                    secondary.getDisplayName(), primary.getDisplayName());
        }
        if (controls(primary, secondary)) {
            return String.format("%s给%s加上一层边界感，让优势更聚焦",
                    primary.getDisplayName(), secondary.getDisplayName());
        }
        if (controls(secondary, primary)) {
            return String.format("%s给%s带来校准力，让你的表达更有分寸",
                    secondary.getDisplayName(), primary.getDisplayName());
        }
        return String.format("%s和%s形成互补，一边提供方向，一边补足温度",
                primary.getDisplayName(), secondary.getDisplayName());
    }

    private String answerRationale(List<AnswerRequest> answers) {
        EnumMap<ElementType, Integer> counts = new EnumMap<>(ElementType.class);
        for (ElementType elementType : ElementType.values()) {
            counts.put(elementType, 0);
        }
        if (answers == null || answers.isEmpty()) {
            return "5题倾向作为当前选择参考";
        }
        for (AnswerRequest answer : answers) {
            ElementType elementType = ElementType.fromCode(answer.getOptionCode());
            counts.put(elementType, counts.get(elementType) + 1);
        }
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<ElementType> topElements = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();
        String names = topElements.stream().map(ElementType::getDisplayName).reduce((a, b) -> a + "、" + b).orElse("五行");
        if (topElements.size() > 1) {
            return String.format("5题选择在%s之间接近（各%d题）", names, max);
        }
        ElementType topElement = topElements.get(0);
        Map.Entry<ElementType, Integer> second = counts.entrySet().stream()
                .filter(entry -> entry.getKey() != topElement)
                .sorted(Map.Entry.<ElementType, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().ordinal()))
                .findFirst()
                .orElse(null);
        if (second != null && second.getValue() > 0) {
            return String.format("5题里%s向最多（%d/5），%s向也有%d题补强",
                    names, max, second.getKey().getDisplayName(), second.getValue());
        }
        return String.format("5题里%s向最多（%d/5）", names, max);
    }

    private BirthTimeRange parseBirthTimeRange(String code) {
        try {
            return BirthTimeRange.fromNullable(code);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean generates(ElementType source, ElementType target) {
        return switch (source) {
            case WOOD -> target == ElementType.FIRE;
            case FIRE -> target == ElementType.EARTH;
            case EARTH -> target == ElementType.METAL;
            case METAL -> target == ElementType.WATER;
            case WATER -> target == ElementType.WOOD;
        };
    }

    private boolean controls(ElementType source, ElementType target) {
        return switch (source) {
            case WOOD -> target == ElementType.EARTH;
            case EARTH -> target == ElementType.WATER;
            case WATER -> target == ElementType.FIRE;
            case FIRE -> target == ElementType.METAL;
            case METAL -> target == ElementType.WOOD;
        };
    }

    private ElementTone toneOf(ElementType elementType) {
        return switch (elementType) {
            case METAL -> new ElementTone("清醒的判断力", "判断和边界", "清醒判断、秩序边界", "清醒裁量", "秩序感与决断力");
            case WOOD -> new ElementTone("向上生长的规划力", "成长和创造", "成长规划、创造耐心", "长期生长力", "生长感与长期耐心");
            case WATER -> new ElementTone("细腻流动的观察力", "观察和共情", "观察共情、灵活适应", "洞察与弹性", "灵动感与洞察力");
            case FIRE -> new ElementTone("把现场点亮的行动力", "热情和表达", "活力斗志、积极乐观", "探索和点燃力", "活力、表达和点睛之笔");
            case EARTH -> new ElementTone("稳稳托住局面的承载力", "稳定和协调", "踏实稳重、坚定执行", "务实基础", "安全感与承接力");
        };
    }

    private record RankedElement(ElementType elementType, int score) {
    }

    private record ElementTone(String mainImage, String coreGift, String directGift, String shortGift, String accentGift) {
        private String supportGift() {
            return directGift;
        }
    }
}
