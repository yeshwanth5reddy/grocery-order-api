package com.groceryorder.service;

import com.groceryorder.dto.request.ProductRequest;
import com.groceryorder.dto.response.ProductResponse;
import com.groceryorder.exception.DuplicateResourceException;
import com.groceryorder.exception.ResourceNotFoundException;
import com.groceryorder.model.entity.Product;
import com.groceryorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Amul Milk");
        product.setDescription("1 litre full cream milk");
        product.setPrice(new BigDecimal("65.00"));
        product.setQuantity(500);
        product.setCategory("Dairy");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        productRequest = new ProductRequest();
        productRequest.setName("Amul Milk");
        productRequest.setDescription("1 litre full cream milk");
        productRequest.setPrice(new BigDecimal("65.00"));
        productRequest.setQuantity(500);
        productRequest.setCategory("Dairy");
    }

    @Test
    void createProduct_Success() {
        when(productRepository.existsByName("Amul Milk")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals("Amul Milk", response.getName());
        assertEquals(new BigDecimal("65.00"), response.getPrice());
        verify(productRepository).existsByName("Amul Milk");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_DuplicateName_ThrowsException() {
        when(productRepository.existsByName("Amul Milk")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> productService.createProduct(productRequest));

        verify(productRepository).existsByName("Amul Milk");
        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Amul Milk", response.getName());
    }

    @Test
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(999L));
    }

    @Test
    void getAllProducts_Success() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(1, responses.size());
        assertEquals("Amul Milk", responses.get(0).getName());
    }

    @Test
    void updateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Amul Toned Milk");
        updateRequest.setDescription("1 litre toned milk");
        updateRequest.setPrice(new BigDecimal("55.00"));
        updateRequest.setQuantity(300);
        updateRequest.setCategory("Dairy");

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertNotNull(response);
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_NotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(999L));

        verify(productRepository, never()).delete(any());
    }
}
