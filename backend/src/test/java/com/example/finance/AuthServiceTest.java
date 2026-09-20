package com.example.finance;

import com.example.finance.dto.auth.AuthResponse;
import com.example.finance.dto.auth.LoginRequest;
import com.example.finance.dto.auth.RegisterRequest;
import com.example.finance.entity.User;

import com.example.finance.exception.DuplicateResourceException;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.UserRepository;
import com.example.finance.security.JwtTokenProvider;

import com.example.finance.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "John Doe", "john@example.com", "encodedPassword", com.example.finance.entity.Role.USER);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token-xyz");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token-xyz", response.getToken());
        assertEquals("john@example.com", response.getUser().getEmail());

        verify(categoryRepository, times(16)).save(any()); // 10 expense + 6 income categories seeded
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token-xyz");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-xyz", response.getToken());
        assertEquals("john@example.com", response.getUser().getEmail());
    }
}
