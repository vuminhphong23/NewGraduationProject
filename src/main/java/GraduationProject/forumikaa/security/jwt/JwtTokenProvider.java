package GraduationProject.forumikaa.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class JwtTokenProvider implements TokenProvider {

    private final String jwtSecret;
    private final String jwtIssuer;
    private final long jwtExpiration;

    public JwtTokenProvider(String jwtSecret, String jwtIssuer, long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.jwtIssuer = jwtIssuer;
        this.jwtExpiration = jwtExpiration;
    }

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(jwtSecret);
    }

    @Override
    public String generateToken(Map<String, Object> payload) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpiration, ChronoUnit.SECONDS);

        return JWT.create()
                .withIssuer(jwtIssuer)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiryDate))
                .withPayload(payload)
                .sign(getAlgorithm());
    }

    @Override
    public boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm())
                    .withIssuer(jwtIssuer)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            
            // Kiểm tra token có hết hạn chưa
            return !jwt.getExpiresAt().before(new Date());
            
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getPayload(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            Map<String, Object> payload = new HashMap<>();
            
            // Lấy các claim cơ bản
            if (jwt.getIssuer() != null) {
                payload.put("iss", jwt.getIssuer());
            }
            if (jwt.getSubject() != null) {
                payload.put("sub", jwt.getSubject());
            }
            if (jwt.getIssuedAt() != null) {
                payload.put("iat", jwt.getIssuedAt());
            }
            if (jwt.getExpiresAt() != null) {
                payload.put("exp", jwt.getExpiresAt());
            }
            
            // Lấy các custom claims từ payload
            jwt.getClaims().forEach((key, claim) -> {
                if (claim.asString() != null) {
                    payload.put(key, claim.asString());
                } else if (claim.asLong() != null) {
                    payload.put(key, claim.asLong());
                } else if (claim.asBoolean() != null) {
                    payload.put(key, claim.asBoolean());
                } else if (claim.asDate() != null) {
                    payload.put(key, claim.asDate());
                } else if (claim.asDouble() != null) {
                    payload.put(key, claim.asDouble());
                }
            });
            
            return payload;
        } catch (Exception e) {
            // Trả về map rỗng nếu có lỗi khi decode token
            return new HashMap<>();
        }
    }


}
