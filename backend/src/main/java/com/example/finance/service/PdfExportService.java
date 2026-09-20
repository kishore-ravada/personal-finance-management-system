package com.example.finance.service;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.repository.TransactionRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PdfExportService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public PdfExportService(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public byte[] exportFinancialReportToPdf(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentAuthenticatedUser();
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Transaction> transactions = transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, start, end);

        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, TransactionType.INCOME, start, end);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, TransactionType.EXPENSE, start, end);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Document Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLUE);
            Paragraph title = new Paragraph("Personal Finance Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
            Paragraph subtitle = new Paragraph("User: " + user.getName() + " (" + user.getEmail() + ")\nPeriod: " + start + " to " + end, subFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Summary Section
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Paragraph summaryTitle = new Paragraph("Financial Summary", sectionFont);
            summaryTitle.setSpacingAfter(10);
            document.add(summaryTitle);

            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);

            addHeaderCell(summaryTable, "Total Income");
            addHeaderCell(summaryTable, "Total Expense");
            addHeaderCell(summaryTable, "Net Balance");

            addValueCell(summaryTable, "₹ " + totalIncome.toString(), new Color(34, 139, 34));
            addValueCell(summaryTable, "₹ " + totalExpense.toString(), new Color(178, 34, 34));
            addValueCell(summaryTable, "₹ " + netBalance.toString(), netBalance.compareTo(BigDecimal.ZERO) >= 0 ? new Color(34, 139, 34) : new Color(178, 34, 34));

            summaryTable.setSpacingAfter(20);
            document.add(summaryTable);

            // Transactions Section
            Paragraph transTitle = new Paragraph("Transaction Details (" + transactions.size() + " entries)", sectionFont);
            transTitle.setSpacingAfter(10);
            document.add(transTitle);

            PdfPTable transTable = new PdfPTable(6);
            transTable.setWidthPercentage(100);
            transTable.setWidths(new float[]{1.5f, 1.5f, 2.5f, 2.5f, 2.0f, 3.0f});

            addHeaderCell(transTable, "Date");
            addHeaderCell(transTable, "Type");
            addHeaderCell(transTable, "Account");
            addHeaderCell(transTable, "Category");
            addHeaderCell(transTable, "Amount");
            addHeaderCell(transTable, "Description");

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (Transaction t : transactions) {
                transTable.addCell(new Phrase(t.getTransactionDate().toString(), cellFont));
                transTable.addCell(new Phrase(t.getType().name(), cellFont));
                transTable.addCell(new Phrase(t.getAccount() != null ? t.getAccount().getName() : "", cellFont));
                transTable.addCell(new Phrase(t.getCategory() != null ? t.getCategory().getName() : "", cellFont));
                transTable.addCell(new Phrase("₹ " + t.getAmount().toString(), cellFont));
                transTable.addCell(new Phrase(t.getDescription() != null ? t.getDescription() : "", cellFont));
            }

            document.add(transTable);
            document.close();

            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(41, 128, 185));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addValueCell(PdfPTable table, String text, Color textColor) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }
}
