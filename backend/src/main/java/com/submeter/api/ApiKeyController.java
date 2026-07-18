package com.submeter.api;

import com.submeter.entity.ApiKey;
import com.submeter.entity.Organization;
import com.submeter.entity.User;
import com.submeter.security.UserPrincipal;
import com.submeter.repository.UserRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public record CreateApiKeyRequest(String name, List<String> scopes, String environment) {}

    @GetMapping
    public ResponseEntity<List<ApiKey>> listApiKeys(@AuthenticationPrincipal UserPrincipal principal) {
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(apiKeyService.listApiKeys(org));
    }

    @PostMapping
    public ResponseEntity<ApiKeyService.GeneratedApiKey> createApiKey(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateApiKeyRequest request) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        ApiKeyService.GeneratedApiKey generated = apiKeyService.createApiKey(
                org, user, request.name(), request.scopes(), request.environment());
        return ResponseEntity.ok(generated);
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeApiKey(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        apiKeyService.revokeApiKey(org, user, id);
        return ResponseEntity.ok().build();
    }
}
