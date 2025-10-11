package com.microservices.inventory.factory;

import com.microservices.inventory.model.InventoryBatch;
import com.microservices.inventory.model.InventoryUpdateRequest;

import java.util.List;

/**
 * Interface for inventory handling strategies
 * This allows for different inventory management approaches
 */
public interface InventoryHandler {
    
    /**
     * Get inventory batches for a product
     */
    List<InventoryBatch> getInventoryBatches(String productId);
    
    /**
     * Update inventory after order placement
     */
    boolean updateInventory(InventoryUpdateRequest request);
    
    /**
     * Check if sufficient inventory is available
     */
    boolean isInventoryAvailable(String productId, Integer requiredQuantity);
    
    /**
     * Get handler type identifier
     */
    String getHandlerType();
}