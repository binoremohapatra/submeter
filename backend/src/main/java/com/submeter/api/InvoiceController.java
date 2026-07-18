package com.submeter.api;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.InvoiceResponse;
import com.submeter.api.dto.PaymentResponse;
import com.submeter.entity.enums.Role;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.InvoiceService;
import com.submeter.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    @GetMapping
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<CursorPageResponse<InvoiceResponse>> listInvoices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID subscriptionId
    ) {
        int safeLimit = Math.min(limit, 100);
        return ResponseEntity.ok(
                invoiceService.listInvoices(principal.getOrgId(), cursor, safeLimit, customerId, subscriptionId));
    }

    @GetMapping("/{id}")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<InvoiceResponse> getInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(invoiceService.getInvoice(principal.getOrgId(), id));
    }

    @GetMapping("/{id}/pdf")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        byte[] pdfBytes = invoiceService.generatePdf(principal.getOrgId(), id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/pay")
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<PaymentResponse> payInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        PaymentResponse response = paymentService.createPaymentOrder(principal.getOrgId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/verify")
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<Void> verifyPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload
    ) {
        // Simple hack for dev environment verification since webhooks don't reach localhost
        String orderId = payload.get("razorpay_order_id");
        String paymentId = payload.get("razorpay_payment_id");
        paymentService.processPaymentSuccess(orderId, paymentId);
        return ResponseEntity.ok().build();
    }
}
