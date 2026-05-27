package com.carddemo.batch.job;

import com.carddemo.common.util.MoneyUtil;
import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CustomerRepository;
import com.carddemo.domain.repository.TransactionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

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
 * Replaces CBSTM03A.CBL / CBSTM03B.CBL — Statement Generation.
 * Reads accounts, customers, and transactions to generate
 * text statements, HTML statements, and PDF statements.
 */
@Slf4j
@Component
public class StatementGenerationJob {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardCrossReferenceRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;

    @Getter
    private long statementsGenerated;

    public StatementGenerationJob(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            CardCrossReferenceRepository cardXrefRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(Path outputDir) throws IOException {
        log.info("START OF EXECUTION OF PROGRAM CBSTM03A (StatementGenerationJob)");
        statementsGenerated = 0;

        Files.createDirectories(outputDir);
        Path textDir = outputDir.resolve("text");
        Path htmlDir = outputDir.resolve("html");
        Path pdfDir = outputDir.resolve("pdf");
        Files.createDirectories(textDir);
        Files.createDirectories(htmlDir);
        Files.createDirectories(pdfDir);

        List<Account> accounts = accountRepository.findAll();
        accounts.sort(Comparator.comparing(Account::getAcctId));

        for (Account account : accounts) {
            List<CardCrossReference> xrefs = cardXrefRepository.findByAcctId(account.getAcctId());
            if (xrefs.isEmpty()) {
                continue;
            }

            CardCrossReference xref = xrefs.get(0);
            Optional<Customer> customerOpt = customerRepository.findById(xref.getCustId());
            if (customerOpt.isEmpty()) {
                continue;
            }
            Customer customer = customerOpt.get();

            List<Transaction> transactions = transactionRepository.findAll().stream()
                    .filter(t -> xrefs.stream().anyMatch(x -> x.getCardNum().equals(t.getCardNum())))
                    .sorted(Comparator.comparing(t -> t.getOrigTs() != null ? t.getOrigTs() : java.time.LocalDateTime.MIN))
                    .toList();

            String textContent = generateTextStatement(account, customer, transactions);
            String htmlContent = generateHtmlStatement(account, customer, transactions);

            String filename = String.format("statement_%011d", account.getAcctId());
            Files.writeString(textDir.resolve(filename + ".txt"), textContent);
            Files.writeString(htmlDir.resolve(filename + ".html"), htmlContent);
            generatePdfStatement(pdfDir.resolve(filename + ".pdf"), account, customer, transactions);

            statementsGenerated++;
        }

        log.info("STATEMENTS GENERATED: {}", statementsGenerated);
        log.info("END OF EXECUTION OF PROGRAM CBSTM03A (StatementGenerationJob)");
    }

    String generateTextStatement(Account account, Customer customer, List<Transaction> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(72)).append("\n");
        sb.append("                    CARDDEMO ACCOUNT STATEMENT\n");
        sb.append("=".repeat(72)).append("\n\n");

        sb.append(String.format("Account Number: %011d%n", account.getAcctId()));
        sb.append(String.format("Customer:       %s %s %s%n",
                nullSafe(customer.getFirstName()),
                nullSafe(customer.getMiddleName()),
                nullSafe(customer.getLastName())));
        sb.append(String.format("Address:        %s%n", nullSafe(customer.getAddrLine1())));
        if (customer.getAddrLine2() != null && !customer.getAddrLine2().isBlank()) {
            sb.append(String.format("                %s%n", customer.getAddrLine2()));
        }
        sb.append(String.format("Statement Date: %s%n", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        sb.append("\n");

        sb.append(String.format("Current Balance:      %12s%n", account.getCurrBal().toPlainString()));
        sb.append(String.format("Credit Limit:         %12s%n", account.getCreditLimit().toPlainString()));
        sb.append(String.format("Available Credit:     %12s%n",
                MoneyUtil.subtract(account.getCreditLimit(), account.getCurrBal()).toPlainString()));
        sb.append("\n");

        sb.append("-".repeat(72)).append("\n");
        sb.append(String.format("%-10s %-16s %-2s %4s %12s %-20s%n",
                "DATE", "TRAN ID", "TY", "CAT", "AMOUNT", "DESCRIPTION"));
        sb.append("-".repeat(72)).append("\n");

        BigDecimal totalDebits = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);

        for (Transaction tx : transactions) {
            String dateStr = tx.getOrigTs() != null ?
                    tx.getOrigTs().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A";
            sb.append(String.format("%-10s %-16s %-2s %4d %12s %-20s%n",
                    dateStr,
                    tx.getTranId(),
                    tx.getTypeCd(),
                    tx.getCatCd(),
                    tx.getAmount().toPlainString(),
                    truncate(tx.getDescription(), 20)));

            if (tx.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                totalCredits = MoneyUtil.add(totalCredits, tx.getAmount());
            } else {
                totalDebits = MoneyUtil.add(totalDebits, tx.getAmount().abs());
            }
        }

        sb.append("-".repeat(72)).append("\n");
        sb.append(String.format("Total Credits: %12s    Total Debits: %12s%n",
                totalCredits.toPlainString(), totalDebits.toPlainString()));
        sb.append(String.format("Transactions:  %d%n", transactions.size()));
        sb.append("=".repeat(72)).append("\n");

        return sb.toString();
    }

    String generateHtmlStatement(Account account, Customer customer, List<Transaction> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append("<title>Account Statement - ").append(account.getAcctId()).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: monospace; margin: 20px; }\n");
        sb.append("table { border-collapse: collapse; width: 100%; }\n");
        sb.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        sb.append("th { background-color: #4472C4; color: white; }\n");
        sb.append(".amount { text-align: right; }\n");
        sb.append(".summary { margin: 10px 0; }\n");
        sb.append("</style>\n</head>\n<body>\n");
        sb.append("<h1>CardDemo Account Statement</h1>\n");

        sb.append("<div class='summary'>\n");
        sb.append("<p><strong>Account:</strong> ").append(String.format("%011d", account.getAcctId())).append("</p>\n");
        sb.append("<p><strong>Customer:</strong> ")
                .append(escapeHtml(nullSafe(customer.getFirstName()))).append(" ")
                .append(escapeHtml(nullSafe(customer.getLastName()))).append("</p>\n");
        sb.append("<p><strong>Current Balance:</strong> $").append(account.getCurrBal().toPlainString()).append("</p>\n");
        sb.append("<p><strong>Credit Limit:</strong> $").append(account.getCreditLimit().toPlainString()).append("</p>\n");
        sb.append("<p><strong>Available Credit:</strong> $")
                .append(MoneyUtil.subtract(account.getCreditLimit(), account.getCurrBal()).toPlainString()).append("</p>\n");
        sb.append("</div>\n");

        sb.append("<table>\n<tr><th>Date</th><th>Transaction ID</th><th>Type</th>");
        sb.append("<th>Category</th><th>Amount</th><th>Description</th></tr>\n");

        for (Transaction tx : transactions) {
            String dateStr = tx.getOrigTs() != null ?
                    tx.getOrigTs().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A";
            sb.append("<tr>");
            sb.append("<td>").append(dateStr).append("</td>");
            sb.append("<td>").append(escapeHtml(tx.getTranId())).append("</td>");
            sb.append("<td>").append(escapeHtml(tx.getTypeCd())).append("</td>");
            sb.append("<td>").append(tx.getCatCd()).append("</td>");
            sb.append("<td class='amount'>").append(tx.getAmount().toPlainString()).append("</td>");
            sb.append("<td>").append(escapeHtml(nullSafe(tx.getDescription()))).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table>\n");
        sb.append("<p>Total transactions: ").append(transactions.size()).append("</p>\n");
        sb.append("</body>\n</html>\n");

        return sb.toString();
    }

    void generatePdfStatement(Path pdfPath, Account account, Customer customer,
                              List<Transaction> transactions) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 750;
                float margin = 50;

                cs.beginText();
                cs.setFont(fontBold, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("CardDemo Account Statement");
                cs.endText();
                y -= 30;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("Account: %011d    Customer: %s %s",
                        account.getAcctId(),
                        nullSafe(customer.getFirstName()),
                        nullSafe(customer.getLastName())));
                cs.endText();
                y -= 15;

                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("Balance: $%s    Credit Limit: $%s    Available: $%s",
                        account.getCurrBal().toPlainString(),
                        account.getCreditLimit().toPlainString(),
                        MoneyUtil.subtract(account.getCreditLimit(), account.getCurrBal()).toPlainString()));
                cs.endText();
                y -= 25;

                // Header
                cs.beginText();
                cs.setFont(fontBold, 8);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("%-10s %-16s %-4s %-4s %12s %-30s",
                        "DATE", "TRAN ID", "TYPE", "CAT", "AMOUNT", "DESCRIPTION"));
                cs.endText();
                y -= 12;

                cs.setFont(fontRegular, 8);
                int maxLines = Math.min(transactions.size(), 50);
                for (int i = 0; i < maxLines; i++) {
                    Transaction tx = transactions.get(i);
                    String dateStr = tx.getOrigTs() != null ?
                            tx.getOrigTs().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A";

                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(String.format("%-10s %-16s %-4s %-4d %12s %-30s",
                            dateStr,
                            tx.getTranId(),
                            tx.getTypeCd(),
                            tx.getCatCd(),
                            tx.getAmount().toPlainString(),
                            truncate(nullSafe(tx.getDescription()), 30)));
                    cs.endText();
                    y -= 10;

                    if (y < 50) {
                        break;
                    }
                }
            }

            document.save(pdfPath.toFile());
        }
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
