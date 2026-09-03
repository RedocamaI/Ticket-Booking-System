package com.redocmi.auth_service.controller;

import com.redocmi.auth_service.dto.request.LoginRequest;
import com.redocmi.auth_service.dto.request.RegisterRequest;
import com.redocmi.auth_service.dto.response.AuthResponse;
import com.redocmi.auth_service.exception.EmailAlreadyExistsException;
import com.redocmi.auth_service.exception.ResourceNotFoundException;
import com.redocmi.auth_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_shouldSucceed_whenValidRequest() throws Exception {
        AuthResponse mockResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("suraj@test.com")
                .role("USER")
                .token("mock-jwt-token")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Suraj",
                                    "email": "suraj@test.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("suraj@test.com"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void register_shouldFail_whenDuplicateEmail() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Suraj",
                            "email": "suraj@test.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_shouldSucceed_whenValidCredentials() throws Exception {
        AuthResponse mockResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("suraj@test.com")
                .role("USER")
                .token("mock-jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "suraj@test.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    void login_shouldFail_whenWrongPassword() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResourceNotFoundException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "suraj@test.com",
                            "password": "wrongpassword"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_shouldFail_whenUserNotFound() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResourceNotFoundException("No account found"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "nonexistent@test.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_shouldFail_whenInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Suraj",
                            "email": "invalidEmail",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_shouldFail_whenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Suraj",
                            "email": "suraj@redocmi.com",
                            "password": "123"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
