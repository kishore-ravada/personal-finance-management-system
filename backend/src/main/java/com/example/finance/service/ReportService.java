package com.example.finance.service;

import com.example.finance.dto.report.CategoryReportResponse;
import com.example.finance.dto.report.IncomeExpenseReportResponse;
import com.example.finance.dto.report.MonthlyReportResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryType;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public ReportService(TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository,
                         UserService userService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportResponse> getMonthlyReport(Integer year) {
        User user = userService.getCurrentAuthenticatedUser();
        int targetYear = year != null ? year : LocalDate.now().getYear();

        List<MonthlyReportResponse> list = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(targetYear, m);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal income = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, TransactionType.INCOME, start, end);
            BigDecimal expense = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, TransactionType.EXPENSE, start, end);
            BigDecimal net = income.subtract(expense);

            list.add(new MonthlyReportResponse(ym.getMonth().name(), m, targetYear, income, expense, net));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<CategoryReportResponse> getCategoryReport(LocalDate startDate, LocalDate endDate, TransactionType type) {
        User user = userService.getCurrentAuthenticatedUser();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        TransactionType targetType = type != null ? type : TransactionType.EXPENSE;

        List<Category> categories = categoryRepository.findByUser(user);
        BigDecimal grandTotal = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, targetType, start, end);
        CategoryType catTargetType = CategoryType.valueOf(targetType.name());

        List<CategoryReportResponse> responses = new ArrayList<>();
        for (Category cat : categories) {
            if (cat.getType() == catTargetType) {
                BigDecimal catTotal = transactionRepository.sumAmountByUserAndCategoryAndDateRange(user, cat, start, end);
                if (catTotal != null && catTotal.compareTo(BigDecimal.ZERO) > 0) {
                    double pct = 0.0;
                    if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                        pct = catTotal.multiply(BigDecimal.valueOf(100))
                                .divide(grandTotal, 2, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                    responses.add(new CategoryReportResponse(cat.getName(), cat.getType().name(), catTotal, pct));
                }
            }
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public IncomeExpenseReportResponse getIncomeExpenseReport(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentAuthenticatedUser();
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, TransactionType.INCOME, start, end);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndTypeAndDateRange(user, TransactionType.EXPENSE, start, end);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        List<MonthlyReportResponse> monthlyBreakdown = getMonthlyReport(start.getYear());

        return new IncomeExpenseReportResponse(totalIncome, totalExpense, netBalance, monthlyBreakdown);
    }
}
