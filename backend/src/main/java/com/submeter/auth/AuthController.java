package com.submeter.auth;

import com.submeter.auth.dto.AuthResponse;
import com.submeter.auth.dto.LoginRequest;
import com.submeter.auth.dto.SignupRequest;
import com.submeter.config.AppProperties;
import com.submeter.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

/**
 * Auth endpoints. All cookie-setting happens here via {@code Set-Cookie} headers.
 *
 * <p>Cookie policy (httpOnly, SameSite=Lax, Secure in prod):
 * <ul>
 *   <li>{@code access_token}  — JWT; 15 min TTL; sent on every API call.</li>
 *   <li>{@code refresh_token} — opaque UUID; 7-day TTL; only sent to /auth/refresh.</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService   authService;
    private final AppProperties props;

    // ── POST /auth/signup ─────────────────────────────────────────────────────

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
        AuthService.TokenPair pair = authService.signup(
                req.getEmail(), req.getPassword(), req.getOrgName());
        return buildAuthResponse(pair, HttpStatus.CREATED);
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq
    ) {
        String clientIp = resolveClientIp(httpReq);
        AuthService.TokenPair pair = authService.login(
                req.getEmail(), req.getPassword(), clientIp);
        return buildAuthResponse(pair, HttpStatus.OK);
    }

    // ── POST /auth/refresh ────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpReq) {
        String refreshToken = extractCookie(httpReq, "refresh_token")
                .orElseThrow(() -> new AuthException("MISSING_REFRESH_TOKEN",
                        "No refresh token cookie present."));
        AuthService.TokenPair pair = authService.refresh(refreshToken);
        return buildAuthResponse(pair, HttpStatus.OK);
    }

    // ── POST /auth/logout ─────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpReq) {
        extractCookie(httpReq, "refresh_token")
                .ifPresent(authService::logout);

        // Expire both cookies by setting Max-Age=0
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, expiredCookie("access_token").toString());
        headers.add(HttpHeaders.SET_COOKIE, expiredCookie("refresh_token").toString());
        return ResponseEntity.noContent().headers(headers).build();
    }

    // ── GET /auth/me ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AuthResponse user = authService.me(principal);
        return ResponseEntity.ok(user);
    }

    // ── Cookie helpers ────────────────────────────────────────────────────────

    private ResponseEntity<AuthResponse> buildAuthResponse(
            AuthService.TokenPair pair, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                buildCookie("access_token", pair.accessToken(),
                        props.getJwt().getAccessTokenExpiryMs() / 1000).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                buildCookie("refresh_token", pair.refreshToken(),
                        props.getJwt().getRefreshTokenExpiryMs() / 1000).toString());

        return ResponseEntity.status(status)
                .headers(headers)
                .body(pair.userInfo());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.getJwt().isSecureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(props.getJwt().isSecureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private Optional<String> extractCookie(HttpServletRequest req, String name) {
        jakarta.servlet.http.Cookie[] cookies = req.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst();
    }

    /**
     * Resolves the real client IP, honouring X-Forwarded-For when set
     * (added by reverse proxies / load balancers). Used for rate-limiting.
     * Only the first IP in the XFF chain is trusted (leftmost = client IP).
     */
    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
