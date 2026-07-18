package com.submeter.api;

import com.submeter.entity.Customer;
import com.submeter.entity.Organization;
import com.submeter.entity.Plan;
import com.submeter.entity.Subscription;
import com.submeter.entity.UsageEvent;
import com.submeter.entity.User;
import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.PricingModel;
import com.submeter.entity.enums.Role;
import com.submeter.entity.enums.SubscriptionStatus;
import com.submeter.repository.CustomerRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.repository.PlanRepository;
import com.submeter.repository.SubscriptionRepository;
import com.submeter.repository.UsageEventRepository;
import com.submeter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.submeter.entity.Invoice;
import com.submeter.entity.enums.InvoiceStatus;
import com.submeter.entity.AuditLog;
import com.submeter.entity.enums.ActorType;
import com.submeter.entity.enums.AuditAction;
import com.submeter.repository.InvoiceRepository;
import com.submeter.repository.AuditLogRepository;
import java.util.Map;

import com.submeter.api.dto.ImportStatsDto;
import com.submeter.entity.ImportHistory;
import com.submeter.repository.ImportHistoryRepository;
import com.submeter.service.DataSeederService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile("!prod") // Safety net: never load in production
public class DevController {

    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final PlanRepository planRepo;
    private final SubscriptionRepository subRepo;
    private final UsageEventRepository usageRepo;
    private final InvoiceRepository invoiceRepo;
    private final AuditLogRepository auditLogRepo;
    private final PasswordEncoder passwordEncoder;
    private final DataSeederService dataSeederService;
    private final ImportHistoryRepository importHistoryRepo;

    @PostMapping("/seed")
    public ResponseEntity<ImportStatsDto> seed(@RequestBody Map<String, String> payload) {
        String dataset = payload.getOrDefault("dataset", "ibm_telco");
        String mode = payload.getOrDefault("mode", "full");
        ImportStatsDto stats = dataSeederService.seed(dataset, mode);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        dataSeederService.reset();
        return ResponseEntity.ok(Map.of("message", "Demo data reset successfully"));
    }

    @GetMapping("/import-history")
    public ResponseEntity<List<ImportHistory>> getImportHistory() {
        return ResponseEntity.ok(importHistoryRepo.findAll());
    }
}
