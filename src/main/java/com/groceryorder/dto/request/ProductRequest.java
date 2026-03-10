package com.groceryorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name required") // it is used only for Strings
    private String name;

    private String description;

    @NotNull(message = "Price cannot be empty")
    @Positive(message = "Price should be greater than zero")
    private BigDecimal price;

    @NotNull(message = "quantity cannot be empty")
    @Positive(message = "Stock Quantity cannot be zero")
    private Integer quantity;

    @NotBlank(message = "Category is required")
    private String category;

}
