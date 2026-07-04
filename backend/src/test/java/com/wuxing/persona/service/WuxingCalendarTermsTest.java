package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wuxing.persona.enums.ElementType;
import org.junit.jupiter.api.Test;

class WuxingCalendarTermsTest {

    @Test
    void dayToneShouldResolveKnownGuiHaiDay() {
        WuxingCalendarTerms.DayTone dayTone = WuxingCalendarTerms.dayTone(2005, 12, 5);

        assertEquals("癸亥", dayTone.ganZhi());
        assertEquals("癸", dayTone.stem());
        assertEquals(ElementType.WATER, dayTone.stemElement());
        assertEquals("亥", dayTone.branch());
        assertEquals(ElementType.WATER, dayTone.branchElement());
    }
}
