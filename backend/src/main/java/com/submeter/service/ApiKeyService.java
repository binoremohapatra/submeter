package com.submeter.service;

import com.submeter.entity.ApiKey;
import com.submeter.entity.Organization;
import com.submeter.entity.User;
import com.submeter.entity.enums.AuditAction;
import com.submeter.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public record GeneratedApiKey(ApiKey entity, String plaintextKey) {}

    @Transactional
    public GeneratedApiKey createApiKey(Organization org, User creator, String name, List<String> scopes, String environment) {
        // Generate 32 bytes of secure entropy
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        
        // Base64URL encode without padding
        String secretPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Generate 6-char key identifier (prefix)
        byte[] idBytes = new byte[4];
        secureRandom.nextBytes(idBytes);
        String keyId = Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes).substring(0, 6);
        
        String prefix = environment.equals("PRODUCTION") ? "sk_live_" : "sk_test_";
        String plaintextKey = prefix + keyId + "_" + secretPart;
        
        String keyHash = hash(plaintextKey);
        String last4 = plaintextKey.substring(plaintextKey.length() - 4);

        ApiKey apiKey = ApiKey.builder()
                .organization(org)
                .keyId(keyId)
                .prefix(prefix)
                .keyHash(keyHash)
                .last4(last4)
                .name(name)
                .scopes(scopes != null ? scopes : List.of())
                .environment(environment)
                .createdBy(creator)
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        auditService.recordExtended(org, creator, "api_key", apiKey.getId(), AuditAction.CREATE,
                null, Map.of("name", name, "scopes", scopes, "environment", environment),
                "api_key", name, true, 0L);

        notificationService.emit(
                org,
                "api_key.created",
                "API Key Created",
                "API Key '" + name + "' was created by " + creator.getEmail() + ".",
                "/dashboard/settings"
        );

        return new GeneratedApiKey(apiKey, plaintextKey);
    }

    @Transactional
    public void revokeApiKey(Organization org, User actor, UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findByOrganizationIdAndId(org.getId(), apiKeyId)
                .orElseThrow(() -> new RuntimeException("API Key not found"));

        if (apiKey.getRevokedAt() != null) {
            return;
        }

        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        auditService.recordExtended(org, actor, "api_key", apiKey.getId(), AuditAction.UPDATE,
                Map.of("revokedAt", "null"), Map.of("revokedAt", apiKey.getRevokedAt().toString()),
                "api_key", apiKey.getName(), true, 0L);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listApiKeys(Organization org) {
        return apiKeyRepository.findAllByOrganizationId(org.getId());
    }

    @Transactional
    public Optional<ApiKey> verifyApiKey(String plaintextKey) {
        String hash = hash(plaintextKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(hash);
        
        apiKeyOpt.ifPresent(apiKey -> {
            if (apiKey.getRevokedAt() == null) {
                apiKey.setLastUsedAt(Instant.now());
                apiKeyRepository.save(apiKey);
            }
        });
        
        return apiKeyOpt.filter(k -> k.getRevokedAt() == null);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
