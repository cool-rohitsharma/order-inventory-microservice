package com.microservices.inventory.factory;

import com.microservices.inventory.model.InventoryBatch;
import com.microservices.inventory.model.InventoryUpdateRequest;
import com.microservices.inventory.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Expiry-based inventory handler that prioritizes items with earlier expiry dates
 * This implements FEFO (First Expired, First Out) strategy
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryBasedInventoryHandler implements InventoryHandler {
    
    private final InventoryBatchRepository inventoryBatchRepository;
    
    @Override
    public List<InventoryBatch> getInventoryBatches(String productId) {
        log.debug("Getting inventory batches for product: {} using expiry-based handler", productId);
        
        List<InventoryBatch> allBatches = inventoryBatchRepository
            .findByProductIdOrderByExpiryDateAsc(productId);
        
        // Filter out expired batches for display purposes
        LocalDate currentDate = LocalDate.now();
        return allBatches.stream()
            .filter(batch -> batch.getExpiryDate().isAfter(currentDate))
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public boolean updateInventory(InventoryUpdateRequest request) {
        log.debug("Updating inventory using expiry-based strategy for product: {} with quantity: {}", 
                 request.getProductId(), request.getQuantity());
        
        // Get available batches sorted by expiry date (FEFO strategy)
        List<InventoryBatch> availableBatches = inventoryBatchRepository
            .findAvailableBatchesByProductId(request.getProductId(), LocalDate.now());
        
        if (availableBatches.isEmpty()) {
            log.warn("No available batches found for product: {}", request.getProductId());
            return false;
        }
        
        Integer remainingQuantity = request.getQuantity();
        
        // Process batches in order of expiry date (earliest first)
        for (InventoryBatch batch : availableBatches) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            Integer batchQuantity = batch.getQuantity();
            Integer quantityToReduce = Math.min(remainingQuantity, batchQuantity);
            
            // Update batch quantity
            batch.setQuantity(batchQuantity - quantityToReduce);
            inventoryBatchRepository.save(batch);
            
            remainingQuantity -= quantityToReduce;
            
            log.debug("FEFO: Updated batch {} (expires: {}) for product {}: reduced by {}, remaining batch quantity: {}", 
                     batch.getBatchNumber(), batch.getExpiryDate(), request.getProductId(), 
                     quantityToReduce, batch.getQuantity());
        }
        
        if (remainingQuantity > 0) {
            log.warn("Insufficient inventory for product: {}. Remaining needed: {}", 
                    request.getProductId(), remainingQuantity);
            return false;
        }
        
        log.info("Successfully updated inventory using FEFO strategy for product: {}", 
                request.getProductId());
        return true;
    }
    
    @Override
    public boolean isInventoryAvailable(String productId, Integer requiredQuantity) {
        // Only consider non-expired inventory
        Integer availableQuantity = inventoryBatchRepository
            .getTotalAvailableQuantity(productId, LocalDate.now());
        
        boolean isAvailable = availableQuantity >= requiredQuantity;
        
        log.debug("Expiry-based inventory availability check for product {}: required={}, available={}, result={}", 
                 productId, requiredQuantity, availableQuantity, isAvailable);
        
        return isAvailable;
    }
    
    @Override
    public String getHandlerType() {
        return "EXPIRY_BASED";
    }
}