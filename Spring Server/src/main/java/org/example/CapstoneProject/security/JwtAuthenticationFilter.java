package org.example.CapstoneProject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.CapstoneProject.service.JwtService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// -------------------------------------------------------------------------
// Validates JWT authentication for protected user-related HTTP requests.
//
// Public endpoints:
// - POST /api/users/login
// - POST /api/users/signup
// - GET  /api/users/health
// - GET  /api/users/stats/bmiDistribution
//
// Protected endpoints:
// - Requests that operate on a specific username.
// -------------------------------------------------------------------------
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Service used to validate JWT tokens and extract usernames.
    private final JwtService jwtService;

    // ---------------------------------------------------------------------
    // Builds the filter using constructor injection.
    // ---------------------------------------------------------------------
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // ---------------------------------------------------------------------
    // Determines whether JWT authentication should be skipped.
    // ---------------------------------------------------------------------
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Read the servlet path without the application context path.
        var path = request.getServletPath();

        return
                // Leave login public.
                "/api/users/login".equals(path)
                // Leave signup public.
                || "/api/users/signup".equals(path)
                // Leave the health endpoint public.
                || "/api/users/health".equals(path)
                // Leave the global BMI
                // statistics endpoint public.
                || "/api/users/stats/bmiDistribution".equals(path)
                // Protect only routes under /api/users/.
                || !path.startsWith("/api/users/");

    }

    // ---------------------------------------------------------------------
    // Validates the Authorization header and JWT.
    // this function check 4 cases:
    // first case: header is null OR not starting with  "Bearer"
    // second case: invalid or expired JWT
    // third case: token does not contain a username
    // fourth case: token does not belong to the requested user
    // params: request, response, filterChain
    // ---------------------------------------------------------------------
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Read the Authorization header.
        var header = request.getHeader("Authorization");

        // Reject requests without a Bearer token.
        if (header == null || !header.startsWith("Bearer ")) {
            // Send a 401 Unauthorized response.
            response.sendError(
                    // 401 Unauthorized
                    HttpServletResponse.SC_UNAUTHORIZED,
                    // Error message
                    "Missing or invalid Authorization header");
            return;
        }

        // Remove the "Bearer " prefix from the header.
        var token = header.substring(7);

        // Reject an invalid or expired JWT.
        if (!jwtService.validateToken(token)) {
            response.sendError(
                    // 401 Unauthorized
                    HttpServletResponse.SC_UNAUTHORIZED,
                    // Error message
                    "Invalid or expired token"
            );
            return;
        }

        // Extract the authenticated username from the JWT.
        String tokenUsername = jwtService.extractUsername(token);

        // Reject a token that does not contain a username.
        if (tokenUsername == null || tokenUsername.isBlank()) {
            response.sendError(
                    // 401 Unauthorized
                    HttpServletResponse.SC_UNAUTHORIZED,
                    // Error message
                    "Token does not contain a valid username"
            );
            return;
        }

        // Read the username from the requested URL when one exists.
        var requestedUsername = this.extractUsernameFromPath(request);

        // When the request contains a username, make sure that the
        // authenticated user is accessing only their own resource.
        if (requestedUsername != null && !tokenUsername.equals(requestedUsername)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Token does not belong to the requested user"
            );
            return;
        }

        // JWT is valid and authorization succeeded.
        filterChain.doFilter(request, response);
    }

    // ---------------------------------------------------------------------
    // Extracts the username from a route under /api/users/.
    //
    // Examples:
    //
    // /api/users/john
    // -> john
    //
    // /api/users/john/water
    // -> john
    //
    // /api/users/john/calories
    // -> john
    //
    // /api/users
    // -> null
    // ---------------------------------------------------------------------
    private String extractUsernameFromPath(HttpServletRequest request) {
        // Read the servlet path.
        var path = request.getServletPath();

        // Define the base path before the username.
        var prefix = "/api/users/";

        // Return null when the path does not contain a username section.
        if (!path.startsWith(prefix)) { return null; }

        // Remove the /api/users/ prefix.
        var remainingPath = path.substring(prefix.length());

        // Return null when nothing remains.
        if (remainingPath.isBlank()) { return null; }

        // Find the next slash after the username.
        var slashIndex = remainingPath.indexOf('/');

        // When there is no second slash, the entire remaining value
        // represents the username.
        if (slashIndex == -1) { return remainingPath; }

        // Return only the first path segment, which is the username.
        return remainingPath.substring(0, slashIndex);
    }
}