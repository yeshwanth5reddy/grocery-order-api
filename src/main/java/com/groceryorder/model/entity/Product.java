package com.groceryorder.model.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @NotBlank(message = "Product Name is Required")
    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @NotNull(message = "price shall always be present")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "stock quantity is required")
    @Positive(message = "Quantity shall always be greater than zero")
    @Column(name = "stock_quantity", nullable = false)
    private Integer quantity;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
