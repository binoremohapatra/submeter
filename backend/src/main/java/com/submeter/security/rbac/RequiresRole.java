package com.submeter.security.rbac;

import com.submeter.entity.enums.Role;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the minimum {@link Role} required to access a controller method or type.
 *
 * <p>Role hierarchy (ascending privilege):
 * <pre>  MEMBER &lt; ADMIN &lt; OWNER</pre>
 *
 * <p>Processed by {@link RbacInterceptor}, which re-verifies the role from
 * the database for ADMIN and OWNER-required actions (never trusts the JWT claim alone).
 *
 * <p>Usage:
 * <pre>
 * {@literal @}PostMapping
 * {@literal @}RequiresRole(minimum = Role.ADMIN)
 * public ResponseEntity<?> createCustomer(...) { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    /**
     * Minimum role required (inclusive).
     * A user with a role of equal or higher privilege will be granted access.
     */
    Role minimum();
}
