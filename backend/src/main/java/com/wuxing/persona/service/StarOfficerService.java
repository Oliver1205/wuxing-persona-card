package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.enums.ElementType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StarOfficerService {

    public StarOfficer byMonth(int month) {
        return switch (month) {
            case 1 -> new StarOfficer("NIU_XIU", "牛宿", ElementType.WATER, List.of("沉静", "承接", "蓄势"));
            case 2 -> new StarOfficer("JIAO_XIU", "角宿", ElementType.WOOD, List.of("生发", "开端", "舒展"));
            case 3 -> new StarOfficer("FANG_XIU", "房宿", ElementType.WOOD, List.of("规划", "生长", "秩序"));
            case 4 -> new StarOfficer("JI_XIU", "箕宿", ElementType.WOOD, List.of("流动", "引导", "方向"));
            case 5 -> new StarOfficer("JING_XIU", "井宿", ElementType.FIRE, List.of("点亮", "行动", "外放"));
            case 6 -> new StarOfficer("XING_XIU", "星宿", ElementType.FIRE, List.of("明朗", "表达", "热度"));
            case 7 -> new StarOfficer("ZHANG_XIU", "张宿", ElementType.EARTH, List.of("铺展", "承接", "稳定"));
            case 8 -> new StarOfficer("KUI_XIU", "奎宿", ElementType.METAL, List.of("分辨", "文气", "标准"));
            case 9 -> new StarOfficer("MAO_XIU", "昴宿", ElementType.METAL, List.of("清醒", "判断", "边界"));
            case 10 -> new StarOfficer("LOU_XIU", "娄宿", ElementType.EARTH, List.of("收束", "统筹", "守成"));
            case 11 -> new StarOfficer("WEI_XIU", "危宿", ElementType.WATER, List.of("洞察", "警觉", "适应"));
            case 12 -> new StarOfficer("XU_XIU", "虚宿", ElementType.WATER, List.of("深思", "留白", "包容"));
            default -> throw new BusinessException("birthMonth must be between 1 and 12");
        };
    }
}
