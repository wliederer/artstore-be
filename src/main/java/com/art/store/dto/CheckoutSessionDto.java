package com.art.store.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class CheckoutSessionDto {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    private String successUrl;
    private String cancelUrl;
    
    public CheckoutSessionDto() {}
    
    public CheckoutSessionDto(String orderId) {
        this.orderId = orderId;
    }
    
    // Getters and setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getSuccessUrl() {
        return successUrl;
    }
    
    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }
    
    public String getCancelUrl() {
        return cancelUrl;
    }
    
    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}