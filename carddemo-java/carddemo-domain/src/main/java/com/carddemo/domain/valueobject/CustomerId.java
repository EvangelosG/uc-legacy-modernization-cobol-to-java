package com.carddemo.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CustomerId {

    private final Long value;

    private CustomerId(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Customer ID must be non-negative");
        }
        this.value = value;
    }

    public static CustomerId of(Long value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return String.format("%09d", value);
    }
}
