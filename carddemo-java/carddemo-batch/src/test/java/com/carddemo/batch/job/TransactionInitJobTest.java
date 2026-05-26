package com.carddemo.batch.job;

import com.carddemo.batch.BatchTestApplication;
import com.carddemo.domain.entity.DailyTransaction;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.DailyTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BatchTestApplication.class)
@Testcontainers
@ActiveProfiles("test")
class TransactionInitJobTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("carddemo_init_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionInitJob initJob;

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Test
    void testParseCobolSignedPositiveAmount() {
        // DALYTRAN-AMT PIC S9(09)V99 — "0000005047G" means +50478 → $504.78
        // Actually: the last char encodes the sign and last digit
        // G = positive 7, so 0000005047 + 7 = 00000050477 → move decimal 2 left = 504.77
        // Wait - the actual format from the data file: 0000005047G
        // Parse: digits = 0000005047, last char = G
        // G maps to... checking MoneyUtil: A=1, B=2, C=3, D=4, E=5, F=6, G=7
        // So lastDigit = 7, positive
        // full = 00000050477, movePointLeft(2) = 504.77
        DailyTransaction dt = initJob.parseDailyTransaction(
                // Build a minimal 350-char line
                buildTestLine("0000000000683580", "01", "0001",
                        "POS TERM  ", padRight("Purchase at Abshire-Lowe", 100),
                        "0000005047G",  // amount: 504.77
                        "800000000",
                        padRight("Abshire-Lowe", 50),
                        padRight("North Enoshaven", 50),
                        padRight("72112", 10),
                        "4859452612877065",
                        "2022-06-10 19:27:53.000000"));

        assertNotNull(dt, "Should parse valid line");
        assertEquals("0000000000683580", dt.getTranId());
        assertEquals("01", dt.getTypeCd());
        assertEquals(1, dt.getCatCd());
        assertEquals(new BigDecimal("504.77").setScale(2, RoundingMode.HALF_UP),
                dt.getAmount().setScale(2, RoundingMode.HALF_UP),
                "Amount must match COBOL signed decimal exactly");
    }

    @Test
    void testParseCobolSignedNegativeAmount() {
        // } = -0, J=-1, K=-2, ..., R=-9
        // 0000009190} means 00000091900 → 919.00, negative → -919.00
        DailyTransaction dt = initJob.parseDailyTransaction(
                buildTestLine("0000000001774260", "03", "0001",
                        "OPERATOR  ", padRight("Return item at Nitzsche", 100),
                        "0000009190}",  // amount: -919.00
                        "800000000",
                        padRight("Nitzsche", 50),
                        padRight("Fidelshire", 50),
                        padRight("53378", 10),
                        "0927987108636232",
                        "2022-06-10 19:27:53.000000"));

        assertNotNull(dt);
        assertEquals(new BigDecimal("-919.00").setScale(2, RoundingMode.HALF_UP),
                dt.getAmount().setScale(2, RoundingMode.HALF_UP),
                "Negative amount must be parsed correctly from COBOL signed format");
    }

    @Test
    void testExecuteWithSampleFile() throws IOException {
        // Clear existing daily transactions
        dailyTransactionRepository.deleteAll();

        // Copy the actual sample file
        Path sampleFile = Path.of("/home/ubuntu/repos/uc-legacy-modernization-cobol-to-java/app/data/ASCII/dailytran.txt");
        if (Files.exists(sampleFile)) {
            initJob.execute(sampleFile);
            assertTrue(initJob.getProcessedCount() > 0, "Should process some transactions");
            assertTrue(dailyTransactionRepository.count() > 0,
                    "Should have saved transactions to database");
        }
    }

    private String buildTestLine(String tranId, String typeCd, String catCd,
                                  String source, String desc, String amount,
                                  String merchantId, String merchantName,
                                  String merchantCity, String merchantZip,
                                  String cardNum, String origTs) {
        StringBuilder sb = new StringBuilder();
        sb.append(tranId);       // 16
        sb.append(typeCd);       // 2
        sb.append(catCd);        // 4
        sb.append(source);       // 10
        sb.append(desc);         // 100
        sb.append(amount);       // 11
        sb.append(merchantId);   // 9
        sb.append(merchantName); // 50
        sb.append(merchantCity); // 50
        sb.append(merchantZip);  // 10
        sb.append(cardNum);      // 16
        sb.append(origTs);       // 26
        // Pad to 350
        while (sb.length() < 350) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return String.format("%-" + len + "s", s);
    }
}
