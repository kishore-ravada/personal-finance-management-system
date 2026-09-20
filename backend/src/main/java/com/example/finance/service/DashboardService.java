package com.example.finance.service;

import com.example.finance.dto.budget.BudgetResponse;
import com.example.finance.dto.dashboard.CategorySpendingDto;
import com.example.finance.dto.dashboard.DashboardResponse;
import com.example.finance.dto.savings.SavingsGoalResponse;
import com.example.finance.dto.transaction.TransactionResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final SavingsGoalService savingsGoalService;
    private final UserService userService;

    public DashboardService(AccountRepository accountRepository,
                             TransactionRepository transactionRepository,
                             TransactionService transactionService,
                             BudgetService budgetService,
                             SavingsGoalService savingsGoalService,
                             UserService userService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.savingsGoalService = savingsGoalService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        User user = userService.getCurrentAuthenticatedUser();
        LocalDate now = LocalDate.now();
        YearMonth currentYearMonth = YearMonth.of(now.getYear(), now.getMonthValue());
        LocalDate startOfMonth = currentYearMonth.atDay(1);
        LocalDate endOfMonth = currentYearMonth.atEndOfMonth();

        // 1. Total Balance across all accounts
        List<Account> accounts = accountRepository.findByUser(user);
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Monthly Income
        BigDecimal monthlyIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                user, TransactionType.INCOME, startOfMonth, endOfMonth
        );

        // 3. Monthly Expenses
        BigDecimal monthlyExpenses = transactionRepository.sumAmountByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, startOfMonth, endOfMonth
        );

        // 4. Monthly Savings
        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);

        // 5. Expense by Category
        List<Object[]> rawCategorySpending = transactionRepository.getExpenseByCategoryAndDateRange(
                user, startOfMonth, endOfMonth
        );
        List<CategorySpendingDto> expenseByCategory = new ArrayList<>();
        for (Object[] row : rawCategorySpending) {
            String catName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            expenseByCategory.add(new CategorySpendingDto(catName, amount));
        }

        // 6. Recent Transactions (top 5)
        List<TransactionResponse> recentTransactions = transactionRepository
                .findTop5ByUserOrderByTransactionDateDescCreatedAtDesc(user)
                .stream()
                .map(transactionService::mapToResponse)
                .collect(Collectors.toList());

        // 7. Budget Status (current month)
        List<BudgetResponse> budgetStatus = budgetService.getAllBudgets(now.getMonthValue(), now.getYear());

        // 8. Savings Goals Progress
        List<SavingsGoalResponse> savingsGoalsProgress = savingsGoalService.getAllSavingsGoals();

        return new DashboardResponse(
                totalBalance,
                monthlyIncome,
                monthlyExpenses,
                monthlySavings,
                expenseByCategory,
                recentTransactions,
                budgetStatus,
                savingsGoalsProgress
        );
    }
}
