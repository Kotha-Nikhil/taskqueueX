package com.taskqueuex.worker.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(0)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
