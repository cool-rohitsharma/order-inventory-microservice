package com.microservices.order.model;

/**
 * Enum representing the status of an order
 */
public enum OrderStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}