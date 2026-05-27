package com.carddemo.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CardNumber {

    private static final int LENGTH = 16;

    private final String value;

    private CardNumber(String value) {
        if (value == null || value.length() != LENGTH) {
            throw new IllegalArgumentException("Card number must be exactly " + LENGTH + " characters");
        }
        this.value = value;
    }

    public static CardNumber of(String value) {
        return new CardNumber(value);
    }

    public String masked() {
        return "****-****-****-" + value.substring(12);
    }

    @Override
    public String toString() {
        return masked();
    }
}
