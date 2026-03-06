package com.groceryorder.repository;

import com.groceryorder.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByName(String name); //Checks if a product exists by a name

    List<Product> findByQuantityLessThan(Integer threshold); //returns stock quantity less than some quantity

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable); //returns Pagenated results for products

    List<Product> findByCategory(String category); //returns list of products that fall in a category

}
