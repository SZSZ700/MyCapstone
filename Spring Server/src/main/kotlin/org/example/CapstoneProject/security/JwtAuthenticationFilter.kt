@file:Suppress("PackageName")
package org.example.CapstoneProject.security
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.CapstoneProject.service.JwtService
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// -------------------------------------------------------------------------
// Validates JWT authentication for protected user-related HTTP requests.
// Public endpoints:
// - POST /api/users/login
// - POST /api/users/signup
// - GET  /api/users/health
// - GET  /api/users/stats/bmiDistribution
// Protected endpoints:
// - Requests that operate on a specific username.
// -------------------------------------------------------------------------
@Component
class JwtAuthenticationFilter(
    // Service used to validate JWT tokens and extract usernames.
    private val jwtService: JwtService) : OncePerRequestFilter() {

    // ---------------------------------------------------------------------
    // Determines whether JWT authentication should be skipped.
    // ---------------------------------------------------------------------
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Read the servlet path without the application context path.
        val path = request.servletPath

        return (
                // Leave login, signup, health, global BMI statistics endpoints public.
                path == "/api/users/login" || path == "/api/users/signup" || path == "/api/users/health" || path == "/api/users/stats/bmiDistribution"
                        // Protect only routes under /api/users/.
                        || !path.startsWith("/api/users/")
                )
    }

    // ---------------------------------------------------------------------
    // Validates the Authorization header and JWT.
    // This function checks four cases:
    // 1. Header is null or does not start with "Bearer ".
    // 2. JWT is invalid or expired.
    // 3. Token does not contain a username.
    // 4. Token does not belong to the requested user.
    // ---------------------------------------------------------------------
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // Read the Authorization header.
        val header = request.getHeader("Authorization")

        // Reject requests without a Bearer token.
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header")
            return
        }

        // Remove the "Bearer " prefix from the header.
        val token = header.substring(7)

        // Reject an invalid or expired JWT.
        if (!jwtService.validateToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
            return
        }

        // Extract the authenticated username from the JWT.
        val tokenUsername = jwtService.extractUsername(token)

        // Reject a token that does not contain a username.
        if (tokenUsername.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token does not contain a valid username")
            return
        }

        // Read the username from the requested URL when one exists.
        val requestedUsername = extractUsernameFromPath(request)

        // When the request contains a username, make sure that the
        // authenticated user is accessing only their own resource.
        if ((requestedUsername != null) && (tokenUsername != requestedUsername)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token does not belong to the requested user")
            return
        }

        // JWT is valid and authorization succeeded.
        filterChain.doFilter(request, response)
    }

    // ---------------------------------------------------------------------
    // Extracts the username from a route under /api/users/.
    // Examples:
    // /api/users/john
    // -> john
    // /api/users/john/water
    // -> john
    // /api/users/john/calories
    // -> john
    // /api/users
    // -> null
    // ---------------------------------------------------------------------
    private fun extractUsernameFromPath(request: HttpServletRequest): String? {
        // Read the servlet path.
        val path = request.servletPath

        // Define the base path before the username.
        val prefix = "/api/users/"

        // Return null when the path does not contain a username section.
        if (!path.startsWith(prefix)) { return null }

        // Remove the /api/users/ prefix.
        val remainingPath = path.substring(prefix.length)

        // Return null when nothing remains.
        if (remainingPath.isBlank()) { return null }

        // Find the next slash after the username.
        val slashIndex = remainingPath.indexOf('/')

        // When there is no second slash, the entire remaining value
        // represents the username.
        if (slashIndex == -1) { return remainingPath }

        // Return only the first path segment, which is the username.
        return remainingPath.substring(0, slashIndex)
    }
}