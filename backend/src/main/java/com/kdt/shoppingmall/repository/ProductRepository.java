package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

  // CAST(:keyword AS string): keyword/tag가 null일 때 PostgreSQL이 파라미터 타입을
  // bytea로 잘못 추론해서 LOWER(bytea) 같은 함수 호출이 실패하는 문제를 막기 위해,
  // JPQL 단계에서 명시적으로 문자열 타입임을 알려준다.
  @Query(
      """
            SELECT p FROM Product p
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            AND (:tag IS NULL
                 OR EXISTS (SELECT t FROM ProductTag t WHERE t.product = p AND t.name = CAST(:tag AS string)))
            """)
  Page<Product> searchProducts(
      @Param("keyword") String keyword, @Param("tag") String tag, Pageable pageable);

  @Query(
      value =
          """
                SELECT p FROM Product p
                LEFT JOIN Review r ON r.product = p
                GROUP BY p
                ORDER BY COALESCE(AVG(r.rating), 0) DESC
                """,
      countQuery = "SELECT COUNT(p) FROM Product p")
  Page<Product> findAllOrderByAverageRatingDesc(Pageable pageable);
}
