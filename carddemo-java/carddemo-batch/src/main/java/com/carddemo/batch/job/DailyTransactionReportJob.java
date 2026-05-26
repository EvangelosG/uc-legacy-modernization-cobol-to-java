package com.carddemo.batch.job;

import com.carddemo.common.util.MoneyUtil;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.TransactionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Replaces CBTRN03C.cbl — Daily Transaction Report.
 * Multi-level break reporting: date → card → account.
 * Generates report totals at each break level.
 */
@Slf4j
@Component
public class DailyTransactionReportJob {

    private final TransactionRepository transactionRepository;
    private final CardCrossReferenceRepository cardXrefRepository;

    @Getter
    private long transactionCount;
    @Getter
    private BigDecimal grandTotal;
    @Getter
    private String lastReportContent;

    public DailyTransactionReportJob(
            TransactionRepository transactionRepository,
            CardCrossReferenceRepository cardXrefRepository) {
        this.transactionRepository = transactionRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    public void execute(Path outputFile) throws IOException {
        log.info("START OF EXECUTION OF PROGRAM CBTRN03C (DailyTransactionReportJob)");
        transactionCount = 0;
        grandTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);

        List<Transaction> transactions = transactionRepository.findAll();
        transactions.sort(Comparator
                .comparing((Transaction t) -> t.getOrigTs() != null ? t.getOrigTs().toLocalDate() : LocalDate.MIN)
                .thenComparing(Transaction::getCardNum)
                .thenComparing(t -> {
                    Optional<CardCrossReference> xref = cardXrefRepository.findByCardNum(t.getCardNum());
                    return xref.map(CardCrossReference::getAcctId).orElse(0L);
                }));

        StringBuilder report = new StringBuilder();

        report.append(String.format("%-80s%n", "CARDDEMO DAILY TRANSACTION REPORT"));
        report.append(String.format("%-80s%n", "=".repeat(80)));
        report.append(String.format("%-26s %-16s %-11s %-2s %4s %12s %-30s%n",
                "DATE", "TRAN ID", "ACCT ID", "TY", "CAT", "AMOUNT", "DESCRIPTION"));
        report.append(String.format("%-80s%n", "-".repeat(80)));

        LocalDate lastDate = null;
        String lastCardNum = null;
        Long lastAcctId = null;
        BigDecimal dateTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
        BigDecimal cardTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
        BigDecimal acctTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);

        for (Transaction tx : transactions) {
            transactionCount++;
            LocalDate txDate = tx.getOrigTs() != null ? tx.getOrigTs().toLocalDate() : LocalDate.MIN;
            Optional<CardCrossReference> xref = cardXrefRepository.findByCardNum(tx.getCardNum());
            Long acctId = xref.map(CardCrossReference::getAcctId).orElse(0L);

            // Date break
            if (lastDate != null && !txDate.equals(lastDate)) {
                writeAccountTotal(report, lastAcctId, acctTotal);
                writeCardTotal(report, lastCardNum, cardTotal);
                writeDateTotal(report, lastDate, dateTotal);
                dateTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
                cardTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
                acctTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
                lastCardNum = null;
                lastAcctId = null;
            } else if (lastCardNum != null && !tx.getCardNum().equals(lastCardNum)) {
                // Card break
                writeAccountTotal(report, lastAcctId, acctTotal);
                writeCardTotal(report, lastCardNum, cardTotal);
                cardTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
                acctTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
                lastAcctId = null;
            } else if (lastAcctId != null && !acctId.equals(lastAcctId)) {
                // Account break
                writeAccountTotal(report, lastAcctId, acctTotal);
                acctTotal = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
            }

            lastDate = txDate;
            lastCardNum = tx.getCardNum();
            lastAcctId = acctId;

            // Detail line
            String dateStr = txDate.equals(LocalDate.MIN) ? "N/A" :
                    txDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            report.append(String.format("%-26s %-16s %-11d %-2s %4d %12s %-30s%n",
                    dateStr,
                    tx.getTranId(),
                    acctId,
                    tx.getTypeCd(),
                    tx.getCatCd(),
                    tx.getAmount().toPlainString(),
                    truncate(tx.getDescription(), 30)));

            acctTotal = MoneyUtil.add(acctTotal, tx.getAmount());
            cardTotal = MoneyUtil.add(cardTotal, tx.getAmount());
            dateTotal = MoneyUtil.add(dateTotal, tx.getAmount());
            grandTotal = MoneyUtil.add(grandTotal, tx.getAmount());
        }

        // Final breaks
        if (lastAcctId != null) {
            writeAccountTotal(report, lastAcctId, acctTotal);
        }
        if (lastCardNum != null) {
            writeCardTotal(report, lastCardNum, cardTotal);
        }
        if (lastDate != null) {
            writeDateTotal(report, lastDate, dateTotal);
        }

        report.append(String.format("%-80s%n", "=".repeat(80)));
        report.append(String.format("GRAND TOTAL: %12s    TRANSACTIONS: %d%n",
                grandTotal.toPlainString(), transactionCount));

        lastReportContent = report.toString();

        if (outputFile != null) {
            Files.createDirectories(outputFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                writer.write(lastReportContent);
            }
        }

        log.info("REPORT GENERATED: {} transactions, grand total: {}", transactionCount, grandTotal);
        log.info("END OF EXECUTION OF PROGRAM CBTRN03C (DailyTransactionReportJob)");
    }

    private void writeAccountTotal(StringBuilder report, Long acctId, BigDecimal total) {
        report.append(String.format("  ** ACCOUNT %d TOTAL: %12s%n", acctId, total.toPlainString()));
    }

    private void writeCardTotal(StringBuilder report, String cardNum, BigDecimal total) {
        report.append(String.format("  * CARD %s TOTAL: %12s%n", cardNum, total.toPlainString()));
    }

    private void writeDateTotal(StringBuilder report, LocalDate date, BigDecimal total) {
        report.append(String.format("*** DATE %s TOTAL: %12s%n",
                date.format(DateTimeFormatter.ISO_LOCAL_DATE), total.toPlainString()));
        report.append(String.format("%-80s%n", "-".repeat(80)));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
