package com.redocmi.auth_service.service;

import com.redocmi.auth_service.dto.request.LoginRequest;
import com.redocmi.auth_service.dto.request.RegisterRequest;
import com.redocmi.auth_service.dto.response.AuthResponse;
import com.redocmi.auth_service.entity.User;
import com.redocmi.auth_service.exception.EmailAlreadyExistsException;
import com.redocmi.auth_service.exception.ResourceNotFoundException;
import com.redocmi.auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldSucceed_whenValidRequest() {
//        Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Suraj");
        registerRequest.setEmail("suraj@redocmi.com");
        registerRequest.setPassword("password123");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("Suraj")
                .email("suraj@redocmi.com")
                .passwordHash("hashed")
                .role(User.Role.USER)
                .build();

        when(userRepository.existsByEmail("suraj@redocmi.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-token");

//        Act
        AuthResponse response = authService.register(registerRequest);

//        Assert:
        assertThat(response.getEmail()).isEqualTo("suraj@redocmi.com");
        assertThat(response.getToken()).isEqualTo("mock-token");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
//        Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Suraj");
        registerRequest.setEmail("suraj@redocmi.com");
        registerRequest.setPassword("password123");

        when(userRepository.existsByEmail("suraj@redocmi.com")).thenReturn(true);

//        Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("suraj@redocmi.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldSucceed_whenValidCredentials() {
//        Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("suraj@redocmi.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("suraj@redocmi.com")
                .passwordHash("hashed")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("suraj@redocmi.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed"))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mock-token");

//        Act
        AuthResponse response = authService.login(request);

//        Assert
        assertThat(response.getEmail()).isEqualTo("suraj@redocmi.com");
        assertThat(response.getToken()).isEqualTo("mock-token");
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
//        Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("suraj@redocmi.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("suraj@redocmi.com"))
                .thenReturn(Optional.empty());

//        Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_shouldThrow_whenWrongPassword() {
//        Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("suraj@redocmi.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("suraj@redocmi.com")
                .passwordHash("hashed")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("suraj@redocmi.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed"))
                .thenReturn(false);

//        Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid password");
    }
}
