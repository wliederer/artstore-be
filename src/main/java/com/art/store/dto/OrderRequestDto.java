package com.art.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class OrderRequestDto {
    
    @NotEmpty(message = "Cart items are required")
    private List<CartItemDto> cart;
    
    @NotNull(message = "Total amount is required")
    private BigDecimal total;
    
    @NotNull(message = "Customer information is required")
    private CustomerInfoDto customerInfo;
    
    private String timestamp;
    
    public OrderRequestDto() {}
    
    // Getters and setters
    public List<CartItemDto> getCart() {
        return cart;
    }
    
    public void setCart(List<CartItemDto> cart) {
        this.cart = cart;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public CustomerInfoDto getCustomerInfo() {
        return customerInfo;
    }
    
    public void setCustomerInfo(CustomerInfoDto customerInfo) {
        this.customerInfo = customerInfo;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public static class CartItemDto {
        @NotNull(message = "Product ID is required")
        private Long id;
        
        @NotBlank(message = "Product name is required")
        private String name;
        
        @NotNull(message = "Price is required")
        private BigDecimal price;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        private String image;
        private String category;
        
        public CartItemDto() {}
        
        // Getters and setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public String getImage() {
            return image;
        }
        
        public void setImage(String image) {
            this.image = image;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
    }
    
    public static class CustomerInfoDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        
        @NotBlank(message = "First name is required")
        private String firstName;
        
        @NotBlank(message = "Last name is required")
        private String lastName;
        
        @NotBlank(message = "Address is required")
        private String address;
        
        @NotBlank(message = "City is required")
        private String city;
        
        @NotBlank(message = "State is required")
        private String state;
        
        @NotBlank(message = "Zip code is required")
        private String zipCode;
        
        @NotBlank(message = "Country is required")
        private String country;
        
        private String phone;
        
        private String billingAddress;
        private String billingCity;
        private String billingState;
        private String billingZipCode;
        private String billingCountry;
        
        @NotNull(message = "Same as billing flag is required")
        private Boolean sameAsBilling;
        
        public CustomerInfoDto() {}
        
        // Getters and setters
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getZipCode() {
            return zipCode;
        }
        
        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
        
        public String getCountry() {
            return country;
        }
        
        public void setCountry(String country) {
            this.country = country;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public String getBillingAddress() {
            return billingAddress;
        }
        
        public void setBillingAddress(String billingAddress) {
            this.billingAddress = billingAddress;
        }
        
        public String getBillingCity() {
            return billingCity;
        }
        
        public void setBillingCity(String billingCity) {
            this.billingCity = billingCity;
        }
        
        public String getBillingState() {
            return billingState;
        }
        
        public void setBillingState(String billingState) {
            this.billingState = billingState;
        }
        
        public String getBillingZipCode() {
            return billingZipCode;
        }
        
        public void setBillingZipCode(String billingZipCode) {
            this.billingZipCode = billingZipCode;
        }
        
        public String getBillingCountry() {
            return billingCountry;
        }
        
        public void setBillingCountry(String billingCountry) {
            this.billingCountry = billingCountry;
        }
        
        public Boolean getSameAsBilling() {
            return sameAsBilling;
        }
        
        public void setSameAsBilling(Boolean sameAsBilling) {
            this.sameAsBilling = sameAsBilling;
        }
    }
}