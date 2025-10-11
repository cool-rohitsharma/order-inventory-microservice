package com.microservices.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.inventory.model.InventoryBatch;
import com.microservices.inventory.model.InventoryUpdateRequest;
import com.microservices.inventory.repository.InventoryBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class InventoryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clear and set up test data
        inventoryBatchRepository.deleteAll();
        
        // Create test inventory batches
        InventoryBatch batch1 = InventoryBatch.builder()
            .productId("TEST001")
            .batchNumber("BATCH001")
            .quantity(100)
            .expiryDate(LocalDate.now().plusDays(30))
            .createdDate(LocalDateTime.now())
            .build();
            
        InventoryBatch batch2 = InventoryBatch.builder()
            .productId("TEST001")
            .batchNumber("BATCH002")
            .quantity(50)
            .expiryDate(LocalDate.now().plusDays(60))
            .createdDate(LocalDateTime.now())
            .build();
            
        inventoryBatchRepository.save(batch1);
        inventoryBatchRepository.save(batch2);
    }

    @Test
    void testGetInventoryBatches_Success() throws Exception {
        mockMvc.perform(get("/inventory/TEST001"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].productId", is("TEST001")))
            .andExpect(jsonPath("$[0].batchNumber", is("BATCH001")))
            .andExpect(jsonPath("$[0].quantity", is(100)))
            .andExpect(jsonPath("$[1].productId", is("TEST001")))
            .andExpect(jsonPath("$[1].batchNumber", is("BATCH002")))
            .andExpect(jsonPath("$[1].quantity", is(50)));
    }

    @Test
    void testGetInventoryBatches_NotFound() throws Exception {
        mockMvc.perform(get("/inventory/NONEXISTENT"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateInventory_Success() throws Exception {
        InventoryUpdateRequest request = InventoryUpdateRequest.builder()
            .productId("TEST001")
            .quantity(25)
            .build();

        mockMvc.perform(post("/inventory/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.message", is("Inventory updated successfully")))
            .andExpect(jsonPath("$.productId", is("TEST001")))
            .andExpect(jsonPath("$.quantityReduced", is(25)));
    }

    @Test
    void testUpdateInventory_InsufficientStock() throws Exception {
        InventoryUpdateRequest request = InventoryUpdateRequest.builder()
            .productId("TEST001")
            .quantity(200) // More than available (150)
            .build();

        mockMvc.perform(post("/inventory/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", is("Insufficient inventory available")));
    }

    @Test
    void testUpdateInventory_InvalidRequest() throws Exception {
        InventoryUpdateRequest request = InventoryUpdateRequest.builder()
            .productId(null)
            .quantity(25)
            .build();

        mockMvc.perform(post("/inventory/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckInventoryAvailability_Available() throws Exception {
        mockMvc.perform(get("/inventory/TEST001/availability")
                .param("quantity", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId", is("TEST001")))
            .andExpect(jsonPath("$.requestedQuantity", is(50)))
            .andExpect(jsonPath("$.available", is(true)))
            .andExpect(jsonPath("$.message", is("Sufficient inventory available")));
    }

    @Test
    void testCheckInventoryAvailability_NotAvailable() throws Exception {
        mockMvc.perform(get("/inventory/TEST001/availability")
                .param("quantity", "200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId", is("TEST001")))
            .andExpect(jsonPath("$.requestedQuantity", is(200)))
            .andExpect(jsonPath("$.available", is(false)))
            .andExpect(jsonPath("$.message", is("Insufficient inventory available")));
    }

    @Test
    void testCheckInventoryAvailability_InvalidQuantity() throws Exception {
        mockMvc.perform(get("/inventory/TEST001/availability")
                .param("quantity", "-5"))
            .andExpect(status().isBadRequest());
    }
}