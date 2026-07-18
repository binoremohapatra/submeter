package com.submeter.security;

import com.submeter.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight principal reconstructed from a verified JWT.
 *
 * <p>This principal is created in {@link JwtAuthenticationFilter} and stored
 * in the {@link org.springframework.security.core.context.SecurityContext}.
 * It contains only what's in the JWT — it is NOT loaded from the DB on every
 * request (that would kill performance).
 *
 * <p>For ADMIN/OWNER-required actions, {@link com.submeter.security.rbac.RbacInterceptor}
 * re-queries the DB to get the current role, ignoring the {@code role} field here.
 */
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID orgId;

    /**
     * Role from JWT claim. INFORMATIONAL ONLY for MEMBER-level access.
     * Never trust this for ADMIN/OWNER actions — use RbacInterceptor which
     * re-verifies from the DB.
     */
    private final Role role;

    /** Not stored — principal is reconstructed from JWT, password not in token. */
    private final boolean accountNonLocked;

    public UserPrincipal(UUID userId, UUID orgId, Role role, boolean accountNonLocked) {
        this.userId = userId;
        this.orgId  = orgId;
        this.role   = role;
        this.accountNonLocked = accountNonLocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Returns userId string (Spring's UserDetails.getUsername() contract). */
    @Override
    public String getUsername() {
        return userId.toString();
    }

    /** Password is not stored in the principal — never needed post-auth. */
    @Override
    public String getPassword() {
        return null;
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }

    // Getters
    public UUID getUserId() {
        return userId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public Role getRole() {
        return role;
    }
}
