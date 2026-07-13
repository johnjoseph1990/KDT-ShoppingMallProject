package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

  @Query(
      """
            SELECT p FROM Product p
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:tag IS NULL
                 OR EXISTS (SELECT t FROM ProductTag t WHERE t.product = p AND t.name = :tag))
            """)
  Page<Product> searchProducts(
      @Param("keyword") String keyword, @Param("tag") String tag, Pageable pageable);
}
