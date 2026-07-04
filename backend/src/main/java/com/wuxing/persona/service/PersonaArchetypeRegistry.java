package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.enums.ElementType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PersonaArchetypeRegistry {

    public static final int EXPECTED_SIZE = 120;
    private static final Map<String, String> PERSONA_LABELS = StarToneRegistry.starToneNames();

    private final Map<String, PersonaArchetype> archetypes;

    public PersonaArchetypeRegistry() {
        PersonaNameRegistry.verifyComplete(PERSONA_LABELS, EXPECTED_SIZE);
        StarToneRegistry.validateStarToneRegistry();
        this.archetypes = buildAll();
        if (archetypes.size() != EXPECTED_SIZE) {
            throw new IllegalStateException("Persona archetype registry must contain " + EXPECTED_SIZE + " entries");
        }
    }

    public PersonaArchetype resolve(ElementScoreResult scoreResult) {
        ElementType primary = scoreResult.getPrimaryElement();
        ElementType secondary = scoreResult.getSecondaryElement();
        ElementType accent = findAccentElement(scoreResult, primary, secondary);
        RelationKind relationKind = relationKind(scoreResult);
        return get(primary, secondary, accent, relationKind);
    }

    public PersonaArchetype get(ElementType primary,
                                ElementType secondary,
                                ElementType accent,
                                RelationKind relationKind) {
        String id = personaTypeId(primary, secondary, accent, relationKind);
        PersonaArchetype archetype = archetypes.get(id);
        if (archetype == null) {
            throw new BusinessException("persona archetype not found: " + id);
        }
        return archetype;
    }

    public Collection<PersonaArchetype> all() {
        return archetypes.values();
    }

    public int size() {
        return archetypes.size();
    }

    public String personaTypeId(ElementType primary,
                                ElementType secondary,
                                ElementType accent,
                                RelationKind relationKind) {
        return PersonaNameRegistry.personaTypeKey(primary, secondary, accent, relationKind);
    }

    private Map<String, PersonaArchetype> buildAll() {
        Map<String, PersonaArchetype> result = new LinkedHashMap<>();
        for (ElementType primary : ElementType.values()) {
            for (ElementType secondary : ElementType.values()) {
                if (secondary == primary) {
                    continue;
                }
                for (ElementType accent : ElementType.values()) {
                    if (accent == primary || accent == secondary) {
                        continue;
                    }
                    for (RelationKind relationKind : RelationKind.values()) {
                        PersonaArchetype archetype = buildArchetype(primary, secondary, accent, relationKind);
                        result.put(archetype.getPersonaTypeId(), archetype);
                    }
                }
            }
        }
        return Map.copyOf(result);
    }

    private PersonaArchetype buildArchetype(ElementType primary,
                                            ElementType secondary,
                                            ElementType accent,
                                            RelationKind relationKind) {
        ElementProfile primaryProfile = profileOf(primary);
        ElementProfile secondaryProfile = profileOf(secondary);
        ElementProfile accentProfile = profileOf(accent);
        String personaTypeId = personaTypeId(primary, secondary, accent, relationKind);
        String personaLabel = personaLabel(primary, secondary, accent, relationKind);
        String dayMasterFrame = dayMasterFrame(primary, secondary, accent, personaLabel);
        String relationText = relationText(primary, secondary, relationKind, primaryProfile, secondaryProfile, personaLabel);
        String accentText = accentText(primary, secondary, accent, primaryProfile, secondaryProfile, accentProfile, personaLabel);
        String heavenText = heavenText(primary, secondary, accent, relationKind, primaryProfile, secondaryProfile, accentProfile);
        String humanText = humanText(primary, secondary, accent, relationKind, primaryProfile, secondaryProfile, accentProfile);
        List<String> keywords = keywords(primary, secondary, accent, relationKind, primaryProfile, secondaryProfile);
        List<GrowthAdvice> growthAdvice = growthAdvice(primary, secondary, accent, relationKind, primaryProfile, secondaryProfile, accentProfile);
        PersonaArchetype archetype = new PersonaArchetype(
                personaTypeId,
                primary,
                secondary,
                accent,
                relationKind,
                personaLabel,
                keywords,
                dayMasterFrame,
                relationText,
                accentText,
                heavenText,
                humanText,
                growthAdvice);
        return archetype;
    }

    private String relationText(ElementType primary,
                                ElementType secondary,
                                RelationKind relationKind,
                                ElementProfile primaryProfile,
                                ElementProfile secondaryProfile,
                                String personaLabel) {
        return String.join("\n\n",
                ElementVoiceRegistry.primaryReaction(primary),
                ElementVoiceRegistry.secondaryCorrection(primary, secondary),
                ElementVoiceRegistry.relationText(primary, secondary, relationKind, personaLabel));
    }

    private String dayMasterFrame(ElementType primary,
                                  ElementType secondary,
                                  ElementType accent,
                                  String personaLabel) {
        return String.format(
                "这张卡会先确认出生日对应的日柱天干，再把日主翻译成性格底色；五题倾向负责显示你当下更自然的反应方式。最后由%s先发声，%s负责校准，%s留下记忆点，落成「%s」。",
                primary.getDisplayName(), secondary.getDisplayName(), accent.getDisplayName(), personaLabel);
    }

    private String accentText(ElementType primary,
                              ElementType secondary,
                              ElementType accent,
                              ElementProfile primaryProfile,
                              ElementProfile secondaryProfile,
                              ElementProfile accentProfile,
                              String personaLabel) {
        return ElementVoiceRegistry.accentText(primary, secondary, accent, personaLabel);
    }

    private String heavenText(ElementType primary,
                              ElementType secondary,
                              ElementType accent,
                              RelationKind relationKind,
                              ElementProfile primaryProfile,
                              ElementProfile secondaryProfile,
                              ElementProfile accentProfile) {
        return ElementVoiceRegistry.innerWorldText(primary, secondary, accent, relationKind);
    }

    private String humanText(ElementType primary,
                             ElementType secondary,
                             ElementType accent,
                             RelationKind relationKind,
                             ElementProfile primaryProfile,
                             ElementProfile secondaryProfile,
                             ElementProfile accentProfile) {
        return ElementVoiceRegistry.outerWorldText(primary, secondary, accent, relationKind);
    }

    private String primaryRoleImage(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "霜后的银针";
            case WOOD -> "山间的青枝";
            case WATER -> "深潭的静水";
            case FIRE -> "灯盏的明焰";
            case EARTH -> "山体的厚土";
        };
    }

    private String secondaryRoleImage(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "袖口的银锁";
            case WOOD -> "水边的竹径";
            case WATER -> "石缝的暗流";
            case FIRE -> "远处的灯火";
            case EARTH -> "潭边的砾土";
        };
    }

    private String primaryAction(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "先分辨边界、标准和轻重，再决定是否出手";
            case WOOD -> "先寻找生长方向，再把想法展开成计划";
            case WATER -> "先接住情绪、细节和气氛，再慢慢判断方向";
            case FIRE -> "先被目标和兴趣点亮，再把热度推成行动";
            case EARTH -> "先确认责任、秩序和安全感，再稳定推进";
        };
    }

    private String secondaryEffect(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "更有边界、标准和判断锋面";
            case WOOD -> "更能表达、延展和持续生长";
            case WATER -> "更细腻、留有弹性，也更能读懂暗流";
            case FIRE -> "更有目标感、启动感和被看见的热度";
            case EARTH -> "更能承重、收束，也更容易落地";
        };
    }

    private String innerQuestion(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "这件事有没有清楚标准";
            case WOOD -> "它能不能继续生长";
            case WATER -> "这里面真实流动的感受是什么";
            case FIRE -> "它值不值得被点亮";
            case EARTH -> "这件事能不能被稳稳接住";
        };
    }

    private String externalSignal(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "你收束信息、给出判断、守住边界的时刻";
            case WOOD -> "你把想法长成计划、持续推进的过程";
            case WATER -> "你读懂气氛、替复杂局面留出余地的时候";
            case FIRE -> "你把目标说亮、带动现场行动的瞬间";
            case EARTH -> "你把责任接住、把节奏稳定下来的时候";
        };
    }

    private String classicalAnchor(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "传统月令把秋金放在肃清、法度和收敛之气里，重点不是冷，而是把边界辨清";
            case WOOD -> "传统月令写春木主萌动、生发与布德，重点不是冒进，而是让方向慢慢伸出来";
            case WATER -> "《老子》以水喻善，重在处下、涵容与不争，重点不是退缩，而是先接住更深的信息";
            case FIRE -> "月令里的火气近炎上、显明和祝融之象，重点不是喧哗，而是让目标被照亮";
            case EARTH -> "月令以中央土配戊己、后土，重在承载、调和和让万物有地可归";
        };
    }

    private List<String> keywords(ElementType primary,
                                  ElementType secondary,
                                  ElementType accent,
                                  RelationKind relationKind,
                                  ElementProfile primaryProfile,
                                  ElementProfile secondaryProfile) {
        List<String> result = new ArrayList<>();
        result.add(keywordForPrimary(primary));
        result.add(keywordForSecondary(secondary));
        result.add(keywordForAccent(accent));
        result.add(relationKind == RelationKind.BALANCED ? "双向节奏" : "自我校准");
        result.add(keywordForPrimaryGift(primary));
        return result.stream().distinct().limit(5).toList();
    }

    private String keywordForPrimary(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "边界感";
            case WOOD -> "成长感";
            case WATER -> "观察力";
            case FIRE -> "目标感";
            case EARTH -> "稳定感";
        };
    }

    private String keywordForSecondary(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "自我校准";
            case WOOD -> "表达欲";
            case WATER -> "共情感";
            case FIRE -> "内在热度";
            case EARTH -> "责任感";
        };
    }

    private String keywordForAccent(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "辨识度";
            case WOOD -> "生长余地";
            case WATER -> "深层回声";
            case FIRE -> "点睛感";
            case EARTH -> "承接力";
        };
    }

    private String keywordForPrimaryGift(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "清醒判断";
            case WOOD -> "长期耐心";
            case WATER -> "适应力";
            case FIRE -> "行动感";
            case EARTH -> "慢热";
        };
    }

    private List<GrowthAdvice> growthAdvice(ElementType primary,
                                            ElementType secondary,
                                            ElementType accent,
                                            RelationKind relationKind,
                                            ElementProfile primaryProfile,
                                            ElementProfile secondaryProfile,
                                            ElementProfile accentProfile) {
        return ElementVoiceRegistry.growthAdvice(primary, secondary, accent, relationKind);
    }

    private String primaryAdviceText(ElementType primary,
                                     ElementType secondary,
                                     ElementProfile primaryProfile,
                                     ElementProfile secondaryProfile) {
        String secondLayer = secondary.getDisplayName() + "的" + secondaryProfile.supportGift();
        return switch (primary) {
            case METAL -> String.format("做决定前先写下完成标准、不能让步的边界和可以调整的余地。金的力量怕含糊，标准一清楚，再让%s补上弹性，行动会更利落。", secondLayer);
            case WOOD -> String.format("把目标拆成一条会生长的路线：今天先发芽，明天再修枝。木的力量怕被憋住，先做出可见的一小段，再让%s把方向稳住。", secondLayer);
            case WATER -> String.format("先把脑内感受分成三栏：事实、猜测、下一步。水的力量怕在心里反复回旋，信息一落纸，再让%s把它接成可执行的节奏。", secondLayer);
            case FIRE -> String.format("行动前给目标一句明亮的定义：我要点亮什么、影响谁、做到哪里算完成。火的力量怕烧散，再让%s托住，就能热而不乱。", secondLayer);
            case EARTH -> String.format("接任务前先确认责任范围、交付时间和收尾方式。土的力量怕一味承接，边界立住之后，再让%s帮你打开下一步。", secondLayer);
        };
    }

    private String balancedAdviceText(ElementType primary,
                                      ElementType secondary,
                                      ElementProfile primaryProfile,
                                      ElementProfile secondaryProfile) {
        return String.format(
                "%s和%s接近时，不必急着判定哪一个才是真正的你。把一件事拆成开场、推进、收尾三段：开场交给%s，推进交给%s，收尾判断哪一股更稳，这样双气会变成节奏，而不是拉扯。",
                primary.getDisplayName(),
                secondary.getDisplayName(),
                primaryProfile.shortGift(),
                secondaryProfile.shortGift());
    }

    private String secondaryAdviceText(ElementType primary,
                                       ElementType secondary,
                                       ElementProfile primaryProfile,
                                       ElementProfile secondaryProfile) {
        return switch (secondary) {
            case METAL -> String.format("%s走得太快或太散时，用金补一道裁量：删掉一个多余选项，定一条验收标准，再继续推进。这样%s不会只靠感觉，而会有清楚锋面。", primary.getDisplayName(), primaryProfile.shortGift());
            case WOOD -> String.format("%s停在判断或承接里太久时，用木开一条生路：给方案加一个小试验、一个表达出口或一个可迭代版本，让%s继续长出来。", primary.getDisplayName(), primaryProfile.shortGift());
            case WATER -> String.format("%s急着定论时，用水多问一层：我看见的是事实，还是情绪的回声？这一问会让%s多出缓冲，也减少误判。", primary.getDisplayName(), primaryProfile.shortGift());
            case FIRE -> String.format("%s沉得太久时，用火做一个小公开动作：说出计划、发出邀请、交付雏形。火会把%s从心里推到现实里。", primary.getDisplayName(), primaryProfile.shortGift());
            case EARTH -> String.format("%s开始漂移时，用土压到地面：写进日程、划出责任、确认收尾。土会让%s从想法变成别人能接住的结果。", primary.getDisplayName(), primaryProfile.shortGift());
        };
    }

    private String accentAdviceText(ElementType primary,
                                    ElementType secondary,
                                    ElementType accent,
                                    ElementProfile primaryProfile,
                                    ElementProfile secondaryProfile,
                                    ElementProfile accentProfile) {
        return switch (accent) {
            case METAL -> String.format("金像袖口忽然响起的清铃。卡住时别再加内容，先删一个不必要的承诺，写下一条最清楚的标准；这一声会让%s和%s都有边界。", primaryProfile.shortGift(), secondaryProfile.shortGift());
            case WOOD -> String.format("木像石缝里冒出的青芽。局面太满时，给自己留一个低成本分支：试写一版、试问一次、试做一天；这一芽会让%s和%s重新有生机。", primaryProfile.shortGift(), secondaryProfile.shortGift());
            case WATER -> String.format("水像杯底返上来的回声。情绪浓的时候，先等三分钟，把别人说过的话复述成事实；这一层回声会让%s和%s不被表面声响带走。", primaryProfile.shortGift(), secondaryProfile.shortGift());
            case FIRE -> String.format("火像砾坡深处的一粒伏火。你不必一直高调，但关键处要点一下：主动开口、提交雏形、亮出目标；这一点光会让%s和%s看见入口。", primaryProfile.shortGift(), secondaryProfile.shortGift());
            case EARTH -> String.format("土像花径尽头的隐台。想法悬着时，给它一个固定位置：一个时间块、一张清单、一次复盘；这一方台面会让%s和%s站稳。", primaryProfile.shortGift(), secondaryProfile.shortGift());
        };
    }

    private String actionAdviceText(ElementType primary,
                                    ElementType secondary,
                                    RelationKind relationKind,
                                    ElementProfile primaryProfile,
                                    ElementProfile secondaryProfile) {
        if (relationKind == RelationKind.BALANCED) {
            return String.format(
                    "每周给两种气各留一个位置：一个任务专门练%s，一个任务专门练%s。你会慢慢分清什么时候该展开，什么时候该收束，而不是让两股力量在同一天互相抢方向。",
                    primaryProfile.innerGift(),
                    secondaryProfile.supportGift());
        }
        return String.format(
                "每天结束前问自己一句：今天我用了%s的第一反应，也有没有补上%s的第二层？只要补一个具体动作，这张卡的优势就不会偏成单线。",
                primary.getDisplayName(),
                secondary.getDisplayName());
    }

    private String primaryAdviceTitle(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "先定标准";
            case WOOD -> "先搭枝架";
            case WATER -> "先沉信息";
            case FIRE -> "先点目标";
            case EARTH -> "先立台基";
        };
    }

    private String secondaryAdviceTitle(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "让边界说话";
            case WOOD -> "给想法生枝";
            case WATER -> "给判断留水";
            case FIRE -> "让目标见光";
            case EARTH -> "把节奏落地";
        };
    }

    private String accentAdviceTitle(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "听清铃一响";
            case WOOD -> "留一条生门";
            case WATER -> "等回声返来";
            case FIRE -> "点那粒伏火";
            case EARTH -> "踩住那隐台";
        };
    }

    private ElementType findAccentElement(ElementScoreResult scoreResult,
                                          ElementType primary,
                                          ElementType secondary) {
        Map<String, Integer> allScores = scoreResult.getAllScores();
        if (allScores != null && !allScores.isEmpty()) {
            return allScores.entrySet().stream()
                    .map(entry -> Map.entry(ElementType.fromCode(entry.getKey()), entry.getValue()))
                    .filter(entry -> entry.getKey() != primary && entry.getKey() != secondary)
                    .sorted(Comparator.<Map.Entry<ElementType, Integer>>comparingInt(Map.Entry::getValue).reversed()
                            .thenComparing(entry -> entry.getKey().ordinal()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseGet(() -> firstRemaining(primary, secondary));
        }
        return firstRemaining(primary, secondary);
    }

    private ElementType firstRemaining(ElementType primary, ElementType secondary) {
        for (ElementType elementType : ElementType.values()) {
            if (elementType != primary && elementType != secondary) {
                return elementType;
            }
        }
        throw new IllegalArgumentException("primary and secondary leave no accent element");
    }

    private RelationKind relationKind(ElementScoreResult scoreResult) {
        return Math.abs(scoreResult.getPrimaryPercent() - scoreResult.getSecondaryPercent()) <= 8
                ? RelationKind.BALANCED
                : RelationKind.DOMINANT;
    }

    private String personaLabel(ElementType primary,
                                ElementType secondary,
                                ElementType accent,
                                RelationKind relationKind) {
        String id = personaTypeId(primary, secondary, accent, relationKind);
        return PersonaNameRegistry.label(PERSONA_LABELS, id);
    }

    private String accentImage(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "袖口的清铃";
            case WOOD -> "石缝的青芽";
            case WATER -> "杯底的回声";
            case FIRE -> "砾坡的伏火";
            case EARTH -> "花径的隐台";
        };
    }

    private String accentRole(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "在热闹处敲出边界，在犹豫处给出标准，让整体气质更清醒";
            case WOOD -> "在停住处留下生门，让计划、表达和成长感继续往外伸展";
            case WATER -> "把直觉和细节往深处带一点，让判断不只停在表面反应";
            case FIRE -> "平时不争亮，关键处忽然照出入口，让行动有被点燃的瞬间";
            case EARTH -> "把飘着的热情、判断或感受放回地面，让它们真正站得住";
        };
    }

    private String relationBridge(ElementType primary, ElementType secondary) {
        if (generates(primary, secondary)) {
            return String.format("按五行相生来看，%s能生%s，所以这组关系更像顺水推舟：先有主气质的积累，再自然长出下一步表达。", primary.getDisplayName(), secondary.getDisplayName());
        }
        if (generates(secondary, primary)) {
            return String.format("按五行相生来看，%s能生%s，所以这组关系像背后有源头托着：第二层力量会反过来滋养你的主气质，让它更稳，也更容易被看见。", secondary.getDisplayName(), primary.getDisplayName());
        }
        if (controls(primary, secondary)) {
            return String.format("按五行相制来看，%s会约束%s，所以这里不是冲突，而是把过强的倾向收出边界，让优势更聚焦。", primary.getDisplayName(), secondary.getDisplayName());
        }
        if (controls(secondary, primary)) {
            return String.format("按五行相制来看，%s会校准%s，所以这组关系像一只手轻轻按住节奏，让表达更有分寸，也更能落地。", secondary.getDisplayName(), primary.getDisplayName());
        }
        return String.format("%s和%s形成互补，一边提供方向，一边补足温度。", primary.getDisplayName(), secondary.getDisplayName());
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

    private ElementProfile profileOf(ElementType elementType) {
        return switch (elementType) {
            case METAL -> new ElementProfile(
                    "金",
                    "金在这里是霜后的银铃，提醒你分辨重点、边界和标准。",
                    "银铃、镜面和清晰的边界",
                    "清醒的银锁",
                    "判断、秩序和自我校准",
                    "判断力和执行感",
                    "边界清晰、标准明确",
                    "边界、标准和清醒裁量",
                    "清响边界",
                    "清醒", "秩序", "判断", "执行", "边界",
                    List.of("清亮", "含光", "澄明", "有棱", "微凉"),
                    List.of("银湾", "铜铃", "星针", "月镜", "霜锁"));
            case WOOD -> new ElementProfile(
                    "木",
                    "木在这里是山间的青枝，把想法慢慢长成计划和作品。",
                    "青枝、竹径和持续的生长",
                    "舒展的青枝",
                    "生长、规划和表达欲",
                    "规划力和创造感",
                    "持续生长、愿意展开",
                    "表达路线和持续生长",
                    "缝隙生门",
                    "成长", "规划", "创造", "耐心", "引导",
                    List.of("舒展", "青醒", "扶疏", "柔韧", "新绿"),
                    List.of("青枝", "竹径", "松针", "春坡", "花径"));
            case WATER -> new ElementProfile(
                    "水",
                    "水在这里是深潭的静水，先观察、理解，再决定怎么行动。",
                    "深水、雨露和薄雾的感知",
                    "深远的瀑布",
                    "感知、学习和深层观察",
                    "洞察力和适应感",
                    "观察细腻、反应灵活",
                    "观察弹性和细腻回旋",
                    "深处回声",
                    "观察", "共情", "适应", "细腻", "洞察",
                    List.of("深远", "澄静", "有回声", "微凉", "流动"),
                    List.of("潮汐", "湖泊", "云河", "浅滩", "清泉"));
            case FIRE -> new ElementProfile(
                    "火",
                    "火在这里是灯盏的明焰，把兴趣、表达和行动点亮。",
                    "灯火、阳光和清楚的目标",
                    "微热的星火",
                    "热情、目标和表达冲动",
                    "行动力和感染力",
                    "表达直接、能点燃现场",
                    "目标感、启动感和热度",
                    "伏火入口",
                    "热情", "行动", "表达", "感染力", "突破",
                    List.of("微热", "明亮", "轻快", "高昂", "会发光"),
                    List.of("星火", "焰灯", "晴烛", "花火", "霞庭"));
            case EARTH -> new ElementProfile(
                    "土",
                    "土在这里是山体的厚土，把关系、责任和节奏托住。",
                    "台基、山坡和厚实的地面",
                    "安静的砾坡",
                    "稳定、责任和承接感",
                    "落地感和安全感",
                    "节奏稳定、愿意承担",
                    "承接边界和稳定节奏",
                    "隐台落点",
                    "稳定", "承载", "协调", "可靠", "安全感",
                    List.of("安稳", "温润", "松软", "沉着", "厚实"),
                    List.of("瓷山", "砾坡", "陶湾", "月台", "石阶"));
        };
    }

    private record ElementProfile(String elementName,
                                  String state,
                                  String scene,
                                  String innerImage,
                                  String innerGift,
                                  String outerGift,
                                  String visibleSignal,
                                  String supportGift,
                                  String accentGift,
                                  String keywordOne,
                                  String keywordTwo,
                                  String keywordThree,
                                  String keywordFour,
                                  String keywordFive,
                                  List<String> adjectives,
                                  List<String> nouns) {
        private String shortGift() {
            return keywordTwo;
        }
    }
}
