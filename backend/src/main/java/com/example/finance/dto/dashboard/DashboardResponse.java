package com.example.finance.dto.dashboard;

import com.example.finance.dto.budget.BudgetResponse;
import com.example.finance.dto.savings.SavingsGoalResponse;
import com.example.finance.dto.transaction.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public class DashboardResponse {

    private BigDecimal totalBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlySavings;
    private List<CategorySpendingDto> expenseByCategory;
    private List<TransactionResponse> recentTransactions;
    private List<BudgetResponse> budgetStatus;
    private List<SavingsGoalResponse> savingsGoalsProgress;

    public DashboardResponse() {}

    public DashboardResponse(BigDecimal totalBalance, BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal monthlySavings, List<CategorySpendingDto> expenseByCategory, List<TransactionResponse> recentTransactions, List<BudgetResponse> budgetStatus, List<SavingsGoalResponse> savingsGoalsProgress) {
        this.totalBalance = totalBalance;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.monthlySavings = monthlySavings;
        this.expenseByCategory = expenseByCategory;
        this.recentTransactions = recentTransactions;
        this.budgetStatus = budgetStatus;
        this.savingsGoalsProgress = savingsGoalsProgress;
    }

    public BigDecimal getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }

    public BigDecimal getMonthlySavings() { return monthlySavings; }
    public void setMonthlySavings(BigDecimal monthlySavings) { this.monthlySavings = monthlySavings; }

    public List<CategorySpendingDto> getExpenseByCategory() { return expenseByCategory; }
    public void setExpenseByCategory(List<CategorySpendingDto> expenseByCategory) { this.expenseByCategory = expenseByCategory; }

    public List<TransactionResponse> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<TransactionResponse> recentTransactions) { this.recentTransactions = recentTransactions; }

    public List<BudgetResponse> getBudgetStatus() { return budgetStatus; }
    public void setBudgetStatus(List<BudgetResponse> budgetStatus) { this.budgetStatus = budgetStatus; }

    public List<SavingsGoalResponse> getSavingsGoalsProgress() { return savingsGoalsProgress; }
    public void setSavingsGoalsProgress(List<SavingsGoalResponse> savingsGoalsProgress) { this.savingsGoalsProgress = savingsGoalsProgress; }
}
