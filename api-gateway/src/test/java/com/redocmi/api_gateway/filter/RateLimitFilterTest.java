package com.redocmi.api_gateway.filter;

import com.redocmi.api_gateway.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuth(3);
        properties.setDefaultLimit(5);
        properties.setWindowMs(60000);

        rateLimitFilter = new RateLimitFilter(properties);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void request_shouldPass_withinDefaultLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trains/search");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(429);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void request_shouldReturn429_afterExceedingDefaultLimit() throws Exception {
        String ip = "10.0.0.1";

//        send 5 requests all should pass
        for(int i=0;i<5;i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trains/search");
            request.setRemoteAddr(ip);
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

//        6th request is rate limited
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trains/search");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    void authEndpoint_shouldReturn429_afterExceedingAuthLimit() throws Exception {
        String ip = "10.0.0.1";

        // send 3 requests all should pass
        for(int i=0;i<3;i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr(ip);
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        // 4th request should be rate limited
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    void differentIps_shouldHaveSeparateLimits() throws Exception {
        // IP 1 exhausts it's limit
        for(int i=0;i<5;i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trains");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // IP 2 should still pass
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trains");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(429);
        verify(filterChain, atLeastOnce()).doFilter(any(), any());
    }
}
