package com.microservices.inventory.service;

import com.microservices.inventory.factory.InventoryHandler;
import com.microservices.inventory.factory.InventoryHandlerFactory;
import com.microservices.inventory.model.InventoryBatch;
import com.microservices.inventory.model.InventoryUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryHandlerFactory inventoryHandlerFactory;

    @Mock
    private InventoryHandler inventoryHandler;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryBatch batch1;
    private InventoryBatch batch2;
    private InventoryUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        batch1 = InventoryBatch.builder()
            .id(1L)
            .productId("PROD001")
            .batchNumber("BATCH001")
            .quantity(100)
            .expiryDate(LocalDate.now().plusDays(30))
            .createdDate(LocalDateTime.now())
            .build();

        batch2 = InventoryBatch.builder()
            .id(2L)
            .productId("PROD001")
            .batchNumber("BATCH002")
            .quantity(50)
            .expiryDate(LocalDate.now().plusDays(60))
            .createdDate(LocalDateTime.now())
            .build();

        updateRequest = InventoryUpdateRequest.builder()
            .productId("PROD001")
            .quantity(25)
            .build();
    }

    @Test
    void testGetInventoryBatches_Success() {
        // Arrange
        List<InventoryBatch> expectedBatches = Arrays.asList(batch1, batch2);
        when(inventoryHandlerFactory.getExpiryBasedHandler()).thenReturn(inventoryHandler);
        when(inventoryHandler.getInventoryBatches("PROD001")).thenReturn(expectedBatches);

        // Act
        List<InventoryBatch> result = inventoryService.getInventoryBatches("PROD001");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("PROD001", result.get(0).getProductId());
        assertEquals("BATCH001", result.get(0).getBatchNumber());
        
        verify(inventoryHandlerFactory).getExpiryBasedHandler();
        verify(inventoryHandler).getInventoryBatches("PROD001");
    }

    @Test
    void testGetInventoryBatches_NullProductId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.getInventoryBatches(null);
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }

    @Test
    void testGetInventoryBatches_EmptyProductId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.getInventoryBatches("  ");
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }

    @Test
    void testUpdateInventory_Success() {
        // Arrange
        when(inventoryHandlerFactory.getExpiryBasedHandler()).thenReturn(inventoryHandler);
        when(inventoryHandler.isInventoryAvailable("PROD001", 25)).thenReturn(true);
        when(inventoryHandler.updateInventory(updateRequest)).thenReturn(true);

        // Act
        boolean result = inventoryService.updateInventory(updateRequest);

        // Assert
        assertTrue(result);
        
        verify(inventoryHandlerFactory).getExpiryBasedHandler();
        verify(inventoryHandler).isInventoryAvailable("PROD001", 25);
        verify(inventoryHandler).updateInventory(updateRequest);
    }

    @Test
    void testUpdateInventory_InsufficientInventory() {
        // Arrange
        when(inventoryHandlerFactory.getExpiryBasedHandler()).thenReturn(inventoryHandler);
        when(inventoryHandler.isInventoryAvailable("PROD001", 25)).thenReturn(false);

        // Act
        boolean result = inventoryService.updateInventory(updateRequest);

        // Assert
        assertFalse(result);
        
        verify(inventoryHandlerFactory).getExpiryBasedHandler();
        verify(inventoryHandler).isInventoryAvailable("PROD001", 25);
        verify(inventoryHandler, never()).updateInventory(any());
    }

    @Test
    void testUpdateInventory_NullProductId() {
        // Arrange
        InventoryUpdateRequest invalidRequest = InventoryUpdateRequest.builder()
            .productId(null)
            .quantity(25)
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.updateInventory(invalidRequest);
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }

    @Test
    void testUpdateInventory_NullQuantity() {
        // Arrange
        InventoryUpdateRequest invalidRequest = InventoryUpdateRequest.builder()
            .productId("PROD001")
            .quantity(null)
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.updateInventory(invalidRequest);
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }

    @Test
    void testUpdateInventory_NegativeQuantity() {
        // Arrange
        InventoryUpdateRequest invalidRequest = InventoryUpdateRequest.builder()
            .productId("PROD001")
            .quantity(-5)
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.updateInventory(invalidRequest);
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }

    @Test
    void testIsInventoryAvailable_Available() {
        // Arrange
        when(inventoryHandlerFactory.getExpiryBasedHandler()).thenReturn(inventoryHandler);
        when(inventoryHandler.isInventoryAvailable("PROD001", 50)).thenReturn(true);

        // Act
        boolean result = inventoryService.isInventoryAvailable("PROD001", 50);

        // Assert
        assertTrue(result);
        
        verify(inventoryHandlerFactory).getExpiryBasedHandler();
        verify(inventoryHandler).isInventoryAvailable("PROD001", 50);
    }

    @Test
    void testIsInventoryAvailable_NotAvailable() {
        // Arrange
        when(inventoryHandlerFactory.getExpiryBasedHandler()).thenReturn(inventoryHandler);
        when(inventoryHandler.isInventoryAvailable("PROD001", 200)).thenReturn(false);

        // Act
        boolean result = inventoryService.isInventoryAvailable("PROD001", 200);

        // Assert
        assertFalse(result);
        
        verify(inventoryHandlerFactory).getExpiryBasedHandler();
        verify(inventoryHandler).isInventoryAvailable("PROD001", 200);
    }

    @Test
    void testIsInventoryAvailable_NullProductId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.isInventoryAvailable(null, 50);
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }

    @Test
    void testIsInventoryAvailable_NullQuantity() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.isInventoryAvailable("PROD001", null);
        });
        
        verifyNoInteractions(inventoryHandlerFactory);
    }
}