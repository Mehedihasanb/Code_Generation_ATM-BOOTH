package com.example.backend.services;

import com.example.backend.dtos.RegisterRequest;
import com.example.backend.entities.CustomerProfile;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.repositories.CustomerProfileRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User customer;
    private static final String RAW_PASSWORD = "Password1!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHashForTesting";

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1);
        customer.setEmail("auth@example.com");
        customer.setPasswordHash(ENCODED_PASSWORD);
        customer.setFirstName("Alice");
        customer.setLastName("Smith");
        customer.setRole(UserRole.CUSTOMER);
    }


    @Test
    void register_happyPath_savesUserAndProfile() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com", RAW_PASSWORD, "Alice", "Smith", "123456789", "0612345678");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(customer);

        User result = authService.register(request);

        assertNotNull(result);
        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(any(User.class));
        verify(customerProfileRepository).save(any(CustomerProfile.class));
    }

    @Test
    void register_withDuplicateEmail_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest(
                "auth@example.com", RAW_PASSWORD, "Alice", "Smith", "123456789", "0612345678");

        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(customer));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verify(customerProfileRepository, never()).save(any());
    }


    @Test
    void login_happyPath_returnsUser() {
        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        User result = authService.login("auth@example.com", RAW_PASSWORD);

        assertEquals(customer, result);
        verify(userRepository).findByEmail("auth@example.com");
        verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("WrongPassword1!", ENCODED_PASSWORD)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("auth@example.com", "WrongPassword1!"));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("nobody@example.com", RAW_PASSWORD));

        assertEquals(401, exception.getStatusCode().value());
        verify(passwordEncoder, never()).matches(any(), any());
    }


    @Test
    void generateToken_delegatesToJwtUtil() {
        // control what token value is returned
        when(jwtUtil.generateToken(customer)).thenReturn("mocked.jwt.token");

        String token = authService.generateToken(customer);

        assertEquals("mocked.jwt.token", token);
        verify(jwtUtil).generateToken(customer);
    }


    @Test
    void getTokenExpirationMs_delegatesToJwtUtil() {
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        long result = authService.getTokenExpirationMs();

        assertEquals(3600000L, result);
        verify(jwtUtil).getExpirationMs();
    }
}
