package com.submeter.api;

import com.submeter.entity.Invitation;
import com.submeter.entity.Organization;
import com.submeter.entity.OrganizationMember;
import com.submeter.entity.User;
import com.submeter.security.UserPrincipal;
import com.submeter.repository.UserRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.entity.enums.Role;
import com.submeter.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public record InviteMemberRequest(String email, Role role) {}
    public record AcceptInvitationRequest(String token) {}

    @GetMapping("/members")
    public ResponseEntity<List<OrganizationMember>> listMembers(@AuthenticationPrincipal UserPrincipal principal) {
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(teamService.listMembers(org));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        teamService.removeMember(org, user, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invites")
    public ResponseEntity<List<Invitation>> listInvitations(@AuthenticationPrincipal UserPrincipal principal) {
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(teamService.listInvitations(org));
    }

    @PostMapping("/invites")
    public ResponseEntity<TeamService.GeneratedInvitation> inviteMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody InviteMemberRequest request) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        TeamService.GeneratedInvitation generated = teamService.inviteMember(
                org, user, request.email(), request.role());
        return ResponseEntity.ok(generated);
    }

    @PostMapping("/invites/accept")
    public ResponseEntity<Void> acceptInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AcceptInvitationRequest request) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        teamService.acceptInvitation(request.token(), user);
        return ResponseEntity.ok().build();
    }
}
