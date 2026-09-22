package com.collab.spreadsheet.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth",
            "/api/auth",
            "/actuator",
            "/ws",
            "/fallback"
    );

    public JwtAuthenticationFilter(@Value("${jwt.secret:your-256-bit-secret-change-this-in-production-please-use-env-variable}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if path is public
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            // Allow guest access for workbooks, sheets, comments, collab state (for link sharing)
            if (isGuestAllowedPath(path)) {
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", "anonymous")
                        .header("X-User-Username", "Guest")
                        .header("X-User-Role", "EDITOR")
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            // Mutate request with authenticated headers downstream
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Username", username != null ? username : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Role", role != null ? role : "USER")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            if (isGuestAllowedPath(path)) {
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", "anonymous")
                        .header("X-User-Username", "Guest")
                        .header("X-User-Role", "EDITOR")
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            log.error("JWT token validation failed for path {}: {}", path, e.getMessage());
            return onError(exchange, "Invalid or expired JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isGuestAllowedPath(String path) {
        return path.equals("/api/v1/workbooks") ||
               path.startsWith("/api/v1/workbooks/") || 
               path.equals("/api/v1/sheets") ||
               path.startsWith("/api/v1/sheets/") ||
               path.startsWith("/api/v1/comments/") ||
               path.startsWith("/api/v1/collab/");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format("{\"status\":%d,\"code\":\"UNAUTHORIZED\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(), message, java.time.Instant.now());

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // High priority in filter chain
    }
}
