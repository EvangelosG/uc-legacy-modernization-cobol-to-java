package com.carddemo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final Map<String, ReportJob> jobs = new ConcurrentHashMap<>();

    public ReportJob requestTransactionReport(Long accountId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        String jobId = UUID.randomUUID().toString();
        ReportJob job = new ReportJob(jobId, accountId, startDate, endDate, "PENDING");
        jobs.put(jobId, job);
        return job;
    }

    public ReportJob getJobStatus(String jobId) {
        ReportJob job = jobs.get(jobId);
        if (job == null) {
            throw new TransactionTypeService.ResourceNotFoundException("Report job not found: " + jobId);
        }
        return job;
    }

    public record ReportJob(String jobId, Long accountId, LocalDate startDate,
                            LocalDate endDate, String status) {}
}
