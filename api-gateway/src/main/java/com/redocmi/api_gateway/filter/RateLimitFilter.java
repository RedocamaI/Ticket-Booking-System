package com.redocmi.api_gateway.filter;

import com.redocmi.api_gateway.config.RateLimitProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
//    window size in milliseconds - 1 minute

    private final RateLimitProperties rateLimitProperties;

//    Map of IP -> [requestCount, windowStartTime]
    private final ConcurrentHashMap<String, long[]> requestCounts
        = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Rate limit config — auth: {}, default: {}, windowMs: {}",
                rateLimitProperties.getAuth(),
                rateLimitProperties.getDefaultLimit(),
                rateLimitProperties.getWindowMs());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClintIp(request);
        String path = request.getRequestURI();
        int authLimit = rateLimitProperties.getAuth();
        int defaultLimit = rateLimitProperties.getDefaultLimit();
        int limit = path.startsWith("/api/auth") ? authLimit : defaultLimit;

        if(!isAllowed(ip, limit)) {
            log.warn("Rate limit exceeded for IP: {}, path: {}", ip, path);
            try {
                writeRateLimitResponse(response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String ip, int limit) {
        long windowMs = rateLimitProperties.getWindowMs();
        long now = Instant.now().toEpochMilli();

        requestCounts.compute(ip, (key, value) -> {
            if(value == null || now - value[1] > windowMs) {
//                new window
                return new long[]{1, now};
            }

            value[0]++;
            return value;
        });

        long[] state = requestCounts.get(ip);

        return state[0] <= limit;
    }

    private String getClintIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if(forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-RateLimit-Retry-After", "60");
        response.getWriter().write(
                "{\"success\": false, \\\"message\\\":\\\"Too many requests. Please try again after 60 seconds.\\\",\\\"data\\\":null}"
        );
        response.getWriter().flush();
    }
}
