package org.example.CapstoneProject.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.example.CapstoneProject.EnvConfiguration.EnvConfig;
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
@SuppressWarnings("ExtractMethodRecommender")
@Service
public class JwtService {

    // Secret key loaded from the .env file.
    private static final String JWT_SECRET = loadJwtSecret();

    // Token lifetime in milliseconds.
    //
    // 24 hours:
    // 24 * 60 * 60 * 1000 = 86,400,000 milliseconds.
    private static final long TOKEN_EXPIRATION_MS = 86_400_000L;


    // ---------------------------------------------------------------------
    // Loads and validates the JWT secret from the environment.
    // ---------------------------------------------------------------------
    private static String loadJwtSecret() {
        // The secret must be at least 32 characters long.
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
                    // Set the subject (username) as the JWT ID.
                    .subject(username)
                    // set the token creation time
                    .issueTime(now)
                    // set the expiration time
                    .expirationTime(expiration)
                    // build the JWT claims set
                    .build();

            // Create the signed JWT object using HS256.
            // (Creates a new minimal JSON Web Signature (JWS) header.)
            SignedJWT signedJWT = new SignedJWT(
                    // Set the JWT header to HS256.
                    // This is the algorithm used to sign the token.
                    new JWSHeader(JWSAlgorithm.HS256),
                    // Set the JWT claims.
                    claims
            );

            // Create the signer using the secret key.
            MACSigner signer = new MACSigner(JWT_SECRET);

            // Signs this JWS object with the specified signer.
            // The JWS object must be in a unsigned state.
            signedJWT.sign(signer);

            // Convert the JWT into the compact string format.
            return signedJWT.serialize();

        } catch (JOSEException e) {
            // Convert JWT signing errors into an unchecked exception.
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }

    // ---------------------------------------------------------------------
    // Validates the JWT signature and expiration time.
    //
    // Returns true only when:
    // - the token can be parsed
    // - the signature is valid
    // - the token has not expired
    // ---------------------------------------------------------------------
    public boolean validateToken(String token) {
        try {
            // Parse the compact JWT string.
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Reject tokens that use a different signing algorithm.
            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                return false;
            }

            // Create a verifier using the same secret key.
            MACVerifier verifier = new MACVerifier(JWT_SECRET);

            // Verify the cryptographic signature.
            boolean validSignature = signedJWT.verify(verifier);

            // Reject the token if its signature is invalid.
            if (!validSignature) { return false; }

            // Read the expiration time from the token.
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();

            // Reject tokens without an expiration time.
            if (expiration == null) { return false;}

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