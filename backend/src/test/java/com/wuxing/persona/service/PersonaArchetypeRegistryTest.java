package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wuxing.persona.enums.ElementType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PersonaArchetypeRegistryTest {

    private final PersonaArchetypeRegistry registry = new PersonaArchetypeRegistry();
    private static final List<String> FORBIDDEN_USER_TEXT = List.of(
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
            "MBTI",
            "WATER",
            "WOOD",
            "FIRE",
            "METAL",
            "EARTH",
            "dominant",
            "balanced");
    private static final List<String> OLD_TEMPLATE_TEXT = List.of(
            "两种材料在同一件器物里",
            "辅助气质，不只是陪衬",
            "藏在画面暗处的一枚记号",
            "两种声音彼此校准",
            "别人会先从你身上看到",
            "先顺着",
            "适合在旁边提醒你",
            "不是主角，却适合当启动机关",
            "三步收束",
            "固定复盘、固定输出和可执行的时间块");

    @Test
    void shouldContainExactlyOneHundredTwentyArchetypes() {
        assertEquals(120, registry.size());
        assertEquals(120, registry.all().stream().map(PersonaArchetype::getPersonaTypeId).distinct().count());
        assertEquals(120, registry.all().stream().map(PersonaArchetype::getPersonaLabel).distinct().count());
    }

    @Test
    void personaLabelShapeShouldUseFourCharacterStarToneName() {
        assertValidPersonaLabel("太阴化衡");
        assertValidPersonaLabel("廉贞入渊");
        assertValidPersonaLabel("青府化衡");
        assertValidPersonaLabel("巨阙生枝");

        assertFalse(isValidPersonaLabel("酷的沙砾"));
        assertFalse(isValidPersonaLabel("太阴化禄"));
        assertFalse(isValidPersonaLabel("水岸的灯"));
        assertFalse(isValidPersonaLabel("WATER化衡"));
        assertFalse(isValidPersonaLabel("太阴化衡长"));
    }

    @Test
    void shouldCoverEveryPrimarySecondaryAccentRelationCombination() {
        Set<String> ids = new HashSet<>();
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
                        PersonaArchetype archetype = registry.get(primary, secondary, accent, relationKind);
                        assertNotNull(archetype);
                        assertEquals(primary, archetype.getPrimaryElement());
                        assertEquals(secondary, archetype.getSecondaryElement());
                        assertEquals(accent, archetype.getAccentElement());
                        assertEquals(relationKind, archetype.getRelationKind());
                        assertFalse(archetype.getPersonaLabel().isBlank());
                        assertValidPersonaLabel(archetype.getPersonaLabel());
                        assertFalse(archetype.getDayMasterFrame().isBlank());
                        assertFalse(archetype.getDayMasterFrame().contains("日主说明框架"));
                        assertFalse(archetype.getPrimarySecondaryText().isBlank());
                        assertFalse(archetype.getAccentText().isBlank());
                        assertFalse(archetype.getHeavenText().isBlank());
                        assertFalse(archetype.getHumanText().isBlank());
                        assertEquals(4, archetype.getGrowthAdvice().size());
                        ids.add(archetype.getPersonaTypeId());
                    }
                }
            }
        }
        assertEquals(120, ids.size());
    }

    @Test
    void everyArchetypeShouldKeepUserFacingCopyComplete() {
        for (PersonaArchetype archetype : registry.all()) {
            assertUserFacingText(archetype, "dayMasterFrame", archetype.getDayMasterFrame(), 48);
            assertUserFacingText(archetype, "primarySecondaryText", archetype.getPrimarySecondaryText(), 72);
            assertUserFacingText(archetype, "accentText", archetype.getAccentText(), 54);
            assertUserFacingText(archetype, "heavenText", archetype.getHeavenText(), 50);
            assertUserFacingText(archetype, "humanText", archetype.getHumanText(), 50);
            assertTrue(archetype.getPrimarySecondaryText().contains(archetype.getPrimaryElement().getDisplayName()),
                    archetype.getPersonaTypeId() + " should mention primary element in primarySecondaryText");
            assertTrue(archetype.getPrimarySecondaryText().contains(archetype.getSecondaryElement().getDisplayName()),
                    archetype.getPersonaTypeId() + " should mention secondary element in primarySecondaryText");
            assertTrue(archetype.getAccentText().contains(archetype.getAccentElement().getDisplayName()),
                    archetype.getPersonaTypeId() + " should mention accent element in accentText");
            assertTrue(archetype.getHeavenText().contains("你"),
                    archetype.getPersonaTypeId() + " heavenText should use second-person wording");
            assertTrue(archetype.getHumanText().contains("你"),
                    archetype.getPersonaTypeId() + " humanText should use second-person wording");
            for (GrowthAdvice advice : archetype.getGrowthAdvice()) {
                assertUserFacingText(archetype, "growthAdvice.title", advice.getTitle(), 3);
                assertUserFacingText(archetype, "growthAdvice.text", advice.getText(), 20);
            }
            String combined = String.join("\n",
                    archetype.getDayMasterFrame(),
                    archetype.getPrimarySecondaryText(),
                    archetype.getAccentText(),
                    archetype.getHeavenText(),
                    archetype.getHumanText(),
                    archetype.getGrowthAdvice().stream()
                            .map(advice -> advice.getTitle() + "\n" + advice.getText())
                            .reduce("", (left, right) -> left + "\n" + right));
            for (String oldTemplate : OLD_TEMPLATE_TEXT) {
                assertFalse(combined.contains(oldTemplate),
                        archetype.getPersonaTypeId() + " kept old thin template: " + oldTemplate);
            }
        }
    }

    @Test
    void resolveShouldUseTopRemainingElementAndStableRelationKind() {
        ElementScoreResult scoreResult = new ElementScoreResult();
        scoreResult.setPrimaryElement(ElementType.WATER);
        scoreResult.setSecondaryElement(ElementType.FIRE);
        scoreResult.setPrimaryPercent(51);
        scoreResult.setSecondaryPercent(47);
        scoreResult.setAllScores(java.util.Map.of(
                "WATER", 51,
                "FIRE", 47,
                "EARTH", 38,
                "METAL", 24,
                "WOOD", 19));

        PersonaArchetype archetype = registry.resolve(scoreResult);

        assertEquals("WATER-FIRE-EARTH-balanced", archetype.getPersonaTypeId());
        assertValidPersonaLabel(archetype.getPersonaLabel());
    }

    @Test
    void sampleArchetypesShouldUseV2ReusableCopySystem() {
        assertV2Sample("WATER-EARTH-FIRE-dominant", ElementType.WATER, ElementType.EARTH, ElementType.FIRE,
                "太阴化衡", "水让你的第一反应先接收信息");
        assertV2Sample("FIRE-METAL-WATER-dominant", ElementType.FIRE, ElementType.METAL, ElementType.WATER,
                "廉贞入渊", "火让你的第一反应先被目标和热度点燃");
        assertV2Sample("WOOD-EARTH-FIRE-dominant", ElementType.WOOD, ElementType.EARTH, ElementType.FIRE,
                "青府化衡", "木让你的第一反应先去寻找生长路径");
        assertV2Sample("METAL-WATER-WOOD-dominant", ElementType.METAL, ElementType.WATER, ElementType.WOOD,
                "巨阙生枝", "金让你的第一反应先判断标准");
        assertV2Sample("EARTH-WOOD-METAL-dominant", ElementType.EARTH, ElementType.WOOD, ElementType.METAL,
                "天梁照鉴", "土让你的第一反应先稳住局面");
    }

    @Test
    void starToneRegistryShouldExposeCompleteSafeNames() {
        assertEquals(120, StarToneRegistry.all().size());
        StarToneRegistry.validateStarToneRegistry();
        for (StarTone tone : StarToneRegistry.all()) {
            assertValidPersonaLabel(tone.getStarToneName());
            assertEquals("星曜取象", tone.getStarToneLabel());
            assertTrue(tone.getStructureTitle().startsWith(tone.getStarToneName() + "："));
            assertFalse(tone.getHeroSummary().isBlank());
            assertFalse(tone.getIdentityLine().isBlank());
            assertFalse(tone.getStarToneExplanation().contains("紫微主星"));
            assertFalse(tone.getStarToneExplanation().contains("命宫主星"));
            assertFalse(tone.isTrueZiweiChart());
        }
    }

    private void assertV2Sample(String id,
                                ElementType primary,
                                ElementType secondary,
                                ElementType accent,
                                String expectedLabel,
                                String expectedSnippet) {
        RelationKind relationKind = id.endsWith("-balanced") ? RelationKind.BALANCED : RelationKind.DOMINANT;
        PersonaArchetype archetype = registry.get(primary, secondary, accent, relationKind);
        assertEquals(id, archetype.getPersonaTypeId());
        assertEquals(expectedLabel, archetype.getPersonaLabel());
        assertTrue(archetype.getPrimarySecondaryText().contains(expectedSnippet));
        assertTrue(archetype.getPrimarySecondaryText().contains(secondary.getDisplayName() + "是第二层力量"));
        assertTrue(archetype.getAccentText().contains(accent.getDisplayName() + "是点睛元素"));
        assertFalse(archetype.getAccentText().contains("让这张卡更有记忆点"));
        assertTrue(archetype.getAccentText().contains("反差、入口和余味"));
        assertTrue(archetype.getHeavenText().contains("你的内心"));
        assertTrue(archetype.getHumanText().contains("刚认识你的人"));
        assertFalse(archetype.getPrimarySecondaryText().contains("主从关系："));
        assertFalse(archetype.getAccentText().contains("占比"));
        assertFalse(archetype.getHeavenText().contains("MBTI"));
        assertEquals(4, archetype.getGrowthAdvice().size());
    }

    private void assertValidPersonaLabel(String label) {
        assertTrue(isValidPersonaLabel(label), "invalid persona label shape: " + label);
    }

    private boolean isValidPersonaLabel(String label) {
        return StarToneRegistry.isValidStarToneName(label);
    }

    private void assertUserFacingText(PersonaArchetype archetype, String field, String text, int minLength) {
        assertNotNull(text, archetype.getPersonaTypeId() + " " + field + " should not be null");
        assertTrue(text.codePointCount(0, text.length()) >= minLength,
                archetype.getPersonaTypeId() + " " + field + " is too thin: " + text);
        for (String forbidden : FORBIDDEN_USER_TEXT) {
            assertFalse(text.contains(forbidden),
                    archetype.getPersonaTypeId() + " " + field + " leaked forbidden text: " + forbidden + " in " + text);
        }
    }
}
