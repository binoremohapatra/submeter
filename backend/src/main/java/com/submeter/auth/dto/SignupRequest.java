package com.submeter.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    /**
     * Raw password. Min 8, max 72 chars (Argon2 maximum useful input).
     * Never stored — hashed immediately in AuthService.
     */
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    /** Organization display name. Used to generate the org slug. */
    @NotBlank
    @Size(min = 2, max = 100)
    private String orgName;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
}
