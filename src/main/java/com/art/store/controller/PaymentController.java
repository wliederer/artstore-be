package com.art.store.controller;

import com.art.store.dto.CheckoutSessionDto;
import com.art.store.dto.PaymentIntentDto;
import com.art.store.entity.Order;
import com.art.store.entity.Payment;
import com.art.store.service.OrderService;
import com.art.store.service.PaymentService;
import com.art.store.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final StripeService stripeService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    
    @Autowired
    public PaymentController(StripeService stripeService, PaymentService paymentService, OrderService orderService) {
        this.stripeService = stripeService;
        this.paymentService = paymentService;
        this.orderService = orderService;
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        return ResponseEntity.ok(Map.of(
            "publishableKey", stripeService.getPublishableKey()
        ));
    }
    
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@Valid @RequestBody PaymentIntentDto paymentIntentDto) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByOrderId(paymentIntentDto.getOrderId());
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }
            
            Order order = orderOpt.get();
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(order, paymentIntentDto.getCurrency());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "clientSecret", paymentIntent.getClientSecret(),
                "paymentIntentId", paymentIntent.getId()
            ));
            
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to create payment intent: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while processing payment"
            ));
        }
    }
    
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@Valid @RequestBody CheckoutSessionDto checkoutSessionDto) {
        try {
            Optional<Order> orderOpt = orderService.getOrderByOrderId(checkoutSessionDto.getOrderId());
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }
            
            Order order = orderOpt.get();
            Session session = paymentService.createCheckoutSession(order);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionId", session.getId(),
                "url", session.getUrl()
            ));
            
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to create checkout session: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while processing payment"
            ));
        }
    }
    
    @PostMapping("/confirm-payment")
    public ResponseEntity<?> confirmPayment(@RequestParam String paymentIntentId) {
        try {
            Payment payment = paymentService.confirmPayment(paymentIntentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment confirmed successfully",
                "paymentId", payment.getId(),
                "status", payment.getStatus()
            ));
            
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to confirm payment: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while confirming payment"
            ));
        }
    }
    
    @GetMapping("/status/{paymentIntentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentIntentId) {
        try {
            Optional<Payment> paymentOpt = paymentService.getPaymentByStripeId(paymentIntentId);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Payment payment = paymentOpt.get();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "status", payment.getStatus(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "orderId", payment.getOrder().getOrderId()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while retrieving payment status"
            ));
        }
    }
    
    @PostMapping(value = "/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody byte[] payload, 
                                               @RequestHeader("Stripe-Signature") String sigHeader,
                                               HttpServletRequest request) {
        try {
            // Convert byte array to string for processing
            String payloadString = new String(payload, "UTF-8");
            
            System.out.println("Webhook received:");
            System.out.println("Signature: " + sigHeader);
            System.out.println("Payload length: " + payload.length);
            System.out.println("Content-Type: " + request.getContentType());
            
            paymentService.handleStripeWebhook(payloadString, sigHeader);
            return ResponseEntity.ok("Webhook handled successfully");
        } catch (Exception e) {
            System.err.println("Webhook error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook handling failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionId) {
        try {
            Session session = stripeService.retrieveCheckoutSession(sessionId);
            Optional<Payment> paymentOpt = paymentService.getPaymentBySessionId(sessionId);
            
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Payment payment = paymentOpt.get();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionStatus", session.getStatus(),
                "paymentStatus", session.getPaymentStatus(),
                "orderId", payment.getOrder().getOrderId(),
                "customerEmail", session.getCustomerEmail()
            ));
            
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve session: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while retrieving session status"
            ));
        }
    }
}