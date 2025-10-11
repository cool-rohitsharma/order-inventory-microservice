package com.microservices.inventory.controller;

import com.microservices.inventory.model.InventoryBatch;
import com.microservices.inventory.model.InventoryUpdateRequest;
import com.microservices.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for inventory operations
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    /**
     * Get inventory batches for a product sorted by expiry date
     * GET /inventory/{productId}
     */
    @GetMapping("/{productId}")
    public ResponseEntity<List<InventoryBatch>> getInventoryBatches(
            @PathVariable String productId) {
        
        log.info("Received request to get inventory for product: {}", productId);
        
        try {
            List<InventoryBatch> batches = inventoryService.getInventoryBatches(productId);
            
            if (batches.isEmpty()) {
                log.info("No inventory batches found for product: {}", productId);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Returning {} inventory batches for product: {}", batches.size(), productId);
            return ResponseEntity.ok(batches);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for product inventory: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting inventory for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update inventory after order placement
     * POST /inventory/update
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateInventory(
            @Valid @RequestBody InventoryUpdateRequest request) {
        
        log.info("Received request to update inventory: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean updated = inventoryService.updateInventory(request);
            
            if (updated) {
                response.put("success", true);
                response.put("message", "Inventory updated successfully");
                response.put("productId", request.getProductId());
                response.put("quantityReduced", request.getQuantity());
                
                log.info("Successfully updated inventory for product: {}", request.getProductId());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Insufficient inventory available");
                response.put("productId", request.getProductId());
                response.put("requestedQuantity", request.getQuantity());
                
                log.warn("Failed to update inventory - insufficient stock for product: {}", 
                        request.getProductId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid request: " + e.getMessage());
            
            log.error("Invalid inventory update request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Internal server error occurred");
            
            log.error("Error updating inventory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Check inventory availability (useful for order service)
     * GET /inventory/{productId}/availability?quantity={quantity}
     */
    @GetMapping("/{productId}/availability")
    public ResponseEntity<Map<String, Object>> checkInventoryAvailability(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        
        log.debug("Checking inventory availability for product: {} (quantity: {})", 
                 productId, quantity);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean available = inventoryService.isInventoryAvailable(productId, quantity);
            
            response.put("productId", productId);
            response.put("requestedQuantity", quantity);
            response.put("available", available);
            
            if (available) {
                response.put("message", "Sufficient inventory available");
            } else {
                response.put("message", "Insufficient inventory available");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid request: " + e.getMessage());
            
            log.error("Invalid availability check request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Internal server error occurred");
            
            log.error("Error checking inventory availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}