package com.submeter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SubMeter — subscription billing SaaS entry point.
 *
 * <p>Key annotations:
 * <ul>
 *   <li>{@code @ConfigurationPropertiesScan} — auto-discovers all
 *       {@code @ConfigurationProperties} classes (e.g. {@code AppProperties})
 *       without requiring explicit {@code @EnableConfigurationProperties} declarations.</li>
 *   <li>{@code @EnableJpaAuditing} — activates {@code @CreationTimestamp} /
 *       {@code @UpdateTimestamp} on all entities.</li>
 *   <li>{@code @EnableScheduling} — activates the nightly billing job
 *       ({@code NightlyBillingJob}) introduced in Milestone 4.</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableScheduling
public class SubMeterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubMeterApplication.class, args);
    }
}
