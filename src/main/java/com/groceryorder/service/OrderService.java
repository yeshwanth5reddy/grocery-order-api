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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED),
            OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.RETURNED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    /**
     * Places a new order for a customer.
     * Validates stock availability, captures prices, deducts stock, and saves the order.
     */
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(request.getDeliveryAddress());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemRequest.getProductId()));

            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        product.getId(), itemRequest.getQuantity(), product.getQuantity());
            }

            BigDecimal priceAtPurchase = product.getPrice();
            BigDecimal subtotal = priceAtPurchase.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPriceAtPurchase(priceAtPurchase);
            orderItem.setSubtotal(subtotal);

            order.addOrderItem(orderItem);
            totalAmount = totalAmount.add(subtotal);

            product.setQuantity(product.getQuantity() - itemRequest.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);

        log.info("Order placed successfully for customer {}, total: {}", customer.getId(), totalAmount);
        return OrderResponse.from(saved);
    }

    /**
     * Retrieves an order by its ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return OrderResponse.from(order);
    }

    /**
     * Retrieves all orders for a customer with pagination.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(Long customerId, Pageable pageable) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(OrderResponse::from);
    }

    /**
     * Updates the status of an order with validation of legal transitions.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        log.info("Order {} status changed from {} to {}", orderId, currentStatus, newStatus);
        return OrderResponse.from(updated);
    }

    /**
     * Cancels an order. Only PENDING or CONFIRMED orders can be cancelled.
     * Restores stock for all items.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    String.format("Cannot cancel order with status %s. Only PENDING or CONFIRMED orders can be cancelled.",
                            order.getStatus()));
        }

        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelled = orderRepository.save(order);

        log.info("Order {} cancelled, stock restored", orderId);
        return OrderResponse.from(cancelled);
    }

    /**
     * Restores product stock for all items in an order.
     */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
            log.info("Restored {} units of product {} (id: {})", item.getQuantity(), product.getName(), product.getId());
        }
    }
}
