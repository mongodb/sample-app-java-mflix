package com.mongodb.samplemflix.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the Sample MFlix API.
 *
 * <p>This configuration customizes Spring MVC behavior, including:
 * <ul>
 *   <li>Trailing slash handling via custom filter</li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Filter to handle trailing slashes in URLs.
     *
     * <p>This filter redirects URLs with trailing slashes to their non-trailing slash equivalents
     * using HTTP 308 (Permanent Redirect) to maintain the HTTP method.
     *
     * <p>For example:
     * <ul>
     *   <li>POST /api/movies/ → redirects to POST /api/movies</li>
     *   <li>GET /api/movies/ → redirects to GET /api/movies</li>
     * </ul>
     */
    @org.springframework.stereotype.Component
    public static class TrailingSlashRedirectFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain
        ) throws ServletException, IOException {
            String requestUri = request.getRequestURI();

            // If URL ends with trailing slash (but not root "/"), redirect to URL without it
            if (requestUri.length() > 1 && requestUri.endsWith("/")) {
                String redirectPath = requestUri.substring(0, requestUri.length() - 1);
                if (!isSafeRelativeRedirectPath(redirectPath)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String location = redirectPath;
                String queryString = request.getQueryString();
                if (queryString != null && !queryString.isBlank()) {
                    if (!isSafeQueryString(queryString)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    location = redirectPath + "?" + queryString;
                }

                // Use 308 Permanent Redirect to preserve the HTTP method (POST, PATCH, DELETE, etc.)
                response.setStatus(HttpStatus.PERMANENT_REDIRECT.value());
                response.setHeader("Location", location);
                return;
            }

            filterChain.doFilter(request, response);
        }

        private static boolean isSafeRelativeRedirectPath(String path) {
            return path.startsWith("/")
                    && !path.startsWith("//")
                    && !path.contains("://")
                    && !path.contains("\\")
                    && !path.contains("\0")
                    && !path.contains("\r")
                    && !path.contains("\n");
        }

        private static boolean isSafeQueryString(String queryString) {
            return !queryString.contains("\r")
                    && !queryString.contains("\n")
                    && !queryString.contains("\0");
        }
    }
}
