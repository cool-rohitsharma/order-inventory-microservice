package com.microservices.order.repository;

import com.microservices.order.model.Order;
import com.microservices.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find orders by customer email
     */
    List<Order> findByCustomerEmailOrderByOrderDateDesc(String customerEmail);
    
    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);
    
    /**
     * Find orders by product ID
     */
    List<Order> findByProductIdOrderByOrderDateDesc(String productId);
    
    /**
     * Find orders created between dates
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);
}