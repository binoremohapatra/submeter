package com.submeter.service;

import com.submeter.api.dto.ImportStatsDto;
import com.submeter.entity.*;
import com.submeter.entity.enums.InvoiceStatus;
import com.submeter.entity.enums.PaymentMethod;
import com.submeter.entity.enums.PaymentStatus;
import com.submeter.entity.enums.SubscriptionStatus;
import com.submeter.repository.*;
import jakarta.persistence.EntityManager;
import net.datafaker.Faker;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("!prod")
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);
    private static final String CSV_URL = "https://raw.githubusercontent.com/IBM/telco-customer-churn-on-icp4d/master/data/Telco-Customer-Churn.csv";

    @Value("${app.seeder.batch-size:500}")
    private int batchSize;

    private final CustomerRepository customerRepo;
    private final SubscriptionRepository subRepo;
    private final PlanRepository planRepo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineItemRepository invoiceLineItemRepo;
    private final PaymentRepository paymentRepo;
    private final UsageEventRepository usageRepo;
    private final OrganizationRepository orgRepo;
    private final ImportHistoryRepository importHistoryRepo;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final Faker faker = new Faker();

    public DataSeederService(CustomerRepository customerRepo, SubscriptionRepository subRepo, PlanRepository planRepo,
                             InvoiceRepository invoiceRepo, InvoiceLineItemRepository invoiceLineItemRepo,
                             PaymentRepository paymentRepo, UsageEventRepository usageRepo,
                             OrganizationRepository orgRepo, ImportHistoryRepository importHistoryRepo,
                             TransactionTemplate transactionTemplate, EntityManager entityManager) {
        this.customerRepo = customerRepo;
        this.subRepo = subRepo;
        this.planRepo = planRepo;
        this.invoiceRepo = invoiceRepo;
        this.invoiceLineItemRepo = invoiceLineItemRepo;
        this.paymentRepo = paymentRepo;
        this.usageRepo = usageRepo;
        this.orgRepo = orgRepo;
        this.importHistoryRepo = importHistoryRepo;
        this.transactionTemplate = transactionTemplate;
        this.entityManager = entityManager;
    }

    public ImportStatsDto seed(String dataset, String mode) {
        log.info("Starting Data Seed. Dataset: {}, Mode: {}", dataset, mode);
        Instant startedAt = Instant.now();

        ImportHistory history = ImportHistory.builder()
                .dataset(dataset)
                .mode(mode)
                .status("IN_PROGRESS")
                .startedAt(startedAt)
                .customers(0)
                .subscriptions(0)
                .invoices(0)
                .payments(0)
                .usageEvents(0)
                .batchCount(0)
                .importedBy("system")
                .build();
        history = importHistoryRepo.save(history);

        try {
            if ("ibm_telco".equalsIgnoreCase(dataset)) {
                processIbmTelco(history);
            } else {
                throw new IllegalArgumentException("Unknown dataset: " + dataset);
            }

            history.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("Seeding failed", e);
            history.setStatus("FAILED");
            history.setErrorMessage(e.getMessage());
        }

        Instant completedAt = Instant.now();
        history.setCompletedAt(completedAt);
        history.setDurationMs(Duration.between(startedAt, completedAt).toMillis());
        importHistoryRepo.save(history);

        return ImportStatsDto.builder()
                .dataset(history.getDataset())
                .status(history.getStatus())
                .duration(history.getDurationMs() + "ms")
                .customers(history.getCustomers())
                .subscriptions(history.getSubscriptions())
                .invoices(history.getInvoices())
                .payments(history.getPayments())
                .usageEvents(history.getUsageEvents())
                .build();
    }

    private void processIbmTelco(ImportHistory history) throws Exception {
        Path dataDir = Paths.get("data");
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        Path csvPath = dataDir.resolve("ibm_telco.csv");
        if (!Files.exists(csvPath)) {
            log.info("Downloading IBM Telco dataset...");
            try (var in = new URL(CSV_URL).openStream()) {
                Files.copy(in, csvPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Download complete.");
        } else {
            log.info("Using cached IBM Telco dataset from {}", csvPath);
        }

        Organization org = orgRepo.findAll().stream().findFirst().orElseGet(() -> {
            Organization newOrg = Organization.builder().name("Demo Org").build();
            return orgRepo.save(newOrg);
        });

        Plan starter = getOrCreatePlan(org, "Starter", 5000); // 50 USD/m
        Plan pro = getOrCreatePlan(org, "Pro", 10000);        // 100 USD/m
        Plan enterprise = getOrCreatePlan(org, "Enterprise", 20000); // 200 USD/m

        Set<String> existingIds = customerRepo.findAllExternalIds();
        log.info("Found {} existing customers. Skipping duplicates.", existingIds.size());

        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            List<CSVRecord> batch = new ArrayList<>();
            int count = 0;

            for (CSVRecord record : csvParser) {
                String externalId = record.get("customerID");
                if (existingIds.contains(externalId)) {
                    continue;
                }

                batch.add(record);
                if (batch.size() >= batchSize) {
                    processBatch(batch, org, starter, pro, enterprise, history);
                    count += batch.size();
                    batch.clear();
                    log.info("Imported {} records so far...", count);
                }
            }

            if (!batch.isEmpty()) {
                processBatch(batch, org, starter, pro, enterprise, history);
                count += batch.size();
            }

            log.info("Telco CSV Import Complete! Inserted {} new customers.", count);
        }
    }

    private Plan getOrCreatePlan(Organization org, String name, long amountCents) {
        return planRepo.findAll().stream()
                .filter(p -> p.getOrganization().getId().equals(org.getId()))
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> planRepo.save(Plan.builder()
                        .organization(org)
                        .name(name)
                        .version(1)
                        .pricingModel(com.submeter.entity.enums.PricingModel.FLAT)
                        .flatAmount(amountCents)
                        .billingInterval(com.submeter.entity.enums.BillingInterval.MONTHLY)
                        .build()));
    }

    private void processBatch(List<CSVRecord> batch, Organization org, Plan starter, Plan pro, Plan enterprise, ImportHistory history) {
        transactionTemplate.executeWithoutResult(status -> {
            int customers = 0, subscriptions = 0, invoices = 0, payments = 0, usageEvents = 0;

            for (CSVRecord record : batch) {
                try {
                    String name = faker.name().fullName();
                    String email = faker.internet().emailAddress(name.toLowerCase().replace(" ", "."));
                    String gender = record.get("gender");
                    boolean senior = "1".equals(record.get("SeniorCitizen"));
                    boolean partner = "Yes".equalsIgnoreCase(record.get("Partner"));

                    Customer customer = Customer.builder()
                            .organization(org)
                            .name(name)
                            .email(email)
                            .externalId(record.get("customerID"))
                            .gender(gender)
                            .senior(senior)
                            .partner(partner)
                            .build();
                    customer = customerRepo.save(customer);
                    customers++;

                    int tenure = parseIntSafely(record.get("tenure"));
                    String contract = record.get("Contract");
                    Plan plan = "Two year".equalsIgnoreCase(contract) ? enterprise :
                                "One year".equalsIgnoreCase(contract) ? pro : starter;

                    boolean churn = "Yes".equalsIgnoreCase(record.get("Churn"));
                    SubscriptionStatus subStatus = churn ? SubscriptionStatus.CANCELED : SubscriptionStatus.ACTIVE;

                    Instant now = Instant.now();
                    Instant startedAt = now.minus(tenure * 30L, ChronoUnit.DAYS);

                    Subscription sub = Subscription.builder()
                            .organization(org)
                            .customer(customer)
                            .plan(plan)
                            .planVersion(1)
                            .status(subStatus)
                            .monthsActive(tenure)
                            .currentPeriodStart(startedAt)
                            .canceledAt(churn ? now : null)
                            .build();
                    sub.setCreatedAt(startedAt);
                    sub = subRepo.save(sub);
                    subscriptions++;

                    double totalCharges = parseDoubleSafely(record.get("TotalCharges"));
                    if (totalCharges > 0) {
                        long amountCents = (long) (totalCharges * 100);
                        Instant lastCycleStart = now.minus(30L, ChronoUnit.DAYS);
                        Instant lastCycleEnd = now;

                        Invoice invoice = Invoice.builder()
                                .organization(org)
                                .subscription(sub)
                                .status(InvoiceStatus.PAID)
                                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .periodStart(lastCycleStart)
                                .periodEnd(lastCycleEnd)
                                .dueAt(lastCycleStart.plus(7, ChronoUnit.DAYS))
                                .paidAt(lastCycleStart.plus(1, ChronoUnit.DAYS))
                                .subtotalCents(amountCents)
                                .totalCents(amountCents)
                                .taxCents(0L)
                                .build();
                        invoice.setCreatedAt(lastCycleStart);

                        com.submeter.entity.InvoiceLineItem lineItem = com.submeter.entity.InvoiceLineItem.builder()
                                .invoice(invoice)
                                .description("Subscription Plan Charge")
                                .quantity(1L)
                                .unitAmount(amountCents)
                                .amount(amountCents)
                                .pricingModel(com.submeter.entity.enums.PricingModel.FLAT)
                                .build();
                        lineItem.setCreatedAt(lastCycleStart);
                        invoice.getLineItems().add(lineItem);

                        invoice = invoiceRepo.save(invoice);
                        invoices++;

                        String paymentMethodStr = record.get("PaymentMethod");
                        PaymentMethod method = PaymentMethod.ELECTRONIC;
                        if (paymentMethodStr != null) {
                            if (paymentMethodStr.toLowerCase().contains("bank")) method = PaymentMethod.BANK;
                            else if (paymentMethodStr.toLowerCase().contains("card")) method = PaymentMethod.CARD;
                        }

                        Payment payment = Payment.builder()
                                .organization(org)
                                .invoice(invoice)
                                .razorpayOrderId("sim_" + UUID.randomUUID().toString().substring(0, 8))
                                .razorpayPaymentId("pay_" + UUID.randomUUID().toString().substring(0, 8))
                                .amountCents(amountCents)
                                .currency("USD")
                                .status(PaymentStatus.SUCCESS)
                                .paymentMethod(method)
                                .build();
                        payment.setCreatedAt(startedAt);
                        paymentRepo.save(payment);
                        payments++;
                    }

                    List<UsageEvent> events = new ArrayList<>();
                    if (isYes(record.get("InternetService"))) {
                        events.add(createEvent(org, sub, "API Requests", faker.number().numberBetween(100, 50000)));
                    }
                    if (isYes(record.get("StreamingTV"))) {
                        events.add(createEvent(org, sub, "Bandwidth Usage", faker.number().numberBetween(10, 2000)));
                    }
                    if (isYes(record.get("StreamingMovies"))) {
                        events.add(createEvent(org, sub, "Storage Usage", faker.number().numberBetween(5, 500)));
                    }
                    if (isYes(record.get("OnlineBackup"))) {
                        events.add(createEvent(org, sub, "Backup Jobs", faker.number().numberBetween(1, 50)));
                    }
                    if (isYes(record.get("TechSupport"))) {
                        events.add(createEvent(org, sub, "Support Tickets", faker.number().numberBetween(1, 10)));
                    }
                    usageRepo.saveAll(events);
                    usageEvents += events.size();

                } catch (Exception e) {
                    log.error("Error processing record: " + record.get("customerID"), e);
                    throw new RuntimeException("Batch failed at record " + record.get("customerID"), e);
                }
            }

            entityManager.flush();
            entityManager.clear();

            // Update history tracking in the current batch
            history.setCustomers(history.getCustomers() + customers);
            history.setSubscriptions(history.getSubscriptions() + subscriptions);
            history.setInvoices(history.getInvoices() + invoices);
            history.setPayments(history.getPayments() + payments);
            history.setUsageEvents(history.getUsageEvents() + usageEvents);
            history.setBatchCount(history.getBatchCount() + 1);
            
            // Note: Since this is inside transactionTemplate, history saves need to happen 
            // after the transaction, but we are just updating the object in memory 
            // which will be persisted at the end. Or we can let it be.
        });
        importHistoryRepo.save(history); // Save progress after each batch completes successfully
    }

    private UsageEvent createEvent(Organization org, Subscription sub, String eventName, long quantity) {
        UsageEvent event = UsageEvent.builder()
                .organization(org)
                .subscription(sub)
                .eventType(eventName)
                .occurredAt(Instant.now())
                .quantity(quantity)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        event.setCreatedAt(Instant.now());
        return event;
    }

    private boolean isYes(String val) {
        return val != null && !val.equalsIgnoreCase("No") && !val.equalsIgnoreCase("No internet service");
    }

    private int parseIntSafely(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    private double parseDoubleSafely(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    public void reset() {
        log.info("Resetting all demo data (Customers, Plans, Subscriptions, UsageEvents, Invoices, Payments)");
        
        // Use explicit DELETEs maintaining foreign key ordering
        usageRepo.deleteAllInBatch();
        paymentRepo.deleteAllInBatch();
        invoiceLineItemRepo.deleteAllInBatch();
        invoiceRepo.deleteAllInBatch();
        subRepo.deleteAllInBatch();
        customerRepo.deleteAllInBatch();
        
        // Also remove plans if necessary
        planRepo.deleteAllInBatch();
        
        log.info("Demo data reset successfully.");
    }
}
