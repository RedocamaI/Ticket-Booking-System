package com.redocmi.api_gateway.filter;

import com.redocmi.api_gateway.config.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        log.info("Gateway request: {} {}", request.getMethod(), path);

//        skip the JWT validation for /auth routes:
        if(path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

//        block internal endpoints from external clients:
        if(path.startsWith("/api/internal")) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, "Access to internal APIs denied.");
            return;
        }

//        validate JWT
        String authHeader = request.getHeader("Authorization");
        log.info("authHeader: {}", authHeader);
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(
                    response, HttpStatus.UNAUTHORIZED, "Authorization header is missing or invalid."
            );
            return;
        }

        String token = authHeader.substring(7);
        if(!jwtUtil.isTokenValid(token)) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            return;
        }

//        Extract the claims and inject headers:
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractedRole(token);

        log.info("Authenticated request: {}, role={}, path={}", userId, role, path);

//        Block non admin users from admin endpoints:
        if(path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied: Admin role required");
            return;
        }

//        Mutate request to add X-User-Id and X-User-Role header:
        HttpServletRequest mutatedRequest = new HeaderMutatingRequest(request, userId, role);

        response.setHeader("X-RateLimit-Limit",
                path.startsWith("/api/auth") ? "20" : "100");
        filterChain.doFilter(mutatedRequest, response);
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("data", null);

        try{
            String json = objectMapper.writeValueAsString(body);
            response.getWriter().write(json);
            response.getWriter().flush();
        } catch (Exception exception) {
            log.error("Failed to write error response: {}", exception.getMessage());

//            fallback:
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
            response.getWriter().flush();
        }
    }
}
