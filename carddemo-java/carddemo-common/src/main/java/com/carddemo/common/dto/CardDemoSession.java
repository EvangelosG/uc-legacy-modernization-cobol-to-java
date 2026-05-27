package com.carddemo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO mapped from COCOM01Y.cpy — CARDDEMO-COMMAREA.
 * Replaces the CICS COMMAREA for inter-program state management.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDemoSession {

    // CDEMO-GENERAL-INFO
    private String fromTransactionId;   // CDEMO-FROM-TRANID PIC X(04)
    private String fromProgram;         // CDEMO-FROM-PROGRAM PIC X(08)
    private String toTransactionId;     // CDEMO-TO-TRANID PIC X(04)
    private String toProgram;           // CDEMO-TO-PROGRAM PIC X(08)
    private String userId;              // CDEMO-USER-ID PIC X(08)
    private String userType;            // CDEMO-USER-TYPE PIC X(01): 'A'=Admin, 'U'=Regular
    private Integer programContext;     // CDEMO-PGM-CONTEXT PIC 9(01): 0=enter, 1=reenter

    // CDEMO-CUSTOMER-INFO
    private Long customerId;            // CDEMO-CUST-ID PIC 9(09)
    private String customerFirstName;   // CDEMO-CUST-FNAME PIC X(25)
    private String customerMiddleName;  // CDEMO-CUST-MNAME PIC X(25)
    private String customerLastName;    // CDEMO-CUST-LNAME PIC X(25)

    // CDEMO-ACCOUNT-INFO
    private Long accountId;             // CDEMO-ACCT-ID PIC 9(11)
    private String accountStatus;       // CDEMO-ACCT-STATUS PIC X(01)

    // CDEMO-CARD-INFO
    private String cardNumber;          // CDEMO-CARD-NUM PIC 9(16)

    // CDEMO-MORE-INFO
    private String lastMap;             // CDEMO-LAST-MAP PIC X(7)
    private String lastMapset;          // CDEMO-LAST-MAPSET PIC X(7)

    public boolean isAdmin() {
        return "A".equals(userType);
    }

    public boolean isRegularUser() {
        return "U".equals(userType);
    }

    public boolean isEntering() {
        return programContext == null || programContext == 0;
    }

    public boolean isReentering() {
        return programContext != null && programContext == 1;
    }
}
