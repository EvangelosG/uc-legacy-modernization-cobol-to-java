package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import com.carddemo.domain.repository.CustomerRepository;
import com.carddemo.service.JwtTokenProvider;
import com.carddemo.service.TransactionTypeService;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {SystemController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private CardRepository cardRepository;

    @MockBean
    private CardCrossReferenceRepository cardCrossReferenceRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void getSystemDateReturnsCurrentDate() throws Exception {
        String token = "t";
        mockAuth(token);

        mockMvc.perform(get("/api/system/date")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.time").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getAccountExtractReturnsFullDetail() throws Exception {
        String token = "t";
        mockAuth(token);

        Account account = Account.builder()
                .acctId(1L).activeStatus("Y")
                .currBal(new BigDecimal("500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .openDate(LocalDate.of(2020, 1, 1))
                .expirationDate(LocalDate.of(2027, 12, 31))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO)
                .addrZip("10001").groupId("GRP01").version(0L).build();

        Customer customer = Customer.builder()
                .custId(100L).firstName("JOHN").lastName("DOE")
                .addrStateCd("NY").addrZip("10001").ficoCreditScore(750)
                .build();

        Card card = Card.builder()
                .cardNum("4111111111111111").acctId(1L)
                .embossedName("JOHN DOE").expirationDate(LocalDate.of(2027, 12, 31))
                .activeStatus("Y").build();

        CardCrossReference xref = CardCrossReference.builder()
                .cardNum("4111111111111111").custId(100L).acctId(1L).build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cardCrossReferenceRepository.findByAcctId(1L)).thenReturn(List.of(xref));
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(cardRepository.findByAcctId(1L)).thenReturn(List.of(card));

        mockMvc.perform(get("/api/accounts/1/extract")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acctId").value(1))
                .andExpect(jsonPath("$.customer.firstName").value("JOHN"))
                .andExpect(jsonPath("$.cards[0].cardNum").value("4111111111111111"));
    }

    @Test
    void getAccountExtractReturns404WhenNotFound() throws Exception {
        String token = "t";
        mockAuth(token);

        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/999/extract")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
