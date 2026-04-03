package com.zorvyn.financedataprocessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DashboardServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void summaryReflectsSeededData() {
        var summary = dashboardService.getSummary();

        assertEquals(new BigDecimal("14600.00"), summary.totalIncome());
        assertEquals(new BigDecimal("4170.70"), summary.totalExpense());
        assertEquals(new BigDecimal("10429.30"), summary.netBalance());
        assertFalse(summary.categoryTotals().isEmpty());
        assertFalse(summary.recentActivity().isEmpty());
    }
}
