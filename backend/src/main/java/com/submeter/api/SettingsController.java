package com.submeter.api;

import com.submeter.entity.Organization;
import com.submeter.entity.User;
import com.submeter.security.UserPrincipal;
import com.submeter.repository.UserRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @GetMapping
    public ResponseEntity<Organization> getSettings(@AuthenticationPrincipal UserPrincipal principal) {
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(org);
    }

    @PutMapping
    public ResponseEntity<Organization> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody OrganizationService.OrganizationSettingsDto dto) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization updated = organizationService.updateOrganizationSettings(principal.getOrgId(), user, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/logo")
    public ResponseEntity<Organization> uploadLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        try {
            User user = userRepository.findById(principal.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String logoUrl = "/api/uploads/" + filename;
            OrganizationService.OrganizationSettingsDto dto = new OrganizationService.OrganizationSettingsDto(
                    null, logoUrl, null, null, null, null, null, null, null, null);
            Organization updated = organizationService.updateOrganizationSettings(principal.getOrgId(), user, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload logo", e);
        }
    }
}
