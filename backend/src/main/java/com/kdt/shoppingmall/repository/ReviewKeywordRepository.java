package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.review.ReviewKeyword;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// ReviewKeyword CRUD + 키워드 빈도 집계 쿼리를 담은 레포지토리.
public interface ReviewKeywordRepository extends JpaRepository<ReviewKeyword, Long> {

  // 특정 상품의 리뷰 전체에서 키워드별 등장 횟수를 집계해 내림차순으로 반환한다.
  // Object[0]=keyword(String), Object[1]=count(Long)
  // Pageable로 상위 N개만 잘라서 반환하므로 "top 10 키워드"처럼 쓸 수 있다.
  @Query(
      """
      SELECT rk.keyword, COUNT(rk)
      FROM ReviewKeyword rk
      WHERE rk.review.product.id = :productId
      GROUP BY rk.keyword
      ORDER BY COUNT(rk) DESC
      """)
  List<Object[]> findTopKeywordsByProductId(@Param("productId") Long productId, Pageable pageable);

  // 리뷰 삭제 시 해당 리뷰의 키워드를 먼저 삭제하는 데 사용한다.
  // Spring Data JPA가 메서드 이름으로 DELETE ... WHERE review_id = ? 를 자동 생성한다.
  void deleteByReviewId(Long reviewId);

  // 회원 탈퇴 시 해당 회원의 모든 리뷰에 연결된 키워드를 일괄 삭제한다.
  // 삭제 순서: review_keyword → review → member (FK 제약 준수).
  // Spring Data JPA가 review.member.id = ? 를 따라가서 DELETE를 생성한다.
  void deleteByReviewMemberId(Long memberId);
}
