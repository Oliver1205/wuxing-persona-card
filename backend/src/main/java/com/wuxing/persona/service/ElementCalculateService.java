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
        add(scores, WuxingCalendarTerms.yearTone(request.getBirthYear()).nayinElement(), 8);
        addMonthWeight(scores, request.getBirthMonth());
        if (request.getBirthDay() != null) {
            add(scores, WuxingCalendarTerms.dayTone(request.getBirthDay()).element(), 6);
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

    private void addMonthWeight(EnumMap<ElementType, Integer> scores, int month) {
        try {
            WuxingCalendarTerms.MonthTone monthTone = WuxingCalendarTerms.monthTone(month);
            add(scores, monthTone.main(), 25);
            add(scores, monthTone.secondary(), 10);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("birthMonth must be between 1 and 12");
        }
    }

    private void addTimeWeight(EnumMap<ElementType, Integer> scores, BirthTimeRange timeRange) {
        if (timeRange != BirthTimeRange.UNKNOWN) {
            add(scores, WuxingCalendarTerms.timeTone(timeRange).element(), 8);
        }
    }

    private void add(EnumMap<ElementType, Integer> scores, ElementType elementType, int delta) {
        scores.put(elementType, scores.get(elementType) + delta);
    }
}
