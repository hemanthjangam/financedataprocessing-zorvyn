package com.zorvyn.financedataprocessing.service;

import com.zorvyn.financedataprocessing.domain.FinancialRecord;
import com.zorvyn.financedataprocessing.domain.RecordType;
import com.zorvyn.financedataprocessing.dto.CategoryTotalResponse;
import com.zorvyn.financedataprocessing.dto.DashboardSummaryResponse;
import com.zorvyn.financedataprocessing.dto.TrendPointResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final FinancialRecordService financialRecordService;

    public DashboardService(FinancialRecordService financialRecordService) {
        this.financialRecordService = financialRecordService;
    }

    public DashboardSummaryResponse getSummary() {
        // Build the dashboard from one record snapshot so all totals line up.
        List<FinancialRecord> records = financialRecordService.listRawRecords();

        BigDecimal totalIncome = sumByType(records, RecordType.INCOME);
        BigDecimal totalExpense = sumByType(records, RecordType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        return new DashboardSummaryResponse(
                totalIncome,
                totalExpense,
                netBalance,
                buildCategoryTotals(records),
                buildMonthlyTrends(records),
                financialRecordService.recentActivity()
        );
    }

    private BigDecimal sumByType(List<FinancialRecord> records, RecordType type) {
        return records.stream()
                .filter(record -> record.getType() == type)
                .map(FinancialRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryTotalResponse> buildCategoryTotals(List<FinancialRecord> records) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        // Aggregate by category before sorting so the response is ready for charts.
        records.forEach(record -> totals.merge(record.getCategory(), record.getAmount(), BigDecimal::add));

        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> new CategoryTotalResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<TrendPointResponse> buildMonthlyTrends(List<FinancialRecord> records) {
        // Group first, then calculate income/expense/net per month in one place.
        Map<YearMonth, List<FinancialRecord>> monthlyRecords = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(record -> YearMonth.from(record.getTransactionDate())));

        return monthlyRecords.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> {
                    BigDecimal income = sumByType(entry.getValue(), RecordType.INCOME);
                    BigDecimal expense = sumByType(entry.getValue(), RecordType.EXPENSE);
                    return new TrendPointResponse(entry.getKey(), income, expense, income.subtract(expense));
                })
                .toList();
    }
}
