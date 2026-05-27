package com.carddemo.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSecurityTest {

    @Test
    void testFieldMappingsMatchCSUSR01Y() {
        UserSecurity user = UserSecurity.builder()
                .usrId("ADMIN01")                // PIC X(08)
                .usrFname("Admin")                // PIC X(20)
                .usrLname("User")                 // PIC X(20)
                .usrPwd("$2a$10$hash...")          // BCrypt hash (not plaintext PIC X(08))
                .usrType("A")                     // PIC X(01)
                .build();

        assertEquals("ADMIN01", user.getUsrId());
        assertEquals("Admin", user.getUsrFname());
        assertEquals("User", user.getUsrLname());
        assertEquals("A", user.getUsrType());
        // Password should be stored as BCrypt hash
        assertTrue(user.getUsrPwd().startsWith("$2a$"));
    }
}
