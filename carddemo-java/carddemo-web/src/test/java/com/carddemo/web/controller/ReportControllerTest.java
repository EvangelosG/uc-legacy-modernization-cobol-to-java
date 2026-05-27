package com.carddemo.web.controller;

import com.carddemo.service.*;
import com.carddemo.web.config.GlobalExceptionHandler;
import com.carddemo.web.config.JwtAuthenticationFilter;
import com.carddemo.web.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {ReportController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void requestTransactionReportReturns202WithJobId() throws Exception {
        String token = "t";
        mockAuth(token);

        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        when(reportService.requestTransactionReport(1L, start, end))
                .thenReturn(new ReportService.ReportJob("job-123", 1L, start, end, "PENDING"));

        mockMvc.perform(post("/api/reports/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReportController.TransactionReportRequest(1L, "2024-01-01", "2024-01-31"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getReportStatusReturnsJob() throws Exception {
        String token = "t";
        mockAuth(token);

        when(reportService.getJobStatus("job-123"))
                .thenReturn(new ReportService.ReportJob("job-123", 1L, null, null, "PENDING"));

        mockMvc.perform(get("/api/reports/transactions/job-123")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getReportStatusNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(reportService.getJobStatus("bad-id"))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Report job not found: bad-id"));

        mockMvc.perform(get("/api/reports/transactions/bad-id")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestReportRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/reports/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
