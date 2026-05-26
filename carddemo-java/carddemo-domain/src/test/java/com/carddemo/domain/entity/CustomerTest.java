package com.carddemo.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void testFieldMappingsMatchCVCUS01Y() {
        Customer customer = Customer.builder()
                .custId(1L)                          // PIC 9(09)
                .firstName("Immanuel")               // PIC X(25)
                .middleName("Madeline")              // PIC X(25)
                .lastName("Kessler")                 // PIC X(25)
                .addrLine1("618 Deshaun Route")      // PIC X(50)
                .addrLine2("Apt. 802")               // PIC X(50)
                .addrLine3("Altenwerthshire")         // PIC X(50)
                .addrStateCd("NC")                   // PIC X(02)
                .addrCountryCd("USA")                // PIC X(03)
                .addrZip("12546")                    // PIC X(10)
                .phoneNum1("(908)119-8310")          // PIC X(15)
                .phoneNum2("(373)693-8684")          // PIC X(15)
                .ssn("020973888")                    // PIC 9(09) — stored as String
                .govtIssuedId("000000000493684371")   // PIC X(20)
                .dob(LocalDate.of(1961, 6, 8))       // PIC X(10)
                .eftAccountId("0053581756")          // PIC X(10)
                .priCardHolderInd("Y")               // PIC X(01)
                .ficoCreditScore(274)                // PIC 9(03)
                .build();

        assertEquals(1L, customer.getCustId());
        assertEquals("Immanuel", customer.getFirstName());
        assertEquals("Kessler", customer.getLastName());
        assertEquals("NC", customer.getAddrStateCd());
        assertEquals("020973888", customer.getSsn());
        assertEquals(274, customer.getFicoCreditScore());
        // SSN must be String per spec, not numeric
        assertInstanceOf(String.class, customer.getSsn());
    }
}
