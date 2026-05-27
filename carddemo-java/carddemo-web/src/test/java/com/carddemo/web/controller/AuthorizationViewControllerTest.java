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
@ContextConfiguration(classes = {AuthorizationViewController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthorizationViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizationViewService authorizationViewService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void getPendingAuthorizationsReturnsPage() throws Exception {
        String token = "t";
        mockAuth(token);

        Transaction txn = Transaction.builder()
                .tranId("AUTH001").typeCd("04").catCd(1).source("ONLINE")
                .description("Auth Hold").amount(new BigDecimal("100.00"))
                .merchantName("Amazon").cardNum("4111111111111111")
                .origTs(LocalDateTime.of(2024, 1, 15, 10, 30, 0)).build();

        when(authorizationViewService.getPendingAuthorizations(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/authorizations/pending")
                        .param("accountId", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tranId").value("AUTH001"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAuthorizationReturnsDetail() throws Exception {
        String token = "t";
        mockAuth(token);

        Transaction txn = Transaction.builder()
                .tranId("AUTH001").typeCd("04").catCd(1).source("ONLINE")
                .description("Auth Hold").amount(new BigDecimal("100.00"))
                .merchantName("Amazon").cardNum("4111111111111111")
                .origTs(LocalDateTime.of(2024, 1, 15, 10, 30, 0)).build();

        when(authorizationViewService.getAuthorization("AUTH001")).thenReturn(txn);

        mockMvc.perform(get("/api/authorizations/AUTH001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranId").value("AUTH001"))
                .andExpect(jsonPath("$.merchantName").value("Amazon"));
    }

    @Test
    void getAuthorizationNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(authorizationViewService.getAuthorization("NOTFOUND"))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Authorization not found: NOTFOUND"));

        mockMvc.perform(get("/api/authorizations/NOTFOUND")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void pendingAuthorizationsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/authorizations/pending").param("accountId", "1"))
                .andExpect(status().isUnauthorized());
    }
}
