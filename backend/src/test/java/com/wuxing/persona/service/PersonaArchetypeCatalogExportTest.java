package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.wuxing.persona.enums.ElementType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersonaArchetypeCatalogExportTest {

    private final PersonaArchetypeRegistry registry = new PersonaArchetypeRegistry();

    @Test
    void exportMarkdownCatalogWhenExplicitlyRequested() throws IOException {
        assumeTrue(Boolean.getBoolean("exportPersonaCatalog"));

        Path output = Path.of(System.getProperty(
                "personaCatalogPath",
                "../docs/persona-star-tone-catalog-20260703.md"));
        Files.createDirectories(output.toAbsolutePath().normalize().getParent());
        Files.writeString(output, buildMarkdown(), StandardCharsets.UTF_8);
    }

    private String buildMarkdown() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 五行人格 120 类星曜取象目录\n\n");
        markdown.append("> 本文件由 `PersonaArchetypeRegistry` 和 `StarToneRegistry` 运行时导出，用于逐条审阅 120 类四字星曜取象、核心标题、主从关系、点睛元素、天人特质和成长建议。更新时间：")
                .append(OffsetDateTime.now())
                .append("。\n\n");
        markdown.append("## 审阅规则\n\n");
        markdown.append("- 每个星曜取象名必须是四个汉字，不允许出现“的”。\n");
        markdown.append("- 不使用真实紫微四化词：化禄、化权、化科、化忌。\n");
        markdown.append("- 用户可见文案不得出现后台字段、英文枚举、比例代码或调试口径。\n");
        markdown.append("- 主元素要有具体状态，辅助元素要讲清调和方式，点睛元素要有记忆点。\n");
        markdown.append("- 天讲内心世界，人讲外部感受，成长建议必须能落地。\n\n");
        markdown.append("> 本目录用于内容审阅，`personaTypeId` 只作为内部审阅索引，不在用户端展示。\n\n");

        for (ElementType primary : ElementType.values()) {
            markdown.append("## 主元素：").append(primary.getDisplayName()).append("\n\n");
            List<PersonaArchetype> archetypes = registry.all().stream()
                    .filter(archetype -> archetype.getPrimaryElement() == primary)
                    .sorted(Comparator
                            .comparing((PersonaArchetype archetype) -> archetype.getSecondaryElement().ordinal())
                            .thenComparing(archetype -> archetype.getAccentElement().ordinal())
                            .thenComparing(archetype -> archetype.getRelationKind().ordinal()))
                    .toList();
            for (PersonaArchetype archetype : archetypes) {
                appendArchetype(markdown, archetype);
            }
        }
        return markdown.toString();
    }

    private void appendArchetype(StringBuilder markdown, PersonaArchetype archetype) {
        StarTone tone = StarToneRegistry.get(archetype.getPersonaTypeId());
        markdown.append("### ")
                .append(tone.getStarToneName())
                .append("（")
                .append(archetype.getPrimaryElement().getDisplayName())
                .append(" / ")
                .append(archetype.getSecondaryElement().getDisplayName())
                .append(" / 点睛")
                .append(archetype.getAccentElement().getDisplayName())
                .append(" / ")
                .append(relationReviewLabel(archetype.getRelationKind()))
                .append("）\n\n");
        markdown.append("- 内部索引：`")
                .append(archetype.getPersonaTypeId())
                .append("`\n");
        markdown.append("- 展示口径：")
                .append(tone.getStarToneLabel())
                .append("\n");
        markdown.append("- 核心标题：")
                .append(tone.getStructureTitle())
                .append("\n");
        markdown.append("- 顶部短句：")
                .append(tone.getHeroSummary())
                .append("\n");
        markdown.append("- 取象解释：")
                .append(tone.getStarToneExplanation())
                .append("\n");
        markdown.append("- 审阅索引：")
                .append(archetype.getPrimaryElement().getDisplayName())
                .append("为主、")
                .append(archetype.getSecondaryElement().getDisplayName())
                .append("校准、")
                .append(archetype.getAccentElement().getDisplayName())
                .append("点睛、")
                .append(relationReviewLabel(archetype.getRelationKind()))
                .append("\n");
        markdown.append("- 关键词：").append(String.join("、", tone.getKeywords())).append("\n");
        markdown.append("- 内部说明：").append(tone.getInternalRationale()).append("\n\n");
        appendBlock(markdown, "日主框架", archetype.getDayMasterFrame());
        appendBlock(markdown, "主从关系", archetype.getPrimarySecondaryText());
        appendBlock(markdown, "点睛元素", archetype.getAccentText());
        appendBlock(markdown, "天 · 内心世界", archetype.getHeavenText());
        appendBlock(markdown, "人 · 外部感受", archetype.getHumanText());
        markdown.append("**成长建议：**\n\n");
        for (GrowthAdvice advice : archetype.getGrowthAdvice()) {
            markdown.append("- **").append(advice.getTitle()).append("**：").append(advice.getText()).append("\n");
        }
        markdown.append("\n");
    }

    private void appendBlock(StringBuilder markdown, String title, String text) {
        markdown.append("**").append(title).append("**\n\n");
        markdown.append(text).append("\n\n");
    }

    private String relationReviewLabel(RelationKind relationKind) {
        return switch (relationKind) {
            case DOMINANT -> "主气较明";
            case BALANCED -> "双气相映";
        };
    }
}
