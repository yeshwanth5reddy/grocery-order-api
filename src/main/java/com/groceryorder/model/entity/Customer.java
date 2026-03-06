package com.groceryorder.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer")

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @NotBlank(message = "first name is required")
    @Size(max = 50)
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "last name is required")
    @Column(nullable = false)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "email must be valid ")
    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String address;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;


}
