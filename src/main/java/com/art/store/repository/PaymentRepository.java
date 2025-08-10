package com.art.store.repository;

import com.art.store.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    @Query("SELECT p FROM Payment p WHERE p.stripePaymentIntentId = :paymentIntentId AND p.stripePaymentIntentId IS NOT NULL")
    Optional<Payment> findByStripePaymentIntentIdNotNull(@Param("paymentIntentId") String stripePaymentIntentId);
    
    Optional<Payment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
    
    Optional<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.order.email = :email ORDER BY p.createdAt DESC")
    List<Payment> findByCustomerEmail(@Param("email") String email);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") Payment.PaymentStatus status);
}