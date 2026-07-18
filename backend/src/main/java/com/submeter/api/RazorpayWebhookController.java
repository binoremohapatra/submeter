package com.submeter.api;

import com.razorpay.Utils;
import com.submeter.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);
    private final PaymentService paymentService;

    @Value("${app.razorpay.webhook-secret:rzp_webhook_secret_stub}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        log.info("Received Razorpay Webhook");
        
        try {
            if (!webhookSecret.equals("rzp_webhook_secret_stub")) {
                boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
                if (!isValid) {
                    log.warn("Invalid Razorpay webhook signature");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
                }
            }

            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            JSONObject paymentEntity = json.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");
            
            String razorpayOrderId = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");

            if ("payment.captured".equals(event)) {
                paymentService.processPaymentSuccess(razorpayOrderId, razorpayPaymentId);
            } else if ("payment.failed".equals(event)) {
                paymentService.processPaymentFailure(razorpayOrderId, razorpayPaymentId);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            // Always return 200 to prevent Razorpay from endlessly retrying our bug
            return ResponseEntity.ok("Processed with errors");
        }
    }
}
