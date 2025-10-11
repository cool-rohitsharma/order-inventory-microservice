package com.microservices.order.service;

import com.microservices.order.model.InventoryUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client service for communicating with Inventory Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;
    
    /**
     * Check if inventory is available for a product
     */
    public boolean checkInventoryAvailability(String productId, Integer quantity) {
        try {
            String url = inventoryServiceUrl + "/inventory/" + productId + "/availability?quantity=" + quantity;
            
            log.debug("Checking inventory availability at: {}", url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean available = (Boolean) responseBody.get("available");
                
                log.debug("Inventory availability response for product {}: {}", productId, available);
                return Boolean.TRUE.equals(available);
            }
            
            log.warn("Invalid response from inventory service for product {}: {}", 
                    productId, response.getStatusCode());
            return false;
            
        } catch (RestClientException e) {
            log.error("Error checking inventory availability for product {}: {}", 
                     productId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update inventory after order placement
     */
    public boolean updateInventory(String productId, Integer quantity) {
        try {
            String url = inventoryServiceUrl + "/inventory/update";
            
            InventoryUpdateRequest request = InventoryUpdateRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<InventoryUpdateRequest> entity = new HttpEntity<>(request, headers);
            
            log.debug("Updating inventory at: {} with request: {}", url, request);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");
                
                log.debug("Inventory update response for product {}: {}", productId, success);
                return Boolean.TRUE.equals(success);
            }
            
            log.warn("Failed to update inventory for product {}: {}", 
                    productId, response.getStatusCode());
            return false;
            
        } catch (RestClientException e) {
            log.error("Error updating inventory for product {}: {}", 
                     productId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get inventory batches for a product (for verification purposes)
     */
    public boolean hasInventoryBatches(String productId) {
        try {
            String url = inventoryServiceUrl + "/inventory/" + productId;
            
            log.debug("Checking inventory batches at: {}", url);
            
            ResponseEntity<Object[]> response = restTemplate.getForEntity(url, Object[].class);
            
            boolean hasInventory = response.getStatusCode().is2xxSuccessful() 
                                 && response.getBody() != null 
                                 && response.getBody().length > 0;
            
            log.debug("Inventory batches check for product {}: {}", productId, hasInventory);
            return hasInventory;
            
        } catch (RestClientException e) {
            log.error("Error checking inventory batches for product {}: {}", 
                     productId, e.getMessage(), e);
            return false;
        }
    }
}