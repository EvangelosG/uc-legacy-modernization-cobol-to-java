package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.service.*;
import com.carddemo.web.config.GlobalExceptionHandler;
import com.carddemo.web.config.JwtAuthenticationFilter;
import com.carddemo.web.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {CardController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void getCardReturnsDetailWithAccountSummary() throws Exception {
        String token = "t";
        mockAuth(token);

        Card card = Card.builder()
                .cardNum("4111111111111111").acctId(1L).cvvCd(123)
                .embossedName("JOHN DOE").expirationDate(LocalDate.of(2025, 12, 31))
                .activeStatus("Y").version(0L).build();

        Account account = Account.builder()
                .acctId(1L).activeStatus("Y").currBal(new BigDecimal("194.00"))
                .creditLimit(new BigDecimal("2020.00")).cashCreditLimit(new BigDecimal("1020.00"))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO)
                .version(0L).build();

        when(cardService.getCardDetail("4111111111111111"))
                .thenReturn(new CardService.CardDetail(card, account));

        mockMvc.perform(get("/api/cards/4111111111111111")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNum").value("4111111111111111"))
                .andExpect(jsonPath("$.embossedName").value("JOHN DOE"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.account.acctId").value(1))
                .andExpect(jsonPath("$.account.currBal").value(194.00));
    }

    @Test
    void getCardNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(cardService.getCardDetail("9999999999999999"))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Card not found: 9999999999999999"));

        mockMvc.perform(get("/api/cards/9999999999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/cards/4111111111111111"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCardsByAccountReturnsPaginatedResults() throws Exception {
        String token = "t";
        mockAuth(token);

        Card card = Card.builder()
                .cardNum("4111111111111111").acctId(1L).cvvCd(123)
                .embossedName("JOHN DOE").expirationDate(LocalDate.of(2025, 12, 31))
                .activeStatus("Y").version(0L).build();

        when(cardService.listCardsByAccount(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/cards")
                        .param("accountId", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardNum").value("4111111111111111"))
                .andExpect(jsonPath("$.content[0].embossedName").value("JOHN DOE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateCardReturnsUpdatedCard() throws Exception {
        String token = "t";
        mockAuth(token);

        Card updatedCard = Card.builder()
                .cardNum("4111111111111111").acctId(1L).cvvCd(123)
                .embossedName("JANE DOE").expirationDate(LocalDate.of(2026, 12, 31))
                .activeStatus("Y").version(1L).build();

        when(cardService.updateCard(eq("4111111111111111"), any(CardService.CardUpdateRequest.class)))
                .thenReturn(updatedCard);

        CardController.CardUpdateRequest request = new CardController.CardUpdateRequest(
                "Y", LocalDate.of(2026, 12, 31), "JANE DOE", 0L);

        mockMvc.perform(put("/api/cards/4111111111111111")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embossedName").value("JANE DOE"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateCardOptimisticLockConflictReturns409() throws Exception {
        String token = "t";
        mockAuth(token);

        when(cardService.updateCard(eq("4111111111111111"), any(CardService.CardUpdateRequest.class)))
                .thenThrow(new CardService.OptimisticLockConflictException(
                        "Card has been modified by another user"));

        CardController.CardUpdateRequest request = new CardController.CardUpdateRequest(
                "Y", null, null, 0L);

        mockMvc.perform(put("/api/cards/4111111111111111")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateCardNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(cardService.updateCard(eq("9999999999999999"), any(CardService.CardUpdateRequest.class)))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Card not found: 9999999999999999"));

        CardController.CardUpdateRequest request = new CardController.CardUpdateRequest(
                "Y", null, null, 0L);

        mockMvc.perform(put("/api/cards/9999999999999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCardInvalidStatusReturns400() throws Exception {
        String token = "t";
        mockAuth(token);

        when(cardService.updateCard(eq("4111111111111111"), any(CardService.CardUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid card status: X"));

        CardController.CardUpdateRequest request = new CardController.CardUpdateRequest(
                "X", null, null, 0L);

        mockMvc.perform(put("/api/cards/4111111111111111")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
