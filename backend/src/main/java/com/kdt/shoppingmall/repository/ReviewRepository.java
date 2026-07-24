package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Review 엔티티의 CRUD와 도메인 전용 쿼리를 담은 레포지토리.
// JpaRepository를 상속하면 save·findById·delete 등 기본 메서드가 자동 제공된다.
public interface ReviewRepository extends JpaRepository<Review, Long> {

  // [P1-5] 최신순 + 페이징: List → Page 전환.
  // 리뷰가 많아져도 한 번에 전체를 로드하지 않고, 페이지 단위로 잘라 반환한다.
  // Spring Data JPA가 메서드 이름으로 "WHERE product_id = ? ORDER BY created_at DESC" 쿼리를 자동 생성한다.
  Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

  // 특정 회원이 특정 상품에 이미 리뷰를 작성했는지 확인한다.
  // createReview에서 중복 방지(409 Conflict)를 위해 INSERT 전에 미리 검사한다.
  boolean existsByMemberIdAndProductId(Long memberId, Long productId);

  // 특정 상품의 평균 별점을 DB에서 계산해 반환한다.
  // 리뷰가 없으면 null을 반환하므로, 호출 측에서 null → 0.0 변환이 필요하다.
  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
  Double findAverageRatingByProductId(@Param("productId") Long productId);

  // 특정 상품의 리뷰 수를 반환한다.
  // 베스트 상품 5개 컷오프 판정에 사용한다.
  long countByProductId(Long productId);

  // 회원 탈퇴 시 해당 회원의 리뷰를 일괄 삭제한다.
  // review_keyword가 먼저 삭제된 후 호출되어야 FK 제약 위반이 발생하지 않는다.
  void deleteAllByMemberId(Long memberId);
}
