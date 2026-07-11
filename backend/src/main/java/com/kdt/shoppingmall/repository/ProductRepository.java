package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
