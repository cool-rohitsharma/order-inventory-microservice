package com.microservices.inventory.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class for creating appropriate inventory handlers
 * Implements Factory Design Pattern for extensibility
 */
@Component
@Slf4j
public class InventoryHandlerFactory {
    
    private final Map<String, InventoryHandler> handlers = new HashMap<>();
    private final InventoryHandler defaultHandler;
    
    @Autowired
    public InventoryHandlerFactory(List<InventoryHandler> inventoryHandlers,
                                 DefaultInventoryHandler defaultInventoryHandler) {
        this.defaultHandler = defaultInventoryHandler;
        
        // Register all available handlers
        for (InventoryHandler handler : inventoryHandlers) {
            handlers.put(handler.getHandlerType(), handler);
            log.info("Registered inventory handler: {}", handler.getHandlerType());
        }
    }
    
    /**
     * Get inventory handler by type
     * @param handlerType the type of handler required
     * @return the appropriate inventory handler
     */
    public InventoryHandler getHandler(String handlerType) {
        if (handlerType == null || handlerType.trim().isEmpty()) {
            log.debug("No handler type specified, returning default handler");
            return defaultHandler;
        }
        
        InventoryHandler handler = handlers.get(handlerType.toUpperCase());
        
        if (handler == null) {
            log.warn("Handler type '{}' not found, returning default handler", handlerType);
            return defaultHandler;
        }
        
        log.debug("Returning handler of type: {}", handlerType);
        return handler;
    }
    
    /**
     * Get default inventory handler
     * @return the default inventory handler
     */
    public InventoryHandler getDefaultHandler() {
        return defaultHandler;
    }
    
    /**
     * Get expiry-based inventory handler
     * @return the expiry-based inventory handler
     */
    public InventoryHandler getExpiryBasedHandler() {
        return getHandler("EXPIRY_BASED");
    }
    
    /**
     * Get all available handler types
     * @return set of available handler types
     */
    public java.util.Set<String> getAvailableHandlerTypes() {
        return handlers.keySet();
    }
}