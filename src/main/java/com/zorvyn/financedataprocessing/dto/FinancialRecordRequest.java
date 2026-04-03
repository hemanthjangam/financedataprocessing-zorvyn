package com.zorvyn.financedataprocessing.dto;

import com.zorvyn.financedataprocessing.domain.RecordType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialRecordRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull RecordType type,
        @NotBlank @Size(max = 80) String category,
        @NotNull LocalDate transactionDate,
        @Size(max = 255) String notes
) {
}
