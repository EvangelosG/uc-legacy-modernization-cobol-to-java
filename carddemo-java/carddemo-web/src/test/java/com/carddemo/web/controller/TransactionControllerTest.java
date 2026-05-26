package com.carddemo.web.controller;

import com.carddemo.domain.entity.Transaction;
import com.carddemo.service.*;
import com.carddemo.web.config.GlobalExceptionHandler;
import com.carddemo.web.config.JwtAuthenticationFilter;
import com.carddemo.web.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {TransactionController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void listTransactionsReturnsPaginatedResults() throws Exception {
        String token = "t";
        mockAuth(token);

        Transaction txn = Transaction.builder()
                .tranId("TXN001").typeCd("01").catCd(1).source("POS")
                .description("Coffee Shop").amount(new BigDecimal("4.50"))
                .merchantName("Starbucks").cardNum("4111111111111111")
                .origTs(LocalDateTime.of(2024, 1, 15, 10, 30, 0)).build();

        when(transactionService.listByAccount(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tranId").value("TXN001"))
                .andExpect(jsonPath("$.content[0].amount").value(4.50))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTransactionReturnsDetail() throws Exception {
        String token = "t";
        mockAuth(token);

        Transaction txn = Transaction.builder()
                .tranId("TXN001").typeCd("01").catCd(1).source("POS")
                .description("Coffee Shop").amount(new BigDecimal("4.50"))
                .merchantName("Starbucks").cardNum("4111111111111111")
                .origTs(LocalDateTime.of(2024, 1, 15, 10, 30, 0)).build();

        when(transactionService.getTransaction("TXN001")).thenReturn(txn);

        mockMvc.perform(get("/api/transactions/TXN001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranId").value("TXN001"))
                .andExpect(jsonPath("$.merchantName").value("Starbucks"));
    }

    @Test
    void getTransactionNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(transactionService.getTransaction("NOTFOUND"))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Transaction not found: NOTFOUND"));

        mockMvc.perform(get("/api/transactions/NOTFOUND")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTransactionsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/transactions").param("accountId", "1"))
                .andExpect(status().isUnauthorized());
    }
}
