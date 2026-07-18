package com.submeter.api;

import com.submeter.api.dto.NotificationResponse;
import com.submeter.entity.Notification;
import com.submeter.repository.NotificationRepository;
import com.submeter.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepo;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        Pageable top10 = PageRequest.of(0, 10);
        List<Notification> latest = notificationRepo.findByUserIdOrderByCreatedAtDesc(principal.getUserId(), top10);
        long unreadCount = notificationRepo.countByUserIdAndReadAtIsNull(principal.getUserId());

        List<NotificationResponse> responses = latest.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "notifications", responses,
                "unreadCount", unreadCount
        ));
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationRepo.markAllAsReadForUser(principal.getUserId());
        return ResponseEntity.ok().build();
    }
}
