package com.zorvyn.financedataprocessing.config;

import com.zorvyn.financedataprocessing.domain.FinancialRecord;
import com.zorvyn.financedataprocessing.domain.RecordType;
import com.zorvyn.financedataprocessing.domain.Role;
import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.domain.UserStatus;
import com.zorvyn.financedataprocessing.repository.FinancialRecordRepository;
import com.zorvyn.financedataprocessing.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
            @Value("${app.seed.enabled:true}") boolean seedEnabled,
            UserAccountRepository userAccountRepository,
            FinancialRecordRepository financialRecordRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!seedEnabled || userAccountRepository.count() > 0) {
                return;
            }

            // Seed a realistic review dataset so the API is immediately explorable.
            Instant now = Instant.now();
            UserAccount admin = userAccountRepository.save(buildUser(
                    "Aarav Admin",
                    "admin@zorvyn.local",
                    "Admin@123",
                    Role.ADMIN,
                    UserStatus.ACTIVE,
                    now,
                    passwordEncoder
            ));

            userAccountRepository.save(buildUser(
                    "Anika Analyst",
                    "analyst@zorvyn.local",
                    "Analyst@123",
                    Role.ANALYST,
                    UserStatus.ACTIVE,
                    now,
                    passwordEncoder
            ));

            userAccountRepository.save(buildUser(
                    "Vihaan Viewer",
                    "viewer@zorvyn.local",
                    "Viewer@123",
                    Role.VIEWER,
                    UserStatus.ACTIVE,
                    now,
                    passwordEncoder
            ));

            financialRecordRepository.saveAll(List.of(
                    buildRecord(new BigDecimal("12500.00"), RecordType.INCOME, "Salary", LocalDate.now().minusDays(12), "Monthly payroll", admin, now),
                    buildRecord(new BigDecimal("2400.00"), RecordType.EXPENSE, "Rent", LocalDate.now().minusDays(10), "Office rent", admin, now),
                    buildRecord(new BigDecimal("780.45"), RecordType.EXPENSE, "Software", LocalDate.now().minusDays(8), "Analytics tooling", admin, now),
                    buildRecord(new BigDecimal("2100.00"), RecordType.INCOME, "Consulting", LocalDate.now().minusDays(6), "Quarterly advisory fee", admin, now),
                    buildRecord(new BigDecimal("560.25"), RecordType.EXPENSE, "Travel", LocalDate.now().minusDays(2), "Client visit", admin, now),
                    buildRecord(new BigDecimal("430.00"), RecordType.EXPENSE, "Marketing", LocalDate.now().minusDays(1), "Social media ads", admin, now)
            ));
        };
    }

    private UserAccount buildUser(
            String fullName,
            String email,
            String rawPassword,
            Role role,
            UserStatus status,
            Instant createdAt,
            PasswordEncoder passwordEncoder
    ) {
        return UserAccount.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    private FinancialRecord buildRecord(
            BigDecimal amount,
            RecordType type,
            String category,
            LocalDate transactionDate,
            String notes,
            UserAccount createdBy,
            Instant timestamp
    ) {
        return FinancialRecord.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .transactionDate(transactionDate)
                .notes(notes)
                .createdBy(createdBy)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}
