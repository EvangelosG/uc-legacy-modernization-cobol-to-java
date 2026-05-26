package com.carddemo.web.controller;

import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.service.*;
import com.carddemo.web.config.GlobalExceptionHandler;
import com.carddemo.web.config.JwtAuthenticationFilter;
import com.carddemo.web.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {BillingController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void getBillingSummaryReturnsAccountAndBalances() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionCategoryBalance bal = TransactionCategoryBalance.builder()
                .acctId(1L).typeCd("01").catCd(1).balance(new BigDecimal("150.00")).build();

        when(billingService.getBillingSummary(1L))
                .thenReturn(new BillingService.BillingSummary(
                        1L, new BigDecimal("194.00"), new BigDecimal("2020.00"),
                        new BigDecimal("1020.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("150.00"), List.of(bal)
                ));

        mockMvc.perform(get("/api/billing/account/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.currentBalance").value(194.00))
                .andExpect(jsonPath("$.creditLimit").value(2020.00))
                .andExpect(jsonPath("$.totalCategoryBalance").value(150.00))
                .andExpect(jsonPath("$.categoryBalances[0].typeCd").value("01"));
    }

    @Test
    void getBillingNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(billingService.getBillingSummary(999L))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Account not found: 999"));

        mockMvc.perform(get("/api/billing/account/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBillingRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/billing/account/1"))
                .andExpect(status().isUnauthorized());
    }
}
