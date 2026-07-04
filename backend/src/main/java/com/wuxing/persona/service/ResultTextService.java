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

    private final PersonaArchetypeRegistry personaArchetypeRegistry;

    public ResultTextService(PersonaArchetypeRegistry personaArchetypeRegistry) {
        this.personaArchetypeRegistry = personaArchetypeRegistry;
    }

    public ResultText build(ElementScoreResult scoreResult, StarOfficer starOfficer) {
        return build(scoreResult, starOfficer, null);
    }

    public ResultText build(ElementScoreResult scoreResult, StarOfficer starOfficer, CreateResultRequest request) {
        PersonaArchetype archetype = personaArchetypeRegistry.resolve(scoreResult);
        StarTone starTone = StarToneRegistry.get(archetype.getPersonaTypeId());
        String layoutExplanation = buildGenerationBasis(request, archetype);
        String dayMasterText = buildDayMasterText(request);
        String starOfficerText = buildStarOfficerText(archetype, starOfficer);
        String stuckText = ElementVoiceRegistry.stuckText(
                archetype.getPrimaryElement(),
                archetype.getSecondaryElement(),
                archetype.getAccentElement());
        List<String> keywords = mergeKeywords(archetype, starOfficer);

        ResultText resultText = new ResultText();
        resultText.setPersonaTypeId(archetype.getPersonaTypeId());
        resultText.setAccentElement(archetype.getAccentElement().name());
        resultText.setAccentElementName(archetype.getAccentElement().getDisplayName());
        resultText.setRelationKind(archetype.getRelationKind().getCode());
        resultText.setPersonaLabel(archetype.getPersonaLabel());
        resultText.setStarToneName(starTone.getStarToneName());
        resultText.setStarToneLabel(starTone.getStarToneLabel());
        resultText.setStructureTitle(starTone.getStructureTitle());
        resultText.setHeroSummary(starTone.getHeroSummary());
        resultText.setIdentityLine(starTone.getIdentityLine());
        resultText.setStarToneExplanation(starTone.getStarToneExplanation());
        resultText.setDayMasterText(dayMasterText);
        resultText.setPrimarySecondaryText(archetype.getPrimarySecondaryText());
        resultText.setAccentText(archetype.getAccentText());
        resultText.setHeavenText(archetype.getHeavenText());
        resultText.setHumanText(archetype.getHumanText());
        resultText.setStarOfficerText(starOfficerText);
        resultText.setGrowthAdvice(archetype.getGrowthAdvice());
        resultText.setKeywords(keywords);
        resultText.setLayoutExplanation(layoutExplanation);
        resultText.setStrengthText(stuckText);
        resultText.setRelationshipText(String.join("\n\n",
                layoutExplanation,
                dayMasterText,
                archetype.getPrimarySecondaryText(),
                archetype.getAccentText(),
                starOfficerText,
                archetype.getHeavenText(),
                archetype.getHumanText(),
                stuckText));
        return resultText;
    }

    private List<String> mergeKeywords(PersonaArchetype archetype, StarOfficer starOfficer) {
        LinkedHashSet<String> result = new LinkedHashSet<>(archetype.getKeywords());
        result.addAll(starOfficer.getTraits());
        return result.stream().limit(5).toList();
    }

    private String buildGenerationBasis(CreateResultRequest request, PersonaArchetype archetype) {
        String answers = request == null ? "五题选择会作为当前反应倾向参考。" : answerRationale(request.getAnswers());
        if (request == null) {
            return String.format(
                    "出生信息作为传统文化里的气质锚点，五题选择记录你当下更自然的反应倾向。当前结果先落在%s、%s和%s的组合里。本结果用于传统文化和性格娱乐参考，不构成现实决策建议。",
                    archetype.getPrimaryElement().getDisplayName(),
                    archetype.getSecondaryElement().getDisplayName(),
                    archetype.getAccentElement().getDisplayName());
        }
        String birthAnchor = String.format("%d年%d月", request.getBirthYear(), request.getBirthMonth());
        if (request.getBirthDay() != null) {
            birthAnchor += request.getBirthDay() + "日";
        }
        BirthTimeRange timeRange = parseBirthTimeRange(request.getBirthTimeRange());
        String timeText = timeRange != null && timeRange != BirthTimeRange.UNKNOWN ? "和出生时段" : "";
        return String.format(
                "你填写的%s%s提供传统文化里的气质锚点，五题选择记录你当下更自然的反应倾向。%s两者合在一起，才形成这张五行人格卡。本结果用于传统文化和性格娱乐参考，不构成现实决策建议。",
                birthAnchor,
                timeText,
                answers);
    }

    private String buildDayMasterText(CreateResultRequest request) {
        if (request == null || request.getBirthDay() == null) {
            return "当前出生信息不足以准确换算日主，因此本卡片先以问卷五行倾向为主生成。你可以补充更准确的出生时间后重新生成完整版本。";
        }
        try {
            WuxingCalendarTerms.DayTone dayTone = WuxingCalendarTerms.dayTone(
                    request.getBirthYear(), request.getBirthMonth(), request.getBirthDay());
            DayMasterCopyRegistry.DayMasterProfile profile = DayMasterCopyRegistry.profile(dayTone.stem());
            String source = String.format(
                    "你的出生日换算到四柱里，对应的是「%s日」。日柱由天干和地支组成，前面的天干「%s」就是日主；在传统命理语境中，日主常被看作“我自己”的核心符号，像性格底色的源头，会影响你最自然的感受方式、判断方式和启动行动的方式。「%s」在十天干里属%s%s，所以你的日主核心是%s。",
                    dayTone.ganZhi(),
                    dayTone.stem(),
                    dayTone.stem(),
                    profile.polarity(),
                    profile.element().getDisplayName(),
                    profile.label());
            String tone = String.format(
                    "%s的传统意象是%s，重在%s。放到现实里，它给你的底色不是简单的元素标签，而是一种反应方式：%s它让你在接触世界时先形成自己的内在温度，再和本次五题选择里的主副元素一起，形成更完整的性格阅读。",
                    profile.label(),
                    profile.image(),
                    profile.traditionalMeaning(),
                    profile.realityMeaning());
            return source + "\n\n" + tone;
        } catch (RuntimeException ex) {
            return "当前出生信息不足以准确换算日主，因此本卡片先以问卷五行倾向为主生成。你可以补充更准确的出生时间后重新生成完整版本。";
        }
    }

    private String buildStarOfficerText(PersonaArchetype archetype, StarOfficer starOfficer) {
        if (starOfficer == null || starOfficer.getName() == null || starOfficer.getName().isBlank()) {
            return "当前星官映射信息不足，系统不会强行补写传统意象。你仍然可以参考前面的日主、主副元素和点睛元素来理解这张人格卡。";
        }
        StarOfficerCopyRegistry.StarLineage lineage = StarOfficerCopyRegistry.lineage(starOfficer);
        String traits = starOfficer.getTraits() == null || starOfficer.getTraits().isEmpty()
                ? "可记忆的气质"
                : String.join("、", starOfficer.getTraits());
        return String.format(
                "你的星官是「%s」。它属于%s，在传统意象里常和%s有关，%s。这里不把星官当作命运判断，而是把它当成一个文化锚点：它让「%s」里的%s更有画面感。",
                starOfficer.getName(),
                lineage.group(),
                lineage.imagery(),
                lineage.traits(),
                archetype.getPersonaLabel(),
                traits);
    }

    private String buildPersonaIntro(PersonaArchetype archetype,
                                     StarOfficer starOfficer,
                                     CreateResultRequest request,
                                     ElementScoreResult scoreResult) {
        ElementType primary = archetype.getPrimaryElement();
        ElementType secondary = archetype.getSecondaryElement();
        ElementType accent = archetype.getAccentElement();
        String coreIntro = buildCoreIntro(request, primary, secondary, scoreResult);
        String relationIntro = archetype.getRelationKind() == RelationKind.BALANCED
                ? String.format("%s%s也有自己的形状，是%s。两种气质彼此接近，所以它们会轮流决定你怎么看问题、怎么启动行动。%s只露出一小笔，是%s，让「%s」更有辨识度，也更有余味。",
                        coreIntro,
                        secondary.getDisplayName(),
                        elementMetaphor(secondary, false),
                        accent.getDisplayName(),
                        accentMetaphor(accent),
                        archetype.getPersonaLabel())
                : String.format("%s%s在这里不是简单陪衬，而是%s，把你的第一反应调成更能落地的节奏。%s只露出一小笔，是%s，让「%s」更有辨识度，也更有余味。",
                        coreIntro,
                        secondary.getDisplayName(),
                        elementMetaphor(secondary, false),
                        accent.getDisplayName(),
                        accentMetaphor(accent),
                        archetype.getPersonaLabel());
        StarLineage starLineage = starLineage(starOfficer);
        String starIntro = String.format(
                "你的出生月份对应「%s」：%s属于二十八宿中的%s，传统星宿体系常把这一组放在%s的意象里；%s。这里不把它当成现实命运判断，而是给「%s」补上一层%s的文化画面。",
                starOfficer.getName(),
                starOfficer.getName(),
                starLineage.group(),
                starLineage.imagery(),
                starLineage.traits(),
                archetype.getPersonaLabel(),
                String.join("、", starOfficer.getTraits()));
        return relationIntro + "\n\n" + starIntro;
    }

    private String buildCoreIntro(CreateResultRequest request,
                                  ElementType primary,
                                  ElementType secondary,
                                  ElementScoreResult scoreResult) {
        if (request != null && request.getBirthDay() != null) {
            WuxingCalendarTerms.DayTone dayTone = WuxingCalendarTerms.dayTone(
                    request.getBirthYear(), request.getBirthMonth(), request.getBirthDay());
            ElementType dayElement = dayTone.element();
            return String.format(
                    "你的日主核心是%s%s。这个词不是结果标签，而是由出生日期换算出的日柱天干得来：你填写的生日对应%s日，日柱由天干和地支组成，其中天干「%s」就是日主；%s属%s%s，所以称为%s%s。这里的%s不是单纯的物质，而是五行里偏%s的气。放到性格上，它更像%s：%s。%s",
                    dayTone.stem(),
                    dayElement.getDisplayName(),
                    dayTone.ganZhi(),
                    dayTone.stem(),
                    dayTone.stem(),
                    stemPolarity(dayTone.stem()),
                    dayElement.getDisplayName(),
                    dayTone.stem(),
                    dayElement.getDisplayName(),
                    dayElement.getDisplayName(),
                    elementEnergy(dayElement),
                    elementCoreImage(dayElement),
                    elementCoreAction(dayElement),
                    resultRatioSentence(primary, secondary, scoreResult));
        }
        return String.format(
                "你没有填写具体出生日，所以这里不强行推具体日主；本次先以选择形成的五行结构为主。%s",
                resultRatioSentence(primary, secondary, scoreResult));
    }

    private String resultRatioSentence(ElementType primary, ElementType secondary, ElementScoreResult scoreResult) {
        if (scoreResult == null) {
            return String.format("本次人格卡最终以%s为主元素、以%s为副元素。",
                    primary.getDisplayName(), secondary.getDisplayName());
        }
        return String.format("本次人格卡最终以%s为主元素，显示为%d%%；以%s为副元素，显示为%d%%。日主给你性格底色，主副元素则说明这次答题里最明显的外显倾向。",
                primary.getDisplayName(),
                scoreResult.getPrimaryPercent(),
                secondary.getDisplayName(),
                scoreResult.getSecondaryPercent());
    }

    private String elementCoreImage(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "霜后的银针";
            case WOOD -> "山间的青枝";
            case WATER -> "深潭的静水";
            case FIRE -> "灯盏的微光";
            case EARTH -> "山体的厚土";
        };
    }

    private String elementCoreAction(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "先替混乱的信息分出边界、标准和轻重，再决定如何出手";
            case WOOD -> "先寻找方向，再把想法长成计划和作品";
            case WATER -> "先把情绪、细节和环境沉下来，再慢慢判断方向";
            case FIRE -> "把兴趣、表达和行动感一起点起来";
            case EARTH -> "先把关系、责任和节奏托住，再让事情稳定推进";
        };
    }

    private String elementMetaphor(ElementType elementType, boolean primary) {
        return switch (elementType) {
            case METAL -> primary
                    ? "霜后的银针，先把混乱的信息分出边界、标准和轻重"
                    : "袖口的银锁，替整体气质收住边界，也让判断更有分寸";
            case WOOD -> primary
                    ? "山间的青枝，先寻找方向，再把想法长成计划和作品"
                    : "水边的竹径，帮主气质找到表达、成长和持续展开的路线";
            case WATER -> primary
                    ? "深潭的静水，先把情绪、细节和环境沉下来，再慢慢判断方向"
                    : "石缝的暗流，让判断多一点观察、弹性和细腻感";
            case FIRE -> primary
                    ? "灯盏的微光，把兴趣、表达和行动感一起点起来"
                    : "远处的灯火，给整体气质补上热度、目标感和出手的勇气";
            case EARTH -> primary
                    ? "山体的厚土，先把关系、责任和节奏托住，再让事情稳定推进"
                    : "潭边的砾土，给主气质一个可以停住、承重、转身的边界";
        };
    }

    private String accentMetaphor(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "袖口的清铃，不喧哗，却会在关键处提醒你看标准和边界";
            case WOOD -> "石缝的青芽，像暗处留下的生门，让稳定结构继续往外长";
            case WATER -> "杯底的回声，把直觉、表达和判断悄悄拖回更深的水面";
            case FIRE -> "砾坡的伏火，平时不争亮，关键处才把入口照出来";
            case EARTH -> "花径的隐台，让热情、流动或判断最终有地方落脚";
        };
    }

    private StarLineage starLineage(StarOfficer starOfficer) {
        return switch (starOfficer.getCode()) {
            case "JIAO_XIU" -> new StarLineage("东方青龙七宿第一宿", "春木初生、开端、伸展和方向感", "「角」有开始、探出、打开局面的意味");
            case "FANG_XIU" -> new StarLineage("东方青龙七宿第四宿", "生长进入结构、位置、规划和安顿", "「房」带空间、容纳和组织感");
            case "JI_XIU" -> new StarLineage("东方青龙七宿第七宿", "风、流动、引导和把方向吹开的力量", "「箕」有筛分、疏导和把杂乱理顺的意味");
            case "JING_XIU" -> new StarLineage("南方朱雀七宿第一宿", "火气升起、资源汇聚、行动和外放", "「井」有水源、秩序和把资源安置好的意味");
            case "XING_XIU" -> new StarLineage("南方朱雀七宿第四宿", "明亮、显现、表达和被看见", "「星」本身带光点、辨识度和抬头可见的意味");
            case "ZHANG_XIU" -> new StarLineage("南方朱雀七宿第五宿", "舒展、铺开、承接热度后的展开", "「张」有展开、拉开格局和把气势铺出去的意味");
            case "KUI_XIU" -> new StarLineage("西方白虎七宿第一宿", "秋金初起、文气、边界和标准", "「奎」常被放在文章、纹理和清晰秩序的语境里");
            case "LOU_XIU" -> new StarLineage("西方白虎七宿第二宿", "收束、统筹、仓储和守成", "「娄」带聚合、收纳和把资源拢住的意味");
            case "MAO_XIU" -> new StarLineage("西方白虎七宿第四宿", "清醒、分辨、判断和锋利边界", "「昴」有聚星成团的意象，适合做清晰辨认的锚点");
            case "NIU_XIU" -> new StarLineage("北方玄武七宿第二宿", "冬水、耐力、承接和蓄势", "「牛」带稳定、耐负和慢慢积攒力气的意味");
            case "XU_XIU" -> new StarLineage("北方玄武七宿第四宿", "冬水、幽静、收敛、藏蓄和留白", "「虚」字本身带空处、容纳、未满和深处仍有余地的意味");
            case "WEI_XIU" -> new StarLineage("北方玄武七宿第五宿", "水边的警觉、临界、适应和风险感知", "「危」不是坏结论，而是提醒人看见边界、变化和需要谨慎的位置");
            default -> new StarLineage("传统二十八宿体系", "可记忆、可归类的星象名称", "它在这里作为传统名称锚点使用");
        };
    }

    private String buildSourceExplanation(CreateResultRequest request,
                                          PersonaArchetype archetype,
                                          ElementScoreResult scoreResult) {
        ElementType primary = archetype.getPrimaryElement();
        ElementType secondary = archetype.getSecondaryElement();
        ElementType accent = archetype.getAccentElement();
        if (request == null) {
            return String.format(
                    "当前结果没有携带完整出生日期，所以不会强行展开具体日柱；系统会综合出生信息和五题倾向，把结果落在以%s铺底、由%s校准、以%s点睛的结构里。",
                    primary.getDisplayName(), secondary.getDisplayName(), accent.getDisplayName());
        }
        WuxingCalendarTerms.YearTone yearTone = WuxingCalendarTerms.yearTone(request.getBirthYear());
        WuxingCalendarTerms.MonthTone monthTone = WuxingCalendarTerms.monthTone(request.getBirthMonth());
        List<String> parts = new ArrayList<>();
        if (request.getBirthDay() != null) {
            WuxingCalendarTerms.DayTone dayTone = WuxingCalendarTerms.dayTone(
                    request.getBirthYear(), request.getBirthMonth(), request.getBirthDay());
            parts.add(buildCoreIntro(request, primary, secondary, scoreResult));
            parts.add(String.format("出生年份为%s年，天干%s属%s、地支%s属%s，就像%s。",
                    yearTone.ganZhi(),
                    yearTone.stem(),
                    yearTone.stemElement().getDisplayName(),
                    yearTone.branch(),
                    yearTone.branchElement().getDisplayName(),
                    dayYearImage(dayTone, yearTone)));
        } else {
            parts.add(String.format("你没有填写具体出生日，所以这张卡不强行展开日柱。%d年取干支纪年为%s年，纳音%s属%s，先作为外层背景参考。",
                    request.getBirthYear(),
                    yearTone.ganZhi(),
                    yearTone.nayinName(),
                    yearTone.nayinElement().getDisplayName()));
        }
        parts.add(String.format("%d月按节令近似为%s，%s，月令会给整体气质补上一层%s与%s交织的环境底色。",
                request.getBirthMonth(),
                monthTone.solarTerms(),
                monthTone.reason(),
                monthTone.main().getDisplayName(),
                monthTone.secondary().getDisplayName()));
        BirthTimeRange timeRange = parseBirthTimeRange(request.getBirthTimeRange());
        if (timeRange != null && timeRange != BirthTimeRange.UNKNOWN) {
            WuxingCalendarTerms.TimeTone timeTone = WuxingCalendarTerms.timeTone(timeRange);
            parts.add(String.format("%s按时辰取象为%s，%s。",
                    timeTone.label(), timeTone.element().getDisplayName(), timeTone.reason()));
        }
        parts.add(answerRationale(request.getAnswers()));
        parts.add(String.format("综合来看，这张卡以%s铺底、由%s校准、以%s点睛，由此落成「%s」这张五行人格卡。",
                primary.getDisplayName(), secondary.getDisplayName(), accent.getDisplayName(), archetype.getPersonaLabel()));
        return String.join(" ", parts);
    }

    private String stemPolarity(String stem) {
        return switch (stem) {
            case "甲", "丙", "戊", "庚", "壬" -> "阳";
            case "乙", "丁", "己", "辛", "癸" -> "阴";
            default -> "";
        };
    }

    private String elementEnergy(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "规则、判断、边界和执行";
            case WOOD -> "生长、规划、表达和创造";
            case WATER -> "感知、学习、流动和深层观察";
            case FIRE -> "热情、目标、表达和行动";
            case EARTH -> "稳定、承载、责任和秩序";
        };
    }

    private String elementScene(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "银铃、镜面和清晰边界";
            case WOOD -> "青枝、竹径和持续生长的方向";
            case WATER -> "深水、雨露和雾气";
            case FIRE -> "灯火、阳光和被点亮的目标";
            case EARTH -> "台基、山坡和能承接重量的地面";
        };
    }

    private String dayYearImage(WuxingCalendarTerms.DayTone dayTone, WuxingCalendarTerms.YearTone yearTone) {
        return String.format("%s遇到%s，日主保留自己的内在反应，年份再给外层气质加上一层%s",
                elementScene(dayTone.element()),
                elementScene(yearTone.stemElement()),
                toneOf(yearTone.branchElement()).shortGift());
    }

    private String answerRationale(List<AnswerRequest> answers) {
        EnumMap<ElementType, Integer> counts = new EnumMap<>(ElementType.class);
        for (ElementType elementType : ElementType.values()) {
            counts.put(elementType, 0);
        }
        if (answers == null || answers.isEmpty()) {
            return "五题倾向会作为当前选择参考。";
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
            return String.format("五题结果在%s之间比较接近，说明当前选择里这几种气质都会出来。", names);
        }
        ElementType topElement = topElements.get(0);
        Map.Entry<ElementType, Integer> second = counts.entrySet().stream()
                .filter(entry -> entry.getKey() != topElement)
                .sorted(Map.Entry.<ElementType, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().ordinal()))
                .findFirst()
                .orElse(null);
        if (second != null && second.getValue() > 0) {
            return String.format("五题结果更偏向%s属性，%s属性也有一点补强。",
                    names, second.getKey().getDisplayName());
        }
        return String.format("五题结果更偏向%s属性，当前选择的第一反应比较集中。", names);
    }

    private BirthTimeRange parseBirthTimeRange(String code) {
        try {
            return BirthTimeRange.fromNullable(code);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private record ElementTone(String mainImage, String coreGift, String directGift, String shortGift, String accentGift) {
    }

    private record StarLineage(String group, String imagery, String traits) {
    }

}
