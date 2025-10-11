package com.microservices.order.controller;

import com.microservices.order.model.Order;
import com.microservices.order.model.OrderRequest;
import com.microservices.order.model.OrderStatus;
import com.microservices.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for order operations
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * Place a new order
     * POST /order
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        log.info("Received order request: {}", orderRequest);
        
        try {
            Order order = orderService.placeOrder(orderRequest);
            
            // Return appropriate HTTP status based on order status
            if (order.getStatus() == OrderStatus.COMPLETED) {
                log.info("Order placed successfully with ID: {}", order.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(order);
            } else {
                log.warn("Order failed with ID: {} - Status: {}", order.getId(), order.getStatus());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(order);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid order request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get order by ID
     * GET /order/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        log.debug("Getting order with ID: {}", orderId);
        
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            log.error("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all orders
     * GET /order
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.debug("Getting all orders");
        
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error getting all orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get orders by customer email
     * GET /order/customer/{customerEmail}
     */
    @GetMapping("/customer/{customerEmail}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String customerEmail) {
        log.debug("Getting orders for customer: {}", customerEmail);
        
        try {
            List<Order> orders = orderService.getOrdersByCustomerEmail(customerEmail);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            log.error("Invalid customer email: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting orders for customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get orders by status
     * GET /order/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        log.debug("Getting orders with status: {}", status);
        
        try {
            List<Order> orders = orderService.getOrdersByStatus(status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error getting orders by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Cancel an order
     * PUT /order/{orderId}/cancel
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long orderId) {
        log.info("Cancelling order with ID: {}", orderId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Order order = orderService.cancelOrder(orderId);
            
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
            response.put("order", order);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Order not found: " + e.getMessage());
            
            log.error("Order not found for cancellation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", "Cannot cancel order: " + e.getMessage());
            
            log.error("Cannot cancel order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Internal server error occurred");
            
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}