package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.product.Product;
import java.util.List;
import java.util.Optional;
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

  // 이름으로 상품 한 건을 찾아온다. 시드가 "이미 있는 상품"의 이미지를 다시 맞출 때 쓴다.
  // Optional<Product>: 없을 수도 있다는 걸 반환 타입으로 드러내 NullPointerException을 막는다.
  Optional<Product> findByName(String name);

  // tags 컬렉션을 JOIN FETCH로 함께 로드한다.
  // ApplicationRunner는 트랜잭션 밖에서 실행되므로 findByName() 이후 p.getTags()를 호출하면
  // LazyInitializationException이 발생한다. 이 메서드는 태그까지 한 번에 가져와 그 문제를 피한다.
  @Query("SELECT p FROM Product p LEFT JOIN FETCH p.tags WHERE p.name = :name")
  Optional<Product> findByNameWithTags(@Param("name") String name);

  // CAST(:keyword AS string): keyword/tag가 null일 때 PostgreSQL이 파라미터 타입을
  // bytea로 잘못 추론해서 LOWER(bytea) 같은 함수 호출이 실패하는 문제를 막기 위해,
  // JPQL 단계에서 명시적으로 문자열 타입임을 알려준다.
  //
  // [별점 필터] minRating은 이 쿼리에 아예 파라미터로 넘기지 않는다 (검색 기준 없음 = 전체 조회).
  // null Double을 넘기면 String과 달리 CAST(:minRating AS double)에서도 여전히
  // "cannot cast type bytea to double precision" 에러가 난다 — Postgres가 이 파라미터를
  // bytea로 추론하는 문제라 JPQL 쪽에서 캐스트 위치를 바꿔도 해결되지 않았다.
  // 그래서 minRating이 null일 때는 이 메서드(별점 조건 없음)를, 값이 있을 때는
  // searchProductsWithMinRating(아래, primitive double이라 null이 될 수 없음)을 쓰도록
  // ProductService에서 분기한다.
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

  // [별점 필터] minRating을 primitive double로 받는다 — Double(래퍼 타입)과 달리
  // null이 될 수 없으므로, 위 searchProducts()에서 겪은 "null 파라미터 타입 오추론" 문제 자체가
  // 생기지 않는다. 리뷰가 없는 상품은 AVG가 NULL이라 `NULL >= minRating`이 거짓이 되어 자동 제외된다.
  @Query(
      """
            SELECT p FROM Product p
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            AND (:tag IS NULL
                 OR EXISTS (SELECT t FROM ProductTag t WHERE t.product = p AND t.name = CAST(:tag AS string)))
            AND (SELECT AVG(r.rating) FROM Review r WHERE r.product = p) >= :minRating
            """)
  Page<Product> searchProductsWithMinRating(
      @Param("keyword") String keyword,
      @Param("tag") String tag,
      @Param("minRating") double minRating,
      Pageable pageable);

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
