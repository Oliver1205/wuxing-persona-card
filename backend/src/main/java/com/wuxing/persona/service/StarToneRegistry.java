package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StarToneRegistry {

    static final int EXPECTED_SIZE = 120;
    static final String STAR_TONE_LABEL = "星曜取象";

    private static final Set<String> FORBIDDEN_NAMES = Set.of("化禄", "化权", "化科", "化忌");
    private static final Set<String> FORBIDDEN_CHARS = Set.of("灾", "厄", "病", "死", "孤", "败", "凶", "煞", "忌", "刑", "亡");
    private static final Map<String, SuffixMeaning> SUFFIX_MEANINGS = suffixMeaningRegistry();
    private static final Map<String, StarTone> STAR_TONES = buildStarTones();

    private StarToneRegistry() {
    }

    static StarTone get(String personaTypeId) {
        StarTone tone = STAR_TONES.get(personaTypeId);
        if (tone == null) {
            throw new IllegalStateException("star tone missing for " + personaTypeId);
        }
        return tone;
    }

    static StarTone get(ElementType primary,
                        ElementType secondary,
                        ElementType accent,
                        RelationKind relationKind) {
        return get(PersonaNameRegistry.personaTypeKey(primary, secondary, accent, relationKind));
    }

    static Collection<StarTone> all() {
        return STAR_TONES.values();
    }

    static Map<String, String> starToneNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (StarTone tone : STAR_TONES.values()) {
            names.put(tone.getPersonaTypeId(), tone.getStarToneName());
        }
        return Map.copyOf(names);
    }

    static boolean isValidStarToneName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.contains("的")) {
            return false;
        }
        for (String forbidden : FORBIDDEN_NAMES) {
            if (value.contains(forbidden)) {
                return false;
            }
        }
        for (String forbidden : FORBIDDEN_CHARS) {
            if (value.contains(forbidden)) {
                return false;
            }
        }
        int[] codePoints = value.codePoints().toArray();
        if (codePoints.length != 4) {
            return false;
        }
        for (int codePoint : codePoints) {
            if (!Character.UnicodeScript.of(codePoint).equals(Character.UnicodeScript.HAN)) {
                return false;
            }
        }
        return true;
    }

    static void validateStarToneRegistry() {
        if (STAR_TONES.size() != EXPECTED_SIZE) {
            throw new IllegalStateException("Star tone registry must contain " + EXPECTED_SIZE
                    + " entries, actual=" + STAR_TONES.size());
        }
        Set<String> names = new HashSet<>();
        for (StarTone tone : STAR_TONES.values()) {
            if (!isValidStarToneName(tone.getStarToneName())) {
                throw new IllegalStateException("invalid star tone name: " + tone.getStarToneName());
            }
            if (!names.add(tone.getStarToneName())) {
                throw new IllegalStateException("duplicated star tone name: " + tone.getStarToneName());
            }
            String suffix = suffixOf(tone.getStarToneName());
            if (!SUFFIX_MEANINGS.containsKey(suffix)) {
                throw new IllegalStateException("unknown star tone suffix: " + tone.getStarToneName());
            }
            requirePublicSafe(tone.getStructureTitle(), tone.getPersonaTypeId(), "structureTitle");
            requirePublicSafe(tone.getHeroSummary(), tone.getPersonaTypeId(), "heroSummary");
            requirePublicSafe(tone.getIdentityLine(), tone.getPersonaTypeId(), "identityLine");
            requirePublicSafe(tone.getStarToneExplanation(), tone.getPersonaTypeId(), "starToneExplanation");
            requireFilled(tone.getInternalRationale(), tone.getPersonaTypeId(), "internalRationale");
            requireFilled(tone.getStructureTitle(), tone.getPersonaTypeId(), "structureTitle");
            requireFilled(tone.getHeroSummary(), tone.getPersonaTypeId(), "heroSummary");
            requireFilled(tone.getIdentityLine(), tone.getPersonaTypeId(), "identityLine");
            requireFilled(tone.getStarToneExplanation(), tone.getPersonaTypeId(), "starToneExplanation");
            if (tone.isTrueZiweiChart()) {
                throw new IllegalStateException("star tone must not claim true Ziwei chart: " + tone.getPersonaTypeId());
            }
        }
    }

    private static String suffixOf(String starToneName) {
        int[] codePoints = starToneName.codePoints().toArray();
        if (codePoints.length != 4) {
            return starToneName;
        }
        return new String(codePoints, 2, 2);
    }

    private static void requireFilled(String value, String personaTypeId, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(personaTypeId + " missing " + field);
        }
    }

    private static void requirePublicSafe(String value, String personaTypeId, String field) {
        requireFilled(value, personaTypeId, field);
        List<String> forbidden = List.of(
                "personaTypeId",
                "internalRationale",
                "WATER",
                "WOOD",
                "FIRE",
                "METAL",
                "EARTH",
                "dominant",
                "balanced",
                "紫微主星",
                "命宫主星",
                "真实紫微",
                "化禄",
                "化权",
                "化科",
                "化忌");
        for (String token : forbidden) {
            if (value.contains(token)) {
                throw new IllegalStateException(personaTypeId + " " + field + " leaked forbidden token: " + token);
            }
        }
    }

    private static Map<String, StarTone> buildStarTones() {
        Map<String, StarTone> result = new LinkedHashMap<>();
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
                        StarTone tone = buildStarTone(primary, secondary, accent, relationKind);
                        StarTone previous = result.put(tone.getPersonaTypeId(), tone);
                        if (previous != null) {
                            throw new IllegalStateException("duplicated star tone id: " + tone.getPersonaTypeId());
                        }
                    }
                }
            }
        }
        Map<String, StarTone> immutable = Map.copyOf(result);
        validate(immutable);
        return immutable;
    }

    private static void validate(Map<String, StarTone> tones) {
        if (tones.size() != EXPECTED_SIZE) {
            throw new IllegalStateException("Star tone registry must contain " + EXPECTED_SIZE
                    + " entries, actual=" + tones.size());
        }
        Set<String> names = new HashSet<>();
        for (StarTone tone : tones.values()) {
            if (!isValidStarToneName(tone.getStarToneName())) {
                throw new IllegalStateException("invalid star tone name: " + tone.getStarToneName());
            }
            if (!names.add(tone.getStarToneName())) {
                throw new IllegalStateException("duplicated star tone name: " + tone.getStarToneName());
            }
        }
    }

    private static StarTone buildStarTone(ElementType primary,
                                          ElementType secondary,
                                          ElementType accent,
                                          RelationKind relationKind) {
        String personaTypeId = PersonaNameRegistry.personaTypeKey(primary, secondary, accent, relationKind);
        String root = starRoot(primary, secondary);
        String suffix = toneSuffix(secondary, accent, relationKind);
        String starToneName = root + suffix;
        String structureTitle = starToneName + "：" + structurePhrase(primary) + "，" + structurePhrase(secondary);
        String heroSummary = heroSummary(primary, secondary, accent, relationKind);
        String identityLine = String.join(" · ", identityKeywords(primary, secondary, accent).subList(0, 3));
        String starToneExplanation = explanation(root, suffix, primary, secondary, accent, relationKind);
        String internalRationale = String.format(
                "%s maps %s/%s/%s/%s to root %s and suffix %s; root follows primary-secondary image, suffix follows secondary-accent relation.",
                personaTypeId,
                primary.name(),
                secondary.name(),
                accent.name(),
                relationKind.getCode(),
                root,
                suffix);
        return new StarTone(
                personaTypeId,
                starToneName,
                STAR_TONE_LABEL,
                structureTitle,
                heroSummary,
                identityLine,
                starToneExplanation,
                identityKeywords(primary, secondary, accent),
                internalRationale,
                false);
    }

    private static String starRoot(ElementType primary, ElementType secondary) {
        if (primary == ElementType.WATER && secondary == ElementType.EARTH) {
            return "太阴";
        }
        return switch (primary) {
            case WATER -> switch (secondary) {
                case METAL -> "巨门";
                case WOOD -> "文曲";
                case FIRE -> "月曜";
                case EARTH -> "太阴";
                default -> "玄水";
            };
            case FIRE -> switch (secondary) {
                case METAL -> "廉贞";
                case WOOD -> "太阳";
                case WATER -> "朱明";
                case EARTH -> "紫微";
                default -> "明曜";
            };
            case WOOD -> switch (secondary) {
                case METAL -> "文昌";
                case WATER -> "天机";
                case FIRE -> "贪狼";
                case EARTH -> "青府";
                default -> "青机";
            };
            case METAL -> switch (secondary) {
                case WOOD -> "武曲";
                case WATER -> "巨阙";
                case FIRE -> "廉锋";
                case EARTH -> "天相";
                default -> "金曜";
            };
            case EARTH -> switch (secondary) {
                case METAL -> "天府";
                case WOOD -> "天梁";
                case WATER -> "天同";
                case FIRE -> "坤曜";
                default -> "坤府";
            };
        };
    }

    private static String toneSuffix(ElementType secondary, ElementType accent, RelationKind relationKind) {
        boolean dominant = relationKind == RelationKind.DOMINANT;
        return switch (secondary) {
            case EARTH -> switch (accent) {
                case FIRE -> dominant ? "化衡" : "藏明";
                case METAL -> dominant ? "定章" : "成律";
                case WOOD -> dominant ? "成垣" : "开机";
                case WATER -> dominant ? "含澜" : "守中";
                default -> dominant ? "定衡" : "化承";
            };
            case METAL -> switch (accent) {
                case WATER -> dominant ? "入渊" : "含澜";
                case WOOD -> dominant ? "开机" : "生章";
                case FIRE -> dominant ? "含光" : "化照";
                case EARTH -> dominant ? "定衡" : "成垣";
                default -> dominant ? "成律" : "照鉴";
            };
            case WOOD -> switch (accent) {
                case WATER -> dominant ? "含澜" : "入渊";
                case FIRE -> dominant ? "启明" : "含光";
                case METAL -> dominant ? "照鉴" : "成律";
                case EARTH -> dominant ? "定衡" : "化承";
                default -> dominant ? "开机" : "生枝";
            };
            case FIRE -> switch (accent) {
                case WATER -> dominant ? "藏思" : "入渊";
                case WOOD -> dominant ? "开阳" : "启明";
                case METAL -> dominant ? "照鉴" : "凝锋";
                case EARTH -> dominant ? "成垣" : "定衡";
                default -> dominant ? "含光" : "化照";
            };
            case WATER -> switch (accent) {
                case FIRE -> dominant ? "藏明" : "流照";
                case WOOD -> dominant ? "生枝" : "开机";
                case METAL -> dominant ? "照鉴" : "定章";
                case EARTH -> dominant ? "守中" : "化承";
                default -> dominant ? "入渊" : "含澜";
            };
        };
    }

    private static String structurePhrase(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "金律定界";
            case WOOD -> "木气开枝";
            case WATER -> "水感入心";
            case FIRE -> "火光照路";
            case EARTH -> "土气成形";
        };
    }

    private static String rootImage(String root) {
        return switch (root) {
            case "太阴" -> "阴水、月光、细感之象";
            case "太阳" -> "火光、显现、行动之象";
            case "天府" -> "府库、承载、收纳之象";
            case "天梁" -> "原则、庇护、秩序之象";
            case "天机" -> "变化、思维、规划之象";
            case "天相" -> "协调、体面、权衡之象";
            case "武曲" -> "执行、规则、现实判断之象";
            case "文昌" -> "学习、条理、文气之象";
            case "文曲" -> "感受、表达、审美之象";
            case "巨门" -> "洞察、辨析、深问之象";
            case "贪狼" -> "生长、探索、表现之象";
            case "廉贞" -> "边界、锋芒、热度之象";
            case "紫微" -> "主轴、统摄、秩序之象";
            case "天同" -> "温和、调适、包容之象";
            case "月曜" -> "水火交照、暗处见光之象";
            case "朱明" -> "明火照水、热度入流之象";
            case "青府" -> "木气入库、成长有地之象";
            case "巨阙" -> "金石开阙、暗流见锋之象";
            case "廉锋" -> "火中见锋、原则带热之象";
            case "坤曜" -> "厚土含光、承载有明之象";
            default -> root + "取传统星曜式意象";
        };
    }

    private static Map<String, SuffixMeaning> suffixMeaningRegistry() {
        Map<String, SuffixMeaning> meanings = new LinkedHashMap<>();
        addSuffix(meanings, ElementType.WATER, "含澜", "入渊", "藏思", "流照");
        addSuffix(meanings, ElementType.FIRE, "含光", "藏明", "化照", "启明", "开阳");
        addSuffix(meanings, ElementType.EARTH, "成垣", "定衡", "化承", "守中", "化衡");
        addSuffix(meanings, ElementType.METAL, "照鉴", "成律", "凝锋", "定章");
        addSuffix(meanings, ElementType.WOOD, "开机", "生章", "生枝", "舒荣");
        return Map.copyOf(meanings);
    }

    private static void addSuffix(Map<String, SuffixMeaning> target, ElementType element, String... suffixes) {
        for (String suffix : suffixes) {
            target.put(suffix, suffixMeaning(element, suffix));
        }
    }

    private static SuffixMeaning suffixMeaning(ElementType element, String suffix) {
        return switch (element) {
            case WATER -> new SuffixMeaning(element, suffix, "水象", "感受、流动、回声、深层信息与回旋空间");
            case FIRE -> new SuffixMeaning(element, suffix, "火象", "照亮、目标、表达、显现与启动感");
            case EARTH -> new SuffixMeaning(element, suffix, "土象", "承接、秩序、稳定、边界与现实落点");
            case METAL -> new SuffixMeaning(element, suffix, "金象", "标准、辨识、判断、边界与规则");
            case WOOD -> new SuffixMeaning(element, suffix, "木象", "路径、表达、生长、延展与展开");
        };
    }

    private static String suffixImage(String suffix) {
        SuffixMeaning meaning = SUFFIX_MEANINGS.get(suffix);
        if (meaning == null) {
            return "一层辅助取象";
        }
        return meaning.familyName() + "里的" + meaning.publicMeaning();
    }

    private static String explanation(String root,
                                      String suffix,
                                      ElementType primary,
                                      ElementType secondary,
                                      ElementType accent,
                                      RelationKind relationKind) {
        String relationText = relationKind == RelationKind.BALANCED
                ? "主副两股力量接近，它更像两种气质在同一处互相映照"
                : "主元素先定下第一反应，副元素负责把反应校准成可落地的节奏";
        String accentText = accentPointText(accent);
        return String.format(
                "「%s」取%s；「%s」取%s。放在本次结果里，它说明%s；%s作为点睛，不负责主导，却会%s。",
                root,
                rootImage(root),
                suffix,
                suffixImage(suffix),
                relationText,
                accent.getDisplayName(),
                accentText);
    }

    private static String heroSummary(ElementType primary,
                                      ElementType secondary,
                                      ElementType accent,
                                      RelationKind relationKind) {
        if (primary == ElementType.WATER && secondary == ElementType.EARTH && accent == ElementType.FIRE) {
            return "你先把世界接进心里，再用秩序把感受落下来；真正重要的事，会在暗处亮起一盏灯。";
        }
        int variant = Math.floorMod(primary.ordinal() * 7 + secondary.ordinal() * 3 + accent.ordinal()
                + relationKind.ordinal(), 5);
        return switch (variant) {
            case 0 -> String.format(
                    "你先以%s进入局势，再让%s把反应收束成形；%s不总是显眼，却会在关键处留下记忆点。",
                    primaryAction(primary), secondaryFunction(secondary), accentImage(accent));
            case 1 -> String.format(
                    "你的第一反应常从%s开始，随后由%s校准节奏；%s像暗处的一笔，让整张卡多出余味。",
                    primaryAction(primary), secondaryFunction(secondary), accentImage(accent));
            case 2 -> String.format(
                    "%s决定你最先看见什么，%s决定你如何把它落下去；%s让这个结构不至于单薄。",
                    primaryAction(primary), secondaryFunction(secondary), accentImage(accent));
            case 3 -> String.format(
                    "你不是单靠%s行动，%s会把它收进更清楚的节奏；%s出现时，整张卡会多一点亮度、分寸或回声。",
                    primaryAction(primary), secondaryFunction(secondary), accentImage(accent));
            default -> String.format(
                    "外界先触发你的%s，随后%s会帮你调整方向；%s是最后那一笔，让这份气质更容易被记住。",
                    primaryAction(primary), secondaryFunction(secondary), accentImage(accent));
        };
    }

    private static List<String> identityKeywords(ElementType primary, ElementType secondary, ElementType accent) {
        return List.of(
                primaryKeyword(primary),
                secondaryKeyword(secondary),
                accentKeyword(accent),
                "自我校准",
                primaryGift(primary));
    }

    private static String primaryKeyword(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "判断力";
            case WOOD -> "生长力";
            case WATER -> "观察力";
            case FIRE -> "行动力";
            case EARTH -> "稳定力";
        };
    }

    private static String secondaryKeyword(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "边界感";
            case WOOD -> "延展感";
            case WATER -> "适应力";
            case FIRE -> "点亮感";
            case EARTH -> "责任感";
        };
    }

    private static String accentKeyword(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "分寸感";
            case WOOD -> "生机感";
            case WATER -> "回声感";
            case FIRE -> "点睛感";
            case EARTH -> "承接感";
        };
    }

    private static String primaryGift(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "清晰标准";
            case WOOD -> "持续成长";
            case WATER -> "深层感知";
            case FIRE -> "目标热度";
            case EARTH -> "稳住局面";
        };
    }

    private static String primaryAction(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "边界和标准";
            case WOOD -> "生长和规划";
            case WATER -> "感受和信息";
            case FIRE -> "热度和目标";
            case EARTH -> "责任和秩序";
        };
    }

    private static String secondaryFunction(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "规则";
            case WOOD -> "路径";
            case WATER -> "流动";
            case FIRE -> "目标";
            case EARTH -> "秩序";
        };
    }

    private static String accentImage(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "一枚清铃";
            case WOOD -> "一枝青芽";
            case WATER -> "一层暗澜";
            case FIRE -> "一盏微灯";
            case EARTH -> "一方台基";
        };
    }

    private static String accentPointText(ElementType elementType) {
        return switch (elementType) {
            case METAL -> "让热闹处多出一声清铃，提醒边界和分寸";
            case WOOD -> "像石缝里探出的新枝，让结构继续生长";
            case WATER -> "像杯底返上的回声，给判断留出深处的余地";
            case FIRE -> "像暗处忽然亮起的一点光，把真正重要的目标照出来";
            case EARTH -> "像一方能承住东西的台面，让反应有现实落点";
        };
    }

    private record SuffixMeaning(ElementType element, String suffix, String familyName, String publicMeaning) {
    }
}
