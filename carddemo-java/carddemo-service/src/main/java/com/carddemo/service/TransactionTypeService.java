package com.carddemo.service;

import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionCategoryId;
import com.carddemo.domain.entity.TransactionType;
import com.carddemo.domain.repository.TransactionCategoryRepository;
import com.carddemo.domain.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    @Transactional(readOnly = true)
    public Page<TransactionType> listTypes(Pageable pageable) {
        return transactionTypeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public TransactionType getType(String typeCd) {
        return transactionTypeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type not found: " + typeCd));
    }

    @Transactional
    public TransactionType createType(String typeCd, String typeDesc) {
        if (transactionTypeRepository.existsById(typeCd)) {
            throw new DuplicateResourceException("Transaction type already exists: " + typeCd);
        }
        TransactionType type = TransactionType.builder()
                .typeCd(typeCd)
                .typeDesc(typeDesc)
                .build();
        return transactionTypeRepository.save(type);
    }

    @Transactional
    public TransactionType updateType(String typeCd, String typeDesc) {
        TransactionType type = transactionTypeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type not found: " + typeCd));
        if (typeDesc != null) {
            type.setTypeDesc(typeDesc);
        }
        return transactionTypeRepository.save(type);
    }

    @Transactional
    public void deleteType(String typeCd) {
        if (!transactionTypeRepository.existsById(typeCd)) {
            throw new ResourceNotFoundException("Transaction type not found: " + typeCd);
        }
        transactionTypeRepository.deleteById(typeCd);
    }

    @Transactional(readOnly = true)
    public Page<TransactionCategory> listCategories(Pageable pageable) {
        return transactionCategoryRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public TransactionCategory getCategory(String typeCd, Integer catCd) {
        return transactionCategoryRepository.findById(new TransactionCategoryId(typeCd, catCd))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction category not found: " + typeCd + "/" + catCd));
    }

    @Transactional(readOnly = true)
    public List<TransactionCategory> getCategoriesByType(String typeCd) {
        return transactionCategoryRepository.findByTypeCd(typeCd);
    }

    @Transactional
    public TransactionCategory createCategory(String typeCd, Integer catCd, String typeDesc) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        if (transactionCategoryRepository.existsById(id)) {
            throw new DuplicateResourceException(
                    "Transaction category already exists: " + typeCd + "/" + catCd);
        }
        TransactionCategory category = TransactionCategory.builder()
                .typeCd(typeCd)
                .catCd(catCd)
                .typeDesc(typeDesc)
                .build();
        return transactionCategoryRepository.save(category);
    }

    @Transactional
    public TransactionCategory updateCategory(String typeCd, Integer catCd, String typeDesc) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        TransactionCategory category = transactionCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction category not found: " + typeCd + "/" + catCd));
        if (typeDesc != null) {
            category.setTypeDesc(typeDesc);
        }
        return transactionCategoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(String typeCd, Integer catCd) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        if (!transactionCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Transaction category not found: " + typeCd + "/" + catCd);
        }
        transactionCategoryRepository.deleteById(id);
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }
}
