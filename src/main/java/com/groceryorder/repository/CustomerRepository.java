package com.groceryorder.repository;

import com.groceryorder.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email); //find a customer by email

    boolean existsByEmail(String email); //checks if an email already exists
}
