package com.zorvyn.financedataprocessing.controller;

import com.zorvyn.financedataprocessing.dto.DashboardSummaryResponse;
import com.zorvyn.financedataprocessing.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public DashboardSummaryResponse getSummary() {
        // Expose a single summary payload so the client does not need multiple dashboard calls.
        return dashboardService.getSummary();
    }
}
