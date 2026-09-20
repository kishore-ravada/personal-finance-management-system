package com.example.finance.service;

import com.example.finance.dto.account.AccountRequest;
import com.example.finance.dto.account.AccountResponse;
import com.example.finance.entity.Account;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        if (accountRepository.existsByNameAndUser(request.getName(), user)) {
            throw new BadRequestException("An account with name '" + request.getName() + "' already exists");
        }

        BigDecimal initialBalance = request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO;
        String currency = request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "INR";

        Account account = new Account(user, request.getName(), request.getType(), initialBalance, currency);
        Account savedAccount = accountRepository.save(account);

        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        User user = userService.getCurrentAuthenticatedUser();
        return accountRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));

        account.setName(request.getName());
        account.setType(request.getType());
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            account.setCurrency(request.getCurrency());
        }

        Account updatedAccount = accountRepository.save(account);
        return mapToResponse(updatedAccount);
    }

    @Transactional
    public void deleteAccount(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
        accountRepository.delete(account);
    }

    public AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
