package com.microservices.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @NotNull
    @Column(name = "batch_number", nullable = false, unique = true)
    private String batchNumber;
    
    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}