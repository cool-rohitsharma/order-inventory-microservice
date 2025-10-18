package com.microservices.order.service;

import com.microservices.order.model.Order;
import com.microservices.order.model.OrderRequest;
import com.microservices.order.model.OrderStatus;
import com.microservices.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private Order pendingOrder;
    private Order completedOrder;

    @BeforeEach
    void setUp() {
        orderRequest = OrderRequest.builder()
            .productId(null)
            .quantity(10)
            .customerEmail("customer@example.com")
            .build();

        pendingOrder = Order.builder()
            .id(1L)
            .productId("PROD001")
            .quantity(10)
            .customerEmail("customer@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.PENDING)
            .build();

        completedOrder = Order.builder()
            .id(1L)
            .productId("PROD001")
            .quantity(10)
            .customerEmail("customer@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.COMPLETED)
            .build();
    }

    @Test
    void testPlaceOrder_Success() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder, completedOrder);
        when(inventoryServiceClient.checkInventoryAvailability("PROD001", 10)).thenReturn(true);
        when(inventoryServiceClient.updateInventory("PROD001", 10)).thenReturn(true);

        // Act
        Order result = orderService.placeOrder(orderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        assertEquals("PROD001", result.getProductId());
        assertEquals(10, result.getQuantity());
        assertEquals("customer@example.com", result.getCustomerEmail());

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(inventoryServiceClient).checkInventoryAvailability("PROD001", 10);
        verify(inventoryServiceClient).updateInventory("PROD001", 10);
    }

    @Test
    void testPlaceOrder_InsufficientInventory() {
        // Arrange
        Order failedOrder = Order.builder()
            .id(1L)
            .productId("PROD001")
            .quantity(10)
            .customerEmail("customer@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.FAILED)
            .failureReason("Insufficient inventory available")
            .build();

        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder, failedOrder);
        when(inventoryServiceClient.checkInventoryAvailability("PROD001", 12)).thenReturn(false);

        // Act
        Order result = orderService.placeOrder(orderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("Insufficient inventory available", result.getFailureReason());

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(inventoryServiceClient).checkInventoryAvailability("PROD001", 10);
        verify(inventoryServiceClient, never()).updateInventory(anyString(), anyInt());
    }

    @Test
    void testPlaceOrder_InventoryUpdateFailed() {
        // Arrange
        Order failedOrder = Order.builder()
            .id(1L)
            .productId("PROD001")
            .quantity(10)
            .customerEmail("customer@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.FAILED)
            .failureReason("Failed to update inventory")
            .build();

        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder, failedOrder);
        when(inventoryServiceClient.checkInventoryAvailability("PROD001", 10)).thenReturn(true);
        when(inventoryServiceClient.updateInventory("PROD001", 10)).thenReturn(false);

        // Act
        Order result = orderService.placeOrder(orderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("Failed to update inventory", result.getFailureReason());

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(inventoryServiceClient).checkInventoryAvailability("PROD001", 10);
        verify(inventoryServiceClient).updateInventory("PROD001", 10);
    }

    @Test
    void testPlaceOrder_NullProductId() {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
            .productId(null)
            .quantity(10)
            .customerEmail("customer@example.com")
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(invalidRequest);
        });

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void testPlaceOrder_NullQuantity() {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
            .productId("PROD001")
            .quantity(null)
            .customerEmail("customer@example.com")
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(invalidRequest);
        });

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void testPlaceOrder_NegativeQuantity() {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
            .productId("PROD001")
            .quantity(-5)
            .customerEmail("customer@example.com")
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(invalidRequest);
        });

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void testGetOrderById_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(completedOrder));

        // Act
        Order result = orderService.getOrderById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PROD001", result.getProductId());

        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrderById_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getOrderById(999L);
        });

        verify(orderRepository).findById(999L);
    }

    @Test
    void testGetOrdersByCustomerEmail_Success() {
        // Arrange
        List<Order> expectedOrders = Arrays.asList(completedOrder);
        when(orderRepository.findByCustomerEmailOrderByOrderDateDesc("customer@example.com"))
            .thenReturn(expectedOrders);

        // Act
        List<Order> result = orderService.getOrdersByCustomerEmail("customer@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("customer@example.com", result.get(0).getCustomerEmail());

        verify(orderRepository).findByCustomerEmailOrderByOrderDateDesc("customer@example.com");
    }

    @Test
    void testGetOrdersByCustomerEmail_NullEmail() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getOrdersByCustomerEmail(null);
        });

        verifyNoInteractions(orderRepository);
    }

    @Test
    void testGetOrdersByStatus_Success() {
        // Arrange
        List<Order> expectedOrders = Arrays.asList(completedOrder);
        when(orderRepository.findByStatus(OrderStatus.COMPLETED)).thenReturn(expectedOrders);

        // Act
        List<Order> result = orderService.getOrdersByStatus(OrderStatus.COMPLETED);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(OrderStatus.COMPLETED, result.get(0).getStatus());

        verify(orderRepository).findByStatus(OrderStatus.COMPLETED);
    }

    @Test
    void testCancelOrder_Success() {
        // Arrange
        Order cancelledOrder = Order.builder()
            .id(1L)
            .productId("PROD001")
            .quantity(10)
            .customerEmail("customer@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.CANCELLED)
            .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // Act
        Order result = orderService.cancelOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCancelOrder_AlreadyCompleted() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(completedOrder));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(1L);
        });

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }
}