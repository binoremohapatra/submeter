package com.submeter.auth;

import com.submeter.auth.dto.AuthResponse;
import com.submeter.config.AppProperties;
import com.submeter.entity.Organization;
import com.submeter.entity.OrganizationMember;
import com.submeter.entity.RefreshToken;
import com.submeter.entity.User;
import com.submeter.entity.enums.Role;
import com.submeter.repository.OrganizationMemberRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.repository.RefreshTokenRepository;
import com.submeter.repository.UserRepository;
import com.submeter.security.JwtService;
import com.submeter.security.UserPrincipal;
import com.submeter.security.ratelimit.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository               userRepo;
    private final OrganizationRepository       orgRepo;
    private final OrganizationMemberRepository memberRepo;
    private final RefreshTokenRepository       refreshTokenRepo;
    private final JwtService                   jwtService;
    private final PasswordEncoder              passwordEncoder;
    private final RateLimiterService           rateLimiter;
    private final AppProperties                props;

    // ── Signup ────────────────────────────────────────────────────────────────

    @Transactional
    public TokenPair signup(String email, String rawPassword, String orgName) {
        if (userRepo.existsByEmailAndDeletedAtIsNull(email.toLowerCase())) {
            throw new AuthException("EMAIL_TAKEN",
                    "An account with this email already exists.");
        }

        // Create org with a unique slug
        String slug = generateUniqueSlug(orgName);
        Organization org = orgRepo.save(Organization.builder()
                .name(orgName)
                .slug(slug)
                .build());

        // Create user (first user in org = OWNER)
        User user = userRepo.save(User.builder()
                .organization(org)
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.OWNER)
                .emailVerified(false)
                .build());

        // *** BUG FIX: Always create the org membership row so the owner can see their data ***
        memberRepo.save(OrganizationMember.builder()
                .organization(org)
                .user(user)
                .role(Role.OWNER)
                .status("ACTIVE")
                .build());

        // Log mock verification email (no SMTP in v1)
        String verifyLink = "/api/dev/verify-email?token=" + UUID.randomUUID();
        log.info("[DEV] Verification email for {}: {}", email, verifyLink);

        return issueTokenPair(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public TokenPair login(String email, String rawPassword, String clientIp) {
        // 1. Rate-limit check (before touching the DB for the email)
        if (rateLimiter.isBlocked(clientIp, email)) {
            throw new RateLimitException(rateLimiter.retryAfterSeconds(clientIp, email));
        }

        // 2. Find user
        User user = userRepo.findByEmailAndDeletedAtIsNull(email.toLowerCase())
                .orElseThrow(() -> {
                    // Record failure even for non-existent users to prevent user enumeration
                    rateLimiter.recordFailure(clientIp, email);
                    return new AuthException("INVALID_CREDENTIALS",
                            "Email or password is incorrect.");
                });

        // 3. Check account lock (DB-level, survives app restarts unlike in-memory)
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new RateLimitException(
                    (user.getLockedUntil().toEpochMilli() - System.currentTimeMillis()) / 1000
            );
        }

        // 4. Verify password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            rateLimiter.recordFailure(clientIp, email);

            // Lock the account at the DB level if threshold reached
            // (in-memory rate limiter handles the window; DB lock persists across restarts)
            int maxAttempts = props.getRateLimit().getLoginMaxAttempts();
            int newCount = user.getFailedLoginCount() + 1;
            Instant lockUntil = newCount >= maxAttempts
                    ? Instant.now().plusSeconds(props.getRateLimit().getLoginWindowMinutes() * 60L)
                    : null;
            userRepo.incrementFailedAttempts(user.getId(), lockUntil);

            throw new AuthException("INVALID_CREDENTIALS",
                    "Email or password is incorrect.");
        }

        // 5. Success: clear counters, update last-login
        rateLimiter.clearAttempts(clientIp, email);
        userRepo.resetFailedAttempts(user.getId());
        userRepo.updateLastLogin(user.getId(), Instant.now());

        return issueTokenPair(user);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken existing = refreshTokenRepo
                .findActiveByToken(rawRefreshToken, Instant.now())
                .orElseThrow(() -> new AuthException("INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or expired. Please log in again."));

        // Rotate: revoke old, issue new
        existing.revoke();
        refreshTokenRepo.save(existing);

        return issueTokenPair(existing.getUser());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepo.findActiveByToken(rawRefreshToken, Instant.now())
                .ifPresent(rt -> {
                    rt.revoke();
                    refreshTokenRepo.save(rt);
                });
        // Silently succeed even if the token doesn't exist (idempotent logout)
    }

    // ── /me ───────────────────────────────────────────────────────────────────

    public AuthResponse me(UserPrincipal principal) {
        User user = userRepo.findActiveByIdAndOrgId(principal.getUserId(), principal.getOrgId())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND",
                        "User account not found or has been deactivated."));

        return toAuthResponse(user);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private TokenPair issueTokenPair(User user) {
        // Revoke all existing refresh tokens for this user (single-session policy, v1)
        refreshTokenRepo.revokeAllForUser(user.getId(), Instant.now());

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getOrganization().getId(), user.getRole()
        );

        String rawRefreshToken = UUID.randomUUID().toString();
        refreshTokenRepo.save(RefreshToken.builder()
                .user(user)
                .token(rawRefreshToken)
                .expiresAt(Instant.now().plusMillis(props.getJwt().getRefreshTokenExpiryMs()))
                .build());

        return new TokenPair(accessToken, rawRefreshToken, toAuthResponse(user));
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .orgId(user.getOrganization().getId())
                .email(user.getEmail())
                .role(user.getRole())
                .orgName(user.getOrganization().getName())
                .orgSlug(user.getOrganization().getSlug())
                .build();
    }

    private String generateUniqueSlug(String orgName) {
        String base = orgName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "org";

        String candidate = base;
        int attempts = 0;
        while (orgRepo.existsBySlugAndDeletedAtIsNull(candidate)) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (++attempts > 10) throw new IllegalStateException("Could not generate unique org slug");
        }
        return candidate;
    }

    /**
     * Value object carrying the access token, refresh token, and user body
     * for the controller to set cookies and write the response.
     */
    public record TokenPair(String accessToken, String refreshToken, AuthResponse userInfo) {}
}
