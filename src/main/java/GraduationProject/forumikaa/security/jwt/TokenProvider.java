package GraduationProject.forumikaa.security.jwt;

import java.util.Map;

public interface TokenProvider {
    String generateToken(Map<String, Object> payload);
    boolean validateToken(String token);
    Map<String, Object> getPayload(String token);
}
