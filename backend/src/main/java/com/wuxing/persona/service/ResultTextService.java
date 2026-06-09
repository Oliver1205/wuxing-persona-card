package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResultTextService {

    public ResultText build(ElementScoreResult scoreResult, StarOfficer starOfficer) {
        ElementType primary = scoreResult.getPrimaryElement();
        ElementType secondary = scoreResult.getSecondaryElement();
        List<String> keywords = buildKeywords(primary, secondary, starOfficer);

        ResultText resultText = new ResultText();
        resultText.setKeywords(keywords);
        resultText.setLayoutExplanation(String.format(
                "你的出生年月和价值取向共同形成了偏%s、带有%s气质的组合。你可能更偏向用%s的方式理解世界，同时保留%s带来的补充力量；这是一种传统文化元素启发下的娱乐性人格倾向。",
                primary.getDisplayName(), secondary.getDisplayName(), primary.getKeywords().get(0), secondary.getKeywords().get(0)));
        resultText.setStrengthText(String.format(
                "你的优势更像是%s与%s的结合：既能在事情里找到自己的节奏，也能把注意力放在真正重要的地方。%s让你更容易形成稳定的个人风格。",
                primary.getKeywords().get(0), secondary.getKeywords().get(1), starOfficer.getName()));
        resultText.setRelationshipText(String.format(
                "这代表你在关系中常常给人一种%s、%s的感受。适合你的相处方式是保留自己的边界，也给彼此留出温和沟通和慢慢靠近的空间。",
                primary.getKeywords().get(2), secondary.getKeywords().get(2)));
        return resultText;
    }

    private List<String> buildKeywords(ElementType primary, ElementType secondary, StarOfficer starOfficer) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(primary.getKeywords().get(0));
        result.add(primary.getKeywords().get(1));
        result.add(primary.getKeywords().get(2));
        result.add(secondary.getKeywords().get(0));
        result.add(starOfficer.getTraits().get(0));
        return new ArrayList<>(result).stream().limit(5).toList();
    }
}
