package com.microservices.inventory.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryHandlerFactoryTest {

    @Mock
    private DefaultInventoryHandler defaultInventoryHandler;

    @Mock
    private ExpiryBasedInventoryHandler expiryBasedInventoryHandler;

    private InventoryHandlerFactory factory;

    @BeforeEach
    void setUp() {
        when(defaultInventoryHandler.getHandlerType()).thenReturn("DEFAULT");
        when(expiryBasedInventoryHandler.getHandlerType()).thenReturn("EXPIRY_BASED");
        
        factory = new InventoryHandlerFactory(
            Arrays.asList(defaultInventoryHandler, expiryBasedInventoryHandler),
            defaultInventoryHandler
        );
    }

    @Test
    void testGetHandler_DefaultType() {
        // Act
        InventoryHandler result = factory.getHandler("DEFAULT");

        // Assert
        assertNotNull(result);
        assertEquals(defaultInventoryHandler, result);
    }

    @Test
    void testGetHandler_ExpiryBasedType() {
        // Act
        InventoryHandler result = factory.getHandler("EXPIRY_BASED");

        // Assert
        assertNotNull(result);
        assertEquals(expiryBasedInventoryHandler, result);
    }

    @Test
    void testGetHandler_CaseInsensitive() {
        // Act
        InventoryHandler result = factory.getHandler("expiry_based");

        // Assert
        assertNotNull(result);
        assertEquals(expiryBasedInventoryHandler, result);
    }

    @Test
    void testGetHandler_NullType() {
        // Act
        InventoryHandler result = factory.getHandler(null);

        // Assert
        assertNotNull(result);
        assertEquals(defaultInventoryHandler, result);
    }

    @Test
    void testGetHandler_EmptyType() {
        // Act
        InventoryHandler result = factory.getHandler("");

        // Assert
        assertNotNull(result);
        assertEquals(defaultInventoryHandler, result);
    }

    @Test
    void testGetHandler_UnknownType() {
        // Act
        InventoryHandler result = factory.getHandler("UNKNOWN");

        // Assert
        assertNotNull(result);
        assertEquals(defaultInventoryHandler, result);
    }

    @Test
    void testGetDefaultHandler() {
        // Act
        InventoryHandler result = factory.getDefaultHandler();

        // Assert
        assertNotNull(result);
        assertEquals(defaultInventoryHandler, result);
    }

    @Test
    void testGetExpiryBasedHandler() {
        // Act
        InventoryHandler result = factory.getExpiryBasedHandler();

        // Assert
        assertNotNull(result);
        assertEquals(expiryBasedInventoryHandler, result);
    }

    @Test
    void testGetAvailableHandlerTypes() {
        // Act
        Set<String> handlerTypes = factory.getAvailableHandlerTypes();

        // Assert
        assertNotNull(handlerTypes);
        assertEquals(2, handlerTypes.size());
        assertTrue(handlerTypes.contains("DEFAULT"));
        assertTrue(handlerTypes.contains("EXPIRY_BASED"));
    }
}