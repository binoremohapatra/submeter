package com.submeter.seeder;

import com.submeter.entity.AuditLog;
import com.submeter.entity.Customer;
import com.submeter.entity.Invoice;
import com.submeter.entity.Organization;
import com.submeter.entity.Subscription;
import com.submeter.entity.enums.ActorType;
import com.submeter.entity.enums.AuditAction;
import com.submeter.repository.AuditLogRepository;
import com.submeter.repository.CustomerRepository;
import com.submeter.repository.InvoiceRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.repository.SubscriptionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.PageRequest;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "submeter.import.audit", havingValue = "true")
public class AuditLogSeederRunner implements ApplicationRunner {

    private final OrganizationRepository orgRepo;
    private final CustomerRepository customerRepo;
    private final SubscriptionRepository subRepo;
    private final InvoiceRepository invoiceRepo;
    private final AuditLogRepository auditLogRepo;

    public AuditLogSeederRunner(OrganizationRepository orgRepo, CustomerRepository customerRepo,
                                SubscriptionRepository subRepo, InvoiceRepository invoiceRepo,
                                AuditLogRepository auditLogRepo) {
        this.orgRepo = orgRepo;
        this.customerRepo = customerRepo;
        this.subRepo = subRepo;
        this.invoiceRepo = invoiceRepo;
        this.auditLogRepo = auditLogRepo;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Organization org = orgRepo.findAll().stream().findFirst().orElseThrow();
        List<AuditLog> logs = new ArrayList<>();
        Instant now = Instant.now();

        // Get 20 customers
        List<Customer> customers = customerRepo.findAll(PageRequest.of(0, 20)).getContent();
        for (Customer c : customers) {
            long offset = ThreadLocalRandom.current().nextLong(1, 30);
            AuditLog log = AuditLog.builder().organization(org).actor(null).actorType(ActorType.SYSTEM).entityType("customer").entityId(c.getId()).action(AuditAction.CREATE).newValue(Map.of("name", c.getName(), "email", c.getEmail())).build();
            log.setCreatedAt(now.minus(offset, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
            logs.add(log);
        }

        // Get 20 subscriptions
        List<Subscription> subs = subRepo.findAll(PageRequest.of(0, 20)).getContent();
        for (Subscription s : subs) {
            long offset = ThreadLocalRandom.current().nextLong(1, 30);
            AuditLog log = AuditLog.builder().organization(org).actor(null).actorType(ActorType.SYSTEM).entityType("subscription").entityId(s.getId()).action(AuditAction.CREATE).newValue(Map.of("plan_id", s.getPlan().getId())).build();
            log.setCreatedAt(now.minus(offset, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
            logs.add(log);
        }

        // Get 20 invoices
        List<Invoice> invs = invoiceRepo.findAll(PageRequest.of(0, 20)).getContent();
        for (Invoice i : invs) {
            long offset = ThreadLocalRandom.current().nextLong(1, 30);
            AuditLog log = AuditLog.builder().organization(org).actor(null).actorType(ActorType.SYSTEM).entityType("invoice").entityId(i.getId()).action(AuditAction.CREATE).newValue(Map.of("invoice_number", i.getInvoiceNumber(), "total_cents", i.getTotalCents())).build();
            log.setCreatedAt(now.minus(offset, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS));
            logs.add(log);
        }

        // Some updates
        for (int i=0; i<10; i++) {
            if(subs.size() > i) {
                Subscription s = subs.get(i);
                long offset = ThreadLocalRandom.current().nextLong(1, 10);
                AuditLog log = AuditLog.builder().organization(org).actor(null).actorType(ActorType.SYSTEM).entityType("subscription").entityId(s.getId()).action(AuditAction.UPDATE).oldValue(Map.of("status", "TRIAL")).newValue(Map.of("status", "ACTIVE")).build();
                log.setCreatedAt(now.minus(offset, ChronoUnit.DAYS));
                logs.add(log);
            }
        }

        auditLogRepo.saveAll(logs);
        System.out.println("Audit logs seeded! Count: " + logs.size());
    }
}
