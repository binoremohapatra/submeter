package com.submeter.security.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.submeter.auth.dto.ApiError;
import com.submeter.entity.User;
import com.submeter.entity.enums.Role;
import com.submeter.repository.UserRepository;
import com.submeter.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Server-side RBAC enforcement.
 *
 * <p>Runs after the JWT filter has populated the SecurityContext. For each
 * request mapped to a method or class annotated with {@link RequiresRole}:
 * <ol>
 *   <li>Extracts the authenticated {@link UserPrincipal} from the SecurityContext.</li>
 *   <li>Unauthenticated requests → 401 (let the security filter chain handle it normally,
 *       but we short-circuit here for consistency).</li>
 *   <li>For MEMBER-level requirements: trusts the principal's role (no DB call).</li>
 *   <li><strong>For ADMIN/OWNER requirements: re-queries the DB</strong> to get the current
 *       role. This catches users whose role was downgraded after their JWT was issued.</li>
 *   <li>Insufficient role → 403 with {@link ApiError} body.</li>
 * </ol>
 *
 * <p>This is the enforcement point for the security requirement:
 * "Never trust a role claim without re-verifying it against the DB on privileged actions."
 */
@Component
@RequiredArgsConstructor
public class RbacInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RbacInterceptor.class);

    private final UserRepository userRepo;
    private final ObjectMapper   objectMapper;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull Object              handler
    ) throws IOException {

        if (!(handler instanceof HandlerMethod method)) {
            return true; // Static resources, etc.
        }

        // Resolve annotation: method-level takes precedence over class-level.
        RequiresRole annotation = method.getMethodAnnotation(RequiresRole.class);
        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(RequiresRole.class);
        }
        if (annotation == null) {
            return true; // No RBAC requirement on this endpoint
        }

        Role required = annotation.minimum();

        // ── Step 1: Require authentication ───────────────────────────────────
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            rejectWith(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "Authentication required.");
            return false;
        }

        // ── Step 2: Determine actual role ─────────────────────────────────────
        Role actualRole;

        if (required == Role.MEMBER) {
            // MEMBER-level: trust JWT claim (no DB call on every read)
            actualRole = principal.getRole();
        } else {
            // ADMIN / OWNER: re-verify from DB — JWT claim is not trusted here
            Optional<User> userOpt = userRepo.findActiveByIdAndOrgId(
                    principal.getUserId(), principal.getOrgId());

            if (userOpt.isEmpty()) {
                // User deleted or moved org since JWT was issued
                rejectWith(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "User account no longer active.");
                return false;
            }

            actualRole = userOpt.get().getRole();
            log.debug("RBAC DB-verified role: userId={} dbRole={} requiredMin={}",
                    principal.getUserId(), actualRole, required);
        }

        // ── Step 3: Enforce hierarchy ─────────────────────────────────────────
        if (!hasRequiredRole(actualRole, required)) {
            log.warn("RBAC denied: userId={} has role={} but endpoint requires minimum={}",
                    principal.getUserId(), actualRole, required);
            rejectWith(response, HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE",
                    "You need at least " + required + " role to perform this action.");
            return false;
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code actual} meets or exceeds {@code required} in the role hierarchy.
     * Hierarchy: MEMBER(1) < ADMIN(2) < OWNER(3).
     */
    private boolean hasRequiredRole(Role actual, Role required) {
        return priority(actual) >= priority(required);
    }

    private int priority(Role role) {
        if (role == Role.OWNER)  return 3;
        if (role == Role.ADMIN)  return 2;
        return 1; // MEMBER
    }

    private void rejectWith(HttpServletResponse response,
                            HttpStatus status, String errorCode, String message) throws IOException {
        ApiError error = ApiError.builder()
                .error(errorCode)
                .message(message)
                .status(status.value())
                .build();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
