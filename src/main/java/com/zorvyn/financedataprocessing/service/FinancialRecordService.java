package com.zorvyn.financedataprocessing.service;

import com.zorvyn.financedataprocessing.domain.FinancialRecord;
import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.dto.FinancialRecordFilter;
import com.zorvyn.financedataprocessing.dto.FinancialRecordRequest;
import com.zorvyn.financedataprocessing.dto.FinancialRecordResponse;
import com.zorvyn.financedataprocessing.dto.PageResponse;
import com.zorvyn.financedataprocessing.exception.NotFoundException;
import com.zorvyn.financedataprocessing.repository.FinancialRecordRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinancialRecordService {

    private final FinancialRecordRepository financialRecordRepository;

    public FinancialRecordService(FinancialRecordRepository financialRecordRepository) {
        this.financialRecordRepository = financialRecordRepository;
    }

    public FinancialRecordResponse createRecord(FinancialRecordRequest request, UserAccount actor) {
        Instant now = Instant.now();
        // Store normalized text fields up front so filtering stays consistent later.
        FinancialRecord record = FinancialRecord.builder()
                .amount(request.amount())
                .type(request.type())
                .category(request.category().trim())
                .transactionDate(request.transactionDate())
                .notes(trimToNull(request.notes()))
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toResponse(financialRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialRecordResponse> listRecords(FinancialRecordFilter filter, int page, int size) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("createdAt")
        ));
        Page<FinancialRecord> records = financialRecordRepository.findAll(withFilter(filter), pageable);

        return new PageResponse<>(
                records.getContent().stream().map(this::toResponse).toList(),
                records.getNumber(),
                records.getSize(),
                records.getTotalElements(),
                records.getTotalPages(),
                records.hasNext()
        );
    }

    public FinancialRecordResponse updateRecord(Long recordId, FinancialRecordRequest request) {
        FinancialRecord record = findRecord(recordId);
        // Updates reuse the same normalization rules as creation.
        record.setAmount(request.amount());
        record.setType(request.type());
        record.setCategory(request.category().trim());
        record.setTransactionDate(request.transactionDate());
        record.setNotes(trimToNull(request.notes()));
        record.setUpdatedAt(Instant.now());
        return toResponse(record);
    }

    public void deleteRecord(Long recordId, UserAccount actor) {
        FinancialRecord record = findRecord(recordId);
        // Soft delete keeps records out of normal queries without losing audit context.
        record.setDeletedAt(Instant.now());
        record.setDeletedBy(actor);
        record.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<FinancialRecord> listRawRecords() {
        // Dashboard calculations need the full filtered dataset instead of a paged slice.
        return financialRecordRepository.findAll(withFilter(null));
    }

    @Transactional(readOnly = true)
    public List<FinancialRecordResponse> recentActivity() {
        // Limit recent activity to a small fixed set for dashboard widgets.
        return financialRecordRepository.findTop5ByDeletedAtIsNullOrderByTransactionDateDescCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private FinancialRecord findRecord(Long recordId) {
        return financialRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new NotFoundException("Financial record not found"));
    }

    private Specification<FinancialRecord> withFilter(FinancialRecordFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (filter == null) {
                return cb.and(predicates.toArray(Predicate[]::new));
            }
            // Keep filtering additive so the API can combine date, category, and type safely.
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.category() != null && !filter.category().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), filter.category().trim().toLowerCase()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), filter.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public FinancialRecordResponse toResponse(FinancialRecord record) {
        // Responses expose creator names, not the full related user entity.
        return new FinancialRecordResponse(
                record.getId(),
                record.getAmount(),
                record.getType(),
                record.getCategory(),
                record.getTransactionDate(),
                record.getNotes(),
                record.getCreatedBy().getFullName(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
