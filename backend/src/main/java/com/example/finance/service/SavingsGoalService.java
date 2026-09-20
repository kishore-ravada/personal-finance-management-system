package com.example.finance.service;

import com.example.finance.dto.savings.SavingsGoalRequest;
import com.example.finance.dto.savings.SavingsGoalResponse;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserService userService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository, UserService userService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userService = userService;
    }

    @Transactional
    public SavingsGoalResponse createSavingsGoal(SavingsGoalRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be positive");
        }

        BigDecimal currentAmount = request.getCurrentAmount() != null ? request.getCurrentAmount() : BigDecimal.ZERO;

        SavingsGoal goal = new SavingsGoal(
                user,
                request.getName(),
                request.getTargetAmount(),
                currentAmount,
                request.getTargetDate()
        );

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        return mapToResponse(savedGoal);
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getAllSavingsGoals() {
        User user = userService.getCurrentAuthenticatedUser();
        return savingsGoalRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getSavingsGoalById(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));
        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse updateSavingsGoal(Long id, SavingsGoalRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be positive");
        }

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }
        goal.setTargetDate(request.getTargetDate());

        SavingsGoal updatedGoal = savingsGoalRepository.save(goal);
        return mapToResponse(updatedGoal);
    }

    @Transactional
    public void deleteSavingsGoal(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));
        savingsGoalRepository.delete(goal);
    }

    public SavingsGoalResponse mapToResponse(SavingsGoal goal) {
        double progressPercentage = 0.0;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = goal.getCurrentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                progressPercentage,
                goal.getTargetDate(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
