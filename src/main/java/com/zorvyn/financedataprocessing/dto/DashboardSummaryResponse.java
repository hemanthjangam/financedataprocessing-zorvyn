package com.zorvyn.financedataprocessing.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        List<CategoryTotalResponse> categoryTotals,
        List<TrendPointResponse> monthlyTrends,
        List<FinancialRecordResponse> recentActivity
) {
}
