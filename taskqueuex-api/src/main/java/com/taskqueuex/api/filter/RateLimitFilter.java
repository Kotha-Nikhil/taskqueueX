package com.taskqueuex.api.filter;

import com.taskqueuex.api.exception.RateLimitExceededException;
import com.taskqueuex.api.ratelimit.RedisRateLimiter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RedisRateLimiter rateLimiter;
    private final boolean enabled;

    public RateLimitFilter(
            RedisRateLimiter rateLimiter,
            @Value("${rate-limit.enabled:true}") boolean enabled) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Only apply rate limiting to POST /jobs endpoint
        if (!"POST".equals(httpRequest.getMethod()) || !httpRequest.getRequestURI().equals("/jobs")) {
            chain.doFilter(request, response);
            return;
        }

        // Get client identifier (IP address)
        String clientId = getClientIdentifier(httpRequest);
        
        if (!rateLimiter.isAllowed(clientId)) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(clientId);
            logger.warn("Rate limit exceeded for client: {}", clientId);
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.", retryAfter);
        }

        chain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
