package com.microservices.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private InventoryServiceClient inventoryServiceClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryServiceClient, "inventoryServiceUrl", "http://localhost:8081");
    }

    @Test
    void testCheckInventoryAvailability_Available() {
        // Arrange
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("available", true);
        responseBody.put("productId", "PROD001");
        responseBody.put("requestedQuantity", 10);

        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.checkInventoryAvailability("PROD001", 10);

        // Assert
        assertTrue(result);
        verify(restTemplate).getForEntity(
            "http://localhost:8081/inventory/PROD001/availability?quantity=10", 
            Map.class
        );
    }

    @Test
    void testCheckInventoryAvailability_NotAvailable() {
        // Arrange
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("available", false);
        responseBody.put("productId", "PROD001");
        responseBody.put("requestedQuantity", 100);

        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.checkInventoryAvailability("PROD001", 100);

        // Assert
        assertFalse(result);
        verify(restTemplate).getForEntity(
            "http://localhost:8081/inventory/PROD001/availability?quantity=100", 
            Map.class
        );
    }

    @Test
    void testCheckInventoryAvailability_ServiceError() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RestClientException("Service unavailable"));

        // Act
        boolean result = inventoryServiceClient.checkInventoryAvailability("PROD001", 10);

        // Assert
        assertFalse(result);
        verify(restTemplate).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    void testCheckInventoryAvailability_InvalidResponse() {
        // Arrange
        ResponseEntity<Map> response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.checkInventoryAvailability("PROD001", 10);

        // Assert
        assertFalse(result);
        verify(restTemplate).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    void testUpdateInventory_Success() {
        // Arrange
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", "Inventory updated successfully");

        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.updateInventory("PROD001", 10);

        // Assert
        assertTrue(result);
        verify(restTemplate).postForEntity(
            eq("http://localhost:8081/inventory/update"), 
            any(), 
            eq(Map.class)
        );
    }

    @Test
    void testUpdateInventory_Failed() {
        // Arrange
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("message", "Insufficient inventory");

        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.CONFLICT);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.updateInventory("PROD001", 100);

        // Assert
        assertFalse(result);
        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void testUpdateInventory_ServiceError() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new RestClientException("Service unavailable"));

        // Act
        boolean result = inventoryServiceClient.updateInventory("PROD001", 10);

        // Assert
        assertFalse(result);
        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void testHasInventoryBatches_HasInventory() {
        // Arrange
        Object[] responseBody = new Object[]{
            Map.of("id", 1, "productId", "PROD001", "quantity", 100)
        };

        ResponseEntity<Object[]> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Object[].class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.hasInventoryBatches("PROD001");

        // Assert
        assertTrue(result);
        verify(restTemplate).getForEntity(
            "http://localhost:8081/inventory/PROD001", 
            Object[].class
        );
    }

    @Test
    void testHasInventoryBatches_NoInventory() {
        // Arrange
        Object[] responseBody = new Object[]{};

        ResponseEntity<Object[]> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Object[].class))).thenReturn(response);

        // Act
        boolean result = inventoryServiceClient.hasInventoryBatches("PROD001");

        // Assert
        assertFalse(result);
        verify(restTemplate).getForEntity(anyString(), eq(Object[].class));
    }

    @Test
    void testHasInventoryBatches_ServiceError() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(Object[].class)))
            .thenThrow(new RestClientException("Service unavailable"));

        // Act
        boolean result = inventoryServiceClient.hasInventoryBatches("PROD001");

        // Assert
        assertFalse(result);
        verify(restTemplate).getForEntity(anyString(), eq(Object[].class));
    }
}