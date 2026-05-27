package com.carddemo.domain.valueobject;

import lombok.Getter;

@Getter
public enum UserType {

    ADMIN('A'),
    REGULAR('U');

    private final char code;

    UserType(char code) {
        this.code = code;
    }

    public static UserType fromCode(char code) {
        for (UserType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user type code: " + code);
    }

    public static UserType fromCode(String code) {
        if (code == null || code.length() != 1) {
            throw new IllegalArgumentException("User type code must be a single character");
        }
        return fromCode(code.charAt(0));
    }
}
