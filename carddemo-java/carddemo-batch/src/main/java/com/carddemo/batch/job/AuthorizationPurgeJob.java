package com.carddemo.batch.job;

import com.carddemo.domain.repository.AuthorizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Replaces CBPAUP0C.cbl (386 LOC) — batch purge of expired authorization records.
 * Deletes authorizations with match_status 'E' (expired) or 'M' (matched)
 * that are older than the specified number of days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationPurgeJob {

    private static final int DEFAULT_EXPIRY_DAYS = 30;

    private final AuthorizationRepository authorizationRepository;

    @Transactional
    public PurgeResult execute(Integer expiryDays) {
        int days = expiryDays != null ? expiryDays : DEFAULT_EXPIRY_DAYS;
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        log.info("Starting authorization purge. Expiry days: {}, cutoff date: {}", days, cutoffDate);

        int deletedCount = authorizationRepository.deleteExpiredAuthorizations(cutoffDate);

        log.info("Authorization purge completed. Records deleted: {}", deletedCount);

        return new PurgeResult(deletedCount, days, cutoffDate);
    }

    public record PurgeResult(int recordsDeleted, int expiryDays, LocalDateTime cutoffDate) {}
}
