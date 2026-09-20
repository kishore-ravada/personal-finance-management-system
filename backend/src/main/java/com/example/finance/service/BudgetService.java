package com.example.finance.service;

import com.example.finance.dto.budget.BudgetRequest;
import com.example.finance.dto.budget.BudgetResponse;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.DuplicateResourceException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository,
                         UserService userService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Budgets can only be set for EXPENSE categories");
        }

        if (budgetRepository.existsByUserAndCategoryAndMonthAndYear(user, category, request.getMonth(), request.getYear())) {
            throw new DuplicateResourceException("A budget for category '" + category.getName() + "' already exists for " + request.getMonth() + "/" + request.getYear());
        }

        Budget budget = new Budget(user, category, request.getAmount(), request.getMonth(), request.getYear());
        Budget savedBudget = budgetRepository.save(budget);
        return mapToResponse(savedBudget, user);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets(Integer month, Integer year) {
        User user = userService.getCurrentAuthenticatedUser();
        List<Budget> budgets;
        if (month != null && year != null) {
            budgets = budgetRepository.findByUserAndMonthAndYear(user, month, year);
        } else {
            budgets = budgetRepository.findByUser(user);
        }

        return budgets.stream()
                .map(b -> mapToResponse(b, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));
        return mapToResponse(budget, user);
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new BadRequestException("Budgets can only be set for EXPENSE categories");
        }

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());

        Budget updatedBudget = budgetRepository.save(budget);
        return mapToResponse(updatedBudget, user);
    }

    @Transactional
    public void deleteBudget(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));
        budgetRepository.delete(budget);
    }

    public BudgetResponse mapToResponse(Budget budget, User user) {
        YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal spentAmount = transactionRepository.sumAmountByUserAndCategoryAndDateRange(
                user, budget.getCategory(), startDate, endDate
        );

        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }

        BigDecimal remainingAmount = budget.getAmount().subtract(spentAmount);
        boolean overBudget = remainingAmount.compareTo(BigDecimal.ZERO) < 0;

        double percentageUsed = 0.0;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = spentAmount.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmount(),
                budget.getMonth(),
                budget.getYear(),
                spentAmount,
                remainingAmount,
                percentageUsed,
                overBudget,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
