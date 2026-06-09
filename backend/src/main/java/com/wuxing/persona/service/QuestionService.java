package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.vo.OptionVO;
import com.wuxing.persona.vo.QuestionVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {

    public List<QuestionVO> listQuestions() {
        return List.of(
                question("Q1", "做决定时，你更看重什么？",
                        option(ElementType.METAL, "标准、边界和清晰判断"),
                        option(ElementType.WOOD, "成长空间和长期可能"),
                        option(ElementType.WATER, "感受、关系和细节变化"),
                        option(ElementType.FIRE, "行动效率和当下热情"),
                        option(ElementType.EARTH, "稳定、安全和可持续")),
                question("Q2", "和别人相处时，你更常扮演什么角色？",
                        option(ElementType.METAL, "帮大家理清规则的人"),
                        option(ElementType.WOOD, "提出计划和方向的人"),
                        option(ElementType.WATER, "倾听情绪和理解细节的人"),
                        option(ElementType.FIRE, "带动气氛和推进节奏的人"),
                        option(ElementType.EARTH, "稳住局面和照顾整体的人")),
                question("Q3", "面对压力时，你更倾向于？",
                        option(ElementType.METAL, "拆解问题，建立秩序"),
                        option(ElementType.WOOD, "寻找新的成长路径"),
                        option(ElementType.WATER, "先感受和观察，再慢慢调整"),
                        option(ElementType.FIRE, "先行动起来，边做边修正"),
                        option(ElementType.EARTH, "保持稳定，把眼前事情做好")),
                question("Q4", "你最欣赏哪种能力？",
                        option(ElementType.METAL, "清醒判断和高效执行"),
                        option(ElementType.WOOD, "持续成长和创造可能"),
                        option(ElementType.WATER, "共情理解和灵活适应"),
                        option(ElementType.FIRE, "热情表达和感染他人"),
                        option(ElementType.EARTH, "可靠承载和长期陪伴")),
                question("Q5", "如果要完成一个项目，你更愿意负责？",
                        option(ElementType.METAL, "规则制定、标准检查、关键决策"),
                        option(ElementType.WOOD, "规划路线、设计方案、推动成长"),
                        option(ElementType.WATER, "观察反馈、沟通协调、情绪支持"),
                        option(ElementType.FIRE, "启动项目、对外表达、快速推进"),
                        option(ElementType.EARTH, "资源统筹、稳定执行、兜底收尾"))
        );
    }

    private QuestionVO question(String code, String title, OptionVO... options) {
        return new QuestionVO(code, title, List.of(options));
    }

    private OptionVO option(ElementType elementType, String text) {
        return new OptionVO(elementType.name(), text, elementType.name(), elementType.getDisplayName());
    }
}
