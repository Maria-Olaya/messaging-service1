package eafit.gruopChat.infrastructure.security;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration-ms}")
    private long expirationMs;
    public Long extractUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }
    public boolean isTokenValid(String token) {
        try { getClaims(token); return true; } catch (Exception e) { return false; }
    }
    public long getExpirationSeconds() { return expirationMs / 1000; }
    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}