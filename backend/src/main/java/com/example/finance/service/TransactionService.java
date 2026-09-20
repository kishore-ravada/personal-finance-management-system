package com.example.finance.service;

import com.example.finance.dto.transaction.*;
import com.example.finance.entity.*;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.AccountRepository;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository,
                              UserService userService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        Account account = accountRepository.findByIdAndUser(request.getAccountId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + request.getAccountId()));

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        // Validate Category Type matches Transaction Type
        validateCategoryAndTransactionType(category, request.getType());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transaction amount must be positive");
        }

        Transaction transaction = new Transaction(
                user,
                account,
                category,
                request.getAmount(),
                request.getType(),
                request.getDescription(),
                request.getTransactionDate()
        );

        // Apply balance update
        applyBalanceEffect(account, request.getType(), request.getAmount());
        accountRepository.save(account);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(
            TransactionType type,
            Long categoryId,
            Long accountId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        User user = userService.getCurrentAuthenticatedUser();

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Transaction> transactionPage = transactionRepository.findFiltered(
                user, type, categoryId, accountId, startDate, endDate, search, pageable
        );

        List<TransactionResponse> content = transactionPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

        Account newAccount = accountRepository.findByIdAndUser(request.getAccountId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + request.getAccountId()));

        Category newCategory = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        validateCategoryAndTransactionType(newCategory, request.getType());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transaction amount must be positive");
        }

        // 1. Reverse old effect on old account
        Account oldAccount = transaction.getAccount();
        reverseBalanceEffect(oldAccount, transaction.getType(), transaction.getAmount());
        accountRepository.save(oldAccount);

        // 2. Apply new effect on new account
        applyBalanceEffect(newAccount, request.getType(), request.getAmount());
        accountRepository.save(newAccount);

        // 3. Update transaction fields
        transaction.setAccount(newAccount);
        transaction.setCategory(newCategory);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return mapToResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        User user = userService.getCurrentAuthenticatedUser();

        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

        Account account = transaction.getAccount();
        reverseBalanceEffect(account, transaction.getType(), transaction.getAmount());
        accountRepository.save(account);

        transactionRepository.delete(transaction);
    }

    private void validateCategoryAndTransactionType(Category category, TransactionType transactionType) {
        if ((transactionType == TransactionType.INCOME && category.getType() != CategoryType.INCOME) ||
            (transactionType == TransactionType.EXPENSE && category.getType() != CategoryType.EXPENSE)) {
            throw new BadRequestException("Category type (" + category.getType() + ") does not match transaction type (" + transactionType + ")");
        }
    }

    private void applyBalanceEffect(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else if (type == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(amount));
        }
    }

    private void reverseBalanceEffect(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(amount));
        } else if (type == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(amount));
        }
    }

    public TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
