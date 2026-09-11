package com.redocmi.api_gateway.filter;

import com.redocmi.api_gateway.config.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtAuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void request_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/trains/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Authorization header is missing or invalid."));
    }

    @Test
    void request_shouldReturn401_whenInvalidToken() throws Exception {
        when(jwtUtil.isTokenValid(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/trains/search")
                .header("Authorization", "Bearer invalidation"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired token"));
    }

    @Test
    void request_shouldReturn403_whenInternalEndpoint() throws Exception {
        when(jwtUtil.isTokenValid(anyString())).thenReturn(true);
        when(jwtUtil.extractUserId(anyString())).thenReturn("some-user-id");
        when(jwtUtil.extractedRole(anyString())).thenReturn("USER");

        mockMvc.perform(get("/api/internal/seats/some-id/lock")
                .header("Authorization", "Bearer validToken"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void request_shouldReturn403_whenUserAccessesAdminEndpoint() throws Exception {
        when(jwtUtil.isTokenValid(anyString())).thenReturn(true);
        when(jwtUtil.extractUserId(anyString())).thenReturn("some-user-id");
        when(jwtUtil.extractedRole(anyString())).thenReturn("USER");

        mockMvc.perform(get("/api/admin/trains")
                .header("Authorization", "Bearer validToken"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void authEndpoint_shouldPassThrough_withoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
//        mock the filterChain:
        FilterChain filterChain = mock(FilterChain.class);

//        execute the filter directly:
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(401);
    }
}
