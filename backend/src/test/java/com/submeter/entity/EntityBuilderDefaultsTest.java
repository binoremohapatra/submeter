package com.submeter.entity;

import com.submeter.entity.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EntityBuilderDefaultsTest {

    @Test
    void organizationBuilderUsesDefaultTimezoneAndCurrency() {
        Organization organization = Organization.builder()
                .name("Acme")
                .slug("acme")
                .build();

        assertEquals("UTC", organization.getTimezone());
        assertEquals("INR", organization.getCurrency());
    }

    @Test
    void userBuilderUsesDefaultVerificationAndLoginFlags() {
        User user = User.builder()
                .organization(new Organization())
                .email("user@example.com")
                .passwordHash("hash")
                .role(Role.OWNER)
                .build();

        assertFalse(user.isEmailVerified());
        assertEquals(0, user.getFailedLoginCount());
    }
}
