package com.microservices.inventory.service;

import com.microservices.inventory.factory.InventoryHandler;
import com.microservices.inventory.factory.InventoryHandlerFactory;
import com.microservices.inventory.model.InventoryBatch;
import com.microservices.inventory.model.InventoryUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for inventory operations
 * Uses Factory Pattern to delegate to appropriate handlers
 */
@Service
@Slf4j
public record InventoryService(InventoryHandlerFactory inventoryHandlerFactory) {

    /**
     * Get inventory batches for a product sorted by expiry date
     * Uses expiry-based handler by default for optimal inventory management
     */
    public List<InventoryBatch> getInventoryBatches(String productId) {
        log.info("Getting inventory batches for product: {}", productId);

        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }

        // Use expiry-based handler for getting inventory to show FEFO strategy
        InventoryHandler handler = inventoryHandlerFactory.getExpiryBasedHandler();

        List<InventoryBatch> batches = handler.getInventoryBatches(productId);

        log.info("Found {} inventory batches for product: {}", batches.size(), productId);
        return batches;
    }

    /**
     * Update inventory after order placement
     * Uses expiry-based handler to implement FEFO (First Expired, First Out) strategy
     */
    public boolean updateInventory(InventoryUpdateRequest request) {
        log.info("Updating inventory for product: {} with quantity: {}",
                request.getProductId(), request.getQuantity());

        if (request.getProductId() == null || request.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Use expiry-based handler for updates to implement FEFO strategy
        InventoryHandler handler = inventoryHandlerFactory.getExpiryBasedHandler();

        // Check if inventory is available before updating
        if (!handler.isInventoryAvailable(request.getProductId(), request.getQuantity())) {
            log.warn("Insufficient inventory for product: {} (required: {})",
                    request.getProductId(), request.getQuantity());
            return false;
        }

        boolean updated = handler.updateInventory(request);

        if (updated) {
            log.info("Successfully updated inventory for product: {}", request.getProductId());
        } else {
            log.error("Failed to update inventory for product: {}", request.getProductId());
        }

        return updated;
    }

    /**
     * Check if sufficient inventory is available for a product
     */
    public boolean isInventoryAvailable(String productId, Integer requiredQuantity) {
        log.debug("Checking inventory availability for product: {} (required: {})",
                productId, requiredQuantity);

        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }

        if (requiredQuantity == null || requiredQuantity <= 0) {
            throw new IllegalArgumentException("Required quantity must be positive");
        }

        InventoryHandler handler = inventoryHandlerFactory.getExpiryBasedHandler();
        return handler.isInventoryAvailable(productId, requiredQuantity);
    }

    /**
     * Get inventory handler by type (for testing or specific use cases)
     */
    public InventoryHandler getHandler(String handlerType) {
        return inventoryHandlerFactory.getHandler(handlerType);
    }
}