package eu.accesa.blinkpay.bank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects requests that do not carry a valid API key.
 *
 * Clients supply the key via the {@code X-Api-Key} request header.
 * Browser-based SSE connections (EventSource) cannot set custom headers,
 * so the key is also accepted as the {@code apiKey} query parameter.
 *
 * Actuator endpoints are exempt so health checks work without credentials.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Api-Key";
    private static final String PARAM_NAME  = "apiKey";

    @Value("${bank.api-key}")
    private String expectedKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // /actuator — health checks; /bank/receive — internal FIPS → bank call
        return uri.startsWith("/actuator") || uri.equals("/bank/receive") || uri.equals("/bank/flow-events");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(HEADER_NAME);
        if (key == null || key.isBlank()) {
            key = request.getParameter(PARAM_NAME);
        }

        if (!expectedKey.equals(key)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Missing or invalid API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
