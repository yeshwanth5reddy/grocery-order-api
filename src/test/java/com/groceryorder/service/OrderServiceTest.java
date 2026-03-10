package com.groceryorder.service;

import com.groceryorder.dto.request.CreateOrderRequest;
import com.groceryorder.dto.request.OrderItemRequest;
import com.groceryorder.dto.request.UpdateOrderStatusRequest;
import com.groceryorder.dto.response.OrderResponse;
import com.groceryorder.enums.OrderStatus;
import com.groceryorder.exception.InsufficientStockException;
import com.groceryorder.exception.ResourceNotFoundException;
import com.groceryorder.model.entity.Customer;
import com.groceryorder.model.entity.Order;
import com.groceryorder.model.entity.OrderItem;
import com.groceryorder.model.entity.Product;
import com.groceryorder.repository.CustomerRepository;
import com.groceryorder.repository.OrderRepository;
import com.groceryorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("Yeshwanth");
        customer.setLastName("Reddy");
        customer.setEmail("yeshwanth@gmail.com");
        customer.setCreatedAt(LocalDateTime.now());

        product = new Product();
        product.setId(1L);
        product.setName("Amul Milk");
        product.setPrice(new BigDecimal("65.00"));
        product.setQuantity(500);
        product.setCategory("Dairy");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void placeOrder_Success_StockDeducted() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setDeliveryAddress("Amsterdam");
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            return order;
        });

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("195.00"), response.getTotalAmount());
        assertEquals(497, product.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void placeOrder_InsufficientStock_ThrowsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(999);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(request));

        assertEquals(500, product.getQuantity());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_CustomerNotFound_ThrowsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(999L);
        request.setItems(List.of(new OrderItemRequest()));

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(request));
    }

    @Test
    void placeOrder_ProductNotFound_ThrowsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(999L);
        itemRequest.setQuantity(1);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(request));
    }

    @Test
    void updateOrderStatus_ValidTransition_Success() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("195.00"));
        order.setItems(new ArrayList<>());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.CONFIRMED);

        OrderResponse response = orderService.updateOrderStatus(1L, statusRequest);

        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DELIVERED);
        order.setItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.PENDING);

        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(1L, statusRequest));
    }

    @Test
    void cancelOrder_Success_RestoresStock() {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(3);
        orderItem.setPriceAtPurchase(new BigDecimal("65.00"));
        orderItem.setSubtotal(new BigDecimal("195.00"));

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("195.00"));
        order.setItems(List.of(orderItem));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        product.setQuantity(497);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(500, product.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void cancelOrder_DeliveredOrder_ThrowsException() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DELIVERED);
        order.setItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.cancelOrder(1L));
    }
}
