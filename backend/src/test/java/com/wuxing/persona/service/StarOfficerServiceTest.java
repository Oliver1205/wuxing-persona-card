package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wuxing.persona.enums.ElementType;
import org.junit.jupiter.api.Test;

class StarOfficerServiceTest {

    private final StarOfficerService service = new StarOfficerService();

    @Test
    void byMonthShouldReturnBailuForAugust() {
        StarOfficer starOfficer = service.byMonth(8);

        assertEquals("BAILU", starOfficer.getCode());
        assertEquals("白露星官", starOfficer.getName());
        assertEquals(ElementType.METAL, starOfficer.getElementType());
    }
}
