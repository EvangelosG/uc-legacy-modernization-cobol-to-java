package com.carddemo.web.controller;

import com.carddemo.service.AuthorizationService;
import com.carddemo.service.JwtTokenProvider;
import com.carddemo.web.config.GlobalExceptionHandler;
import com.carddemo.web.config.JwtAuthenticationFilter;
import com.carddemo.web.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AuthorizationController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void submitAuthRequestReturnsApproval() throws Exception {
        String token = "t";
        mockAuth(token);

        when(authorizationService.processAuthorizationRequest(any()))
                .thenReturn(new AuthorizationService.AuthorizationResult(
                        "4111111111111111", "TXN001", "120000",
                        "00", "0000", new BigDecimal("100.00")));

        mockMvc.perform(post("/api/authorizations/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "cardNum": "4111111111111111",
                                    "transactionAmt": 100.00,
                                    "merchantName": "TEST MERCHANT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authRespCode").value("00"))
                .andExpect(jsonPath("$.approvedAmt").value(100.00));
    }

    @Test
    void submitAuthRequestReturnsDecline() throws Exception {
        String token = "t";
        mockAuth(token);

        when(authorizationService.processAuthorizationRequest(any()))
                .thenReturn(new AuthorizationService.AuthorizationResult(
                        "4111111111111111", "TXN001", "120000",
                        "05", "4100", BigDecimal.ZERO));

        mockMvc.perform(post("/api/authorizations/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "cardNum": "4111111111111111",
                                    "transactionAmt": 99999.99,
                                    "merchantName": "TEST"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authRespCode").value("05"))
                .andExpect(jsonPath("$.authRespReason").value("4100"));
    }

    @Test
    void submitAuthRequestRequiresCardNum() throws Exception {
        String token = "t";
        mockAuth(token);

        mockMvc.perform(post("/api/authorizations/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionAmt": 100.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitAuthRequestRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/authorizations/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardNum": "4111111111111111", "transactionAmt": 100}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
