package com.art.store.service;

import com.art.store.dto.OrderRequestDto;
import com.art.store.entity.Order;
import com.art.store.entity.OrderItem;
import com.art.store.entity.Product;
import com.art.store.repository.OrderRepository;
import com.art.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    
    @Autowired
    public OrderService(OrderRepository orderRepository, 
                       ProductRepository productRepository,
                       ProductService productService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }
    
    public Order createOrder(OrderRequestDto orderRequest) {
        // Create order entity
        Order order = new Order();
        OrderRequestDto.CustomerInfoDto customerInfo = orderRequest.getCustomerInfo();
        
        // Set customer information
        order.setEmail(customerInfo.getEmail());
        order.setFirstName(customerInfo.getFirstName());
        order.setLastName(customerInfo.getLastName());
        order.setAddress(customerInfo.getAddress());
        order.setCity(customerInfo.getCity());
        order.setState(customerInfo.getState());
        order.setZipCode(customerInfo.getZipCode());
        order.setCountry(customerInfo.getCountry());
        order.setPhone(customerInfo.getPhone());
        
        // Set billing information
        order.setSameAsBilling(customerInfo.getSameAsBilling());
        if (!customerInfo.getSameAsBilling()) {
            order.setBillingAddress(customerInfo.getBillingAddress());
            order.setBillingCity(customerInfo.getBillingCity());
            order.setBillingState(customerInfo.getBillingState());
            order.setBillingZipCode(customerInfo.getBillingZipCode());
            order.setBillingCountry(customerInfo.getBillingCountry());
        }
        
        order.setTotalAmount(orderRequest.getTotal());
        order.setStatus(Order.OrderStatus.PENDING);
        
        // Save order first to get ID
        Order savedOrder = orderRepository.save(order);
        
        // Create order items
        for (OrderRequestDto.CartItemDto cartItem : orderRequest.getCart()) {
            Optional<Product> productOpt = productRepository.findById(cartItem.getId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                // Check stock availability
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                
                // Create order item
                OrderItem orderItem = new OrderItem(product, cartItem.getQuantity(), cartItem.getPrice());
                savedOrder.addOrderItem(orderItem);
                
                // Reduce product stock
                productService.reduceStock(product.getId(), cartItem.getQuantity());
            } else {
                throw new RuntimeException("Product not found with ID: " + cartItem.getId());
            }
        }
        
        // Save order with items
        return orderRepository.save(savedOrder);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public Optional<Order> updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(newStatus);
                    return orderRepository.save(order);
                });
    }
    
    public boolean cancelOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    if (order.getStatus() == Order.OrderStatus.PENDING || 
                        order.getStatus() == Order.OrderStatus.CONFIRMED) {
                        
                        // Restore stock for all items
                        for (OrderItem item : order.getOrderItems()) {
                            Product product = item.getProduct();
                            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                            productRepository.save(product);
                        }
                        
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public Long getOrderCountByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
}