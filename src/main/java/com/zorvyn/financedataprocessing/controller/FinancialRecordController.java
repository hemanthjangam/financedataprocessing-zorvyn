package com.zorvyn.financedataprocessing.controller;

import com.zorvyn.financedataprocessing.domain.RecordType;
import com.zorvyn.financedataprocessing.dto.FinancialRecordFilter;
import com.zorvyn.financedataprocessing.dto.FinancialRecordRequest;
import com.zorvyn.financedataprocessing.dto.FinancialRecordResponse;
import com.zorvyn.financedataprocessing.dto.PageResponse;
import com.zorvyn.financedataprocessing.security.CurrentUserService;
import com.zorvyn.financedataprocessing.service.FinancialRecordService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class FinancialRecordController {

    private final FinancialRecordService financialRecordService;
    private final CurrentUserService currentUserService;

    public FinancialRecordController(FinancialRecordService financialRecordService, CurrentUserService currentUserService) {
        this.financialRecordService = financialRecordService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PageResponse<FinancialRecordResponse> listRecords(
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Keep filtering at the API edge so the service receives one normalized filter object.
        return financialRecordService.listRecords(new FinancialRecordFilter(type, category, from, to), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public FinancialRecordResponse createRecord(@Valid @RequestBody FinancialRecordRequest request) {
        // Record creation is tied to the authenticated admin for traceability.
        return financialRecordService.createRecord(request, currentUserService.requireCurrentUser());
    }

    @PutMapping("/{recordId}")
    @PreAuthorize("hasRole('ADMIN')")
    public FinancialRecordResponse updateRecord(@PathVariable Long recordId, @Valid @RequestBody FinancialRecordRequest request) {
        return financialRecordService.updateRecord(recordId, request);
    }

    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRecord(@PathVariable Long recordId) {
        // Deletes are soft deletes so dashboard history stays recoverable.
        financialRecordService.deleteRecord(recordId, currentUserService.requireCurrentUser());
    }
}
