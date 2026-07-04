package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.List;

final class ElementVoiceRegistry {

    private ElementVoiceRegistry() {
    }

    static String primaryReaction(ElementType element) {
        return switch (element) {
            case WOOD -> "木让你的第一反应先去寻找生长路径。事情一出现，你会自然地想它能不能展开、能不能规划、能不能带来新的可能。你不是只看眼前结果，而是容易看见下一步、下一层和更长的延展。";
            case FIRE -> "火让你的第一反应先被目标和热度点燃。你会很快感受到自己想不想做、有没有意义、能不能被看见。你的动力往往不是冷冰冰的任务，而是内心那一下被点亮的感觉。";
            case EARTH -> "土让你的第一反应先稳住局面。你会关心这件事是否可靠、是否能落地、是否有清晰边界。你不喜欢完全失控的状态，更愿意把事情放进一个可承接、可持续的节奏里。";
            case METAL -> "金让你的第一反应先判断标准。你会先分辨哪里对、哪里不对，规则是什么，边界在哪里。你对混乱和含糊比较敏感，更希望事情有清楚的逻辑、标准和执行方式。";
            case WATER -> "水让你的第一反应先接收信息。你会先观察环境、捕捉细节、理解关系，再决定如何行动。你不是没有行动力，而是需要先让局势在心里变得清楚。";
        };
    }

    static String secondaryCorrection(ElementType primary, ElementType secondary) {
        return String.format(
                "%s是第二层力量，像%s旁边的%s。它不负责抢走主旋律，而是给第一反应加上第二层处理方式：%s",
                secondary.getDisplayName(),
                primary.getDisplayName(),
                secondaryImage(secondary),
                pairCorrection(primary, secondary));
    }

    static String relationText(ElementType primary, ElementType secondary, RelationKind relationKind, String personaLabel) {
        String relationTone = relationTone(primary, secondary);
        if (relationKind == RelationKind.BALANCED) {
            return String.format(
                    "当%s和%s接近时，你会呈现双声部：有些场景先出现%s，有些场景又会被%s拉回。它们不是互相否定，而是在共同塑造你；所以「%s」更像两套反应系统轮流接管。%s",
                    primary.getDisplayName(),
                    secondary.getDisplayName(),
                    shortReaction(primary),
                    shortReaction(secondary),
                    personaLabel,
                    relationTone);
        }
        return String.format(
                "%s先发声，%s随后给它加边界、温度或落点。真正先推动你的是%s这股反应，但让它不至于走偏的是%s。所以「%s」不是只靠本能推进，而是会把第一反应交给第二层力量再处理一遍。%s",
                primary.getDisplayName(),
                secondary.getDisplayName(),
                shortReaction(primary),
                shortCorrection(secondary),
                personaLabel,
                relationTone);
    }

    static String accentText(ElementType primary, ElementType secondary, ElementType accent, String personaLabel) {
        return String.format(
                "%s是点睛元素，像%s。%s于是你不只停在%s，也不只靠%s行动；你身上还有一点%s，让「%s」更有反差、入口和余味。",
                accent.getDisplayName(),
                accentImage(accent),
                accentOpening(accent, primary, secondary),
                primaryFlavor(primary),
                secondaryFlavor(secondary),
                accentHiddenPower(accent),
                personaLabel);
    }

    static String innerWorldText(ElementType primary, ElementType secondary, ElementType accent, RelationKind relationKind) {
        if (relationKind == RelationKind.BALANCED) {
            return String.format(
                    "你的内心更像两套反应系统在轮流接管。信息进来以后，%s会先让你%s，%s又会提醒你%s。你真正需要的不是立刻把自己定成某一种样子，而是允许两种反应按场景切换；当%s出现时，它会给这套内在节奏补上一点%s，让你更容易从感受走向判断。",
                    primary.getDisplayName(),
                    innerProcess(primary),
                    secondary.getDisplayName(),
                    innerProcess(secondary),
                    accent.getDisplayName(),
                    accentInnerGift(accent));
        }
        return String.format(
                "你的内心通常先沿着%s的路径启动：信息进来以后，你会%s。随后%s会补上一层%s，让反应不只停在本能里。真正需要的不是压掉第一反应，而是给它一个更清楚的出口；%s则像暗处的记号，在关键时刻带来%s。",
                primary.getDisplayName(),
                innerProcess(primary),
                secondary.getDisplayName(),
                shortCorrection(secondary),
                accent.getDisplayName(),
                accentInnerGift(accent));
    }

    static String outerWorldText(ElementType primary, ElementType secondary, ElementType accent, RelationKind relationKind) {
        String opening = relationKind == RelationKind.BALANCED
                ? String.format("刚认识你的人，可能会感觉你身上同时有%s和%s两种节奏", primary.getDisplayName(), secondary.getDisplayName())
                : String.format("刚认识你的人，通常会先感到你身上有%s的%s", primary.getDisplayName(), outerSignal(primary));
        return String.format(
                "%s。熟悉之后，他们会发现你不只是表面的这一层，%s会让你多出%s，%s则在不显眼处补上一点%s。在合作里，你给人的感觉往往不是随便出手，而是会用自己的节奏把事情推到更合适的位置。",
                opening,
                secondary.getDisplayName(),
                outerSignal(secondary),
                accent.getDisplayName(),
                accentOuterGift(accent));
    }

    static String stuckText(ElementType primary, ElementType secondary, ElementType accent) {
        return String.format(
                "这个结构最容易卡住的地方，不是你没有能力，而是%s用得太满时会变成%s。当事情压力变大，你可能会%s；如果%s没有及时接住，%s的点睛也容易只停在心里。这不是缺陷，而是提醒你要给自己的反应模式留一个现实出口。",
                primary.getDisplayName(),
                overusePattern(primary),
                stuckBehavior(primary),
                secondary.getDisplayName(),
                accent.getDisplayName());
    }

    static List<GrowthAdvice> growthAdvice(ElementType primary,
                                           ElementType secondary,
                                           ElementType accent,
                                           RelationKind relationKind) {
        return List.of(
                new GrowthAdvice(primaryAdviceTitle(primary), primaryAdviceText(primary)),
                new GrowthAdvice(secondaryAdviceTitle(secondary), secondaryAdviceText(primary, secondary, relationKind)),
                new GrowthAdvice(accentAdviceTitle(accent), accentAdviceText(accent)),
                new GrowthAdvice("固定复盘出口", "每天结束前只复盘一个问题：今天哪一次反应最像这张卡？把它写成事实、感受和下一步。这样你不是反复揣摩自己，而是在把人格结构沉淀成可观察、可调整的行动习惯。"));
    }

    private static String pairCorrection(ElementType primary, ElementType secondary) {
        if (primary == ElementType.WATER && secondary == ElementType.EARTH) {
            return "水让你先观察、吸收和理解，土则像河岸与砾坡，把这股流动收进计划、责任和结果里。没有土，你容易想很多；有了土，感受会更容易落成可执行的节奏。";
        }
        if (primary == ElementType.FIRE && secondary == ElementType.METAL) {
            return "火让你被目标点燃，金像镜面和清铃，提醒你看清标准与边界。没有金，火容易一时上头；有了金，你会更愿意把热情变成清晰的判断和执行。";
        }
        if (primary == ElementType.WOOD && secondary == ElementType.EARTH) {
            return "木让你看到生长和可能，土像可以种下种子的台地，把可能放进现实。没有土，木容易想得很远但落得很散；有了土，你会更在意节奏、承接和持续。";
        }
        return String.format("%s让你先%s，%s则负责%s。这层力量不推翻底色，只是把第一反应导向更清楚的边界、出口和落点。",
                primary.getDisplayName(), shortReaction(primary), secondary.getDisplayName(), shortCorrection(secondary));
    }

    private static String relationTone(ElementType primary, ElementType secondary) {
        if (generates(primary, secondary)) {
            return String.format("这组关系比较顺，%s先提供%s，%s再把它推向%s，所以你的气质里会有一种从内在反应到外在行动的自然流动。",
                    primary.getDisplayName(), shortReaction(primary), secondary.getDisplayName(), shortReaction(secondary));
        }
        if (controls(primary, secondary) || controls(secondary, primary)) {
            return String.format("这组关系会带来一点内在张力：一部分你会沿着%s的节奏走，另一部分又会提醒你%s。如果用得好，这不是消耗，而是让你既有反应速度，也有自我校准。",
                    shortReaction(primary), shortCorrection(secondary));
        }
        return "这组关系不靠单一方向取胜，而是靠相互校准：一边提供第一反应，一边提醒你怎样把它放进现实。";
    }

    private static boolean generates(ElementType from, ElementType to) {
        return (from == ElementType.WOOD && to == ElementType.FIRE)
                || (from == ElementType.FIRE && to == ElementType.EARTH)
                || (from == ElementType.EARTH && to == ElementType.METAL)
                || (from == ElementType.METAL && to == ElementType.WATER)
                || (from == ElementType.WATER && to == ElementType.WOOD);
    }

    private static boolean controls(ElementType from, ElementType to) {
        return (from == ElementType.WOOD && to == ElementType.EARTH)
                || (from == ElementType.EARTH && to == ElementType.WATER)
                || (from == ElementType.WATER && to == ElementType.FIRE)
                || (from == ElementType.FIRE && to == ElementType.METAL)
                || (from == ElementType.METAL && to == ElementType.WOOD);
    }

    private static String shortReaction(ElementType element) {
        return switch (element) {
            case WOOD -> "寻找生长路径";
            case FIRE -> "目标被点亮";
            case EARTH -> "稳住局面";
            case METAL -> "判断标准边界";
            case WATER -> "接收信息";
        };
    }

    private static String shortCorrection(ElementType element) {
        return switch (element) {
            case WOOD -> "给想法一条生长路线";
            case FIRE -> "给反应一点亮度和启动感";
            case EARTH -> "把反应放进责任、秩序和承接里";
            case METAL -> "立起标准、边界和判断";
            case WATER -> "保留观察、理解和回旋空间";
        };
    }

    private static String primaryFlavor(ElementType element) {
        return switch (element) {
            case WOOD -> "向前生长";
            case FIRE -> "靠热度推进";
            case EARTH -> "负责稳定承接";
            case METAL -> "用标准裁量";
            case WATER -> "暗处观察";
        };
    }

    private static String secondaryFlavor(ElementType element) {
        return switch (element) {
            case WOOD -> "表达和延展";
            case FIRE -> "目标和热度";
            case EARTH -> "承接和落地";
            case METAL -> "边界和标准";
            case WATER -> "观察和回旋";
        };
    }

    private static String accentTaste(ElementType element) {
        return switch (element) {
            case WOOD -> "生长感";
            case FIRE -> "亮度";
            case EARTH -> "落地感";
            case METAL -> "辨识度";
            case WATER -> "深度";
        };
    }

    private static String accentOpening(ElementType accent, ElementType primary, ElementType secondary) {
        String pair = primary.getDisplayName() + "和" + secondary.getDisplayName();
        return switch (accent) {
            case WOOD -> "木不一定站在最前面，却像石缝里探出的新枝，让" + pair + "之间多一条可以继续展开的路。";
            case FIRE -> "火像暗处忽然亮起的一点光，让" + pair + "构成的结构在真正重视的事上出现目标和热度。";
            case EARTH -> "土像一方能承住东西的台面，让" + pair + "带来的反应多一个现实落点。";
            case METAL -> "金像袖口里轻轻一响的清铃，让" + pair + "之间多出分寸、边界和辨识力。";
            case WATER -> "水像杯底返上来的回声，让" + pair + "之外保留理解、感受和回旋的余地。";
        };
    }

    private static String accentImage(ElementType element) {
        return switch (element) {
            case WOOD -> "石缝里探出的一点青芽";
            case FIRE -> "深处忽然露出的伏火";
            case EARTH -> "花径尽头一方隐台";
            case METAL -> "袖口里轻轻一响的清铃";
            case WATER -> "杯底返上来的回声";
        };
    }

    private static String secondaryImage(ElementType element) {
        return switch (element) {
            case WOOD -> "竹径与青枝";
            case FIRE -> "灯火与余温";
            case EARTH -> "河岸与砾坡";
            case METAL -> "镜面与清铃";
            case WATER -> "暗流与回声";
        };
    }

    private static String accentHiddenPower(ElementType element) {
        return switch (element) {
            case WOOD -> "把事情讲出来、展开成路径的生长感";
            case FIRE -> "真正重视时被点燃的表达欲和证明欲";
            case EARTH -> "关键时刻回到责任、秩序和承接的能力";
            case METAL -> "在心里立起标准、划清边界的辨识力";
            case WATER -> "保留观察、理解和回旋空间的深度";
        };
    }

    private static String innerProcess(ElementType element) {
        return switch (element) {
            case WOOD -> "把感受整理成方向，寻找下一步能否继续生长";
            case FIRE -> "先确认目标是否值得被点亮，再决定要不要投入热度";
            case EARTH -> "先确认安全边界、责任位置和能不能稳定承接";
            case METAL -> "先分辨标准、轻重和哪里需要划清界线";
            case WATER -> "先接住语气、细节和环境暗流，让局势慢慢变清楚";
        };
    }

    private static String outerSignal(ElementType element) {
        return switch (element) {
            case WOOD -> "生长感和持续推进";
            case FIRE -> "行动感和表达热度";
            case EARTH -> "稳定、耐心和可交付感";
            case METAL -> "清醒、分寸和边界感";
            case WATER -> "观察力、适应感和留白";
        };
    }

    private static String accentInnerGift(ElementType element) {
        return switch (element) {
            case WOOD -> "把模糊感受长成路径的能力";
            case FIRE -> "把重要目标照出来的勇气";
            case EARTH -> "把情绪放回现实节奏的能力";
            case METAL -> "在混乱里听见标准的清醒";
            case WATER -> "让判断多一层深处回声的空间";
        };
    }

    private static String accentOuterGift(ElementType element) {
        return switch (element) {
            case WOOD -> "愿意继续生长和表达的余地";
            case FIRE -> "关键时刻会亮出来的目标感";
            case EARTH -> "让别人放心交托的落地感";
            case METAL -> "清楚、不含糊的辨识度";
            case WATER -> "愿意多理解一层的柔和回旋";
        };
    }

    private static String overusePattern(ElementType element) {
        return switch (element) {
            case WOOD -> "路径太散、想法不断变化";
            case FIRE -> "热度太急、急于证明自己";
            case EARTH -> "太求稳、把责任都扛在自己身上";
            case METAL -> "标准太高、判断太快";
            case WATER -> "信息摄入过量、反复比较";
        };
    }

    private static String stuckBehavior(ElementType element) {
        return switch (element) {
            case WOOD -> "同时开启很多方向，却迟迟不收束";
            case FIRE -> "先被情绪和目标推着走，后面又因为热度波动而疲惫";
            case EARTH -> "为了稳妥不断延后启动，或者把别人的重量也接到自己身上";
            case METAL -> "太快进入评判，容易让自己和别人都紧绷";
            case WATER -> "在脑子里演算太久，行动被思考拖住";
        };
    }

    private static String primaryAdviceTitle(ElementType element) {
        return switch (element) {
            case WOOD -> "先剪枝再生长";
            case FIRE -> "把热度放进排期";
            case EARTH -> "降低启动成本";
            case METAL -> "保留一点弹性";
            case WATER -> "把信息落到纸面";
        };
    }

    private static String primaryAdviceText(ElementType element) {
        return switch (element) {
            case WOOD -> "你不是缺想法，而是容易同时看见太多路径。每次只保留一个主枝：一个目标、一个版本、一个交付时间，让生长先集中起来。";
            case FIRE -> "你的热度来时很有力量，但不能只靠一阵状态。把目标写进日程，配上检查点和休息点，会让火更像稳定的灯，而不是一阵风里的焰。";
            case EARTH -> "你能承接事情，但容易因为想稳妥而启动太慢。把任务拆成十分钟就能开始的一步，先让地面出现一个脚印，再慢慢加重量。";
            case METAL -> "你的判断力很有用，但不要让标准一开始就把行动拦住。先给自己一个可试错版本，再用标准修正它，会比一开始追求完美更有效。";
            case WATER -> "你会接收很多信息，所以更需要把感受写下来。用事实、猜测、下一步三栏整理，能让水的理解力从脑内流动变成可行动的判断。";
        };
    }

    private static String secondaryAdviceTitle(ElementType element) {
        return switch (element) {
            case WOOD -> "给反应开生门";
            case FIRE -> "让目标见一点光";
            case EARTH -> "把节奏压到地面";
            case METAL -> "先定边界标准";
            case WATER -> "多留一层回声";
        };
    }

    private static String secondaryAdviceText(ElementType primary, ElementType secondary, RelationKind relationKind) {
        if (relationKind == RelationKind.BALANCED) {
            return String.format("当%s和%s都很有存在感时，不要急着选边。把一件事拆成开场、推进、收尾三段，让两股气各有位置，拉扯就会变成节奏。",
                    primary.getDisplayName(), secondary.getDisplayName());
        }
        return String.format("当%s的第一反应太满时，主动调用%s：%s。这样你不是被第一反应带走，而是在给它一个更稳、更清楚的出口。",
                primary.getDisplayName(), secondary.getDisplayName(), shortCorrection(secondary));
    }

    private static String accentAdviceTitle(ElementType element) {
        return switch (element) {
            case WOOD -> "留一条低成本分支";
            case FIRE -> "点亮关键动作";
            case EARTH -> "给想法一方台面";
            case METAL -> "听见那声清铃";
            case WATER -> "等回声再判断";
        };
    }

    private static String accentAdviceText(ElementType element) {
        return switch (element) {
            case WOOD -> "局面太满时，给自己留一个低成本分支：试写一版、试问一次、试做一天。木的点睛不靠声势，而靠让事情重新长出路。";
            case FIRE -> "你不必一直高调，但关键处要点一下：主动开口、提交雏形、亮出目标。火的点睛会让别人看见入口，也让你看见自己真的在推进。";
            case EARTH -> "想法悬着时，给它一个固定位置：一个时间块、一张清单、一次复盘。土的点睛会让漂浮的反应站稳，变成别人能接住的结果。";
            case METAL -> "卡住时先别再加内容，删掉一个多余承诺，写下一条最清楚的标准。金的点睛像清铃一响，会帮你重新听见边界。";
            case WATER -> "情绪浓的时候，先等三分钟，把别人说过的话复述成事实。水的点睛会让你不被表面声响带走，多留一层理解的余地。";
        };
    }
}
