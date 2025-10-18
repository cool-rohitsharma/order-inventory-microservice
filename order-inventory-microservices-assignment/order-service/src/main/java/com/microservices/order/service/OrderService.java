package com.microservices.order.service;

import com.microservices.order.model.Order;
import com.microservices.order.model.OrderRequest;
import com.microservices.order.model.OrderStatus;
import com.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for order operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryServiceClient;
    
    /**
     * Place a new order
     * This method coordinates with inventory service to check availability and update stock
     */
    @Transactional
    public Order placeOrder(OrderRequest orderRequest) {
        log.info("Processing order request: {}", orderRequest);
        
        // Validate request
        if (orderRequest.getProductId().isEmpty() || orderRequest.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        
        if (orderRequest.getQuantity() == null || orderRequest.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (orderRequest.getCustomerEmail() == null || orderRequest.getCustomerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        
        // Create order entity
        Order order = Order.builder()
            .productId(orderRequest.getProductId())
            .quantity(orderRequest.getQuantity())
            .customerEmail(orderRequest.getCustomerEmail())
            .status(OrderStatus.PENDING)
            .build();
        
        // Save order with PENDING status first
        order = orderRepository.save(order);
        log.debug("Created order with ID: {}", order.getId());
        
        try {
            // Check inventory availability
            boolean inventoryAvailable = inventoryServiceClient.checkInventoryAvailability(
                orderRequest.getProductId(), 
                orderRequest.getQuantity()
            );
            
            if (!inventoryAvailable) {
                order.setStatus(OrderStatus.FAILED);
                order.setFailureReason("Insufficient inventory available");
                order = orderRepository.save(order);
                
                log.warn("Order {} failed due to insufficient inventory for product: {}", 
                        order.getId(), orderRequest.getProductId());
                return order;
            }
            
            // Update inventory
            boolean inventoryUpdated = inventoryServiceClient.updateInventory(
                orderRequest.getProductId(), 
                orderRequest.getQuantity()
            );
            
            if (!inventoryUpdated) {
                order.setStatus(OrderStatus.FAILED);
                order.setFailureReason("Failed to update inventory");
                order = orderRepository.save(order);
                
                log.error("Order {} failed due to inventory update failure for product: {}", 
                         order.getId(), orderRequest.getProductId());
                return order;
            }
            
            // Mark order as completed
            order.setStatus(OrderStatus.COMPLETED);
            order = orderRepository.save(order);
            
            log.info("Successfully completed order {} for product: {}", 
                    order.getId(), orderRequest.getProductId());
            
        } catch (Exception e) {
            // Handle any unexpected errors
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("System error: " + e.getMessage());
            order = orderRepository.save(order);
            
            log.error("Order {} failed due to system error: {}", order.getId(), e.getMessage(), e);
        }
        
        return order;
    }
    
    /**
     * Get order by ID
     */
    public Order getOrderById(Long orderId) {
        log.debug("Getting order by ID: {}", orderId);
        
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }
    
    /**
     * Get all orders for a customer
     */
    public List<Order> getOrdersByCustomerEmail(String customerEmail) {
        log.debug("Getting orders for customer: {}", customerEmail);
        
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        
        return orderRepository.findByCustomerEmailOrderByOrderDateDesc(customerEmail);
    }
    
    /**
     * Get all orders
     */
    public List<Order> getAllOrders() {
        log.debug("Getting all orders");
        return orderRepository.findAll();
    }
    
    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.debug("Getting orders by status: {}", status);
        return orderRepository.findByStatus(status);
    }
    
    /**
     * Cancel an order (if it's still pending)
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        log.info("Cancelling order with ID: {}", orderId);
        
        Order order = getOrderById(orderId);
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        
        log.info("Successfully cancelled order: {}", orderId);
        return order;
    }
}