package com.submeter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.submeter.auth.dto.ApiError;
import com.submeter.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>CSRF disabled: REST API using JWT cookies with SameSite=Lax. SameSite
 *       prevents cross-site POST requests in all modern browsers, providing CSRF
 *       protection without needing a CSRF token (which complicates SPAs).</li>
 *   <li>Stateless sessions: no HttpSession created; every request is authenticated
 *       from the JWT cookie by {@link JwtAuthenticationFilter}.</li>
 *   <li>Argon2id: OWASP-recommended for password hashing. BouncyCastle provides
 *       the native implementation on the classpath.</li>
 *   <li>401/403 handling: custom JSON responses using {@link ApiError} shape,
 *       not Spring's default HTML error pages.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper            objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // ── CSRF: disabled (SameSite=Lax + stateless JWT is sufficient for our threat model)
                .csrf(AbstractHttpConfigurer::disable)

                // ── Sessions: strictly stateless
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no token required
                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout"
                        ).permitAll()
                        .requestMatchers("/webhooks/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/dev/**").permitAll() // blocked by controller in non-dev profile
                        // Everything else requires a valid JWT cookie
                        .anyRequest().authenticated()
                )

                // ── JWT filter runs before Spring's UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // ── Custom 401 / 403 error responses (JSON, not Spring's default HTML)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(res.getWriter(),
                                    ApiError.builder()
                                            .error("UNAUTHENTICATED")
                                            .message("Valid authentication is required.")
                                            .status(401)
                                            .build());
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpStatus.FORBIDDEN.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(res.getWriter(),
                                    ApiError.builder()
                                            .error("FORBIDDEN")
                                            .message("You do not have permission to perform this action.")
                                            .status(403)
                                            .build());
                        })
                )
                .build();
    }

    /**
     * Argon2id with OWASP-recommended parameters for interactive login (2025):
     * salt=16 bytes, hash=32 bytes, parallelism=1, memory=64MB, iterations=3.
     *
     * <p>These are stronger than Spring's defaults (memory=16MB, iterations=2)
     * while still completing in ~300ms on modern hardware.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKiB, iterations)
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
