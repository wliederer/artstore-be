package com.art.store.service;

import com.art.store.entity.Order;
import com.art.store.entity.OrderItem;
import com.art.store.entity.Payment;
import com.art.store.repository.OrderRepository;
import com.art.store.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {
    
    private final StripeService stripeService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
//    private final EmailService emailService;
    
    @Value("${stripe.webhook.secret:whsec_YOUR_WEBHOOK_SECRET_HERE}")
    private String webhookSecret;
    
    @Autowired
    public PaymentService(StripeService stripeService, PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.stripeService = stripeService;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
//        this.emailService = emailService;
    }
    
    public PaymentIntent createPaymentIntent(Order order, String currency) throws StripeException {
        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(order.getId());
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            // Only prevent if payment is succeeded or processing, allow if pending/failed
            if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED || 
                payment.getStatus() == Payment.PaymentStatus.PROCESSING) {
                throw new RuntimeException("Payment already processed for this order");
            }
            // If payment exists but is pending/failed, we'll update it
        }
        
        // Create metadata for the payment
        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", order.getId().toString());
        metadata.put("customer_email", order.getEmail());
        metadata.put("customer_name", order.getFirstName() + " " + order.getLastName());
        
        // Create payment intent with Stripe
        PaymentIntent paymentIntent = stripeService.createPaymentIntent(
            order.getTotalAmount(), 
            currency, 
            order.getEmail(), 
            metadata
        );
        
        // Create or update payment record
        Payment payment = existingPayment.orElse(new Payment());
        payment.setOrder(order);
        payment.setStripePaymentIntentId(paymentIntent.getId());
        // Clear checkout session ID if switching from checkout to payment intent
        payment.setStripeCheckoutSessionId(null);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(currency);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        
        paymentRepository.save(payment);
        
        // Update order payment status
        order.setPaymentStatus(Order.PaymentStatus.PROCESSING);
        orderRepository.save(order);
        
        return paymentIntent;
    }
    
    public Session createCheckoutSession(Order order) throws StripeException {
        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(order.getId());
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            // Only prevent if payment is succeeded or processing, allow if pending/failed
            if (payment.getStatus() == Payment.PaymentStatus.SUCCEEDED || 
                payment.getStatus() == Payment.PaymentStatus.PROCESSING) {
                throw new RuntimeException("Payment already processed for this order");
            }
            // If payment exists but is pending/failed, we'll update it
        }
        
        // Create line items from order items
        List<SessionCreateParams.LineItem> lineItems = order.getOrderItems().stream()
            .map(orderItem -> stripeService.createLineItem(
                orderItem.getProduct().getName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity().longValue(),
                orderItem.getProduct().getDescription()
            ))
            .collect(Collectors.toList());
        
        // Create checkout session
        Session session = stripeService.createCheckoutSession(
            order.getTotalAmount(),
            "usd",
            order.getEmail(),
            order.getId(),
            lineItems
        );
        
        // Create or update payment record
        Payment payment = existingPayment.orElse(new Payment());
        payment.setOrder(order);
        payment.setStripeCheckoutSessionId(session.getId());
        // Clear payment intent ID if switching from payment intent to checkout
        payment.setStripePaymentIntentId(null);
        
        // Payment intent may not be available immediately for checkout sessions
        if (session.getPaymentIntent() != null && !session.getPaymentIntent().isEmpty()) {
            payment.setStripePaymentIntentId(session.getPaymentIntent());
        }
        
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency("usd");
        payment.setStatus(Payment.PaymentStatus.PENDING);
        
        paymentRepository.save(payment);
        
        // Update order payment status
        order.setPaymentStatus(Order.PaymentStatus.PROCESSING);
        orderRepository.save(order);
        
        return session;
    }
    
    public Payment confirmPayment(String paymentIntentId) throws StripeException {
        // Retrieve payment from database
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentIdNotNull(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found for payment intent: " + paymentIntentId);
        }
        
        Payment payment = paymentOpt.get();
        
        // Retrieve payment intent from Stripe
        PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);
        
        // Update payment status based on Stripe status
        updatePaymentStatus(payment, paymentIntent.getStatus());
        
        return paymentRepository.save(payment);
    }
    
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByStripeId(String stripePaymentIntentId) {
        return paymentRepository.findByStripePaymentIntentIdNotNull(stripePaymentIntentId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentBySessionId(String sessionId) {
        return paymentRepository.findByStripeCheckoutSessionId(sessionId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    public void handleStripeWebhook(String payload, String sigHeader) throws Exception {
        Event event;
        
        System.out.println("Webhook secret configured: " + (webhookSecret != null && !webhookSecret.equals("whsec_YOUR_WEBHOOK_SECRET_HERE")));
        System.out.println("Webhook secret starts with 'whsec_': " + (webhookSecret != null && webhookSecret.startsWith("whsec_")));
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("Webhook signature validation successful");
        } catch (Exception e) {
            System.err.println("Webhook signature validation failed: " + e.getMessage());
            System.err.println("Expected webhook secret format: whsec_xxx");
            System.err.println("Current webhook secret: " + (webhookSecret != null ? webhookSecret.substring(0, Math.min(10, webhookSecret.length())) + "..." : "null"));
            throw new Exception("Invalid signature: " + e.getMessage(), e);
        }
        
        // Handle the event
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event);
                break;
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            default:
                System.out.println("Unhandled event type: " + event.getType());
        }
    }
    
    private void handlePaymentIntentSucceeded(Event event) {
        try {
            // Stripe v29.4.0 returns Optional<StripeObject>
            Optional<StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            
            if (stripeObjectOpt.isPresent()) {
                StripeObject stripeObject = stripeObjectOpt.get();
                
                if (stripeObject instanceof PaymentIntent) {
                    PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                    handlePaymentIntentSucceededById(paymentIntent.getId(), paymentIntent.toJson());
                } else {
                    System.err.println("Expected PaymentIntent but got: " + stripeObject.getClass().getSimpleName());
                }
            } else {
                // Fallback to raw JSON parsing when deserialization fails
                String rawJson = event.getData().getObject().toString();
                System.out.println("Falling back to raw JSON parsing for payment_intent.succeeded: " + rawJson);
                
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
                String paymentIntentId = jsonObject.get("id").getAsString();
                String status = jsonObject.get("status").getAsString();
                
                if ("succeeded".equals(status)) {
                    handlePaymentIntentSucceededById(paymentIntentId, rawJson);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to handle payment_intent.succeeded event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handlePaymentIntentSucceededById(String paymentIntentId, String stripeResponse) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentIdNotNull(paymentIntentId);
        
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setStripeResponse(stripeResponse);
            paymentRepository.save(payment);
            
            // Update order status
            Order order = payment.getOrder();
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            
            // Send email notifications
//            try {
//                emailService.sendOrderConfirmationToCustomer(order);
//                emailService.sendOrderNotificationToAdmin(order);
//            } catch (Exception e) {
//                System.err.println("Failed to send email notifications for order: " + order.getId() + ", Error: " + e.getMessage());
//            }
            
            System.out.println("Payment succeeded for payment intent: " + paymentIntentId + ", Order: " + order.getId());
        } else {
            System.err.println("Payment not found for payment intent: " + paymentIntentId);
        }
    }
    
    private void handlePaymentIntentFailed(Event event) {
        try {
            // Stripe v29.4.0 returns Optional<StripeObject>
            Optional<StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            
            if (stripeObjectOpt.isPresent()) {
                StripeObject stripeObject = stripeObjectOpt.get();
                
                if (stripeObject instanceof PaymentIntent) {
                    PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                    String failureReason = paymentIntent.getLastPaymentError() != null ? 
                        paymentIntent.getLastPaymentError().getMessage() : "Payment failed";
                    handlePaymentIntentFailedById(paymentIntent.getId(), failureReason, paymentIntent.toJson());
                } else {
                    System.err.println("Expected PaymentIntent but got: " + stripeObject.getClass().getSimpleName());
                }
            } else {
                // Fallback to raw JSON parsing when deserialization fails
                String rawJson = event.getData().getObject().toString();
                System.out.println("Falling back to raw JSON parsing for payment_intent.payment_failed: " + rawJson);
                
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
                String paymentIntentId = jsonObject.get("id").getAsString();
                
                String failureReason = "Payment failed";
                if (jsonObject.has("last_payment_error") && !jsonObject.get("last_payment_error").isJsonNull()) {
                    com.google.gson.JsonObject errorObj = jsonObject.getAsJsonObject("last_payment_error");
                    if (errorObj.has("message")) {
                        failureReason = errorObj.get("message").getAsString();
                    }
                }
                
                handlePaymentIntentFailedById(paymentIntentId, failureReason, rawJson);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle payment_intent.payment_failed event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handlePaymentIntentFailedById(String paymentIntentId, String failureReason, String stripeResponse) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentIdNotNull(paymentIntentId);
        
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            payment.setStripeResponse(stripeResponse);
            paymentRepository.save(payment);
            
            // Update order status
            Order order = payment.getOrder();
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderRepository.save(order);
            
            System.out.println("Payment failed for payment intent: " + paymentIntentId + ", Reason: " + failureReason);
        } else {
            System.err.println("Payment not found for payment intent: " + paymentIntentId);
        }
    }
    
    private void handleCheckoutSessionCompleted(Event event) {
        try {
            // Stripe v29.4.0 returns Optional<StripeObject>
            Optional<StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            
            if (stripeObjectOpt.isPresent()) {
                StripeObject stripeObject = stripeObjectOpt.get();
                
                if (stripeObject instanceof Session) {
                    Session session = (Session) stripeObject;
                    Optional<Payment> paymentOpt = paymentRepository.findByStripeCheckoutSessionId(session.getId());
                    
                    if (paymentOpt.isPresent()) {
                        Payment payment = paymentOpt.get();
                        
                        // Update payment intent ID if available
                        if (session.getPaymentIntent() != null && !session.getPaymentIntent().isEmpty()) {
                            payment.setStripePaymentIntentId(session.getPaymentIntent());
                        }
                        
                        // Update payment status based on session payment status
                        if ("paid".equals(session.getPaymentStatus())) {
                            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                            
                            // Update order status
                            Order order = payment.getOrder();
                            order.setPaymentStatus(Order.PaymentStatus.PAID);
                            order.setStatus(Order.OrderStatus.CONFIRMED);
                            orderRepository.save(order);
                            
                            // Send email notifications
//                            try {
//                                emailService.sendOrderConfirmationToCustomer(order);
//                                emailService.sendOrderNotificationToAdmin(order);
//                            } catch (Exception e) {
//                                System.err.println("Failed to send email notifications for order: " + order.getId() + ", Error: " + e.getMessage());
//                            }
                            
                            System.out.println("Checkout session completed successfully for session: " + session.getId() + ", Order: " + order.getId());
                        }
                        
                        paymentRepository.save(payment);
                    } else {
                        System.err.println("Payment not found for checkout session: " + session.getId());
                    }
                } else {
                    System.err.println("Expected Session but got: " + stripeObject.getClass().getSimpleName());
                }
            } else {
                System.err.println("Deserialization failed for checkout.session.completed event");
            }
        } catch (Exception e) {
            System.err.println("Failed to handle checkout.session.completed event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updatePaymentStatus(Payment payment, String stripeStatus) {
        switch (stripeStatus) {
            case "succeeded":
                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                payment.getOrder().setPaymentStatus(Order.PaymentStatus.PAID);
                payment.getOrder().setStatus(Order.OrderStatus.CONFIRMED);
                break;
            case "processing":
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                payment.getOrder().setPaymentStatus(Order.PaymentStatus.PROCESSING);
                break;
            case "requires_payment_method":
            case "requires_confirmation":
                payment.setStatus(Payment.PaymentStatus.PENDING);
                break;
            case "canceled":
                payment.setStatus(Payment.PaymentStatus.CANCELLED);
                payment.getOrder().setPaymentStatus(Order.PaymentStatus.FAILED);
                break;
            default:
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.getOrder().setPaymentStatus(Order.PaymentStatus.FAILED);
        }
        
        orderRepository.save(payment.getOrder());
    }
}