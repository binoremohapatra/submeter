package com.submeter.service;

import com.submeter.entity.Notification;
import com.submeter.entity.Organization;
import com.submeter.entity.User;
import com.submeter.repository.NotificationRepository;
import com.submeter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    /**
     * Emits a notification to ALL users in the given organization.
     * Note: For small/medium B2B SaaS orgs, this fan-out is acceptable.
     * If orgs grow to thousands of members, this should be refactored 
     * to a batch insert or pub/sub model to avoid unbounded inserts.
     */
    @Transactional
    public void emit(Organization org, String type, String title, String body, String link) {
        List<User> members = userRepo.findAllByOrganizationIdAndDeletedAtIsNull(org.getId());
        List<Notification> notifications = new ArrayList<>();
        
        for (User user : members) {
            notifications.add(Notification.builder()
                    .organization(org)
                    .user(user)
                    .type(type)
                    .title(title)
                    .body(body)
                    .link(link)
                    .build());
        }
        
        // Use batch saving to mitigate N inserts
        notificationRepo.saveAll(notifications);
        log.info("Emitted notification '{}' to {} users in org {}", type, notifications.size(), org.getId());
    }
}
