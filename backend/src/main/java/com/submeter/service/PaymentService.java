package com.submeter.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.submeter.api.dto.PaymentResponse;
import com.submeter.entity.Invoice;
import com.submeter.entity.Payment;
import com.submeter.entity.enums.InvoiceStatus;
import com.submeter.entity.enums.PaymentStatus;
import com.submeter.repository.InvoiceRepository;
import com.submeter.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final NotificationService notificationService;

    @Value("${app.razorpay.key-id:rzp_test_stub}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret:rzp_test_secret}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        if (!razorpayKeyId.equals("rzp_test_stub")) {
            try {
                this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            } catch (RazorpayException e) {
                log.error("Failed to initialize Razorpay Client", e);
            }
        }
    }

    @Transactional
    public PaymentResponse createPaymentOrder(UUID orgId, UUID invoiceId) {
        Invoice invoice = invoiceRepo.findByIdAndOrganizationIdWithLineItems(invoiceId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.OPEN) {
            throw new IllegalArgumentException("Invoice is not open for payment.");
        }

        if (invoice.getRazorpayOrderId() != null) {
            // Already created an order, just return it so frontend can resume
            return PaymentResponse.builder()
                    .razorpayOrderId(invoice.getRazorpayOrderId())
                    .amountCents(invoice.getTotalCents())
                    .currency("INR")
                    .build();
        }

        // Create Razorpay Order
        String orderId = "order_stub_" + UUID.randomUUID().toString().substring(0, 8);
        
        if (razorpayClient != null) {
            try {
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", invoice.getTotalCents()); // amount in paisa
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", invoice.getInvoiceNumber());
                
                Order order = razorpayClient.orders.create(orderRequest);
                orderId = order.get("id");
            } catch (RazorpayException e) {
                log.error("Razorpay order creation failed", e);
                throw new RuntimeException("Payment gateway error", e);
            }
        }

        invoice.setRazorpayOrderId(orderId);
        invoice = invoiceRepo.save(invoice);

        Payment payment = Payment.builder()
                .organization(invoice.getOrganization())
                .invoice(invoice)
                .amountCents(invoice.getTotalCents())
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .razorpayOrderId(orderId)
                .build();
        
        paymentRepo.save(payment);

        return PaymentResponse.builder()
                .razorpayOrderId(orderId)
                .amountCents(invoice.getTotalCents())
                .currency("INR")
                .build();
    }

    @Transactional
    public void processPaymentSuccess(String razorpayOrderId, String razorpayPaymentId) {
        Payment payment = paymentRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return; // Already processed
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        paymentRepo.save(payment);

        Invoice invoice = payment.getInvoice();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(java.time.Instant.now());
        invoiceRepo.save(invoice);

        com.submeter.entity.Subscription sub = invoice.getSubscription();
        if (sub.getStatus() == com.submeter.entity.enums.SubscriptionStatus.PAST_DUE) {
            sub.setStatus(com.submeter.entity.enums.SubscriptionStatus.ACTIVE);
            // We should use subscriptionRepo.save(sub) but we are in a transaction and it's managed,
            // however for clarity and to not rely strictly on dirty checking alone we can assume it will be flushed.
        }

        notificationService.emit(
                invoice.getOrganization(),
                "invoice.paid",
                "Invoice Paid",
                "Invoice " + invoice.getInvoiceNumber() + " has been successfully paid.",
                "/dashboard/invoices/" + invoice.getId()
        );
    }

    @Transactional
    public void processPaymentFailure(String razorpayOrderId, String razorpayPaymentId) {
        Payment payment = paymentRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        paymentRepo.save(payment);

        Invoice invoice = payment.getInvoice();
        // If it was already paid by some other payment, don't change status
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            com.submeter.entity.Subscription sub = invoice.getSubscription();
            if (sub.getStatus() == com.submeter.entity.enums.SubscriptionStatus.ACTIVE) {
                sub.setStatus(com.submeter.entity.enums.SubscriptionStatus.PAST_DUE);
            }
        }
    }
}
