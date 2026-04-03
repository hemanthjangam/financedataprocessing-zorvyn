package com.zorvyn.financedataprocessing.dto;

import com.zorvyn.financedataprocessing.domain.RecordType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FinancialRecordResponse(
        Long id,
        BigDecimal amount,
        RecordType type,
        String category,
        LocalDate transactionDate,
        String notes,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
