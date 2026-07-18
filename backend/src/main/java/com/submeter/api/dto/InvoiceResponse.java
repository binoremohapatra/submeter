package com.submeter.api.dto;

import com.submeter.entity.Invoice;
import com.submeter.entity.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class InvoiceResponse {
    private final UUID id;
    private final UUID subscriptionId;
    private final String invoiceNumber;
    private final InvoiceStatus status;
    private final String customerName;
    private final String customerEmail;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final long subtotalCents;
    private final long taxCents;
    private final long totalCents;
    private final Instant dueAt;
    private final Instant paidAt;
    private final String razorpayOrderId;
    private final String pdfPath;
    private final Instant createdAt;
    private final List<InvoiceLineItemDto> lineItems;
    private final List<PaymentDto> payments;

    public InvoiceResponse(UUID id, UUID subscriptionId, String invoiceNumber, InvoiceStatus status, String customerName, String customerEmail, Instant periodStart, Instant periodEnd, long subtotalCents, long taxCents, long totalCents, Instant dueAt, Instant paidAt, String razorpayOrderId, String pdfPath, Instant createdAt, List<InvoiceLineItemDto> lineItems, List<PaymentDto> payments) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.invoiceNumber = invoiceNumber;
        this.status = status;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.subtotalCents = subtotalCents;
        this.taxCents = taxCents;
        this.totalCents = totalCents;
        this.dueAt = dueAt;
        this.paidAt = paidAt;
        this.razorpayOrderId = razorpayOrderId;
        this.pdfPath = pdfPath;
        this.createdAt = createdAt;
        this.lineItems = lineItems;
        this.payments = payments;
    }

    public static InvoiceResponse fromEntity(Invoice invoice) {
        List<InvoiceLineItemDto> items;
        try {
            items = invoice.getLineItems() == null ? List.of() :
                    invoice.getLineItems().stream()
                            .map(InvoiceLineItemDto::fromEntity)
                            .collect(Collectors.toList());
        } catch (Exception e) {
            items = List.of();
        }

        List<PaymentDto> paymentDtos;
        try {
            paymentDtos = invoice.getPayments() == null ? List.of() :
                    invoice.getPayments().stream()
                            .map(PaymentDto::fromEntity)
                            .collect(Collectors.toList());
        } catch (Exception e) {
            paymentDtos = List.of();
        }

        String custName = null, custEmail = null;
        try {
            if (invoice.getSubscription() != null && invoice.getSubscription().getCustomer() != null) {
                custName  = invoice.getSubscription().getCustomer().getName();
                custEmail = invoice.getSubscription().getCustomer().getEmail();
            }
        } catch (Exception ignored) {}

        UUID subId = null;
        try {
            if (invoice.getSubscription() != null) subId = invoice.getSubscription().getId();
        } catch (Exception ignored) {}

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .subscriptionId(subId)
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus())
                .customerName(custName)
                .customerEmail(custEmail)
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .subtotalCents(invoice.getSubtotalCents())
                .taxCents(invoice.getTaxCents())
                .totalCents(invoice.getTotalCents())
                .dueAt(invoice.getDueAt())
                .paidAt(invoice.getPaidAt())
                .razorpayOrderId(invoice.getRazorpayOrderId())
                .pdfPath(invoice.getPdfPath())
                .createdAt(invoice.getCreatedAt())
                .lineItems(items)
                .payments(paymentDtos)
                .build();
    }

    // Builder
    public static InvoiceResponseBuilder builder() {
        return new InvoiceResponseBuilder();
    }

    public static class InvoiceResponseBuilder {
        private UUID id;
        private UUID subscriptionId;
        private String invoiceNumber;
        private InvoiceStatus status;
        private String customerName;
        private String customerEmail;
        private Instant periodStart;
        private Instant periodEnd;
        private long subtotalCents;
        private long taxCents;
        private long totalCents;
        private Instant dueAt;
        private Instant paidAt;
        private String razorpayOrderId;
        private String pdfPath;
        private Instant createdAt;
        private List<InvoiceLineItemDto> lineItems;
        private List<PaymentDto> payments;

        public InvoiceResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public InvoiceResponseBuilder subscriptionId(UUID subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public InvoiceResponseBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public InvoiceResponseBuilder status(InvoiceStatus status) {
            this.status = status;
            return this;
        }

        public InvoiceResponseBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public InvoiceResponseBuilder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public InvoiceResponseBuilder periodStart(Instant periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public InvoiceResponseBuilder periodEnd(Instant periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public InvoiceResponseBuilder subtotalCents(long subtotalCents) {
            this.subtotalCents = subtotalCents;
            return this;
        }

        public InvoiceResponseBuilder taxCents(long taxCents) {
            this.taxCents = taxCents;
            return this;
        }

        public InvoiceResponseBuilder totalCents(long totalCents) {
            this.totalCents = totalCents;
            return this;
        }

        public InvoiceResponseBuilder dueAt(Instant dueAt) {
            this.dueAt = dueAt;
            return this;
        }

        public InvoiceResponseBuilder paidAt(Instant paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public InvoiceResponseBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }

        public InvoiceResponseBuilder pdfPath(String pdfPath) {
            this.pdfPath = pdfPath;
            return this;
        }

        public InvoiceResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public InvoiceResponseBuilder lineItems(List<InvoiceLineItemDto> lineItems) {
            this.lineItems = lineItems;
            return this;
        }

        public InvoiceResponseBuilder payments(List<PaymentDto> payments) {
            this.payments = payments;
            return this;
        }

        public InvoiceResponse build() {
            return new InvoiceResponse(id, subscriptionId, invoiceNumber, status, customerName, customerEmail, periodStart, periodEnd, subtotalCents, taxCents, totalCents, dueAt, paidAt, razorpayOrderId, pdfPath, createdAt, lineItems, payments);
        }
    }
}
