package com.example.finance.service;

import com.example.finance.dto.auth.UserSummaryDto;
import com.example.finance.dto.user.ChangePasswordRequest;
import com.example.finance.dto.user.UpdateProfileRequest;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.DuplicateResourceException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.UserRepository;
import com.example.finance.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getCurrentAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        }
        throw new BadRequestException("No authenticated user found in security context");
    }

    public UserSummaryDto getProfile() {
        User user = getCurrentAuthenticatedUser();
        return new UserSummaryDto(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public UserSummaryDto updateProfile(UpdateProfileRequest request) {
        User user = getCurrentAuthenticatedUser();

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email address already in use: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        User updatedUser = userRepository.save(user);

        return new UserSummaryDto(updatedUser.getId(), updatedUser.getName(), updatedUser.getEmail(), updatedUser.getRole());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
