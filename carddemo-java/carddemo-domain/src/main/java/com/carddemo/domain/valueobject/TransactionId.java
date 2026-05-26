package com.carddemo.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class TransactionId {

    private final String value;

    private TransactionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transaction ID must not be blank");
        }
        this.value = value;
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
