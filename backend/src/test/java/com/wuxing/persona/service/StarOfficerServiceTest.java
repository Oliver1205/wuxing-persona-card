package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wuxing.persona.enums.ElementType;
import org.junit.jupiter.api.Test;

class StarOfficerServiceTest {

    private final StarOfficerService service = new StarOfficerService();

    @Test
    void byMonthShouldReturnDocumentedMansionForAugust() {
        StarOfficer starOfficer = service.byMonth(8);

        assertEquals("KUI_XIU", starOfficer.getCode());
        assertEquals("奎宿", starOfficer.getName());
        assertEquals(ElementType.METAL, starOfficer.getElementType());
    }
}
