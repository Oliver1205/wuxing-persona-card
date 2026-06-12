package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.enums.BirthTimeRange;
import com.wuxing.persona.enums.ElementType;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ElementCalculateService {

    public ElementScoreResult calculate(CreateResultRequest request) {
        validateBirth(request);
        EnumMap<ElementType, Integer> scores = new EnumMap<>(ElementType.class);
        for (ElementType elementType : ElementType.values()) {
            scores.put(elementType, 20);
        }
        add(scores, yearElement(request.getBirthYear()), 8);
        addMonthWeight(scores, request.getBirthMonth());
        if (request.getBirthDay() != null) {
            add(scores, elementByMod(request.getBirthDay() % 5), 6);
        }
        BirthTimeRange timeRange = parseBirthTimeRange(request.getBirthTimeRange());
        if (timeRange != null) {
            addTimeWeight(scores, timeRange);
        }
        validateAnswers(request.getAnswers());
        for (AnswerRequest answer : request.getAnswers()) {
            ElementType optionElement = parseElement(answer.getOptionCode());
            add(scores, optionElement, 12);
        }

        List<Map.Entry<ElementType, Integer>> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<ElementType, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().ordinal()))
                .toList();
        ElementType primary = ranked.get(0).getKey();
        ElementType secondary = ranked.get(1).getKey();
        int primaryScore = ranked.get(0).getValue();
        int secondaryScore = ranked.get(1).getValue();
        int primaryPercent = (int) Math.round(primaryScore * 100.0 / (primaryScore + secondaryScore));

        LinkedHashMap<String, Integer> allScores = new LinkedHashMap<>();
        for (ElementType elementType : ElementType.values()) {
            allScores.put(elementType.name(), scores.get(elementType));
        }

        ElementScoreResult result = new ElementScoreResult();
        result.setPrimaryElement(primary);
        result.setSecondaryElement(secondary);
        result.setPrimaryPercent(primaryPercent);
        result.setSecondaryPercent(100 - primaryPercent);
        result.setAllScores(allScores);
        return result;
    }

    private void validateBirth(CreateResultRequest request) {
        LocalDate today = LocalDate.now();
        int birthYear = request.getBirthYear();
        int birthMonth = request.getBirthMonth();
        Integer birthDay = request.getBirthDay();

        if (birthYear > today.getYear()) {
            throw new BusinessException("birthYear must not be greater than current year");
        }
        if (birthYear == today.getYear() && birthMonth > today.getMonthValue()) {
            throw new BusinessException("birthMonth must not be in the future");
        }
        if (birthDay == null) {
            return;
        }
        LocalDate birthDate;
        try {
            birthDate = LocalDate.of(birthYear, birthMonth, birthDay);
        } catch (DateTimeException ex) {
            throw new BusinessException("birthDate must be a real calendar date");
        }
        if (birthDate.isAfter(today)) {
            throw new BusinessException("birthDate must not be in the future");
        }
    }

    private void validateAnswers(List<AnswerRequest> answers) {
        long uniqueQuestions = answers.stream().map(AnswerRequest::getQuestionCode).distinct().count();
        if (uniqueQuestions != 5) {
            throw new BusinessException("answers must contain 5 unique questions");
        }
        for (int i = 1; i <= 5; i++) {
            String questionCode = "Q" + i;
            boolean exists = answers.stream().anyMatch(answer -> questionCode.equals(answer.getQuestionCode()));
            if (!exists) {
                throw new BusinessException("missing answer for " + questionCode);
            }
        }
    }

    private ElementType parseElement(String code) {
        try {
            return ElementType.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("optionCode must be a valid element");
        }
    }

    private BirthTimeRange parseBirthTimeRange(String code) {
        try {
            return BirthTimeRange.fromNullable(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("birthTimeRange must be a valid value");
        }
    }

    private ElementType yearElement(int birthYear) {
        return switch (birthYear % 5) {
            case 0 -> ElementType.METAL;
            case 1 -> ElementType.WATER;
            case 2 -> ElementType.WOOD;
            case 3 -> ElementType.FIRE;
            case 4 -> ElementType.EARTH;
            default -> throw new IllegalStateException("unexpected mod");
        };
    }

    private ElementType elementByMod(int mod) {
        return switch (mod) {
            case 0 -> ElementType.METAL;
            case 1 -> ElementType.WATER;
            case 2 -> ElementType.WOOD;
            case 3 -> ElementType.FIRE;
            case 4 -> ElementType.EARTH;
            default -> throw new IllegalStateException("unexpected mod");
        };
    }

    private void addMonthWeight(EnumMap<ElementType, Integer> scores, int month) {
        switch (month) {
            case 1 -> { add(scores, ElementType.WATER, 25); add(scores, ElementType.EARTH, 10); }
            case 2 -> { add(scores, ElementType.WOOD, 25); add(scores, ElementType.WATER, 10); }
            case 3 -> { add(scores, ElementType.WOOD, 25); add(scores, ElementType.FIRE, 10); }
            case 4 -> { add(scores, ElementType.WOOD, 25); add(scores, ElementType.EARTH, 10); }
            case 5 -> { add(scores, ElementType.FIRE, 25); add(scores, ElementType.WOOD, 10); }
            case 6 -> { add(scores, ElementType.FIRE, 25); add(scores, ElementType.EARTH, 10); }
            case 7 -> { add(scores, ElementType.EARTH, 25); add(scores, ElementType.FIRE, 10); }
            case 8 -> { add(scores, ElementType.METAL, 25); add(scores, ElementType.EARTH, 10); }
            case 9 -> { add(scores, ElementType.METAL, 25); add(scores, ElementType.WATER, 10); }
            case 10 -> { add(scores, ElementType.EARTH, 25); add(scores, ElementType.METAL, 10); }
            case 11 -> { add(scores, ElementType.WATER, 25); add(scores, ElementType.METAL, 10); }
            case 12 -> { add(scores, ElementType.WATER, 25); add(scores, ElementType.EARTH, 10); }
            default -> throw new BusinessException("birthMonth must be between 1 and 12");
        }
    }

    private void addTimeWeight(EnumMap<ElementType, Integer> scores, BirthTimeRange timeRange) {
        switch (timeRange) {
            case MORNING -> add(scores, ElementType.WOOD, 8);
            case NOON -> add(scores, ElementType.FIRE, 8);
            case AFTERNOON -> add(scores, ElementType.EARTH, 8);
            case EVENING -> add(scores, ElementType.METAL, 8);
            case NIGHT -> add(scores, ElementType.WATER, 8);
            case UNKNOWN -> { }
        }
    }

    private void add(EnumMap<ElementType, Integer> scores, ElementType elementType, int delta) {
        scores.put(elementType, scores.get(elementType) + delta);
    }
}
