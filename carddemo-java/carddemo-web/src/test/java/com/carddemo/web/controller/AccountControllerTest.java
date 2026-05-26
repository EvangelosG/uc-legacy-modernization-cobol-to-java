package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.Customer;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {AccountController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void getAccountReturnsDetailWithCustomerAndCards() throws Exception {
        String token = "t";
        mockAuth(token);

        Account account = Account.builder()
                .acctId(1L).activeStatus("Y").currBal(new BigDecimal("194.00"))
                .creditLimit(new BigDecimal("2020.00")).cashCreditLimit(new BigDecimal("1020.00"))
                .openDate(LocalDate.of(2014, 11, 20)).expirationDate(LocalDate.of(2025, 5, 20))
                .reissueDate(LocalDate.of(2025, 5, 20))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO)
                .addrZip("A000000000").groupId("").version(0L).build();

        Customer customer = Customer.builder()
                .custId(1L).firstName("John").middleName("M").lastName("Doe")
                .addrZip("12345").ficoCreditScore(750).build();

        Card card = Card.builder()
                .cardNum("4111111111111111").acctId(1L).cvvCd(123)
                .embossedName("JOHN DOE").expirationDate(LocalDate.of(2025, 12, 31))
                .activeStatus("Y").build();

        when(accountService.getAccountDetail(1L))
                .thenReturn(new AccountService.AccountDetail(account, customer, List.of(card)));

        mockMvc.perform(get("/api/accounts/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acctId").value(1))
                .andExpect(jsonPath("$.currBal").value(194.00))
                .andExpect(jsonPath("$.customer.firstName").value("John"))
                .andExpect(jsonPath("$.customer.ficoCreditScore").value(750))
                .andExpect(jsonPath("$.cards[0].cardNum").value("4111111111111111"))
                .andExpect(jsonPath("$.cards[0].embossedName").value("JOHN DOE"));
    }

    @Test
    void getAccountNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(accountService.getAccountDetail(999L))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Account not found: 999"));

        mockMvc.perform(get("/api/accounts/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isUnauthorized());
    }
}
