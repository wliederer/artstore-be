package com.art.store.controller;

import com.art.store.dto.OrderRequestDto;
import com.art.store.entity.Order;
import com.art.store.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Validated
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    // TODO: Consider adding rate limiting to prevent order spam
    // TODO: Add request size limits to prevent large payload attacks
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequestDto orderRequest) {
        try {
            // Log order creation attempt (without sensitive data)
            String email = orderRequest.getCustomerInfo() != null ? orderRequest.getCustomerInfo().getEmail() : null;
            logger.info("Order creation requested for email: {}", 
                email != null ? email.replaceAll("(.{2})(.*)(@.*)", "$1***$3") : "null");
            
            // Validate request data
            if (orderRequest == null) {
                logger.warn("Order creation attempted with null request");
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order request cannot be empty"
                ));
            }
            
            Order createdOrder = orderService.createOrder(orderRequest);
            
            logger.info("Order created successfully with ID: {}", createdOrder.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Order created successfully",
                "orderId", createdOrder.getOrderId(),
                "status", createdOrder.getStatus()
            ));
        } catch (RuntimeException e) {
            logger.warn("Order creation failed with runtime exception: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Order creation failed with unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while processing your order"
            ));
        }
    }
    
//    @GetMapping
//    public ResponseEntity<List<Order>> getAllOrders() {
//        List<Order> orders = orderService.getAllOrders();
//        return ResponseEntity.ok(orders);
//    }
    
    // TODO: Add authentication to verify customer can only access their own orders
    // TODO: Consider masking sensitive customer data in response
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderByOrderId(@PathVariable @NotBlank String orderId) {
        try {
            logger.info("Order retrieval requested for orderId: {}", orderId);
            
            // Additional validation
            if (orderId == null || orderId.trim().isEmpty()) {
                logger.warn("Invalid order ID requested: {}", orderId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid order ID"
                ));
            }
            
            return orderService.getOrderByOrderId(orderId.trim())
                    .map(order -> {
                        logger.info("Order found and returned for orderId: {}", orderId);
                        return ResponseEntity.ok().body((Object) order);
                    })
                    .orElseGet(() -> {
                        logger.warn("Order not found for orderId: {}", orderId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "success", false,
                            "message", "Order not found"
                        ));
                    });
        } catch (Exception e) {
            logger.error("Error retrieving order with orderId: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "An error occurred while retrieving the order"
            ));
        }
    }
    
    // COMMENTED OUT - Not used by frontend and would expose all customer orders
    // This endpoint could be a privacy/security risk without proper authentication
    // @GetMapping("/customer/{email}")
    // public ResponseEntity<List<Order>> getOrdersByEmail(@PathVariable String email) {
    //     List<Order> orders = orderService.getOrdersByEmail(email);
    //     return ResponseEntity.ok(orders);
    // }
    
    // COMMENTED OUT - Not used by frontend and exposes all orders by status
    // This is an admin-level endpoint that should require authentication
    // @GetMapping("/status/{status}")
    // public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
    //     try {
    //         Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
    //         List<Order> orders = orderService.getOrdersByStatus(orderStatus);
    //         return ResponseEntity.ok(orders);
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().build();
    //     }
    // }
    
    // COMMENTED OUT - Not used by frontend and is admin functionality
    // This endpoint modifies order state and should require admin authentication
    // @PutMapping("/{id}/status")
    // public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, 
    //                                               @RequestParam String status) {
    //     try {
    //         Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
    //         return orderService.updateOrderStatus(id, orderStatus)
    //                 .map(order -> ResponseEntity.ok(order))
    //                 .orElse(ResponseEntity.notFound().build());
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().build();
    //     }
    // }
    
    // COMMENTED OUT - Not used by frontend and allows anyone to cancel orders
    // This endpoint should require customer authentication to verify order ownership
    // @PutMapping("/{id}/cancel")
    // public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id) {
    //     if (orderService.cancelOrder(id)) {
    //         return ResponseEntity.ok(Map.of(
    //             "success", true,
    //             "message", "Order cancelled successfully"
    //         ));
    //     }
    //     return ResponseEntity.badRequest().body(Map.of(
    //         "success", false,
    //         "message", "Order cannot be cancelled or does not exist"
    //     ));
    // }
    
    // COMMENTED OUT - Not used by frontend and exposes business metrics
    // This is admin/analytics functionality that should require admin authentication
    // @GetMapping("/stats/count")
    // public ResponseEntity<Map<String, Long>> getOrderCountsByStatus() {
    //     Map<String, Long> stats = Map.of(
    //         "pending", orderService.getOrderCountByStatus(Order.OrderStatus.PENDING),
    //         "confirmed", orderService.getOrderCountByStatus(Order.OrderStatus.CONFIRMED),
    //         "processing", orderService.getOrderCountByStatus(Order.OrderStatus.PROCESSING),
    //         "shipped", orderService.getOrderCountByStatus(Order.OrderStatus.SHIPPED),
    //         "delivered", orderService.getOrderCountByStatus(Order.OrderStatus.DELIVERED),
    //         "cancelled", orderService.getOrderCountByStatus(Order.OrderStatus.CANCELLED)
    //     );
    //     return ResponseEntity.ok(stats);
    // }
}