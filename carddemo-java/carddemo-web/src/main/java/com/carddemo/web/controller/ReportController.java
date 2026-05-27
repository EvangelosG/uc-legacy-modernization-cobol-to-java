package com.carddemo.web.controller;

import com.carddemo.common.util.DateUtil;
import com.carddemo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/transactions")
    public ResponseEntity<ReportJobResponse> requestTransactionReport(
            @RequestBody TransactionReportRequest request) {

        LocalDate startDate = null;
        LocalDate endDate = null;

        if (request.startDate() != null && !request.startDate().isBlank()) {
            if (!DateUtil.isValidDate(request.startDate(), "YYYY-MM-DD")) {
                return ResponseEntity.badRequest().build();
            }
            startDate = DateUtil.parseDate(request.startDate(), "YYYY-MM-DD");
        }
        if (request.endDate() != null && !request.endDate().isBlank()) {
            if (!DateUtil.isValidDate(request.endDate(), "YYYY-MM-DD")) {
                return ResponseEntity.badRequest().build();
            }
            endDate = DateUtil.parseDate(request.endDate(), "YYYY-MM-DD");
        }

        ReportService.ReportJob job = reportService.requestTransactionReport(
                request.accountId(), startDate, endDate);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReportJobResponse.from(job));
    }

    @GetMapping("/transactions/{jobId}")
    public ResponseEntity<ReportJobResponse> getReportStatus(@PathVariable("jobId") String jobId) {
        ReportService.ReportJob job = reportService.getJobStatus(jobId);
        return ResponseEntity.ok(ReportJobResponse.from(job));
    }

    public record TransactionReportRequest(Long accountId, String startDate, String endDate) {}

    public record ReportJobResponse(String jobId, Long accountId, String startDate,
                                    String endDate, String status) {
        static ReportJobResponse from(ReportService.ReportJob job) {
            return new ReportJobResponse(
                    job.jobId(), job.accountId(),
                    job.startDate() != null ? job.startDate().toString() : null,
                    job.endDate() != null ? job.endDate().toString() : null,
                    job.status()
            );
        }
    }
}
