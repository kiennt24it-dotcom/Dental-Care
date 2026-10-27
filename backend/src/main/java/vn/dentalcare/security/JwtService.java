package vn.dentalcare.security;

import vn.dentalcare.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-expiration-ms}") long expiration) {
        byte[] bytes;
        try { bytes = java.util.Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException ex) { bytes = secret.getBytes(StandardCharsets.UTF_8); }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
        this.key = Keys.hmacShaKeyFor(bytes); this.expiration = expiration;
    }
    public String createAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name())
            .issuedAt(now).expiration(new Date(now.getTime() + expiration)).signWith(key).compact();
    }
    public String subject(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean valid(String token, String email) {
        try { return email.equals(subject(token)); } catch (JwtException | IllegalArgumentException ex) { return false; }
    }
}
