package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.order.OrderStatus;
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

  // [P1-2 + P1-1] 베스트 상품 쿼리 — 5개 컷오프 + N+1 제거.
  //
  // 핵심 설계 결정 3가지:
  //   1. INNER JOIN(→ JOIN): 리뷰 없는 상품은 조인 결과 행이 없어 자동으로 제외된다.
  //   2. HAVING COUNT(r) >= 5: 리뷰 5개 미만 상품을 후보에서 배제 (규칙 2).
  //   3. Object[]{Product, Double}: 평균을 쿼리에서 함께 반환해 ProductService의
  //      상품별 평균 재조회(N+1)를 제거한다.
  //
  // tie-break: 평균이 같으면 리뷰 수 많은 쪽을 먼저 (P2-1).
  //
  // countQuery: HAVING을 포함한 GROUP BY 결과 개수는 서브쿼리로 별도 계산한다.
  //             Hibernate가 기본 count 쿼리를 잘못 생성하는 경우가 있어 명시.
  @Query(
      value =
          """
            SELECT r.product, AVG(r.rating)
            FROM Review r
            GROUP BY r.product
            HAVING COUNT(r) >= 5
            ORDER BY AVG(r.rating) DESC, COUNT(r) DESC
            """,
      countQuery =
          """
            SELECT COUNT(p) FROM Product p
            WHERE (SELECT COUNT(r) FROM Review r WHERE r.product = p) >= 5
            """)
  Page<Object[]> findBestProductsWithAvgRating(Pageable pageable);

  // [P1-3] 콜드스타트 2순위: 결제 완료(PAID) 이상 주문에서 판매량이 가장 많은 상품.
  // Object[0]=Product, Object[1]=Long(판매 수량 합계)
  // tie-break: 같은 판매량이면 조회수(viewCount) 높은 쪽을 먼저.
  @Query(
      value =
          """
            SELECT oi.product, COUNT(oi)
            FROM OrderItem oi
            WHERE oi.order.status IN :statuses
            GROUP BY oi.product
            ORDER BY COUNT(oi) DESC, oi.product.viewCount DESC
            """,
      countQuery =
          """
            SELECT COUNT(DISTINCT oi.product)
            FROM OrderItem oi
            WHERE oi.order.status IN :statuses
            """)
  Page<Object[]> findTopBySales(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

  // [추천 폴백] 태그가 없거나 같은 태그 상품이 없을 때 조회수 높은 상품을 폴백으로 반환한다.
  // excludeId: 현재 보고 있는 상품은 추천 목록에서 제외한다.
  // Spring Data JPA 메서드 이름으로 "WHERE id <> ? ORDER BY viewCount DESC" 쿼리가 생성된다.
  Page<Product> findByIdNotOrderByViewCountDesc(Long excludeId, Pageable pageable);

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
