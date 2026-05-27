package com.carddemo.web.controller;

import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionType;
import com.carddemo.service.TransactionTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    @GetMapping("/transaction-types")
    public ResponseEntity<Page<TransactionTypeResponse>> listTypes(Pageable pageable) {
        Page<TransactionTypeResponse> page = transactionTypeService.listTypes(pageable)
                .map(TransactionTypeResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/transaction-types/{code}")
    public ResponseEntity<TransactionTypeResponse> getType(@PathVariable("code") String code) {
        TransactionType type = transactionTypeService.getType(code);
        return ResponseEntity.ok(TransactionTypeResponse.from(type));
    }

    @PostMapping("/transaction-types")
    public ResponseEntity<TransactionTypeResponse> createType(
            @Valid @RequestBody CreateTransactionTypeRequest request) {
        TransactionType type = transactionTypeService.createType(request.typeCd(), request.typeDesc());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionTypeResponse.from(type));
    }

    @PutMapping("/transaction-types/{code}")
    public ResponseEntity<TransactionTypeResponse> updateType(
            @PathVariable("code") String code,
            @Valid @RequestBody UpdateTransactionTypeRequest request) {
        TransactionType type = transactionTypeService.updateType(code, request.typeDesc());
        return ResponseEntity.ok(TransactionTypeResponse.from(type));
    }

    @DeleteMapping("/transaction-types/{code}")
    public ResponseEntity<Map<String, String>> deleteType(@PathVariable("code") String code) {
        transactionTypeService.deleteType(code);
        return ResponseEntity.ok(Map.of("message", "Transaction type deleted successfully"));
    }

    @GetMapping("/transaction-categories")
    public ResponseEntity<Page<TransactionCategoryResponse>> listCategories(Pageable pageable) {
        Page<TransactionCategoryResponse> page = transactionTypeService.listCategories(pageable)
                .map(TransactionCategoryResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/transaction-categories/{typeCd}/{catCd}")
    public ResponseEntity<TransactionCategoryResponse> getCategory(
            @PathVariable("typeCd") String typeCd,
            @PathVariable("catCd") Integer catCd) {
        TransactionCategory category = transactionTypeService.getCategory(typeCd, catCd);
        return ResponseEntity.ok(TransactionCategoryResponse.from(category));
    }

    @PostMapping("/transaction-categories")
    public ResponseEntity<TransactionCategoryResponse> createCategory(
            @Valid @RequestBody CreateTransactionCategoryRequest request) {
        TransactionCategory category = transactionTypeService.createCategory(
                request.typeCd(), request.catCd(), request.typeDesc());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionCategoryResponse.from(category));
    }

    @PutMapping("/transaction-categories/{typeCd}/{catCd}")
    public ResponseEntity<TransactionCategoryResponse> updateCategory(
            @PathVariable("typeCd") String typeCd,
            @PathVariable("catCd") Integer catCd,
            @Valid @RequestBody UpdateTransactionCategoryRequest request) {
        TransactionCategory category = transactionTypeService.updateCategory(
                typeCd, catCd, request.typeDesc());
        return ResponseEntity.ok(TransactionCategoryResponse.from(category));
    }

    @DeleteMapping("/transaction-categories/{typeCd}/{catCd}")
    public ResponseEntity<Map<String, String>> deleteCategory(
            @PathVariable("typeCd") String typeCd,
            @PathVariable("catCd") Integer catCd) {
        transactionTypeService.deleteCategory(typeCd, catCd);
        return ResponseEntity.ok(Map.of("message", "Transaction category deleted successfully"));
    }

    public record CreateTransactionTypeRequest(
            @NotBlank @Size(min = 1, max = 2) String typeCd,
            String typeDesc
    ) {}

    public record UpdateTransactionTypeRequest(String typeDesc) {}

    public record CreateTransactionCategoryRequest(
            @NotBlank @Size(min = 1, max = 2) String typeCd,
            @NotNull Integer catCd,
            String typeDesc
    ) {}

    public record UpdateTransactionCategoryRequest(String typeDesc) {}

    public record TransactionTypeResponse(String typeCd, String typeDesc) {
        static TransactionTypeResponse from(TransactionType t) {
            return new TransactionTypeResponse(t.getTypeCd(), t.getTypeDesc());
        }
    }

    public record TransactionCategoryResponse(String typeCd, Integer catCd, String typeDesc) {
        static TransactionCategoryResponse from(TransactionCategory c) {
            return new TransactionCategoryResponse(c.getTypeCd(), c.getCatCd(), c.getTypeDesc());
        }
    }
}
