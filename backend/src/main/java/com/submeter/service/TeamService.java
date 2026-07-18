package com.submeter.service;

import com.submeter.entity.Invitation;
import com.submeter.entity.Organization;
import com.submeter.entity.OrganizationMember;
import com.submeter.entity.User;
import com.submeter.entity.enums.AuditAction;
import com.submeter.entity.enums.Role;
import com.submeter.repository.InvitationRepository;
import com.submeter.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final InvitationRepository invitationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public record GeneratedInvitation(Invitation entity, String plaintextToken) {}

    @Transactional
    public GeneratedInvitation inviteMember(Organization org, User inviter, String email, Role role) {
        // Enforce RBAC
        if (inviter.getRole() == Role.MEMBER) {
            throw new RuntimeException("Members cannot invite new users");
        }

        // Check for existing pending invite
        Optional<Invitation> existing = invitationRepository.findByOrganizationIdAndEmailAndStatus(org.getId(), email, "PENDING");
        if (existing.isPresent()) {
            throw new RuntimeException("An invitation is already pending for this email");
        }

        String plaintextToken = UUID.randomUUID().toString();
        String tokenHash = hash(plaintextToken);

        Invitation invitation = Invitation.builder()
                .organization(org)
                .email(email)
                .role(role)
                .tokenHash(tokenHash)
                .status("PENDING")
                .invitedBy(inviter)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        invitation = invitationRepository.save(invitation);

        auditService.recordExtended(org, inviter, "invitation", invitation.getId(), AuditAction.CREATE,
                null, Map.of("email", email, "role", role),
                "invitation", email, true, 0L);

        return new GeneratedInvitation(invitation, plaintextToken);
    }

    @Transactional
    public void acceptInvitation(String plaintextToken, User user) {
        String tokenHash = hash(plaintextToken);
        Invitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        if (!invitation.getStatus().equals("PENDING")) {
            throw new RuntimeException("Invitation is no longer pending");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new RuntimeException("Invitation has expired");
        }

        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);

        OrganizationMember member = OrganizationMember.builder()
                .organization(invitation.getOrganization())
                .user(user)
                .role(invitation.getRole())
                .status("ACTIVE")
                .joinedAt(Instant.now())
                .build();

        organizationMemberRepository.save(member);

        auditService.recordExtended(invitation.getOrganization(), user, "organization_member", member.getId(), AuditAction.CREATE,
                null, Map.of("userId", user.getId(), "role", member.getRole()),
                "organization_member", user.getEmail(), true, 0L);

        notificationService.emit(
                invitation.getOrganization(),
                "invite.accepted",
                "Invitation Accepted",
                user.getEmail() + " has accepted the invitation and joined the team.",
                "/dashboard/settings"
        );
    }

    @Transactional
    public void removeMember(Organization org, User actor, UUID memberId) {
        if (actor.getRole() == Role.MEMBER) {
            throw new RuntimeException("Insufficient permissions to remove members");
        }

        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndId(org.getId(), memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getUser().getId().equals(actor.getId())) {
            throw new RuntimeException("Cannot remove yourself");
        }

        member.setDeletedAt(Instant.now());
        organizationMemberRepository.save(member);

        auditService.recordExtended(org, actor, "organization_member", member.getId(), AuditAction.DELETE,
                Map.of("status", "ACTIVE"), Map.of("deletedAt", member.getDeletedAt().toString()),
                "organization_member", member.getUser().getEmail(), true, 0L);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMember> listMembers(Organization org) {
        return organizationMemberRepository.findAllByOrganizationId(org.getId());
    }
    
    @Transactional(readOnly = true)
    public List<Invitation> listInvitations(Organization org) {
        return invitationRepository.findAllByOrganizationId(org.getId());
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
