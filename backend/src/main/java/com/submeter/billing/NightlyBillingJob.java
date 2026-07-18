package com.submeter.billing;

import com.submeter.entity.Invoice;
import com.submeter.entity.InvoiceLineItem;
import com.submeter.entity.Subscription;
import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.InvoiceStatus;
import com.submeter.entity.enums.PricingModel;
import com.submeter.entity.enums.SubscriptionStatus;
import com.submeter.repository.InvoiceLineItemRepository;
import com.submeter.repository.InvoiceRepository;
import com.submeter.repository.SubscriptionRepository;
import com.submeter.repository.UsageEventRepository;
import com.submeter.service.AuditService;
import com.submeter.entity.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NightlyBillingJob {

    private static final Logger log = LoggerFactory.getLogger(NightlyBillingJob.class);

    private final SubscriptionRepository subscriptionRepo;
    private final UsageEventRepository usageRepo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineItemRepository lineItemRepo;
    private final BillingCalculator calculator;
    private final AuditService auditService;

    /**
     * Runs daily at 02:00 UTC (configured in application.yml via app.billing.nightly-cron).
     */
    @Scheduled(cron = "${app.billing.nightly-cron}")
    public void executeNightlyBilling() {
        log.info("Starting Nightly Billing Job");
        Instant now = Instant.now();

        // 1. Convert expired TRIAL -> ACTIVE
        List<Subscription> expiringTrials = subscriptionRepo.findTrialsExpiringBefore(now);
        for (Subscription sub : expiringTrials) {
            try {
                // Must be isolated so one failure doesn't roll back the whole batch
                processTrialExpiry(sub, now);
            } catch (Exception e) {
                log.error("Failed to process trial expiry for sub {}", sub.getId(), e);
            }
        }

        // 2. Invoice ACTIVE subscriptions whose period has ended
        List<Subscription> endingPeriods = subscriptionRepo.findActiveSubscriptionsEndingBefore(now);
        for (Subscription sub : endingPeriods) {
            try {
                processBillingCycle(sub);
            } catch (Exception e) {
                log.error("Failed to process billing cycle for sub {}", sub.getId(), e);
            }
        }

        // 3. Process Dunning Emails (Mock for Extensions)
        try {
            processDunningEmails(now);
        } catch (Exception e) {
            log.error("Failed to process dunning emails", e);
        }

        log.info("Finished Nightly Billing Job");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTrialExpiry(Subscription sub, Instant now) {
        sub.setStatus(SubscriptionStatus.ACTIVE);
        // Anchor the billing period to now
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(calculateNextPeriodEnd(now, sub.getPlan().getBillingInterval()));
        subscriptionRepo.save(sub);
        log.info("Subscription {} transitioned TRIAL -> ACTIVE", sub.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBillingCycle(Subscription sub) {
        Instant periodStart = sub.getCurrentPeriodStart();
        Instant periodEnd = sub.getCurrentPeriodEnd();

        // 1. Calculate usage if METERED or TIERED
        long totalUsage = 0L;
        if (sub.getPlan().getPricingModel() != PricingModel.FLAT) {
            totalUsage = usageRepo.sumUsageForSubscriptionInPeriod(sub.getId(), periodStart, periodEnd);
        }

        // 2. Calculate Line Items
        List<InvoiceLineItem> items = calculator.calculateLineItems(sub, totalUsage);

        long totalAmount = items.stream().mapToLong(InvoiceLineItem::getAmount).sum();

        // 3. Create Draft Invoice
        // We create it as OPEN immediately because we expect payment to happen async.
        // If amount is 0, we could mark it PAID, but for simplicity we let it be processed.
        Invoice invoice = Invoice.builder()
                .organization(sub.getOrganization())
                .subscription(sub)
                .invoiceNumber(invoiceRepo.generateInvoiceNumber(sub.getOrganization().getId()))
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .subtotalCents(totalAmount)
                .totalCents(totalAmount)
                .dueAt(periodEnd.plus(7, java.time.temporal.ChronoUnit.DAYS))
                .status(totalAmount > 0 ? InvoiceStatus.OPEN : InvoiceStatus.PAID)
                .build();

        try {
            invoice = invoiceRepo.save(invoice);
        } catch (DataIntegrityViolationException ex) {
            // Idempotency: Unique constraint on (subscription_id, period_start)
            // If this throws, we already invoiced this period. Safely ignore and skip.
            log.warn("Invoice already exists for sub {} period_start {}. Skipping.", sub.getId(), periodStart);
            return;
        }

        // Save line items
        for (InvoiceLineItem item : items) {
            item.setInvoice(invoice);
            lineItemRepo.save(item);
        }

        // 4. Advance Subscription Period
        sub.setCurrentPeriodStart(periodEnd);
        sub.setCurrentPeriodEnd(calculateNextPeriodEnd(periodEnd, sub.getPlan().getBillingInterval()));
        subscriptionRepo.save(sub);

        log.info("Generated invoice {} for sub {} amount {}", invoice.getId(), sub.getId(), totalAmount);
    }

    private Instant calculateNextPeriodEnd(Instant start, BillingInterval interval) {
        ZonedDateTime zdt = start.atZone(ZoneOffset.UTC);
        return switch (interval) {
            case MONTHLY -> zdt.plusMonths(1).toInstant();
            case ANNUAL  -> zdt.plusYears(1).toInstant();
        };
    }

    @Transactional
    public void processDunningEmails(Instant now) {
        // Mock Dunning Email extension: Find OPEN invoices past their due date
        // Note: In v1, currency is INR, but architecture supports Multi-currency (e.g. USD) via Invoice currency column in future.
        List<Invoice> pastDueInvoices = invoiceRepo.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OPEN && inv.getDueAt().isBefore(now))
                .toList();

        for (Invoice inv : pastDueInvoices) {
            // Log a mock dunning email being sent to the audit log
            auditService.record(
                    inv.getOrganization(),
                    null, // System actor
                    "invoice",
                    inv.getId(),
                    AuditAction.UPDATE,
                    java.util.Map.of("dunning_event", "Payment Reminder Email Sent (Mock)")
            );
            log.info("Dispatched dunning email for invoice {}", inv.getId());
        }
    }
}
