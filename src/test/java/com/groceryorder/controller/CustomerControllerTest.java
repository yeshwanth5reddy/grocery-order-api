package com.groceryorder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groceryorder.dto.request.CustomerRequest;
import com.groceryorder.dto.response.CustomerResponse;
import com.groceryorder.exception.DuplicateResourceException;
import com.groceryorder.exception.ResourceNotFoundException;
import com.groceryorder.security.JwtService;
import com.groceryorder.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerRequest customerRequest;
    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Yeshwanth");
        customerRequest.setLastName("Reddy");
        customerRequest.setEmail("yeshwanth@gmail.com");
        customerRequest.setPhone("9876543210");
        customerRequest.setAddress("Amsterdam, Netherlands");

        customerResponse = CustomerResponse.builder()
                .id(1L)
                .firstName("Yeshwanth")
                .lastName("Reddy")
                .email("yeshwanth@gmail.com")
                .phone("9876543210")
                .address("Amsterdam, Netherlands")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createCustomer_ReturnsCreated() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Yeshwanth"))
                .andExpect(jsonPath("$.email").value("yeshwanth@gmail.com"));
    }

    @Test
    void createCustomer_DuplicateEmail_ReturnsConflict() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("Customer", "email", "yeshwanth@gmail.com"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void createCustomer_InvalidRequest_ReturnsBadRequest() throws Exception {
        CustomerRequest invalid = new CustomerRequest();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCustomers_ReturnsList() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Yeshwanth"));
    }

    @Test
    void getCustomerById_ReturnsCustomer() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(customerResponse);

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getCustomerById_NotFound_Returns404() throws Exception {
        when(customerService.getCustomerById(999L))
                .thenThrow(new ResourceNotFoundException("Customer", 999L));

        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCustomer_ReturnsUpdated() throws Exception {
        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Yeshwanth"));
    }

    @Test
    void deleteCustomer_ReturnsNoContent() throws Exception {
        doNothing().when(customerService).deleteCustomer(1L);

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());
    }
}
