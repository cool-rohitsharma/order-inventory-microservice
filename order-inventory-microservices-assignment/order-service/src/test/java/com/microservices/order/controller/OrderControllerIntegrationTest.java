package com.microservices.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderRequest;
import com.microservices.order.model.OrderStatus;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.service.InventoryServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clear test data
        orderRepository.deleteAll();
        
        // Set up test order
        Order testOrder = Order.builder()
            .productId("TEST001")
            .quantity(5)
            .customerEmail("test@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.COMPLETED)
            .build();
            
        orderRepository.save(testOrder);
    }

    @Test
    void testPlaceOrder_Success() throws Exception {
        // Arrange
        OrderRequest orderRequest = OrderRequest.builder()
            .productId("PROD001")
            .quantity(10)
            .customerEmail("customer@example.com")
            .build();

        when(inventoryServiceClient.checkInventoryAvailability(anyString(), anyInt())).thenReturn(true);
        when(inventoryServiceClient.updateInventory(anyString(), anyInt())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId", is("PROD001")))
            .andExpect(jsonPath("$.quantity", is(10)))
            .andExpect(jsonPath("$.customerEmail", is("customer@example.com")))
            .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testPlaceOrder_InsufficientInventory() throws Exception {
        // Arrange
        OrderRequest orderRequest = OrderRequest.builder()
            .productId("PROD001")
            .quantity(100)
            .customerEmail("customer@example.com")
            .build();

        when(inventoryServiceClient.checkInventoryAvailability(anyString(), anyInt())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.productId", is("PROD001")))
            .andExpect(jsonPath("$.quantity", is(100)))
            .andExpect(jsonPath("$.customerEmail", is("customer@example.com")))
            .andExpect(jsonPath("$.status", is("FAILED")))
            .andExpect(jsonPath("$.failureReason", is("Insufficient inventory available")));
    }

    @Test
    void testPlaceOrder_InvalidRequest() throws Exception {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
            .productId(null)
            .quantity(10)
            .customerEmail("customer@example.com")
            .build();

        // Act & Assert
        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrder_Success() throws Exception {
        // Arrange
        Order savedOrder = orderRepository.findAll().get(0);

        // Act & Assert
        mockMvc.perform(get("/order/" + savedOrder.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(savedOrder.getId().intValue())))
            .andExpect(jsonPath("$.productId", is("TEST001")))
            .andExpect(jsonPath("$.quantity", is(5)))
            .andExpect(jsonPath("$.customerEmail", is("test@example.com")))
            .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testGetOrder_NotFound() throws Exception {
        mockMvc.perform(get("/order/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllOrders() throws Exception {
        mockMvc.perform(get("/order"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].productId", is("TEST001")))
            .andExpect(jsonPath("$[0].quantity", is(5)))
            .andExpect(jsonPath("$[0].customerEmail", is("test@example.com")));
    }

    @Test
    void testGetOrdersByCustomer() throws Exception {
        mockMvc.perform(get("/order/customer/test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].customerEmail", is("test@example.com")));
    }

    @Test
    void testGetOrdersByStatus() throws Exception {
        mockMvc.perform(get("/order/status/COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status", is("COMPLETED")));
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        // Arrange - Create a pending order
        Order pendingOrder = Order.builder()
            .productId("PENDING001")
            .quantity(3)
            .customerEmail("pending@example.com")
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.PENDING)
            .build();
        Order savedPendingOrder = orderRepository.save(pendingOrder);

        // Act & Assert
        mockMvc.perform(put("/order/" + savedPendingOrder.getId() + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.message", is("Order cancelled successfully")))
            .andExpect(jsonPath("$.order.status", is("CANCELLED")));
    }

    @Test
    void testCancelOrder_AlreadyCompleted() throws Exception {
        // Arrange
        Order completedOrder = orderRepository.findAll().get(0); // This is already completed

        // Act & Assert
        mockMvc.perform(put("/order/" + completedOrder.getId() + "/cancel"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", containsString("Cannot cancel order")));
    }

    @Test
    void testCancelOrder_NotFound() throws Exception {
        mockMvc.perform(put("/order/999/cancel"))
            .andExpect(status().isNotFound());
    }
}