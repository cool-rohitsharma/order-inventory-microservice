package com.microservices.inventory.repository;

import com.microservices.inventory.model.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    
    /**
     * Find all inventory batches for a product ordered by expiry date
     */
    List<InventoryBatch> findByProductIdOrderByExpiryDateAsc(String productId);
    
    /**
     * Find all batches for a product that are not expired and have quantity > 0
     */
    @Query("SELECT ib FROM InventoryBatch ib WHERE ib.productId = :productId " +
           "AND ib.expiryDate > :currentDate AND ib.quantity > 0 " +
           "ORDER BY ib.expiryDate ASC")
    List<InventoryBatch> findAvailableBatchesByProductId(@Param("productId") String productId, 
                                                        @Param("currentDate") LocalDate currentDate);
    
    /**
     * Get total available quantity for a product
     */
    @Query("SELECT COALESCE(SUM(ib.quantity), 0) FROM InventoryBatch ib " +
           "WHERE ib.productId = :productId AND ib.expiryDate > :currentDate")
    Integer getTotalAvailableQuantity(@Param("productId") String productId, 
                                    @Param("currentDate") LocalDate currentDate);
    
    /**
     * Update quantity of a specific batch
     */
    @Modifying
    @Query("UPDATE InventoryBatch ib SET ib.quantity = ib.quantity - :quantity " +
           "WHERE ib.id = :batchId AND ib.quantity >= :quantity")
    int updateBatchQuantity(@Param("batchId") Long batchId, @Param("quantity") Integer quantity);
}