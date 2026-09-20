package com.example.finance.dto.report;

import java.math.BigDecimal;
import java.util.List;

public class IncomeExpenseReportResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;
    private List<MonthlyReportResponse> monthlyBreakdown;

    public IncomeExpenseReportResponse() {}

    public IncomeExpenseReportResponse(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netBalance, List<MonthlyReportResponse> monthlyBreakdown) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netBalance = netBalance;
        this.monthlyBreakdown = monthlyBreakdown;
    }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }

    public BigDecimal getNetBalance() { return netBalance; }
    public void setNetBalance(BigDecimal netBalance) { this.netBalance = netBalance; }

    public List<MonthlyReportResponse> getMonthlyBreakdown() { return monthlyBreakdown; }
    public void setMonthlyBreakdown(List<MonthlyReportResponse> monthlyBreakdown) { this.monthlyBreakdown = monthlyBreakdown; }
}
