package com.groceryorder.service;

import com.groceryorder.dto.request.CustomerRequest;
import com.groceryorder.dto.response.CustomerResponse;
import com.groceryorder.exception.DuplicateResourceException;
import com.groceryorder.exception.ResourceNotFoundException;
import com.groceryorder.model.entity.Customer;
import com.groceryorder.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("Yeshwanth");
        customer.setLastName("Reddy");
        customer.setEmail("yeshwanth@gmail.com");
        customer.setPhone("9876543210");
        customer.setAddress("Amsterdam, Netherlands");
        customer.setCreatedAt(LocalDateTime.now());

        customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Yeshwanth");
        customerRequest.setLastName("Reddy");
        customerRequest.setEmail("yeshwanth@gmail.com");
        customerRequest.setPhone("9876543210");
        customerRequest.setAddress("Amsterdam, Netherlands");
    }

    @Test
    void createCustomer_Success() {
        when(customerRepository.existsByEmail("yeshwanth@gmail.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.createCustomer(customerRequest);

        assertNotNull(response);
        assertEquals("Yeshwanth", response.getFirstName());
        assertEquals("yeshwanth@gmail.com", response.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_DuplicateEmail_ThrowsException() {
        when(customerRepository.existsByEmail("yeshwanth@gmail.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> customerService.createCustomer(customerRequest));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getCustomerById_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getCustomerById_NotFound_ThrowsException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> customerService.getCustomerById(999L));
    }

    @Test
    void deleteCustomer_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1L);

        verify(customerRepository).delete(customer);
    }
}
