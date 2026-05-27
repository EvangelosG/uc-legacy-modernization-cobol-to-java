package com.carddemo.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class AccountId {

    private final Long value;

    private AccountId(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Account ID must be non-negative");
        }
        this.value = value;
    }

    public static AccountId of(Long value) {
        return new AccountId(value);
    }

    @Override
    public String toString() {
        return String.format("%011d", value);
    }
}
