package com.example.finance.service;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class CsvExportService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public CsvExportService(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToCsv(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentAuthenticatedUser();
        LocalDate start = startDate != null ? startDate : LocalDate.of(2000, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.of(2099, 12, 31);

        List<Transaction> transactions = transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Date", "Type", "Account", "Category", "Amount", "Description")
                .build();

        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), format)) {
            for (Transaction t : transactions) {
                printer.printRecord(
                        t.getId(),
                        t.getTransactionDate(),
                        t.getType(),
                        t.getAccount() != null ? t.getAccount().getName() : "",
                        t.getCategory() != null ? t.getCategory().getName() : "",
                        t.getAmount(),
                        t.getDescription() != null ? t.getDescription() : ""
                );
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV export", e);
        }
    }
}
