package com.example.finance;

import com.example.finance.dto.transaction.TransactionRequest;
import com.example.finance.dto.transaction.TransactionResponse;
import com.example.finance.entity.*;
import com.example.finance.exception.BadRequestException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.service.TransactionService;
import com.example.finance.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Category expenseCategory;
    private Category incomeCategory;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Alice", "alice@example.com", "password", Role.USER);
        account = new Account(user, "Bank Account", AccountType.BANK, new BigDecimal("10000.00"), "INR");
        account.setId(10L);

        expenseCategory = new Category(user, "Food", CategoryType.EXPENSE);
        expenseCategory.setId(20L);

        incomeCategory = new Category(user, "Salary", CategoryType.INCOME);
        incomeCategory.setId(21L);
    }

    @Test
    void createExpenseTransaction_DecreasesAccountBalance() {
        TransactionRequest request = new TransactionRequest(
                10L, 20L, new BigDecimal("2000.00"), TransactionType.EXPENSE, "Groceries", LocalDate.now()
        );

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(accountRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUser(20L, user)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("8000.00"), account.getBalance()); // 10000 - 2000
        verify(accountRepository).save(account);
    }

    @Test
    void createIncomeTransaction_IncreasesAccountBalance() {
        TransactionRequest request = new TransactionRequest(
                10L, 21L, new BigDecimal("5000.00"), TransactionType.INCOME, "Bonus", LocalDate.now()
        );

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(accountRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUser(21L, user)).thenReturn(Optional.of(incomeCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(101L);
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("15000.00"), account.getBalance()); // 10000 + 5000
        verify(accountRepository).save(account);
    }

    @Test
    void createExpenseTransaction_WithIncomeCategory_ThrowsBadRequest() {
        TransactionRequest request = new TransactionRequest(
                10L, 21L, new BigDecimal("1000.00"), TransactionType.EXPENSE, "Mismatched", LocalDate.now()
        );

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(accountRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUser(21L, user)).thenReturn(Optional.of(incomeCategory));

        assertThrows(BadRequestException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void updateTransaction_ReversesOldEffectAndAppliesNewEffect() {
        // Initial transaction: Expense of 2000 (Account balance was reduced to 8000)
        account.setBalance(new BigDecimal("8000.00"));
        Transaction oldTransaction = new Transaction(user, account, expenseCategory, new BigDecimal("2000.00"), TransactionType.EXPENSE, "Old", LocalDate.now());
        oldTransaction.setId(100L);

        // Updated request: Expense changed to 3000
        TransactionRequest updateRequest = new TransactionRequest(
                10L, 20L, new BigDecimal("3000.00"), TransactionType.EXPENSE, "Updated", LocalDate.now()
        );

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUser(100L, user)).thenReturn(Optional.of(oldTransaction));
        when(accountRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUser(20L, user)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any())).thenReturn(oldTransaction);

        transactionService.updateTransaction(100L, updateRequest);

        // Reverse old (-2000 => +2000 = 10000), Apply new (-3000 = 7000)
        assertEquals(new BigDecimal("7000.00"), account.getBalance());
    }
}
