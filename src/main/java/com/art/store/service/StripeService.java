package com.art.store.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.publishable.key:pk_test_YOUR_PUBLISHABLE_KEY_HERE}")
    private String publishableKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public String getPublishableKey() {
        return publishableKey;
    }

    /**
     * Create a PaymentIntent for collecting payment details on the client
     */
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String customerEmail, Map<String, String> metadata) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                .setCurrency(currency)
                .setReceiptEmail(customerEmail)
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * Create a Checkout Session for hosted payment page
     */
    public Session createCheckoutSession(BigDecimal amount, String currency, String customerEmail, 
                                       Long orderId, List<SessionCreateParams.LineItem> lineItems) throws StripeException {
        
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "?success=true&session_id={CHECKOUT_SESSION_ID}&order_id=" + orderId)
                .setCancelUrl(frontendUrl + "?canceled=true&order_id=" + orderId)
                .setCustomerEmail(customerEmail)
                .addAllLineItem(lineItems)
                .setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("order_id", orderId.toString())
                        .build()
                );

        return Session.create(paramsBuilder.build());
    }

    /**
     * Create line items for Stripe Checkout from order items
     */
    public SessionCreateParams.LineItem createLineItem(String name, BigDecimal price, Long quantity, String description) {
        return SessionCreateParams.LineItem.builder()
                .setPriceData(
                    SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(price.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                        .setProductData(
                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(name)
                                .setDescription(description)
                                .build()
                        )
                        .build()
                )
                .setQuantity(quantity)
                .build();
    }

    /**
     * Retrieve a PaymentIntent by ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Retrieve a Checkout Session by ID
     */
    public Session retrieveCheckoutSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    /**
     * Confirm a PaymentIntent
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.confirm();
    }

    /**
     * Cancel a PaymentIntent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.cancel();
    }
}