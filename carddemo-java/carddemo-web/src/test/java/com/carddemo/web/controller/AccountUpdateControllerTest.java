package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Customer;
import com.carddemo.service.*;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AccountUpdateController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AccountUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountUpdateService accountUpdateService;

    @MockBean
    private CustomerUpdateService customerUpdateService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void updateAccountReturnsUpdatedAccount() throws Exception {
        String token = "t";
        mockAuth(token);

        Account updated = Account.builder()
                .acctId(1L).activeStatus("Y")
                .currBal(new BigDecimal("500.00"))
                .creditLimit(new BigDecimal("10000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.of(2020, 1, 1))
                .expirationDate(LocalDate.of(2027, 12, 31))
                .groupId("GRP01").version(1L).build();

        when(accountUpdateService.updateAccount(eq(1L), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/accounts/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"creditLimit": 10000.00, "cashCreditLimit": 2000.00, "version": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acctId").value(1))
                .andExpect(jsonPath("$.creditLimit").value(10000.00));
    }

    @Test
    void updateAccountReturns400OnValidationError() throws Exception {
        String token = "t";
        mockAuth(token);

        when(accountUpdateService.updateAccount(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Credit limit must not be negative"));

        mockMvc.perform(put("/api/accounts/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"creditLimit": -100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit limit must not be negative"));
    }

    @Test
    void updateAccountReturns404WhenNotFound() throws Exception {
        String token = "t";
        mockAuth(token);

        when(accountUpdateService.updateAccount(eq(999L), any()))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Account not found: 999"));

        mockMvc.perform(put("/api/accounts/999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAccountReturns409OnVersionConflict() throws Exception {
        String token = "t";
        mockAuth(token);

        when(accountUpdateService.updateAccount(eq(1L), any()))
                .thenThrow(new CardService.OptimisticLockConflictException("Account has been modified"));

        mockMvc.perform(put("/api/accounts/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 0}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCustomerReturnsUpdatedCustomer() throws Exception {
        String token = "t";
        mockAuth(token);

        Customer updated = Customer.builder()
                .custId(100L).firstName("JANE").middleName("Q").lastName("SMITH")
                .addrLine1("456 OAK AVE").addrStateCd("CA").addrCountryCd("US")
                .addrZip("90210").phoneNum1("3105551234").ssn("123456789")
                .dob(LocalDate.of(1990, 1, 15)).ficoCreditScore(780)
                .build();

        when(customerUpdateService.updateCustomer(eq(100L), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/customers/100")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "JANE", "lastName": "SMITH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custId").value(100))
                .andExpect(jsonPath("$.firstName").value("JANE"))
                .andExpect(jsonPath("$.lastName").value("SMITH"));
    }

    @Test
    void updateCustomerRequiresAuth() throws Exception {
        mockMvc.perform(put("/api/customers/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
