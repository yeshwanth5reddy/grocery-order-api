package com.groceryorder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groceryorder.dto.request.ProductRequest;
import com.groceryorder.dto.response.ProductResponse;
import com.groceryorder.exception.ResourceNotFoundException;
import com.groceryorder.security.JwtService;
import com.groceryorder.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductRequest productRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productRequest = new ProductRequest();
        productRequest.setName("Organic Milk");
        productRequest.setDescription("Fresh organic whole milk 1L");
        productRequest.setPrice(new BigDecimal("2.49"));
        productRequest.setQuantity(50);
        productRequest.setCategory("Dairy");

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Organic Milk")
                .description("Fresh organic whole milk 1L")
                .price(new BigDecimal("2.49"))
                .quantity(50)
                .category("Dairy")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createProduct_ReturnsCreated() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Organic Milk"))
                .andExpect(jsonPath("$.price").value(2.49))
                .andExpect(jsonPath("$.category").value("Dairy"));
    }

    @Test
    void createProduct_InvalidRequest_ReturnsBadRequest() throws Exception {
        ProductRequest invalid = new ProductRequest();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_ReturnsList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Organic Milk"));
    }

    @Test
    void getProductById_ReturnsProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Organic Milk"));
    }

    @Test
    void getProductById_NotFound_Returns404() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Product")));
    }

    @Test
    void getProductsByCategory_ReturnsList() throws Exception {
        when(productService.getProductsByCategory("Dairy")).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/products/category/Dairy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateProduct_ReturnsUpdated() throws Exception {
        ProductResponse updated = ProductResponse.builder()
                .id(1L)
                .name("Organic Milk")
                .description("Updated description")
                .price(new BigDecimal("2.99"))
                .quantity(45)
                .category("Dairy")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(updated);

        productRequest.setPrice(new BigDecimal("2.99"));
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(2.99));
    }

    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
