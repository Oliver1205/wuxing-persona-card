package com.wuxing.persona.service;

import com.wuxing.persona.enums.BirthTimeRange;
import com.wuxing.persona.enums.ElementType;

final class WuxingCalendarTerms {

    private static final String[] HEAVENLY_STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] EARTHLY_BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final NayinTone[] NAYIN_TONES = {
            new NayinTone("海中金", ElementType.METAL),
            new NayinTone("炉中火", ElementType.FIRE),
            new NayinTone("大林木", ElementType.WOOD),
            new NayinTone("路旁土", ElementType.EARTH),
            new NayinTone("剑锋金", ElementType.METAL),
            new NayinTone("山头火", ElementType.FIRE),
            new NayinTone("涧下水", ElementType.WATER),
            new NayinTone("城头土", ElementType.EARTH),
            new NayinTone("白蜡金", ElementType.METAL),
            new NayinTone("杨柳木", ElementType.WOOD),
            new NayinTone("泉中水", ElementType.WATER),
            new NayinTone("屋上土", ElementType.EARTH),
            new NayinTone("霹雳火", ElementType.FIRE),
            new NayinTone("松柏木", ElementType.WOOD),
            new NayinTone("长流水", ElementType.WATER),
            new NayinTone("砂中金", ElementType.METAL),
            new NayinTone("山下火", ElementType.FIRE),
            new NayinTone("平地木", ElementType.WOOD),
            new NayinTone("壁上土", ElementType.EARTH),
            new NayinTone("金箔金", ElementType.METAL),
            new NayinTone("覆灯火", ElementType.FIRE),
            new NayinTone("天河水", ElementType.WATER),
            new NayinTone("大驿土", ElementType.EARTH),
            new NayinTone("钗钏金", ElementType.METAL),
            new NayinTone("桑柘木", ElementType.WOOD),
            new NayinTone("大溪水", ElementType.WATER),
            new NayinTone("沙中土", ElementType.EARTH),
            new NayinTone("天上火", ElementType.FIRE),
            new NayinTone("石榴木", ElementType.WOOD),
            new NayinTone("大海水", ElementType.WATER)
    };

    private WuxingCalendarTerms() {
    }

    static YearTone yearTone(int gregorianYear) {
        int index = Math.floorMod(gregorianYear - 4, 60);
        int stemIndex = index % 10;
        int branchIndex = index % 12;
        NayinTone nayinTone = NAYIN_TONES[index / 2];
        return new YearTone(
                HEAVENLY_STEMS[stemIndex],
                stemElement(stemIndex),
                EARTHLY_BRANCHES[branchIndex],
                branchElement(branchIndex),
                nayinTone.name(),
                nayinTone.element());
    }

    static MonthTone monthTone(int month) {
        return switch (month) {
            case 1 -> new MonthTone(ElementType.WATER, ElementType.EARTH, "小寒大寒", "丑月余寒，湿土藏水");
            case 2 -> new MonthTone(ElementType.WOOD, ElementType.WATER, "立春雨水", "寅月木气初生，水来滋养");
            case 3 -> new MonthTone(ElementType.WOOD, ElementType.FIRE, "惊蛰春分", "卯月木旺，阳气升发");
            case 4 -> new MonthTone(ElementType.WOOD, ElementType.EARTH, "清明谷雨", "辰月湿土收春，木气入土");
            case 5 -> new MonthTone(ElementType.FIRE, ElementType.WOOD, "立夏小满", "巳月火起，木来生火");
            case 6 -> new MonthTone(ElementType.FIRE, ElementType.EARTH, "芒种夏至", "午月火旺，土承火势");
            case 7 -> new MonthTone(ElementType.EARTH, ElementType.FIRE, "小暑大暑", "未月暑土，火留余温");
            case 8 -> new MonthTone(ElementType.METAL, ElementType.EARTH, "立秋处暑", "申月金气初成，土来生金");
            case 9 -> new MonthTone(ElementType.METAL, ElementType.WATER, "白露秋分", "酉月金旺，金生水意");
            case 10 -> new MonthTone(ElementType.EARTH, ElementType.METAL, "寒露霜降", "戌月燥土收敛，金气入藏");
            case 11 -> new MonthTone(ElementType.WATER, ElementType.METAL, "立冬小雪", "亥月水起，金来生水");
            case 12 -> new MonthTone(ElementType.WATER, ElementType.EARTH, "大雪冬至", "子水当令，寒土收束");
            default -> throw new IllegalArgumentException("month must be between 1 and 12");
        };
    }

    static DayTone dayTone(int day) {
        ElementType element = elementByMod(day % 5);
        return new DayTone(element, "日序五行作轻量修饰，不替代完整日柱");
    }

    static TimeTone timeTone(BirthTimeRange timeRange) {
        return switch (timeRange) {
            case MORNING -> new TimeTone("清晨", ElementType.WOOD, "卯辰之时，木气升发");
            case NOON -> new TimeTone("正午", ElementType.FIRE, "巳午之时，火气当令");
            case AFTERNOON -> new TimeTone("午后", ElementType.EARTH, "未申之际，土气承接");
            case EVENING -> new TimeTone("傍晚", ElementType.METAL, "酉戌之际，金气清肃");
            case NIGHT -> new TimeTone("夜间", ElementType.WATER, "亥子之时，水气深藏");
            case UNKNOWN -> new TimeTone("未知", ElementType.EARTH, "时段未知，不单独加权");
        };
    }

    static ElementType elementByMod(int mod) {
        return switch (mod) {
            case 0 -> ElementType.METAL;
            case 1 -> ElementType.WATER;
            case 2 -> ElementType.WOOD;
            case 3 -> ElementType.FIRE;
            case 4 -> ElementType.EARTH;
            default -> throw new IllegalStateException("unexpected mod");
        };
    }

    private static ElementType stemElement(int stemIndex) {
        return switch (stemIndex) {
            case 0, 1 -> ElementType.WOOD;
            case 2, 3 -> ElementType.FIRE;
            case 4, 5 -> ElementType.EARTH;
            case 6, 7 -> ElementType.METAL;
            case 8, 9 -> ElementType.WATER;
            default -> throw new IllegalStateException("unexpected stem index");
        };
    }

    private static ElementType branchElement(int branchIndex) {
        return switch (branchIndex) {
            case 0, 11 -> ElementType.WATER;
            case 1, 4, 7, 10 -> ElementType.EARTH;
            case 2, 3 -> ElementType.WOOD;
            case 5, 6 -> ElementType.FIRE;
            case 8, 9 -> ElementType.METAL;
            default -> throw new IllegalStateException("unexpected branch index");
        };
    }

    private record NayinTone(String name, ElementType element) {
    }

    record YearTone(String stem,
                    ElementType stemElement,
                    String branch,
                    ElementType branchElement,
                    String nayinName,
                    ElementType nayinElement) {
        String ganZhi() {
            return stem + branch;
        }
    }

    record MonthTone(ElementType main, ElementType secondary, String solarTerms, String reason) {
    }

    record DayTone(ElementType element, String reason) {
    }

    record TimeTone(String label, ElementType element, String reason) {
    }
}
