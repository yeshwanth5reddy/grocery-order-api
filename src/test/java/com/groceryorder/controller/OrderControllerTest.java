package com.groceryorder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groceryorder.dto.request.CreateOrderRequest;
import com.groceryorder.dto.request.OrderItemRequest;
import com.groceryorder.dto.request.UpdateOrderStatusRequest;
import com.groceryorder.dto.response.OrderItemResponse;
import com.groceryorder.dto.response.OrderResponse;
import com.groceryorder.enums.OrderStatus;
import com.groceryorder.exception.InsufficientStockException;
import com.groceryorder.exception.ResourceNotFoundException;
import com.groceryorder.security.JwtService;
import com.groceryorder.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderResponse orderResponse;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .id(1L)
                .productId(1L)
                .productName("Organic Milk")
                .quantity(3)
                .priceAtPurchase(new BigDecimal("2.49"))
                .subtotal(new BigDecimal("7.47"))
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("Yeshwanth Reddy")
                .items(List.of(itemResponse))
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("7.47"))
                .deliveryAddress("Amsterdam")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(1L);
        createOrderRequest.setItems(List.of(itemRequest));
        createOrderRequest.setDeliveryAddress("Amsterdam");
    }

    @Test
    void placeOrder_ReturnsCreated() throws Exception {
        when(orderService.placeOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(7.47))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void placeOrder_InsufficientStock_ReturnsBadRequest() throws Exception {
        when(orderService.placeOrder(any(CreateOrderRequest.class)))
                .thenThrow(new InsufficientStockException(1L, 100, 5));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("stock")));
    }

    @Test
    void placeOrder_CustomerNotFound_Returns404() throws Exception {
        when(orderService.placeOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer", 999L));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeOrder_InvalidRequest_ReturnsBadRequest() throws Exception {
        CreateOrderRequest invalid = new CreateOrderRequest();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_ReturnsOrder() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Yeshwanth Reddy"));
    }

    @Test
    void getOrderById_NotFound_Returns404() throws Exception {
        when(orderService.getOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Order", 999L));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByCustomer_ReturnsPage() throws Exception {
        Page<OrderResponse> page = new PageImpl<>(
                List.of(orderResponse), PageRequest.of(0, 10), 1);
        when(orderService.getOrdersByCustomer(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders/customer/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateOrderStatus_ReturnsUpdated() throws Exception {
        OrderResponse confirmed = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("Yeshwanth Reddy")
                .items(List.of())
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("7.47"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class)))
                .thenReturn(confirmed);

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.CONFIRMED);

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatus_InvalidTransition_ReturnsBadRequest() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class)))
                .thenThrow(new IllegalStateException("Cannot transition from DELIVERED to PENDING"));

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest();
        statusRequest.setNewStatus(OrderStatus.PENDING);

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("transition")));
    }

    @Test
    void cancelOrder_ReturnsCancelled() throws Exception {
        OrderResponse cancelled = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("Yeshwanth Reddy")
                .items(List.of())
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("7.47"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderService.cancelOrder(1L)).thenReturn(cancelled);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_DeliveredOrder_ReturnsBadRequest() throws Exception {
        when(orderService.cancelOrder(1L))
                .thenThrow(new IllegalStateException("Cannot cancel order with status DELIVERED"));

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("DELIVERED")));
    }
}
