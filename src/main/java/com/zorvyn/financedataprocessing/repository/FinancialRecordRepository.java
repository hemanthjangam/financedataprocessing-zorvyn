package com.zorvyn.financedataprocessing.repository;

import com.zorvyn.financedataprocessing.domain.FinancialRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long>, JpaSpecificationExecutor<FinancialRecord> {

    List<FinancialRecord> findTop5ByDeletedAtIsNullOrderByTransactionDateDescCreatedAtDesc();

    Optional<FinancialRecord> findByIdAndDeletedAtIsNull(Long id);
}
