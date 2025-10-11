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

/**
 * Default implementation of inventory handler
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultInventoryHandler implements InventoryHandler {
    
    private final InventoryBatchRepository inventoryBatchRepository;
    
    @Override
    public List<InventoryBatch> getInventoryBatches(String productId) {
        log.debug("Getting inventory batches for product: {}", productId);
        return inventoryBatchRepository.findByProductIdOrderByExpiryDateAsc(productId);
    }
    
    @Override
    @Transactional
    public boolean updateInventory(InventoryUpdateRequest request) {
        log.debug("Updating inventory for product: {} with quantity: {}", 
                 request.getProductId(), request.getQuantity());
        
        List<InventoryBatch> availableBatches = inventoryBatchRepository
            .findAvailableBatchesByProductId(request.getProductId(), LocalDate.now());
        
        if (availableBatches.isEmpty()) {
            log.warn("No available batches found for product: {}", request.getProductId());
            return false;
        }
        
        Integer remainingQuantity = request.getQuantity();
        
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
            
            log.debug("Updated batch {} for product {}: reduced by {}, remaining batch quantity: {}", 
                     batch.getBatchNumber(), request.getProductId(), quantityToReduce, 
                     batch.getQuantity());
        }
        
        if (remainingQuantity > 0) {
            log.warn("Insufficient inventory for product: {}. Remaining needed: {}", 
                    request.getProductId(), remainingQuantity);
            return false;
        }
        
        log.info("Successfully updated inventory for product: {}", request.getProductId());
        return true;
    }
    
    @Override
    public boolean isInventoryAvailable(String productId, Integer requiredQuantity) {
        Integer availableQuantity = inventoryBatchRepository
            .getTotalAvailableQuantity(productId, LocalDate.now());
        
        boolean isAvailable = availableQuantity >= requiredQuantity;
        
        log.debug("Inventory availability check for product {}: required={}, available={}, result={}", 
                 productId, requiredQuantity, availableQuantity, isAvailable);
        
        return isAvailable;
    }
    
    @Override
    public String getHandlerType() {
        return "DEFAULT";
    }
}