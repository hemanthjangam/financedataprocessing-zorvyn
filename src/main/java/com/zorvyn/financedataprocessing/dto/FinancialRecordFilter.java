package com.zorvyn.financedataprocessing.dto;

import com.zorvyn.financedataprocessing.domain.RecordType;
import java.time.LocalDate;

public record FinancialRecordFilter(
        RecordType type,
        String category,
        LocalDate from,
        LocalDate to
) {
}
