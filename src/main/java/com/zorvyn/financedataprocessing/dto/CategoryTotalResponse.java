package com.zorvyn.financedataprocessing.dto;

import java.math.BigDecimal;

public record CategoryTotalResponse(
        String category,
        BigDecimal total
) {
}
