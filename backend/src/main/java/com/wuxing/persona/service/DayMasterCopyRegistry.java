package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.Map;

final class DayMasterCopyRegistry {

    private static final Map<String, DayMasterProfile> PROFILES = Map.ofEntries(
            Map.entry("甲", new DayMasterProfile("甲", ElementType.WOOD, "阳", "大树", "向上、生长、支撑和方向感",
                    "你更容易先看见一件事能不能长成长期路径，也会本能地寻找骨架、目标和可持续的支撑点。")),
            Map.entry("乙", new DayMasterProfile("乙", ElementType.WOOD, "阴", "藤蔓花草", "柔韧、适应、细腻和表达",
                    "你不一定用很硬的方式推进自己，但会沿着环境慢慢调整路径，把感受、关系和想法长成更合适的表达。")),
            Map.entry("丙", new DayMasterProfile("丙", ElementType.FIRE, "阳", "太阳", "外放、照亮、热情和感染力",
                    "你遇到真正有意义的目标时，内在会很快被点亮，也更容易把情绪、想法和行动带到明处。")),
            Map.entry("丁", new DayMasterProfile("丁", ElementType.FIRE, "阴", "灯火", "专注、温度、精神亮点和细腻表达",
                    "你的力量不一定是铺天盖地的热闹，而是能在关键处给自己和别人一点清楚的光。")),
            Map.entry("戊", new DayMasterProfile("戊", ElementType.EARTH, "阳", "山岳", "承载、稳定、责任和边界",
                    "你会先确认事情能不能站稳、责任在哪里、边界有没有立住，然后才更愿意长期投入。")),
            Map.entry("己", new DayMasterProfile("己", ElementType.EARTH, "阴", "田地", "滋养、整理、包容和现实感",
                    "你更擅长把复杂细节慢慢整理成能被承接的秩序，也容易在关系和任务里照顾到现实落点。")),
            Map.entry("庚", new DayMasterProfile("庚", ElementType.METAL, "阳", "矿石和刀剑", "决断、执行、锋芒和原则",
                    "你对混乱和含糊不太迟钝，常会先分辨标准、轻重和边界，再决定自己该如何出手。")),
            Map.entry("辛", new DayMasterProfile("辛", ElementType.METAL, "阴", "珠玉和镜面", "精致、辨别、标准和审美",
                    "你会更敏锐地看见细节、质感和分寸，也更在意一件事是否清楚、准确、值得被认真对待。")),
            Map.entry("壬", new DayMasterProfile("壬", ElementType.WATER, "阳", "江河", "格局、探索、变化和流动",
                    "你不容易只盯着眼前一点，会自然地把信息放进更大的系统里，寻找可以流动、转换和探索的空间。")),
            Map.entry("癸", new DayMasterProfile("癸", ElementType.WATER, "阴", "雨露和泉脉", "感知、细腻、吸收和深思",
                    "你更习惯先接收环境、情绪和细节，让局势在心里慢慢变清楚，再决定自己该怎么行动。"))
    );

    private DayMasterCopyRegistry() {
    }

    static DayMasterProfile profile(String stem) {
        DayMasterProfile profile = PROFILES.get(stem);
        if (profile == null) {
            throw new IllegalArgumentException("unknown heavenly stem: " + stem);
        }
        return profile;
    }

    record DayMasterProfile(String stem,
                            ElementType element,
                            String polarity,
                            String image,
                            String traditionalMeaning,
                            String realityMeaning) {

        String label() {
            return stem + element.getDisplayName();
        }
    }
}
