package com.carddemo.web.controller;

import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionType;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {TransactionTypeController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class TransactionTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionTypeService transactionTypeService;

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
    void listTypesReturnsPaginatedResults() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionType type = TransactionType.builder().typeCd("01").typeDesc("Purchase").build();
        when(transactionTypeService.listTypes(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(type), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/reference/transaction-types")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].typeCd").value("01"))
                .andExpect(jsonPath("$.content[0].typeDesc").value("Purchase"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTypeReturnsTransactionType() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionType type = TransactionType.builder().typeCd("01").typeDesc("Purchase").build();
        when(transactionTypeService.getType("01")).thenReturn(type);

        mockMvc.perform(get("/api/reference/transaction-types/01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeCd").value("01"))
                .andExpect(jsonPath("$.typeDesc").value("Purchase"));
    }

    @Test
    void getTypeNotFoundReturns404() throws Exception {
        String token = "t";
        mockAuth(token);

        when(transactionTypeService.getType("99"))
                .thenThrow(new TransactionTypeService.ResourceNotFoundException("Transaction type not found: 99"));

        mockMvc.perform(get("/api/reference/transaction-types/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTypeReturns201() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionType created = TransactionType.builder().typeCd("08").typeDesc("New Type").build();
        when(transactionTypeService.createType("08", "New Type")).thenReturn(created);

        mockMvc.perform(post("/api/reference/transaction-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionTypeController.CreateTransactionTypeRequest("08", "New Type"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeCd").value("08"));
    }

    @Test
    void createDuplicateTypeReturns409() throws Exception {
        String token = "t";
        mockAuth(token);

        when(transactionTypeService.createType("01", "Duplicate"))
                .thenThrow(new TransactionTypeService.DuplicateResourceException("Transaction type already exists: 01"));

        mockMvc.perform(post("/api/reference/transaction-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionTypeController.CreateTransactionTypeRequest("01", "Duplicate"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateTypeReturnsUpdated() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionType updated = TransactionType.builder().typeCd("01").typeDesc("Updated Purchase").build();
        when(transactionTypeService.updateType("01", "Updated Purchase")).thenReturn(updated);

        mockMvc.perform(put("/api/reference/transaction-types/01")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionTypeController.UpdateTransactionTypeRequest("Updated Purchase"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeDesc").value("Updated Purchase"));
    }

    @Test
    void deleteTypeReturnsOk() throws Exception {
        String token = "t";
        mockAuth(token);

        doNothing().when(transactionTypeService).deleteType("01");

        mockMvc.perform(delete("/api/reference/transaction-types/01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction type deleted successfully"));
    }

    @Test
    void listCategoriesReturnsPaginatedResults() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionCategory cat = TransactionCategory.builder()
                .typeCd("01").catCd(1).typeDesc("Regular Sales Draft").build();
        when(transactionTypeService.listCategories(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/reference/transaction-categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].typeCd").value("01"))
                .andExpect(jsonPath("$.content[0].catCd").value(1));
    }

    @Test
    void getCategoryReturnsResult() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionCategory cat = TransactionCategory.builder()
                .typeCd("01").catCd(1).typeDesc("Regular Sales Draft").build();
        when(transactionTypeService.getCategory("01", 1)).thenReturn(cat);

        mockMvc.perform(get("/api/reference/transaction-categories/01/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeDesc").value("Regular Sales Draft"));
    }

    @Test
    void createCategoryReturns201() throws Exception {
        String token = "t";
        mockAuth(token);

        TransactionCategory created = TransactionCategory.builder()
                .typeCd("01").catCd(99).typeDesc("New Category").build();
        when(transactionTypeService.createCategory("01", 99, "New Category")).thenReturn(created);

        mockMvc.perform(post("/api/reference/transaction-categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionTypeController.CreateTransactionCategoryRequest("01", 99, "New Category"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.catCd").value(99));
    }

    @Test
    void deleteCategoryReturnsOk() throws Exception {
        String token = "t";
        mockAuth(token);

        doNothing().when(transactionTypeService).deleteCategory("01", 1);

        mockMvc.perform(delete("/api/reference/transaction-categories/01/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction category deleted successfully"));
    }

    @Test
    void listTypesRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/reference/transaction-types"))
                .andExpect(status().isUnauthorized());
    }
}
