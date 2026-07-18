package com.submeter.security;

import com.submeter.config.AppProperties;
import com.submeter.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT access-token service.
 *
 * <p>Token contents (claims):
 * <ul>
 *   <li>{@code sub}   — userId (UUID string)</li>
 *   <li>{@code orgId} — orgId (UUID string)</li>
 *   <li>{@code role}  — Role enum name</li>
 *   <li>{@code iat}   — issued-at (epoch seconds)</li>
 *   <li>{@code exp}   — expiration (epoch seconds)</li>
 * </ul>
 *
 * <p>The role claim in the JWT is used only for informational purposes and
 * for MEMBER-level access (lower-security). ADMIN/OWNER-required actions
 * ALWAYS re-verify the role from the DB in {@code RbacInterceptor} — the JWT
 * role is never trusted alone for privileged operations.
 *
 * <p>Algorithm: HS256 (HMAC-SHA256). Key must be ≥ 32 bytes.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final AppProperties props;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters. " +
                "Generate one with: openssl rand -hex 32"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtService initialized with HS256, access-token TTL={}ms",
                props.getJwt().getAccessTokenExpiryMs());
    }

    /**
     * Generate a signed JWT access token.
     *
     * @param userId the authenticated user's UUID
     * @param orgId  the user's organization UUID
     * @param role   the user's current role (for informational use; privileged actions re-verify from DB)
     */
    public String generateAccessToken(UUID userId, UUID orgId, Role role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("orgId", orgId.toString())
                .claim("role", role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.getJwt().getAccessTokenExpiryMs()))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse and validate a JWT. Throws {@link JwtException} if the token is
     * expired, malformed, or has an invalid signature.
     *
     * @param token the raw JWT string
     * @return the verified claims payload
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long accessTokenExpirySeconds() {
        return props.getJwt().getAccessTokenExpiryMs() / 1000;
    }
}
