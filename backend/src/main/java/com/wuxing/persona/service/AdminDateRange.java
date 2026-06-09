package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminDateRange {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime startAt;
    private final LocalDateTime endExclusive;

    private AdminDateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.startAt = startDate == null ? null : startDate.atStartOfDay();
        this.endExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }

    public static AdminDateRange of(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(400, "startDate must be before or equal to endDate");
        }
        return new AdminDateRange(startDate, endDate);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndExclusive() {
        return endExclusive;
    }
}
