package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.enums.ElementType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StarOfficerService {

    public StarOfficer byMonth(int month) {
        return switch (month) {
            case 1 -> new StarOfficer("XUANBING", "玄冰星官", ElementType.WATER, List.of("安静", "沉淀", "观察"));
            case 2 -> new StarOfficer("QINGYA", "青芽星官", ElementType.WOOD, List.of("生长", "开始", "温和"));
            case 3 -> new StarOfficer("CHUNLIN", "春林星官", ElementType.WOOD, List.of("规划", "舒展", "创造"));
            case 4 -> new StarOfficer("ZHUfeng".toUpperCase(), "竹风星官", ElementType.WOOD, List.of("耐心", "引导", "方向"));
            case 5 -> new StarOfficer("CHIYANG", "赤阳星官", ElementType.FIRE, List.of("行动", "热情", "表达"));
            case 6 -> new StarOfficer("YANTING", "炎庭星官", ElementType.FIRE, List.of("推进", "点燃", "外放"));
            case 7 -> new StarOfficer("SHANYU", "山雨星官", ElementType.EARTH, List.of("承接", "稳定", "协调"));
            case 8 -> new StarOfficer("BAILU", "白露星官", ElementType.METAL, List.of("清醒", "判断", "秩序"));
            case 9 -> new StarOfficer("JINGUI", "金桂星官", ElementType.METAL, List.of("标准", "边界", "执行"));
            case 10 -> new StarOfficer("YANHENG", "岩衡星官", ElementType.EARTH, List.of("平衡", "统筹", "守护"));
            case 11 -> new StarOfficer("CHENGYE", "澄夜星官", ElementType.WATER, List.of("洞察", "倾听", "适应"));
            case 12 -> new StarOfficer("XUECHUAN", "雪川星官", ElementType.WATER, List.of("冷静", "深度", "包容"));
            default -> throw new BusinessException("birthMonth must be between 1 and 12");
        };
    }
}
