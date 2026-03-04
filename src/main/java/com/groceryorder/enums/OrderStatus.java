package com.groceryorder.enums;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    RETURNED,
    CANCELLED
}
// we are using enum instead of string to ensure type safety