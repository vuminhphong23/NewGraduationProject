package GraduationProject.forumikaa.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import java.util.Date;
import java.util.Map;

public class JwtTokenProvider implements TokenProvider {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenProvider(String secret, String issuer, long expirationSeconds
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
        this.verifier = JWT.require(algorithm)
                .withIssuer(this.issuer)
                .build();
    }

    @Override
    public String generateToken(Map<String, Object> payload) {
        long nowMillis = System.currentTimeMillis();
        Date issuedAt = new Date(nowMillis);
        Date expiresAt = new Date(nowMillis + (expirationSeconds * 1000));

        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt);

        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;
                // Handle common types explicitly; fallback to toString
                if (value instanceof String v) builder.withClaim(key, v);
                else if (value instanceof Integer v) builder.withClaim(key, v);
                else if (value instanceof Long v) builder.withClaim(key, v);
                else if (value instanceof Boolean v) builder.withClaim(key, v);
                else if (value instanceof Double v) builder.withClaim(key, v);
                else if (value instanceof Date v) builder.withClaim(key, v);
                else builder.withClaim(key, String.valueOf(value));
            }
        }
        return builder.sign(algorithm);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getPayload(String token) {
        try {
            var jwt = verifier.verify(token);
            var result = new java.util.HashMap<String, Object>();

            // Standard registered claims
            if (jwt.getIssuer() != null) result.put("iss", jwt.getIssuer());
            if (jwt.getIssuedAt() != null) result.put("iat", jwt.getIssuedAt());
            if (jwt.getExpiresAt() != null) result.put("exp", jwt.getExpiresAt());
            if (jwt.getSubject() != null) result.put("sub", jwt.getSubject());
            if (jwt.getId() != null) result.put("jti", jwt.getId());
            if (jwt.getAudience() != null && !jwt.getAudience().isEmpty()) result.put("aud", jwt.getAudience());

            // Custom and other claims
            var claims = jwt.getClaims();
            for (var entry : claims.entrySet()) {
                String key = entry.getKey();
                var claim = entry.getValue();
                if (claim == null || claim.isNull()) continue;

                Object value = null;
                // Try to decode into common types used by generator
                Boolean asBool = claim.asBoolean();
                if (asBool != null) {
                    value = asBool;
                } else {
                    Integer asInt = claim.asInt();
                    if (asInt != null) value = asInt;
                    else {
                        Long asLong = claim.asLong();
                        if (asLong != null) value = asLong;
                        else {
                            Double asDouble = claim.asDouble();
                            if (asDouble != null) value = asDouble;
                            else {
                                java.util.Date asDate = claim.asDate();
                                if (asDate != null) value = asDate;
                                else {
                                    String asString = claim.asString();
                                    if (asString != null) value = asString;
                                }
                            }
                        }
                    }
                }

                if (value != null) {
                    result.put(key, value);
                }
            }
            return result;
        } catch (JWTVerificationException e) {
            // Invalid token
            return Map.of();
        }
    }
}