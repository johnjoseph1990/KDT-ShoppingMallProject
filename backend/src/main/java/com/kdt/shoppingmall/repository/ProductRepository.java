package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.product.Product;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

  // 같은 이름의 상품이 이미 있는지 확인한다. 시드(DevDataInitializer)에서
  // 상품별 중복 여부를 이름으로 판단해, 여러 번 실행돼도 중복이 안 쌓이게(멱등) 하려고 사용한다.
  // 메서드 이름만으로 SELECT ... WHERE name = ? 쿼리를 Spring Data JPA가 자동 생성한다.
  boolean existsByName(String name);

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

  // 같은 태그(tagNames)를 하나라도 가진 다른 상품(excludeId 제외)을 찾는다.
  // DISTINCT: 한 상품이 tagNames 중 여러 개를 동시에 가지고 있어도 한 번만 나오게 함.
  @Query(
      """
            SELECT DISTINCT p FROM Product p
            JOIN p.tags t
            WHERE t.name IN :tagNames AND p.id <> :excludeId
            """)
  List<Product> findByTagNamesExcludingProduct(
      @Param("tagNames") List<String> tagNames,
      @Param("excludeId") Long excludeId,
      Pageable pageable);
}
