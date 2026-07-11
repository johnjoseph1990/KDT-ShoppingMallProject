package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.product.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
}
