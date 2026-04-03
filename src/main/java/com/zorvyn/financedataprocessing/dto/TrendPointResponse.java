package com.zorvyn.financedataprocessing.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record TrendPointResponse(
        YearMonth month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal net
) {
}
