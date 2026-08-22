package org.example.CapstoneProject.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.example.CapstoneProject.EnvConfiguration.EnvConfig;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

// -------------------------------------------------------------------------
// Handles JWT creation, validation and username extraction.
//
// The token is signed using the HS256 algorithm.
//
// The JWT contains:
// - subject: username of the authenticated user
// - issuedAt: time when the token was created
// - expirationTime: time when the token becomes invalid
// -------------------------------------------------------------------------
@Service
public class JwtService {

    // Secret key used to sign and verify JWT tokens.
    private final String jwtSecret;

    // Token lifetime in milliseconds.
    //
    // 24 hours:
    // 24 * 60 * 60 * 1000 = 86,400,000 milliseconds.
    private static final long TOKEN_EXPIRATION_MS = 86_400_000L;


    // ---------------------------------------------------------------------
    // Creates the production JwtService.
    //
    // The real JWT secret is loaded from the local environment.
    // ---------------------------------------------------------------------
    public JwtService() {
        this.jwtSecret = loadJwtSecret();
    }

    // ---------------------------------------------------------------------
    // Creates JwtService with an explicitly provided secret.
    //
    // This constructor allows automated tests to use a dedicated fake
    // secret without exposing or depending on the real production secret.
    //
    // jwtSecret: secret used to sign and verify JWT tokens
    // ---------------------------------------------------------------------
    public JwtService(String jwtSecret) {

        // Reject missing or insufficiently long secrets.
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must exist and contain at least 32 characters"
            );
        }

        // Store the provided secret for this JwtService instance.
        this.jwtSecret = jwtSecret;
    }


    // ---------------------------------------------------------------------
    // Loads and validates the JWT secret from the environment.
    // ---------------------------------------------------------------------
    private static String loadJwtSecret() {

        // Read the real secret from the local environment configuration.
        var secret = EnvConfig.getJwtSecret();

        // Throw an exception if the secret is missing or too short.
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must exist and contain at least 32 characters"
            );
        }

        return secret;
    }


    // ---------------------------------------------------------------------
    // Generates a signed JWT for the provided username.
    // ---------------------------------------------------------------------
    public String generateToken(String username) {

        try {
            // Store the current time.
            Date now = new Date();

            // Calculate when the token should expire.
            Date expiration = new Date(now.getTime() + TOKEN_EXPIRATION_MS);

            // Build the JWT claims.
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    // Set the subject to the authenticated username.
                    .subject(username)
                    // Set the token creation time.
                    .issueTime(now)
                    // Set the expiration time.
                    .expirationTime(expiration)
                    // Build the JWT claims set.
                    .build();

            // Create the signed JWT object using HS256.
            SignedJWT signedJWT = new SignedJWT(
                    // Set the required signing algorithm.
                    new JWSHeader(JWSAlgorithm.HS256),
                    // Attach the JWT claims.
                    claims
            );

            // Create the signer using this JwtService instance secret.
            MACSigner signer = new MACSigner(jwtSecret);

            // Sign the JWT using HS256 and the configured secret.
            signedJWT.sign(signer);

            // Convert the JWT into compact serialization format.
            return signedJWT.serialize();

        } catch (JOSEException e) {
            // Convert JWT signing errors into an unchecked exception.
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }


    // ---------------------------------------------------------------------
    // Validates the JWT algorithm, signature and expiration time.
    //
    // Returns true only when:
    // - the token can be parsed
    // - the token uses HS256
    // - the signature is valid
    // - the token contains an expiration time
    // - the token has not expired
    // ---------------------------------------------------------------------
    public boolean validateToken(String token) {

        try {
            // Parse the compact JWT string.
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Reject tokens that use a signing algorithm other than HS256.
            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                return false;
            }

            // Create a verifier using this JwtService instance secret.
            MACVerifier verifier = new MACVerifier(jwtSecret);

            // Verify the cryptographic signature.
            boolean validSignature = signedJWT.verify(verifier);

            // Reject the token if its signature is invalid.
            if (!validSignature) { return false; }

            // Read the expiration time from the token.
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();

            // Reject tokens without an expiration time.
            if (expiration == null) { return false; }

            // Return true only when the token has not expired.
            return expiration.after(new Date());

        }
        // Invalid or malformed tokens are considered invalid.
        catch (ParseException | JOSEException e) { return false; }
    }


    // ---------------------------------------------------------------------
    // Extracts the username stored in the JWT subject claim.
    //
    // Returns null when the token cannot be parsed.
    // ---------------------------------------------------------------------
    public String extractUsername(String token) {

        try {
            // Parse the JWT.
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Return the subject claim, which contains the username.
            return signedJWT.getJWTClaimsSet().getSubject();

        } catch (ParseException e) {
            // Return null when the JWT format is invalid.
            return null;
        }
    }
}