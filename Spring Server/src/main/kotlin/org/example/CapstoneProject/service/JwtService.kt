@file:Suppress("PackageName")
package org.example.CapstoneProject.service
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.example.CapstoneProject.EnvConfiguration.EnvConfig
import org.springframework.stereotype.Service
import java.text.ParseException
import java.util.Date

// -------------------------------------------------------------------------
// Handles JWT creation, validation and username extraction.
// Tokens are signed using the HS256 algorithm.
// Each JWT contains:
// - subject: username of the authenticated user
// - issuedAt: time when the token was created
// - expirationTime: time when the token becomes invalid
// -------------------------------------------------------------------------
@Service
class JwtService {
    // Secret key used to sign and verify JWT tokens.
    private val jwtSecret: String

    companion object {
        // Token lifetime in milliseconds.
        // 24 hours:
        // 24 * 60 * 60 * 1000 = 86,400,000 milliseconds.
        private const val TOKEN_EXPIRATION_MS = 86_400_000L

        // -----------------------------------------------------------------
        // Loads and validates the JWT secret from the environment.
        // -----------------------------------------------------------------
        private fun loadJwtSecret(): String {
            // Read the real secret from the local environment configuration.
            val secret = EnvConfig.getJwtSecret()

            // Throw an exception if the secret is too short.
            if (secret.length < 32) {
                // Stop application startup because the JWT secret is invalid.
                throw IllegalStateException("JWT_SECRET must exist and contain at least 32 characters")
            }

            // Return the validated production secret.
            return secret
        }
    }

    // ---------------------------------------------------------------------
    // Creates the production JwtService.
    // The real JWT secret is loaded from the local environment.
    // ---------------------------------------------------------------------
    constructor() {
        // Load and store the real JWT secret.
        this.jwtSecret = loadJwtSecret()
    }

    // ---------------------------------------------------------------------
    // Creates JwtService with an explicitly provided secret.
    // This constructor allows automated tests to use a dedicated fake
    // secret without exposing or depending on the real production secret.
    // jwtSecret: secret used to sign and verify JWT tokens
    // ---------------------------------------------------------------------
    constructor(jwtSecret: String?) {
        // Reject missing or insufficiently long secrets.
        if (jwtSecret == null || jwtSecret.length < 32) {
            // Stop construction when the supplied secret is invalid.
            throw IllegalArgumentException("JWT secret must exist and contain at least 32 characters")
        }

        // Store the provided secret for this JwtService instance.
        this.jwtSecret = jwtSecret
    }

    // ---------------------------------------------------------------------
    // Generates a signed JWT for the provided username.
    // ---------------------------------------------------------------------
    fun generateToken(username: String): String {
        try {
            // Store the current time.
            val now = Date()

            // Calculate when the token should expire.
            val expiration = Date(now.time + TOKEN_EXPIRATION_MS)

            // Build the JWT claims.
            val claims = JWTClaimsSet.Builder().subject(username).issueTime(now).expirationTime(expiration).build()

            // Create the JWT using the required HS256 algorithm.
            val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)

            // Create the signer using this JwtService instance secret.
            val signer = MACSigner(jwtSecret)

            // Sign the JWT.
            signedJWT.sign(signer)

            // Convert the JWT into compact serialization format.
            return signedJWT.serialize()

        } catch (e: JOSEException) {
            // Convert JWT signing errors into an unchecked exception.
            throw IllegalStateException("Failed to generate JWT", e)
        }
    }

    // ---------------------------------------------------------------------
    // Validates the JWT algorithm, signature and expiration time.
    // Returns true only when:
    // - the token can be parsed
    // - the token uses HS256
    // - the signature is valid
    // - the token contains an expiration time
    // - the token has not expired
    // ---------------------------------------------------------------------
    fun validateToken(token: String): Boolean {
        try {
            // Parse the compact JWT string.
            val signedJWT = SignedJWT.parse(token)

            // Read the signing algorithm from the JWT header.
            val algorithm = signedJWT.header.algorithm
            // Reject tokens that use a signing algorithm other than HS256.
            if (algorithm != JWSAlgorithm.HS256) { return false }

            // Create a verifier using this JwtService instance secret.
            val verifier = MACVerifier(jwtSecret)
            // Verify the cryptographic signature.
            val validSignature = signedJWT.verify(verifier)

            // Reject the token if its signature is invalid.
            if (!validSignature) { return false }

            // Read the expiration time from the token.
            val expiration = signedJWT.jwtClaimsSet.expirationTime ?: return false
            // Reject tokens without an expiration time.
            // Store the current time.
            val now = Date()

            // Return true only when the token has not expired.
            return expiration.after(now)

        } catch (_: ParseException) {
            // Invalid or malformed tokens are considered invalid.
            return false

        } catch (_: JOSEException) {
            // Tokens that fail cryptographic verification are considered invalid.
            return false
        }
    }

    // ---------------------------------------------------------------------
    // Extracts the username stored in the JWT subject claim.
    // Returns null when the token cannot be parsed.
    // ---------------------------------------------------------------------
    fun extractUsername(token: String): String? {
        try {
            // Parse the JWT.
            val signedJWT = SignedJWT.parse(token)
            // Read the JWT claims.
            val claims = signedJWT.jwtClaimsSet
            // Return the subject claim containing the username.
            return claims.subject
        } catch (_: ParseException) {
            // Return null when the JWT format is invalid.
            return null
        }
    }
}