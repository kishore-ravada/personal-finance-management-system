package com.example.finance.controller;

import com.example.finance.dto.report.CategoryReportResponse;
import com.example.finance.dto.report.IncomeExpenseReportResponse;
import com.example.finance.dto.report.MonthlyReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.service.CsvExportService;
import com.example.finance.service.PdfExportService;
import com.example.finance.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    public ReportController(ReportService reportService,
                            CsvExportService csvExportService,
                            PdfExportService pdfExportService) {
        this.reportService = reportService;
        this.csvExportService = csvExportService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyReportResponse>> getMonthlyReport(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryReportResponse>> getCategoryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type
    ) {
        return ResponseEntity.ok(reportService.getCategoryReport(startDate, endDate, type));
    }

    @GetMapping("/income-expense")
    public ResponseEntity<IncomeExpenseReportResponse> getIncomeExpenseReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getIncomeExpenseReport(startDate, endDate));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] csvData = csvExportService.exportTransactionsToCsv(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdfData = pdfExportService.exportFinancialReportToPdf(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
