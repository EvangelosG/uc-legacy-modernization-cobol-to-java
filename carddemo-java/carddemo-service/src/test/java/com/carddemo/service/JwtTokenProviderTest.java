package com.carddemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac", 3600000L);
    }

    @Test
    void generateAndValidateToken() {
        String token = provider.generateToken("USER01", "A", "John", "Doe");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo("USER01");
        assertThat(provider.getUserType(token)).isEqualTo("A");
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertThat(provider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void expiredTokenReturnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac", 0L);
        String token = shortLived.generateToken("USER01", "A", "John", "Doe");

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void nullFieldsHandledGracefully() {
        String token = provider.generateToken("USER01", "U", null, null);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo("USER01");
        assertThat(provider.getUserType(token)).isEqualTo("U");
    }
}
